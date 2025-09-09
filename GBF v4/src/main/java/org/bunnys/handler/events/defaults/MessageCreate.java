package org.bunnys.handler.events.defaults;

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
 * Thin orchestrator for message commands:
 * - Delegates parsing, validation, metrics, execution
 */
@SuppressWarnings("unused")
public class MessageCreate extends ListenerAdapter implements Event {
    private static final String EVENT_NAME = "MessageCreate";
    private static final int COMMAND_TIMEOUT_SECONDS = 30;
    private static final int ERROR_DELETE_SECONDS = 8; // how long error notices stay visible

    private final BunnyNexus client;
    private final ExecutorService commandExecutor;
    private final CommandMetrics metrics;

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

    @Override
    public void register(JDA jda) {
        jda.addEventListener(this);
    }

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

    private static long tId() {
        return Thread.currentThread().threadId();
    }
}
