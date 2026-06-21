package org.bunnys.nexus.timers.services;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.bunnys.nexus.timers.buttons.SessionMenuManager;
import org.bunnys.utils.AppDesign;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;

public class PendingSessionManager {

    private static final Map<String, PendingSession> PENDING_SESSIONS = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2, r -> {
        Thread thread = new Thread(r, "Pending-Session-Timer");
        thread.setDaemon(true);
        return thread;
    });

    public static void createPendingSession(String userId, String topic, String objective, String channelId,
            String guildId, InteractionHook hook) {
        cancelPendingSession(userId);

        PendingSession session = new PendingSession(userId, topic, objective, channelId, guildId, hook);
        PENDING_SESSIONS.put(userId, session);

        session.reminderTask = SCHEDULER.schedule(() -> {
            hook.sendMessage("<@" + userId + ">, your terminal for **" + topic.split("-")[0].trim().toUpperCase()
                    + "** is standing by! Click 'Start Session' when ready.")
                    .queue(msg -> session.reminderMessageId = msg.getId(), err -> {
                    });
        }, 5, TimeUnit.MINUTES);

        session.timeoutTask = SCHEDULER.schedule(() -> {
            PENDING_SESSIONS.remove(userId);

            if (session.reminderMessageId != null) {
                session.hook.deleteMessageById(session.reminderMessageId).queue(null, err -> {
                });
            }

            EmbedBuilder eb = new EmbedBuilder()
                    .setColor(AppDesign.ColorCodes.ERROR_RED)
                    .setTitle("🛑 Initialization Aborted")
                    .setDescription("> *The session for **" + topic.split("-")[0].trim().toUpperCase()
                            + "** timed out due to inactivity.*")
                    .setTimestamp(Instant.now());

            hook.editOriginalEmbeds(eb.build())
                    .setComponents(net.dv8tion.jda.api.interactions.components.ActionRow.of(
                            SessionMenuManager.buildButtons(userId, SessionMenuManager.SessionState.ENDED)))
                    .queue(null, err -> {
                    });

        }, 10, TimeUnit.MINUTES);
    }

    public static PendingSession getAndRemove(String userId) {
        PendingSession session = PENDING_SESSIONS.remove(userId);
        if (session != null) {
            if (session.reminderTask != null)
                session.reminderTask.cancel(false);
            if (session.timeoutTask != null)
                session.timeoutTask.cancel(false);
            if (session.reminderMessageId != null && session.hook != null)
                session.hook.deleteMessageById(session.reminderMessageId).queue(null, err -> {
                });
        }
        return session;
    }

    public static void cancelPendingSession(String userId) {
        getAndRemove(userId);
    }

    public static class PendingSession {
        public final String userId, topic, objective, channelId, guildId;
        public final InteractionHook hook;
        public ScheduledFuture<?> reminderTask;
        public ScheduledFuture<?> timeoutTask;
        public String reminderMessageId;

        public PendingSession(String userId, String topic, String objective, String channelId, String guildId,
                InteractionHook hook) {
            this.userId = userId;
            this.topic = topic;
            this.objective = objective;
            this.channelId = channelId;
            this.guildId = guildId;
            this.hook = hook;
        }
    }
}