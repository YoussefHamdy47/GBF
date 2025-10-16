package org.bunnys.executors.timer.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bunnys.database.services.GBFUserService;
import org.bunnys.database.services.TimerDataService;
import org.bunnys.executors.timer.TimerEventPublisher;
import org.bunnys.executors.timer.Timers;
import org.bunnys.handler.utils.handler.Emojis;
import org.bunnys.handler.utils.handler.colors.ColorCodes;
import org.bunnys.handler.utils.handler.logging.Logger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

/**
 * Thin wrapper for stats command - delegates all logic to Timers class.
 * Only handles Discord-specific concerns (embeds, async, interaction).
 */
@Component
public class StatsSubcommandExecutor {

    private final TimerDataService timerDataService;
    private final GBFUserService userService;
    private final TimerEventPublisher eventPublisher;

    public StatsSubcommandExecutor(
            TimerDataService timerDataService,
            GBFUserService userService,
            TimerEventPublisher eventPublisher) {
        this.timerDataService = timerDataService;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    public void execute(SlashCommandInteractionEvent interaction, boolean ephemeral) {
        String userId = interaction.getUser().getId();
        String username = interaction.getUser().getName();

        CompletableFuture.supplyAsync(() -> generateStats(userId))
                .thenAccept(result -> {
                    if (result.isError()) {
                        sendErrorEmbed(interaction, result.errorMessage());
                        return;
                    }

                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle(username + " — Study Stats")
                            .setDescription(result.statsContent())
                            .setColor(ColorCodes.DEFAULT)
                            .setFooter("Stats for " + username)
                            .setTimestamp(Instant.now());

                    interaction.replyEmbeds(embed.build()).setEphemeral(ephemeral).queue();
                })
                .exceptionally(throwable -> {
                    Logger.error("Unexpected error in stats command for user " + userId, throwable);
                    sendErrorEmbed(interaction,
                            "An unexpected error occurred. Please try again later.");
                    return null;
                });
    }

    private StatsResult generateStats(String userId) {
        try {
            Timers timers = Timers.create(
                    userId,
                    timerDataService,
                    userService,
                    eventPublisher,
                    null,
                    true, // initStats = true
                    false);

            // Delegate all formatting to Timers
            String stats = timers.getFormattedStats();
            return StatsResult.success(stats);

        } catch (IllegalStateException e) {
            // Handle "no account" or "no semester" errors
            return StatsResult.error(e.getMessage());
        } catch (Exception e) {
            return StatsResult
                    .error("You do not have a Timers account or an active semester, start one to use this command.");
        }
    }

    private void sendErrorEmbed(
            SlashCommandInteractionEvent interaction,
            String message) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(Emojis.DEFAULT_ERROR + " No Timers Account")
                .setDescription(message)
                .setColor(ColorCodes.ERROR_RED)
                .setTimestamp(Instant.now());

        interaction.replyEmbeds(embed.build()).setEphemeral(true).queue();
    }

    private record StatsResult(String statsContent, String errorMessage) {
        public boolean isError() {
            return errorMessage != null;
        }

        public static StatsResult success(String stats) {
            return new StatsResult(stats, null);
        }

        public static StatsResult error(String message) {
            return new StatsResult(null, message);
        }
    }
}