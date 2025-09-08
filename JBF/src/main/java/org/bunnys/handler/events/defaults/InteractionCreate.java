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
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("unused")
public final class InteractionCreate extends ListenerAdapter implements Event {

    private static final String EVENT_NAME = "InteractionCreate";
    private static final Duration COMMAND_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(10);
    private static final int TRACE_ID_LENGTH = 8;

    private final BunnyNexus client;
    private final ExecutorService commandExecutor;
    private final CommandMetrics metrics;
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);

    public InteractionCreate(@NotNull BunnyNexus client) {
        this.client = Objects.requireNonNull(client, "BunnyNexus client cannot be null");
        this.commandExecutor = createCommandExecutor();
        this.metrics = new CommandMetrics();

        Logger.debug(
                () -> "[" + EVENT_NAME + "] Initialized with command timeout: " + COMMAND_TIMEOUT.toSeconds() + "s");
    }

    @Override
    public void register(@NotNull JDA jda) {
        Objects.requireNonNull(jda, "JDA instance cannot be null");
        jda.addEventListener(this);
        Logger.info("[" + EVENT_NAME + "] Event listener registered with JDA");
    }

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

    private record ErrorMessage(String title, String description) {
    }

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

    private void logError(String commandName, String userId, String traceId, Throwable exception, boolean isTimeout) {
        if (isTimeout) {
            Logger.warning("[" + EVENT_NAME + "] Command '" + commandName + "' timed out after "
                    + COMMAND_TIMEOUT.toSeconds() + "s for user " + userId + " (traceId=" + traceId + ")");
        } else {
            Logger.error("[" + EVENT_NAME + "] Command '" + commandName + "' failed for user " + userId + " (traceId="
                    + traceId + ")", exception);
        }
    }

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

    @NotNull
    private static Throwable unwrapCompletionException(@NotNull Throwable exception) {
        if (exception instanceof CompletionException || exception instanceof ExecutionException) {
            Throwable cause = exception.getCause();
            return cause != null ? cause : exception;
        }
        return exception;
    }

    @NotNull
    private static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, TRACE_ID_LENGTH);
    }
}