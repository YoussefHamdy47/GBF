package org.bunnys.nexus.timers.buttons;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.emoji.Emoji; // <-- Added this import
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bunnys.nexus.timers.services.PendingSessionManager;
import org.bunnys.nexus.timers.Timers;
import org.bunnys.nexus.timers.services.TimerSessionService;
import org.bunnys.utils.AppDesign;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class SessionMenuManager {

    private static final String PREFIX = "session";
    private static final Map<String, Long> lastEmbedUpdate = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 15000; // 15 Seconds

    public enum SessionState {
        PENDING, RUNNING, PAUSED, ENDED
    }

    public enum SessionAction {
        START("start", "▶ Start"),
        PAUSE("pause", "⏸ Pause"),
        RESUME("resume", "▶ Resume"),
        STATS("stats", "💎 Telemetry"),
        END("end", "End Session"); 

        public final String id;
        public final String label;

        SessionAction(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public static SessionAction fromString(String id) {
            for (SessionAction action : values())
                if (action.id.equalsIgnoreCase(id)) return action;
            return null;
        }
    }

    private SessionMenuManager() {}

    public static List<Button> buildButtons(String userId, SessionState state) {
        if (state == SessionState.ENDED) {
            return List.of(
                    Button.secondary(PREFIX + ":start:" + userId, SessionAction.START.label).asDisabled(),
                    Button.secondary(PREFIX + ":pause:" + userId, SessionAction.PAUSE.label).asDisabled(),
                    Button.primary(PREFIX + ":stats:" + userId, SessionAction.STATS.label).asDisabled(),
                    Button.danger(PREFIX + ":end:" + userId, SessionAction.END.label)
                            .withEmoji(Emoji.fromFormatted(AppDesign.Emojis.STOP)).asDisabled() // <-- Attached emoji properly
            );
        }

        if (state == SessionState.PENDING) {
            return List.of(
                    Button.secondary(PREFIX + ":" + SessionAction.START.id + ":" + userId, SessionAction.START.label),
                    Button.secondary(PREFIX + ":" + SessionAction.PAUSE.id + ":" + userId, SessionAction.PAUSE.label).asDisabled(),
                    Button.primary(PREFIX + ":" + SessionAction.STATS.id + ":" + userId, SessionAction.STATS.label).asDisabled(),
                    Button.danger(PREFIX + ":" + SessionAction.END.id + ":" + userId, "Cancel")
                            .withEmoji(Emoji.fromFormatted(AppDesign.Emojis.STOP)) // <-- Attached emoji properly
            );
        }

        if (state == SessionState.PAUSED) {
            // EVERYTHING is grayed out and disabled except Resume
            return List.of(
                    Button.secondary(PREFIX + ":start:" + userId, SessionAction.START.label).asDisabled(),
                    Button.secondary(PREFIX + ":" + SessionAction.RESUME.id + ":" + userId, SessionAction.RESUME.label),
                    Button.primary(PREFIX + ":" + SessionAction.STATS.id + ":" + userId, SessionAction.STATS.label).asDisabled(),
                    Button.danger(PREFIX + ":" + SessionAction.END.id + ":" + userId, SessionAction.END.label)
                            .withEmoji(Emoji.fromFormatted(AppDesign.Emojis.STOP)).asDisabled() // <-- Attached emoji properly
            );
        }

        // RUNNING State - STATS is Blue (primary), END is Red (danger)
        return List.of(
                Button.secondary(PREFIX + ":start:" + userId, SessionAction.START.label).asDisabled(),
                Button.secondary(PREFIX + ":" + SessionAction.PAUSE.id + ":" + userId, SessionAction.PAUSE.label),
                Button.primary(PREFIX + ":" + SessionAction.STATS.id + ":" + userId, SessionAction.STATS.label),
                Button.danger(PREFIX + ":" + SessionAction.END.id + ":" + userId, SessionAction.END.label)
                        .withEmoji(Emoji.fromFormatted(AppDesign.Emojis.STOP)) // <-- Attached emoji properly
        );
    }

    public static void handle(ButtonInteractionEvent event, String actionId, String targetUserId) {
        if (!event.getUser().getId().equals(targetUserId)) {
            event.reply("> " + AppDesign.Emojis.ERROR + " **Access Denied:** This telemetry terminal belongs to someone else.")
                    .setEphemeral(true).queue();
            return;
        }

        SessionAction action = SessionAction.fromString(actionId);
        if (action == null) return;

        try {
            switch (action) {
                case START -> {
                    PendingSessionManager.PendingSession pSession = PendingSessionManager.getAndRemove(targetUserId);
                    if (pSession == null) {
                        event.reply("> ❌ **Error:** Pending session expired or not found.").setEphemeral(true).queue();
                        return;
                    }

                    Timers timerSystem = new Timers(targetUserId, event);
                    MessageEmbed responseEmbed = timerSystem.startSession(event.getMessageId(), pSession.channelId, pSession.guildId, pSession.topic, pSession.objective);

                    event.editMessageEmbeds(responseEmbed)
                            .setComponents(ActionRow.of(buildButtons(targetUserId, SessionState.RUNNING)))
                            .queue();
                }
                case PAUSE -> {
                    TimerSessionService.pauseSession(targetUserId);

                    Timers timerSystem = new Timers(targetUserId, event);
                    MessageEmbed refreshedMain = timerSystem.refreshMainEmbed(event.getMessage().getEmbeds().getFirst());

                    org.bunnys.database.models.timers.Session dbSession = TimerSessionService.getTimerDataOrThrow(targetUserId).getSessionData();
                    long startMs = dbSession.getSessionStartTime().getTime();
                    long now = System.currentTimeMillis();
                    double elapsedSecs = (now - startMs) / 1000.0;
                    double activeStudySecs = Math.max(0, elapsedSecs - dbSession.getSessionBreaks().getSessionBreakTime());
                    String formattedElapsed = org.bunnys.utils.Utils.msToTime((long)(activeStudySecs * 1000)).orElse("0s");

                    EmbedBuilder pauseEb = new EmbedBuilder()
                            .setColor(AppDesign.ColorCodes.CYAN)
                            .setTitle("⏸ Session Paused")
                            .setDescription("✦ Time Logged: `" + formattedElapsed + "`\n" +
                                    "✦ Break Started: <t:" + (now / 1000) + ":R>");

                    event.editMessageEmbeds(refreshedMain)
                            .setComponents(ActionRow.of(buildButtons(targetUserId, SessionState.PAUSED)))
                            .queue(v -> event.getHook().sendMessageEmbeds(pauseEb.build())
                                    .queue(msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES, null, err -> {})));
                }
                case RESUME -> {
                    long breakMs = TimerSessionService.unpauseSession(targetUserId);

                    Timers timerSystem = new Timers(targetUserId, event);
                    MessageEmbed refreshedMain = timerSystem.refreshMainEmbed(event.getMessage().getEmbeds().getFirst());

                    String formattedBreak = org.bunnys.utils.Utils.msToTime(breakMs).orElse("0s");
                    EmbedBuilder resumeEb = new EmbedBuilder()
                            .setColor(AppDesign.ColorCodes.CYAN)
                            .setTitle("▶ Session Resumed")
                            .setDescription("✦ Time spent on break: `" + formattedBreak + "`\n" +
                                    "✦ Telemetry feed is live again.");

                    event.editMessageEmbeds(refreshedMain)
                            .setComponents(ActionRow.of(buildButtons(targetUserId, SessionState.RUNNING)))
                            .queue(v -> event.getHook().sendMessageEmbeds(resumeEb.build())
                                    .queue(msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES, null, err -> {})));
                }
                case STATS -> {
                    long lastUpdate = lastEmbedUpdate.getOrDefault(targetUserId, 0L);
                    long now = System.currentTimeMillis();
                    boolean canUpdateMain = (now - lastUpdate) > COOLDOWN_MS;

                    MessageEmbed telemetryEmbed = TimerSessionService.getTelemetryEmbed(targetUserId);

                    if (canUpdateMain) {
                        lastEmbedUpdate.put(targetUserId, now);
                        Timers timerSystem = new Timers(targetUserId, event);
                        MessageEmbed refreshedMain = timerSystem.refreshMainEmbed(event.getMessage().getEmbeds().getFirst());

                        event.editMessageEmbeds(refreshedMain).queue(v -> event.getHook().sendMessageEmbeds(telemetryEmbed).setEphemeral(true).queue());
                    } else {
                        event.replyEmbeds(telemetryEmbed).setEphemeral(true).queue();
                    }
                }
                case END -> {
                    if (PendingSessionManager.getAndRemove(targetUserId) != null) {
                        EmbedBuilder eb = new EmbedBuilder()
                                .setColor(AppDesign.ColorCodes.ERROR_RED)
                                .setTitle("✖️ Initialization Cancelled")
                                .setDescription("> *The session startup was aborted.*")
                                .setTimestamp(Instant.now());
                        event.editMessageEmbeds(eb.build())
                                .setComponents(ActionRow.of(buildButtons(targetUserId, SessionState.ENDED)))
                                .queue();
                        return;
                    }

                    String recap = TimerSessionService.stopSession(targetUserId, event);

                    event.editComponents(ActionRow.of(buildButtons(targetUserId, SessionState.ENDED))).queue();

                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(AppDesign.ColorCodes.CYAN)
                            .setTitle("💎 Session Concluded")
                            .setDescription("**Session Recap**\n\n" + recap)
                            .setFooter("🌴 Timer offline", event.getUser().getEffectiveAvatarUrl())
                            .setTimestamp(Instant.now());

                    // The ping is moved OUTSIDE the embed to the message content string
                    event.getHook().sendMessage("<@" + targetUserId + ">")
                            .addEmbeds(eb.build())
                            .queue();
                }
            }
        } catch (Exception e) {
            event.reply("> " + AppDesign.Emojis.ERROR + " **Action failed:** " + e.getMessage())
                    .setEphemeral(true).queue();
        }
    }
}