package org.bunnys.handler.events.core;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.CommandRegistry;
import org.bunnys.handler.commands.slash.ContextCommandConfig;
import org.bunnys.handler.commands.slash.SlashCommandConfig;
import org.bunnys.handler.spi.ContextCommand;
import org.bunnys.handler.spi.Event;
import org.bunnys.handler.spi.SlashCommand;
import org.bunnys.handler.utils.commands.handlers.CommandVerification;
import org.bunnys.handler.utils.commands.metrics.CommandMetrics;
import org.bunnys.handler.utils.handler.Emojis;
import org.bunnys.handler.utils.handler.colors.ColorCodes;
import org.bunnys.handler.utils.handler.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A core event listener for handling all types of JDA interactions
 *
 * <p>
 * This class serves as the primary dispatcher for slash commands, context
 * commands
 * (user and message), and command autocomplete interactions. It manages the
 * asynchronous execution of commands, handles command timeouts and errors,
 * and records metrics for command usage
 * </p>
 */
@SuppressWarnings("unused")
public final class InteractionCreate extends ListenerAdapter implements Event {

    /** The name of this event handler for logging purposes */
    private static final String EVENT_NAME = "InteractionCreate";
    /** The maximum time a command is allowed to run before being cancelled */
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(45);
    /** The duration to wait for the command executor to shut down gracefully */
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(10);
    /** The length of the unique trace ID used for logging */
    private static final int TRACE_ID_LENGTH = 8;

    /** The main {@link BunnyNexus} client instance */
    private final BunnyNexus client;
    /** The executor service for running commands on a separate thread pool */
    private final ExecutorService commandExecutor;
    /** The metrics tracker for recording command performance and usage */
    private final CommandMetrics metrics;
    /**
     * A flag to indicate if a shutdown has been initiated, preventing new commands
     * from running
     */
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    /**
     * Constructs the interaction handler
     *
     * <p>
     * Initializes the client, creates a dedicated thread pool for command
     * execution, and sets up the command metrics tracker
     * </p>
     *
     * @param client The {@link BunnyNexus} client instance, cannot be null
     * @throws NullPointerException if the client is null
     */
    public InteractionCreate(@NotNull BunnyNexus client) {
        this.client = Objects.requireNonNull(client, "BunnyNexus client cannot be null");
        this.commandExecutor = createCommandExecutor();
        this.metrics = new CommandMetrics();

        Logger.debug(
                () -> "[" + EVENT_NAME + "] Initialized with command timeout: " + COMMAND_TIMEOUT.toSeconds() + "s");
    }

    /**
     * Registers this listener with the JDA instance
     *
     * @param jda The {@link JDA} instance to register with, cannot be null
     * @throws NullPointerException if the JDA instance is null
     */
    @Override
    public void register(@NotNull JDA jda) {
        Objects.requireNonNull(jda, "JDA instance cannot be null");
        Logger.info("[" + EVENT_NAME + "] Event listener registered with JDA");
    }

    /**
     * Handles all incoming slash command interactions
     *
     * <p>
     * This method retrieves the command from the registry, performs validation,
     * and then delegates the execution to a separate thread. It also manages
     * error reporting and metrics for each command invocation
     * </p>
     *
     * @param event The {@link SlashCommandInteractionEvent}
     */
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (isShutdown.get()) {
            Logger.warning("[" + EVENT_NAME + "] Ignoring command '" + event.getName() + "' - system is shutting down");
            return;
        }

        final String commandName = event.getName();
        final String userId = event.getUser().getId();
        final String traceId = generateTraceId();

