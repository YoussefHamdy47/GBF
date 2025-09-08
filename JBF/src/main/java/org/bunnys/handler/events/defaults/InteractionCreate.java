package org.bunnys.handler.events.defaults;

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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Event listener for handling slash command interactions
 *
 * <p>
 * This class serves as the primary gateway for processing all incoming
 * {@link SlashCommandInteractionEvent}s and
 * {@link CommandAutoCompleteInteractionEvent}s
 * It manages the complete lifecycle of command execution, from initial
 * validation
 * to asynchronous execution, timeout handling, and comprehensive error
 * reporting
 *
 * <p>
 * The key features of this handler include:
 * <ul>
 * <li><b>Asynchronous Execution:</b> Commands are executed on a dedicated
 * thread pool to avoid blocking the main JDA event loop, ensuring the bot
 * remains responsive under load.</li>
 * <li><b>Validation and Verification:</b> Pre-execution checks are performed
 * using {@link CommandVerification} to handle common failures, such as
 * cooldowns or permission issues, before a command is executed.</li>
 * <li><b>Timeout Handling:</b> A configurable timeout is applied to each
 * command
 * execution via {@link CompletableFuture#orTimeout(long, TimeUnit)} to prevent
 * long-running or stalled commands from consuming resources indefinitely.</li>
 * <li><b>Metrics Integration:</b> Detailed metrics are captured for command
 * attempts, successes, failures, and execution times using the
 * {@link CommandMetrics} class.</li>
 * <li><b>Robust Error Handling:</b> A centralized error handler provides
 * user-friendly feedback for both execution errors and timeouts, logging
 * detailed information for debugging with a unique trace ID.</li>
 * <li><b>Graceful Shutdown:</b> Proper resource cleanup with configurable
 * shutdown timeouts to ensure clean application termination.</li>
 * </ul>
 *
 * <p>
 * This design promotes a scalable, reliable, and observable command handling
 * system, which is crucial for production-level Discord bot applications.
 *
 * <p>
 * <b>Thread Safety:</b> This class is thread-safe and can handle concurrent
 * command executions safely.
 *
 * <p>
 * <b>Resource Management:</b> The internal thread pool is automatically managed
 * and should be properly shutdown using {@link #shutdown()} during application
 * termination to prevent resource leaks.
 *
 * @author Bunny
 * @see SlashCommandInteractionEvent
 * @see CommandAutoCompleteInteractionEvent
 * @see SlashCommand
 * @see CommandMetrics
 * @see CommandVerification
 */
@SuppressWarnings("unused")
public final class InteractionCreate extends ListenerAdapter implements Event {

    /**
     * The name of the event handled by this listener, used primarily for logging
     * and metrics correlation.
     */
    private static final String EVENT_NAME = "InteractionCreate";

    /**
     * The maximum time a command is allowed to execute before being canceled.
     * This prevents resource exhaustion from long-running commands.
     */
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(30);

    /**
     * The maximum time to wait for graceful executor shutdown during application
     * termination.
     */
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(10);

    /**
     * A counter for naming threads in the command execution thread pool.
     * This aids in debugging by providing unique, identifiable thread names.
     */
    private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger(1);

    /**
     * The length of the generated trace ID for log correlation.
     * 8 characters provides sufficient uniqueness for correlation while remaining
     * compact.
     */
    private static final int TRACE_ID_LENGTH = 8;

    /**
     * The main client instance, providing access to shared services like the
     * command registry and configuration.
     */
    private final BunnyNexus client;

    /**
     * An {@link ExecutorService} for running commands asynchronously. A cached
     * thread pool is used to efficiently reuse threads for command execution
     * while scaling based on demand.
     */
    private final ExecutorService commandExecutor;

    /**
     * An instance of {@link CommandMetrics} for recording and tracking
     * command-related performance and usage data.
     */
    private final CommandMetrics metrics;

    /**
     * Flag to track whether this listener has been shut down to prevent
     * processing events during the shutdown.
     */
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    /**
     * Constructs a new {@code InteractionCreate} event listener.
     *
     * <p>
     * Initializes the command executor thread pool and metrics collection system.
     * The thread pool uses a cached thread pool strategy for optimal performance
     * under varying load conditions.
     *
     * @param client The {@link BunnyNexus} client instance providing access to
     *               the command registry and other shared services.
     *               Must not be
     *               null.
     * @throws NullPointerException if the client parameter is null
     */
    public InteractionCreate(@NotNull BunnyNexus client) {
        this.client = Objects.requireNonNull(client, "BunnyNexus client cannot be null");
        this.commandExecutor = createCommandExecutor();
        this.metrics = new CommandMetrics();

        Logger.debug(
                () -> "[" + EVENT_NAME + "] Initialized with command timeout: " + COMMAND_TIMEOUT.toSeconds() + "s");
    }

    /**
     * Registers this listener with the provided JDA instance, enabling it to
     * receive Discord interaction events.
     *
     * <p>
     * This method should be called during bot initialization to activate
     * command processing capabilities.
     *
     * @param jda The JDA instance to which this listener will be added. Must not be
     *            null.
     * @throws NullPointerException if the jda parameter is null
     * @see JDA#addEventListener(Object...)
     */
    @Override
    public void register(@NotNull JDA jda) {
        Objects.requireNonNull(jda, "JDA instance cannot be null");
        jda.addEventListener(this);
        Logger.info("[" + EVENT_NAME + "] Event listener registered with JDA");
    }

    /**
     * Handles an incoming slash command interaction event.
     *
     * <p>
     * This method is the entry point for all slash commands. It performs the
     * following steps:
     * <ol>
     * <li>Validates that the system is not shutdown</li>
     * <li>Looks up the command in the client's registry</li>
     * <li>Records a command attempt metric</li>
     * <li>Validates the command's execution context (permissions, cooldowns,
     * etc.)</li>
     * <li>If validation fails, records a failure metric and handles the
     * response</li>
     * <li>If validation passes, submits the command for asynchronous execution</li>
     * <li>Applies a timeout to the execution to prevent resource exhaustion</li>
     * <li>Handles the outcome (success, timeout, or error) with appropriate metrics
     * and user feedback</li>
     * </ol>
     *
     * <p>
     * <b>Error Handling:</b> All exceptions are caught and handled gracefully,
     * ensuring users receive appropriate feedback while detailed error information
     * is logged for debugging purposes.
     *
     * @param event The {@link SlashCommandInteractionEvent} representing the user's
     *              interaction. Must not be null.
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

            // Lookup command in registry
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

            // Record attempt metrics
            metrics.recordCommandAttempt(commandName, userId);

            // Validate execution context
            CommandVerification.ValidationResult validation = CommandVerification.validateExecution(client, event,
                    config);

            if (validation.hasFailed()) {
                metrics.recordCommandFailure(commandName, userId, validation.getFailureType());
                CommandVerification.handleValidationFailure(event, validation);
                Logger.debug(() -> "[" + EVENT_NAME + "] Command '" + commandName + "' validation failed: "
                        + validation.getFailureType() + " (traceId=" + traceId + ")");
                return;
            }

            // Execute command asynchronously
            executeCommandAsync(event, command, config, commandName, userId, traceId);

        } catch (Exception e) {
            Logger.error("[" + EVENT_NAME + "] Unexpected error processing command '" + commandName + "' for user "
                    + userId + " (traceId=" + traceId + ")", e);
            metrics.recordCommandError(commandName, userId, e);
            handleError(event, commandName, userId, traceId, e);
        }
    }

    /**
     * Handles an incoming command autocomplete interaction event.
     *
     * <p>
     * This method processes autocomplete requests for slash command options,
     * allowing for dynamic suggestion generation based on user input.
     * It includes
     * comprehensive error handling to ensure that autocomplete failures don't
     * disrupt the user experience.
     *
     * <p>
     * <b>Performance Note:</b> Autocomplete handlers should be lightweight and
     * fast-executing to provide responsive user experience.
     *
     * @param event The {@link CommandAutoCompleteInteractionEvent} representing
     *              the autocomplete request.
     *              Must not be null.
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
            // Note: We don't send user-facing errors for autocomplete failures
            // as they would disrupt the user experience
        }
    }

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
        if (isShutdown.get()) return;

        final String commandName = event.getName();
        final String userId = event.getUser().getId();
        final String traceId = generateTraceId();

        try {
            CommandRegistry.CommandEntry entry = client.commandRegistry().findContext(commandName);

            if (entry == null || entry.contextCommand() == null) {
                Logger.warning("[" + EVENT_NAME + "] User context command '" + commandName + "' not found (traceId=" + traceId + ")");
                return;
            }

            final ContextCommand command = entry.contextCommand();
            final ContextCommandConfig config = command.initAndGetConfig();

            CompletableFuture
                    .runAsync(() -> command.onUserCommand(client, event), commandExecutor)
                    .orTimeout(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            Logger.error("[" + EVENT_NAME + "] User context command '" + commandName + "' failed (traceId=" + traceId + ")", unwrapCompletionException(ex));
                        } else {
                            Logger.info("[" + EVENT_NAME + "] User context command '" + commandName + "' executed (traceId=" + traceId + ")");
                        }
                    });
        } catch (Exception e) {
            Logger.error("[" + EVENT_NAME + "] Unexpected error in user context command '" + commandName + "' (traceId=" + traceId + ")", e);
        }
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent event) {
        if (isShutdown.get()) return;

        final String commandName = event.getName();
        final String userId = event.getUser().getId();
        final String traceId = generateTraceId();

        try {
            CommandRegistry.CommandEntry entry = client.commandRegistry().findContext(commandName);

            if (entry == null || entry.contextCommand() == null) {
                Logger.warning("[" + EVENT_NAME + "] Message context command '" + commandName + "' not found (traceId=" + traceId + ")");
                return;
            }

            final ContextCommand command = entry.contextCommand();
            final ContextCommandConfig config = command.initAndGetConfig();

            CompletableFuture
                    .runAsync(() -> command.onMessageCommand(client, event), commandExecutor)
                    .orTimeout(COMMAND_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                    .whenComplete((res, ex) -> {
                        if (ex != null) {
                            Logger.error("[" + EVENT_NAME + "] Message context command '" + commandName + "' failed (traceId=" + traceId + ")", unwrapCompletionException(ex));
                        } else {
                            Logger.info("[" + EVENT_NAME + "] Message context command '" + commandName + "' executed (traceId=" + traceId + ")");
                        }
                    });
        } catch (Exception e) {
            Logger.error("[" + EVENT_NAME + "] Unexpected error in message context command '" + commandName + "' (traceId=" + traceId + ")", e);
        }
    }


    /**
     * Executes a slash command asynchronously with timeout and completion handling.
     *
     * <p>
     * This method submits the command for execution on the dedicated thread pool,
     * applies a timeout to prevent resource exhaustion, and handles the completion
     * outcome with appropriate metrics recording and user feedback.
     *
     * @param event       The original slash command event
     * @param command     The command to execute
     * @param config      The command configuration
     * @param commandName The name of the command for logging/metrics
     * @param userId      The ID of the user who invoked the command
     * @param traceId     The unique trace ID for this execution
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
                        handleError(event, commandName, userId, traceId, rootCause);
                    } else {
                        metrics.recordCommandSuccess(commandName, userId);
                        Logger.info("[" + EVENT_NAME + "] Slash command '" + commandName
                                + "' completed successfully (traceId=" + traceId + ")");
                    }
                });
    }

    /**
     * Executes the provided slash command and records its execution time.
     *
     * <p>
     * This method is designed to be run within the {@link #commandExecutor} thread
     * pool.
     * It measures the command's execution time and records it using
     * {@link CommandMetrics}.
     * Any exceptions thrown during execution are wrapped in a
     * {@link RuntimeException}
     * to be caught by the calling {@link CompletableFuture}'s error handler.
     *
     * <p>
     * <b>Thread Safety:</b> This method is called from the command executor thread
     * pool
     * and must be thread-safe.
     *
     * @param event   The {@link SlashCommandInteractionEvent} that triggered the
     *                command
     * @param command The {@link SlashCommand} to execute
     * @param config  The configuration for the command
     * @param traceId A unique trace ID for log correlation
     * @throws RuntimeException if the command's execution fails
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
     * Handles and reports an error that occurred during command execution.
     *
     * <p>
     * This method distinguishes between different types of exceptions (timeouts vs
     * general errors) and provides appropriate user feedback while logging detailed
     * error information for debugging purposes.
     * The error messages are designed to
     * be user-friendly while providing developers with the necessary trace
     * information.
     *
     * <p>
     * <b>Error Types Handled:</b>
     * <ul>
     * <li>{@link TimeoutException} - Command execution exceeded the timeout
     * limit</li>
     * <li>General {@link Exception} - Any other execution failure</li>
     * </ul>
     *
     * @param event       The original slash command event for sending user feedback
     * @param commandName The name of the command that failed
     * @param userId      The ID of the user who ran the command
     * @param traceId     The unique trace ID for the execution
     * @param exception   The root cause of the failure
     */
    private void handleError(@NotNull SlashCommandInteractionEvent event,
            @NotNull String commandName,
            @NotNull String userId,
            @NotNull String traceId,
            @NotNull Throwable exception) {

        final boolean isTimeout = exception instanceof TimeoutException;

        final String title = isTimeout
                ? Emojis.DEFAULT_ERROR + " Command Timed Out"
                : Emojis.DEFAULT_ERROR + " Something went wrong";

        final String description = isTimeout
                ? String.format("Your command `/%s` took too long and was cancelled.%n" +
                        "Please try again in a moment. (`%s`)", commandName, traceId)
                : String.format("We ran into an issue while running `/%s`. This isn't your fault.%n" +
                        "Please try again. If this keeps happening, share this code with support: `%s`",
                        commandName, traceId);

        // Log the error with appropriate level based on type
        if (isTimeout) {
            Logger.warning("[" + EVENT_NAME + "] Command '" + commandName + "' timed out after "
                    + COMMAND_TIMEOUT.toSeconds() + "s for user " + userId + " (traceId=" + traceId + ")");
        } else {
            Logger.error("[" + EVENT_NAME + "] Command '" + commandName + "' failed for user " + userId + " (traceId="
                    + traceId + ")", exception);
        }

        sendUserErrorMessage(event, title, description);
    }

    /**
     * Sends an ephemeral error message to the user.
     *
     * <p>
     * This method intelligently handles whether the interaction has been
     * acknowledged
     * and responds accordingly, ensuring the user always receives error feedback.
     * The message is formatted as an embed with distinctive styling to clearly
     * indicate an error state.
     *
     * <p>
     * <b>Interaction States Handled:</b>
     * <ul>
     * <li>Acknowledged interactions - Uses followup messages via webhook</li>
     * <li>Unacknowledged interactions - Uses direct reply</li>
     * </ul>
     *
     * @param event       The {@link SlashCommandInteractionEvent} to reply to
     * @param title       The title of the error embed
     * @param description The detailed error message body
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
     * Initiates graceful shutdown of the command execution thread pool.
     *
     * <p>
     * This method should be called during application termination to ensure
     * proper resource cleanup.
     * It first attempts a graceful shutdown, allowing
     * currently executing commands to complete.
     * If the graceful shutdown doesn't
     * complete within the specified timeout, it forces immediate termination.
     *
     * <p>
     * <b>Shutdown Process:</b>
     * <ol>
     * <li>Sets the shutdown flag to prevent new command processing</li>
     * <li>Initiates graceful executor shutdown</li>
     * <li>Waits for completion up to the shutdown timeout</li>
     * <li>Forces immediate shutdown if graceful shutdown fails</li>
     * <li>Handles interruption during shutdown gracefully</li>
     * </ol>
     *
     * <p>
     * <b>Thread Safety:</b> This method is thread-safe and can be called multiple
     * times.
     *
     * @see ExecutorService#shutdown()
     * @see ExecutorService#awaitTermination(long, TimeUnit)
     * @see ExecutorService#shutdownNow()
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
     * Creates and configures the ExecutorService for command execution.
     *
     * <p>
     * This factory method creates a cached thread pool with custom thread naming
     * and exception handling.
     * The resulting executor is optimized for the
     * command execution workload pattern.
     *
     * @return A configured {@link ExecutorService} for command execution
     */
    @NotNull
    private ExecutorService createCommandExecutor() {
        int corePoolSize = 2;
        int maxPoolSize = 20; // cap threads to prevent unbounded growth
        long keepAliveTime = 60L;

        ThreadFactory factory = r -> {
            Thread t = new Thread(r, "command-executor-thread");
            t.setDaemon(true);
            t.setUncaughtExceptionHandler((th, ex) ->
                    Logger.error("Uncaught exception in command executor thread: " + th.getName(), ex)
            );
            return t;
        };

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100), // backpressure instead of unbounded
                factory,
                new ThreadPoolExecutor.CallerRunsPolicy() // fallback if the queue is full
        );
    }

    /**
     * Creates a {@link ThreadFactory} for the command execution thread pool.
     *
     * <p>
     * This factory ensures that all threads created for command execution are
     * properly configured for optimal operation:
     * <ul>
     * <li><b>Naming:</b> Threads are named with a sequential pattern for easy
     * identification</li>
     * <li><b>Daemon Status:</b> Threads are marked as daemon threads to allow JVM
     * shutdown</li>
     * <li><b>Exception Handling:</b> Global uncaught exception handler for
     * logging</li>
     * </ul>
     *
     * @return A new {@link ThreadFactory} instance configured for command execution
     */
    @NotNull
    private static ThreadFactory createThreadFactory() {
        return runnable -> {
            Thread thread = new Thread(runnable, "SlashCommand-" + THREAD_SEQUENCE.getAndIncrement());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler(
                    (t, ex) -> Logger.error("[" + EVENT_NAME + "] Uncaught exception in thread " + t.getName(), ex));
            return thread;
        };
    }

    /**
     * Unwraps the root cause of an exception from completion wrappers.
     *
     * <p>
     * This utility method extracts the actual cause from
     * {@link CompletionException}
     * and {@link ExecutionException} wrappers that are commonly thrown by
     * {@link CompletableFuture} operations.
     * This is essential for accurate error
     * classification and handling.
     *
     * <p>
     * <b>Wrapper Types Handled:</b>
     * <ul>
     * <li>{@link CompletionException} - Thrown by CompletableFuture operations</li>
     * <li>{@link ExecutionException} - Thrown by Future.get() operations</li>
     * </ul>
     *
     * @param exception The exception to unwrap. Must not be null.
     * @return The root cause of the exception, or the original exception if it's
     *         not a wrapper
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
     * Generates a short, unique trace ID for log correlation.
     *
     * <p>
     * This method creates a random UUID and returns its first
     * {@value #TRACE_ID_LENGTH}
     * characters, which provides sufficient uniqueness for correlating logs within
     * a single command execution while remaining compact and readable.
     *
     * <p>
     * <b>Uniqueness:</b> While theoretically possible, collisions are extremely
     * rare in practice for the intended use case of correlating logs within
     * individual command executions.
     *
     * @return A short trace ID string of length {@value #TRACE_ID_LENGTH}
     */
    @NotNull
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, TRACE_ID_LENGTH);
    }
}