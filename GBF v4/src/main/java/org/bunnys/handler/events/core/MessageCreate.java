package org.bunnys.handler.events.core;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.message.MessageCommandConfig;
import org.bunnys.handler.spi.Event;
import org.bunnys.handler.utils.commands.handlers.CommandVerification;
import org.bunnys.handler.utils.commands.handlers.MessageCommandParser;
import org.bunnys.handler.utils.commands.metrics.CommandMetrics;
import org.bunnys.handler.utils.handler.Emojis;
import org.bunnys.handler.utils.handler.colors.ColorCodes;
import org.bunnys.handler.utils.handler.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * An orchestrator for handling and executing message commands
 *
 * <p>
 * This class acts as a central handler for all incoming Discord messages. It
 * delegates the parsing, validation, and execution of message-based commands to
 * specialized utility classes, while also managing command metrics, timeouts,
 * and error handling in a robust, asynchronous manner
 * </p>
 */
@SuppressWarnings("unused")
public class MessageCreate extends ListenerAdapter implements Event {
    /** The name of this event handler for logging purposes */
    private static final String EVENT_NAME = "MessageCreate";
    /** The maximum time a command is allowed to run before being cancelled */
    private static final int COMMAND_TIMEOUT_SECONDS = 30;
    /** The duration in seconds an ephemeral error message remains visible */
    private static final int ERROR_DELETE_SECONDS = 8;

    /** The main {@link BunnyNexus} client instance */
    private final BunnyNexus client;
    /** The executor service for running commands asynchronously */
    private final ExecutorService commandExecutor;
    /** The metrics tracker for recording command performance and usage */
    private final CommandMetrics metrics;