        try {
            Logger.debug(() -> "[" + EVENT_NAME + "] Processing slash command '" + commandName + "' for user " + userId
                    + " (traceId=" + traceId + ")");

            CommandRegistry.CommandEntry entry = client.commandRegistry().findSlash(commandName);

            if (entry == null || entry.slashCommand() == null) {
                Logger.warning("[" + EVENT_NAME + "] Command '" + commandName + "' not found in registry (traceId="
                        + traceId + ")");
                metrics.recordCommandFailure(commandName, userId,
                        CommandVerification.ValidationFailureType.valueOf("COMMAND_NOT_FOUND"));
                return;
            }

            final SlashCommand command = entry.slashCommand();
            final SlashCommandConfig config = command.initAndGetConfig();

            metrics.recordCommandAttempt(commandName, userId);

            CommandVerification.ValidationResult validation = CommandVerification.validateExecution(client, event,
                    config);

            if (validation.hasFailed()) {
                metrics.recordCommandFailure(commandName, userId, validation.getFailureType());
                CommandVerification.handleValidationFailure(event, validation);
                Logger.debug(() -> "[" + EVENT_NAME + "] Command '" + commandName + "' validation failed: "
                        + validation.getFailureType() + " (traceId=" + traceId + ")");
                return;
            }

            executeCommandAsync(event, command, config, commandName, userId, traceId);

        } catch (Exception e) {
            Logger.error("[" + EVENT_NAME + "] Unexpected error processing command '" + commandName + "' for user "
                    + userId + " (traceId=" + traceId + ")", e);
            metrics.recordCommandError(commandName, userId, e);
            handleSlashCommandError(event, commandName, userId, traceId, e);
        }
    }

    /**
     * Handles all incoming command autocomplete interactions
     *
     * <p>
     * This method finds the appropriate command and delegates the autocomplete
     * logic to it. Autocomplete requests are not run on the command executor
     * since they should be fast and non-blocking
     * </p>
     *
     * @param event The {@link CommandAutoCompleteInteractionEvent}
     */
    @Override
    public void onCommandAutoCompleteInteraction(@NotNull CommandAutoCompleteInteractionEvent event) {
        if (isShutdown.get()) {
            Logger.debug(() -> "[" + EVENT_NAME + "] Ignoring autocomplete for '" + event.getName()
                    + "' - system is shutting down");
            return;
        }

        final String commandName = event.getName();
        final String traceId = generateTraceId();

        try {
            Logger.debug(() -> "[" + EVENT_NAME + "] Processing autocomplete for '" + commandName + "' (traceId="
                    + traceId + ")");

            CommandRegistry.CommandEntry entry = client.commandRegistry().findSlash(commandName);

            if (entry == null || entry.slashCommand() == null) {
                Logger.warning("[" + EVENT_NAME + "] Autocomplete command '" + commandName
                        + "' not found in registry (traceId=" + traceId + ")");
                return;
            }

            final SlashCommand command = entry.slashCommand();
            command.onAutoComplete(client, event);

            Logger.debug(() -> "[" + EVENT_NAME + "] Autocomplete for '" + commandName
                    + "' completed successfully (traceId=" + traceId + ")");

        } catch (Exception error) {
            Logger.error("[" + EVENT_NAME + "] Autocomplete for '" + commandName + "' failed (traceId=" + traceId + ")",
                    error);
        }
    }

    /**
     * Handles user context menu interactions
     *
     * <p>
     * This method finds the user context command and executes it on a
     * separate thread, logging any failures
     * </p>
     *
     * @param event The {@link UserContextInteractionEvent}
     */
    @Override
    public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
        if (isShutdown.get())
            return;

        final String commandName = event.getName();
        final String userId = event.getUser().getId();
        final String traceId = generateTraceId();

        try {
            CommandRegistry.CommandEntry entry = client.commandRegistry().findContext(commandName);

            if (entry == null || entry.contextCommand() == null) {
                Logger.warning("[" + EVENT_NAME + "] User context command '" + commandName + "' not found (traceId="
                        + traceId + ")");
                return;
            }

            final ContextCommand command = entry.contextCommand();
            final ContextCommandConfig config = command.initAndGetConfig();

            CompletableFuture
                    .runAsync(() -> command.onUserCommand(client, event), commandExecutor)
                    .orTimeout(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            Logger.error("[" + EVENT_NAME + "] User context command '" + commandName
                                    + "' failed (traceId=" + traceId + ")", unwrapCompletionException(ex));
                        } else {
                            Logger.info("[" + EVENT_NAME + "] User context command '" + commandName
                                    + "' executed (traceId=" + traceId + ")");
                        }
                    });
        } catch (Exception e) {
            Logger.error("[" + EVENT_NAME + "] Unexpected error in user context command '" + commandName + "' (traceId="
                    + traceId + ")", e);
        }
    }

    /**
     * Handles message context menu interactions
     *
     * <p>
     * This method finds the message context command and executes it on a
     * separate thread, logging any failures
     * </p>
     *
     * @param event The {@link MessageContextInteractionEvent}
     */
    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        if (isShutdown.get())
            return;

        final String commandName = event.getName();
        final String userId = event.getUser().getId();
        final String traceId = generateTraceId();

        try {
            CommandRegistry.CommandEntry entry = client.commandRegistry().findContext(commandName);

            if (entry == null || entry.contextCommand() == null) {
                Logger.warning("[" + EVENT_NAME + "] Message context command '" + commandName + "' not found (traceId="
                        + traceId + ")");
                return;
            }

            final ContextCommand command = entry.contextCommand();
            final ContextCommandConfig config = command.initAndGetConfig();

            CompletableFuture
                    .runAsync(() -> command.onMessageCommand(client, event), commandExecutor)
                    .orTimeout(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            Logger.error("[" + EVENT_NAME + "] Message context command '" + commandName
                                    + "' failed (traceId=" + traceId + ")", unwrapCompletionException(ex));
                        } else {
                            Logger.info("[" + EVENT_NAME + "] Message context command '" + commandName
                                    + "' executed (traceId=" + traceId + ")");
                        }
                    });
        } catch (Exception e) {
            Logger.error("[" + EVENT_NAME + "] Unexpected error in message context command '" + commandName
                    + "' (traceId=" + traceId + ")", e);
        }
    }

    /**
     * Asynchronously executes a slash command
     *
     * <p>
     * This method wraps the command execution in a {@link CompletableFuture} with a
     * timeout,
     * ensuring that long-running commands do not block the thread pool. It also
     * handles
     * success and failure callbacks
     * </p>
     *
     * @param event       The {@link SlashCommandInteractionEvent}
     * @param command     The {@link SlashCommand} to execute
     * @param config      The command's configuration
     * @param commandName The name of the command
     * @param userId      The ID of the user who invoked the command
     * @param traceId     The unique trace ID for this interaction
     */
    private void executeCommandAsync(@NotNull SlashCommandInteractionEvent event,
            @NotNull SlashCommand command,
            @NotNull SlashCommandConfig config,
            @NotNull String commandName,
            @NotNull String userId,
            @NotNull String traceId) {

        CompletableFuture
                .runAsync(() -> runCommand(event, command, config, traceId), commandExecutor)
                .orTimeout(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                .whenComplete((result, exception) -> {
                    if (exception != null) {
                        final Throwable rootCause = unwrapCompletionException(exception);
                        metrics.recordCommandError(commandName, userId, rootCause);
                        handleSlashCommandError(event, commandName, userId, traceId, rootCause);
                    } else {
                        metrics.recordCommandSuccess(commandName, userId);
                        Logger.info("[" + EVENT_NAME + "] Command '" + commandName
                                + "' completed successfully (traceId=" + traceId + ")");
                    }
                });
    }

    /**
     * The core command execution logic
     *
     * <p>
     * This method is run on a worker thread. It measures the execution time
     * of the command and calls the command's {@code execute} method. It's
     * designed to handle exceptions by wrapping them in a {@link RuntimeException}
     * for the parent {@link CompletableFuture} to catch
     * </p>
     *
     * @param event   The {@link SlashCommandInteractionEvent}
     * @param command The {@link SlashCommand} to execute
     * @param config  The command's configuration
     * @param traceId The unique trace ID
     */
    private void runCommand(@NotNull SlashCommandInteractionEvent event,
            @NotNull SlashCommand command,
            @NotNull SlashCommandConfig config,
            @NotNull String traceId) {
        final long startTime = System.currentTimeMillis();

        try {
            Logger.debug(() -> "[" + EVENT_NAME + "] Executing '" + config.name() + "' (traceId=" + traceId + ")");

            command.execute(client, event);

            final long executionTime = System.currentTimeMillis() - startTime;
            metrics.recordExecutionTime(config.name(), executionTime);

            Logger.debug(() -> "[" + EVENT_NAME + "] Command '" + config.name() + "' executed in " + executionTime
                    + "ms (traceId=" + traceId + ")");

        } catch (Exception e) {
            final long executionTime = System.currentTimeMillis() - startTime;
            Logger.error("[" + EVENT_NAME + "] Command '" + config.name() + "' failed after " + executionTime
                    + "ms (traceId=" + traceId + ")", e);
            throw new RuntimeException("Execution failed for '/" + config.name() + "'", e);
        }
    }

    /**
     * Handles errors that occur during slash command execution
     *
     * <p>
     * This method logs the error, determines if it was a timeout, and then
     * sends an appropriate, user-friendly error message to the interaction channel
     * </p>
     *
     * @param event       The {@link SlashCommandInteractionEvent}
     * @param commandName The name of the failed command
     * @param userId      The ID of the user who invoked the command
     * @param traceId     The unique trace ID
     * @param exception   The exception that occurred
     */
    private void handleSlashCommandError(@NotNull SlashCommandInteractionEvent event,
            @NotNull String commandName,
            @NotNull String userId,
            @NotNull String traceId,
            @NotNull Throwable exception) {

        final boolean isTimeout = exception instanceof TimeoutException;
        final ErrorMessage errorMsg = createErrorMessage(commandName, traceId, isTimeout);

        logError(commandName, userId, traceId, exception, isTimeout);
        sendUserErrorMessage(event, errorMsg.title(), errorMsg.description());
    }

    /**
     * A simple record for holding error message data
     */
    private record ErrorMessage(String title, String description) {
    }

    /**
     * Creates a user-friendly error message based on the exception type
     *
     * @param commandName The name of the failed command
     * @param traceId     The unique trace ID
     * @param isTimeout   A flag indicating if the error was a timeout
     * @return An {@link ErrorMessage} record
     */
    private ErrorMessage createErrorMessage(String commandName, String traceId, boolean isTimeout) {
        if (isTimeout) {
            return new ErrorMessage(
                    Emojis.DEFAULT_ERROR + " Command Timed Out",
                    String.format("Your command `/%s` took too long and was cancelled.%n" +
                            "Please try again in a moment. (`%s`)", commandName, traceId));
        } else {
            return new ErrorMessage(
                    Emojis.DEFAULT_ERROR + " Something went wrong",
                    String.format("We ran into an issue while running `/%s`. This isn't your fault.%n" +
                            "Please try again. If this keeps happening, share this code with support: `%s`",
                            commandName, traceId));
        }
    }

    /**
     * Logs the command error to the console
     *
     * @param commandName The name of the failed command
     * @param userId      The ID of the user
     * @param traceId     The unique trace ID
     * @param exception   The exception that occurred
     * @param isTimeout   A flag indicating a timeout error
     */
    private void logError(String commandName, String userId, String traceId, Throwable exception, boolean isTimeout) {
        if (isTimeout) {
            Logger.warning("[" + EVENT_NAME + "] Command '" + commandName + "' timed out after "
                    + COMMAND_TIMEOUT.toSeconds() + "s for user " + userId + " (traceId=" + traceId + ")");
        } else {
            Logger.error("[" + EVENT_NAME + "] Command '" + commandName + "' failed for user " + userId + " (traceId="
                    + traceId + ")", exception);
        }
    }

    /**
     * Sends an ephemeral error message to the user via the interaction hook
     *
     * @param event       The {@link SlashCommandInteractionEvent}
     * @param title       The title of the error embed
     * @param description The description of the error embed
     */
    private void sendUserErrorMessage(@NotNull SlashCommandInteractionEvent event,
            @NotNull String title,
            @NotNull String description) {
        try {
            EmbedBuilder embed = new EmbedBuilder()
                    .setColor(ColorCodes.ERROR_RED)
                    .setTitle(title)
                    .setDescription(description);

            if (event.isAcknowledged()) {
                event.getHook().sendMessageEmbeds(embed.build()).setEphemeral(true).queue();
            } else {
                event.replyEmbeds(embed.build()).setEphemeral(true).queue();
            }
        } catch (Exception e) {
            Logger.error("[" + EVENT_NAME + "] Failed to send error message to user", e);
        }
    }

    /**
     * Initiates a graceful shutdown of the command executor service
     *
     * <p>
     * This method prevents new commands from being accepted, waits for a
     * specified period for running tasks to complete, and then forces
     * termination if necessary
     * </p>
     */
    public void shutdown() {
        if (isShutdown.getAndSet(true)) {
            Logger.debug(() -> "[" + EVENT_NAME + "] Shutdown already initiated");
            return;
        }

        Logger.info("[" + EVENT_NAME + "] Initiating graceful shutdown...");

        commandExecutor.shutdown();
        try {
            if (!commandExecutor.awaitTermination(SHUTDOWN_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                Logger.warning("[" + EVENT_NAME + "] Graceful shutdown timed out, forcing immediate shutdown");
                commandExecutor.shutdownNow();
            } else {
                Logger.info("[" + EVENT_NAME + "] Graceful shutdown completed");
            }
        } catch (InterruptedException e) {
            Logger.warning("[" + EVENT_NAME + "] Shutdown interrupted, forcing immediate shutdown");
            commandExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates a dedicated thread pool for command execution
     *
     * <p>
     * This is a custom thread pool configured with a core size, max size, and
     * a bounded queue to prevent resource exhaustion. It uses a
     * {@link java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy} to handle
     * saturated queues
     * </p>
     *
     * @return A configured {@link ExecutorService}
     */
    @NotNull
    private ExecutorService createCommandExecutor() {
        int corePoolSize = 2;
        int maxPoolSize = 20;
        long keepAliveTime = 60L;

        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "command-executor-thread");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(
                    (th, ex) -> Logger.error("Uncaught exception in command executor thread: " + th.getName(), ex));
            return t;
        };

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                factory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * Unwraps a {@link CompletionException} or {@link ExecutionException} to get
     * the true cause
     *
     * <p>
     * This is a utility method to simplify error handling for
     * {@link CompletableFuture}
     * </p>
     *
     * @param exception The exception to unwrap
     * @return The root cause of the exception
     */
    @NotNull
    private static Throwable unwrapCompletionException(@NotNull Throwable exception) {
        if (exception instanceof CompletionException || exception instanceof ExecutionException) {
            Throwable cause = exception.getCause();
            return cause != null ? cause : exception;
        }
        return exception;
    }

    /**
     * Generates a unique, short trace ID for logging purposes
     *
     * @return A string representing a unique trace ID
     */
    @NotNull
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, TRACE_ID_LENGTH);
    }
}