    /**
     * Constructs the message command handler
     *
     * <p>
     * Initializes a cached thread pool to handle command execution asynchronously,
     * ensuring the main JDA event thread remains unblocked
     * </p>
     *
     * @param client The {@link BunnyNexus} client instance
     */
    public MessageCreate(BunnyNexus client) {
        this.client = client;
        this.commandExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "MessageCommand-" + tId());
            t.setDaemon(true);
            t.setUncaughtExceptionHandler(
                    (thr, ex) -> Logger.error("[" + EVENT_NAME + "] Uncaught in " + thr.getName(), ex));
            return t;
        });
        this.metrics = new CommandMetrics();
    }

    /**
     * Registers this listener with the JDA instance
     *
     * @param jda The {@link JDA} instance to register with
     */
    @Override
    public void register(JDA jda) {
    }

    /**
     * The main event handler for incoming messages
     *
     * <p>
     * This method is invoked by JDA for every message received. It first
     * attempts to parse the message as a command. If a valid command is found,
     * it performs validation and then executes the command on a separate thread
     * using a {@link CompletableFuture} with a timeout
     * </p>
     *
     * @param event The {@link MessageReceivedEvent} containing the message data
     */
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        MessageCommandParser.ParseResult parseResult = MessageCommandParser.parse(client, event);
        if (parseResult == null)
            return;

        String cmd = parseResult.commandName();
        String userId = event.getAuthor().getId();

        MessageCommandConfig config = parseResult.commandConfig();

        if (config == null) {
            Logger.debug(() -> "[" + EVENT_NAME + "] Unknown command '" + cmd + "'");
            return;
        }

        metrics.recordCommandAttempt(cmd, userId);

        CommandVerification.ValidationResult validation = CommandVerification.validateExecution(client, event, config);
        if (validation.hasFailed()) {
            metrics.recordCommandFailure(cmd, userId, validation.getFailureType());
            CommandVerification.handleValidationFailure(event, validation);
            return;
        }

        // Execute async
        CompletableFuture
                .runAsync(() -> runCommand(event, parseResult, config), commandExecutor)
                .orTimeout(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .whenComplete((ok, ex) -> {
                    if (ex != null) {
                        metrics.recordCommandError(cmd, userId, ex);
                        handleError(event, cmd, userId, ex);
                    } else {
                        metrics.recordCommandSuccess(cmd, userId);
                    }
                });
    }

    /**
     * Executes the command logic
     *
     * <p>
     * This method is run asynchronously. It measures the execution time and
     * records it via the metrics system. Any exceptions during command execution
     * are wrapped and re-thrown for the error handler to catch
     * </p>
     *
     * @param event       The {@link MessageReceivedEvent}
     * @param parseResult The result of the command parsing
     * @param config      The command's configuration
     */
    private void runCommand(MessageReceivedEvent event,
            MessageCommandParser.ParseResult parseResult,
            MessageCommandConfig config) {
        long start = System.currentTimeMillis();
        try {
            parseResult.command().execute(client, event, parseResult.args());
            metrics.recordExecutionTime(parseResult.commandName(), System.currentTimeMillis() - start);
        } catch (Exception e) {
            throw new RuntimeException("Execution failed", e);
        }
    }

    /**
     * Handles command execution errors and sends a user-friendly message
     *
     * <p>
     * This method logs the exception, generates a unique trace ID for
     * correlation, and sends an embed to the user informing them of the failure,
     * differentiating between a timeout and a generic error
     * </p>
     *
     * @param event  The {@link MessageReceivedEvent}
     * @param cmd    The name of the command that failed
     * @param userId The ID of the user who ran the command
     * @param ex     The exception that occurred
     */
    private void handleError(MessageReceivedEvent event, String cmd, String userId, Throwable ex) {
        // Correlate logs & user message with a short trace id
        String traceId = UUID.randomUUID().toString().substring(0, 8);

        if (ex instanceof java.util.concurrent.TimeoutException) {
            Logger.error("[" + EVENT_NAME + "] Command '" + cmd + "' timed out for user " + userId + " (traceId="
                    + traceId + ")");
            sendUserError(
                    event,
                    Emojis.DEFAULT_ERROR + " Command Timed Out",
                    "Your command `" + cmd + "` took too long and was cancelled.\n" +
                            "Please try again in a moment. (`" + traceId + "`)");
        } else {
            Logger.error("[" + EVENT_NAME + "] Command '" + cmd + "' errored for user " + userId + " (traceId="
                    + traceId + ")", ex);
            sendUserError(
                    event,
                    Emojis.DEFAULT_ERROR + " Something went wrong",
                    "We ran into an issue while running `" + cmd + "`. This isn’t your fault.\n" +
                            "Please try again. If this keeps happening, share this code with support: `" + traceId
                            + "`");
        }
    }

    /**
     * Sends an ephemeral error message to the user
     *
     * <p>
     * This utility method builds an embed and sends it to the channel where
     * the command was invoked. The message is queued to be deleted automatically
     * after a short duration for a clean user experience
     * </p>
     *
     * @param event       The {@link MessageReceivedEvent}
     * @param title       The title of the error embed
     * @param description The description of the error embed
     */
    private void sendUserError(MessageReceivedEvent event, String title, String description) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(ColorCodes.ERROR_RED)
                .setTitle(title)
                .setDescription(description);

        // Mention outside the embed so it actually pings
        event.getChannel()
                .sendMessage(event.getAuthor().getAsMention())
                .setEmbeds(embed.build())
                .queue(
                        msg -> msg.delete().queueAfter(ERROR_DELETE_SECONDS, TimeUnit.SECONDS),
                        err -> Logger.warning("[" + EVENT_NAME + "] Failed to send error message in " +
                                event.getChannel().getId() + ": " + err.getMessage()));

    }

    /**
     * Shuts down the command executor service gracefully
     *
     * <p>
     * This method is called during the bot's shutdown process to ensure all
     * running command tasks are completed or terminated within a reasonable
     * timeframe, preventing resource leaks
     * </p>
     */
    public void shutdown() {
        commandExecutor.shutdown();
        try {
            if (!commandExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                commandExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            commandExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Generates a unique thread ID for logging purposes
     *
     * @return The thread ID
     */
    private static long tId() {
        return Thread.currentThread().threadId();
    }
}