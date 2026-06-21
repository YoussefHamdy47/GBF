package org.bunnys.nexus.timers.buttons;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bunnys.utils.AppDesign;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public final class GPAPaginator {

    private static final String PREFIX = "gpa";
    private static final long TIMEOUT_SECONDS = 120;

    private static final Map<String, SessionState> SESSIONS = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "GPA-Paginator-Timeout");
        thread.setDaemon(true);
        return thread;
    });

    private GPAPaginator() {
    }

    /**
     * Defines the available actions for the GPA menu.
     */
    public enum MenuAction {
        FIRST("first", "⏮ First"),
        PREV("prev", "◀ Previous"),
        CLOSE("close", "✖ Close"),
        NEXT("next", "Next ▶"),
        LAST("last", "Last ⏭");

        public final String id;
        public final String label;

        MenuAction(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public static MenuAction fromString(String id) {
            for (MenuAction action : values()) {
                if (action.id.equalsIgnoreCase(id))
                    return action;
            }
            return null;
        }
    }

    public static String createSession(String userId, List<MessageEmbed> pages) {
        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        SESSIONS.put(sessionId, new SessionState(sessionId, userId, pages));
        return sessionId;
    }

    public static void attachHook(String sessionId, InteractionHook hook) {
        SessionState session = SESSIONS.get(sessionId);
        if (session != null) {
            session.hook = hook;
            session.resetTimeout();
        }
    }

    public static List<Button> buildButtons(String sessionId, int currentPage, int totalPages) {
        boolean isFirst = currentPage <= 0;
        boolean isLast = currentPage >= totalPages - 1;

        return List.of(
                createButton(MenuAction.FIRST, sessionId, isFirst, false),
                createButton(MenuAction.PREV, sessionId, isFirst, false),
                createButton(MenuAction.CLOSE, sessionId, false, true),
                createButton(MenuAction.NEXT, sessionId, isLast, false),
                createButton(MenuAction.LAST, sessionId, isLast, false));
    }

    private static Button createButton(MenuAction action, String sessionId, boolean disabled, boolean isDanger) {
        String customId = PREFIX + ":" + action.id + ":" + sessionId;
        Button btn = isDanger ? Button.danger(customId, action.label) : Button.secondary(customId, action.label);
        return disabled ? btn.asDisabled() : btn;
    }

    public static void handle(ButtonInteractionEvent event, String actionId, String sessionId) {
        SessionState session = SESSIONS.get(sessionId);

        if (session == null) {
            event.editComponents(Collections.emptyList()).queue();
            return;
        }

        if (!event.getUser().getId().equals(session.userId)) {
            event.reply(
                    "> " + AppDesign.Emojis.ERROR + " **Access Denied:** This academic record belongs to someone else.")
                    .setEphemeral(true).queue();
            return;
        }

        MenuAction action = MenuAction.fromString(actionId);
        if (action == null) {
            event.reply("> " + AppDesign.Emojis.ERROR + " **Error:** Unknown menu action.")
                    .setEphemeral(true).queue();
            return;
        }

        switch (action) {
            case FIRST -> session.currentPage = 0;
            case PREV -> session.currentPage = Math.max(0, session.currentPage - 1);
            case NEXT -> session.currentPage = Math.min(session.pages.size() - 1, session.currentPage + 1);
            case LAST -> session.currentPage = session.pages.size() - 1;
            case CLOSE -> {
                session.close();
                SESSIONS.remove(sessionId);
                event.editMessageEmbeds(session.getCurrentEmbed())
                        .setComponents(Collections.emptyList())
                        .queue();
                return;
            }
        }

        session.resetTimeout();
        event.editMessageEmbeds(session.getCurrentEmbed())
                .setActionRow(buildButtons(sessionId, session.currentPage, session.pages.size()))
                .queue();
    }

    /**
     * Internal state manager for a specific GPA menu instance.
     */
    private static final class SessionState {
        private final String sessionId;
        private final String userId;
        private final List<MessageEmbed> pages;

        private int currentPage = 0;
        private InteractionHook hook;
        private ScheduledFuture<?> timeoutTask;

        private SessionState(String sessionId, String userId, List<MessageEmbed> pages) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.pages = pages;
        }

        private MessageEmbed getCurrentEmbed() {
            return pages.get(currentPage);
        }

        private void resetTimeout() {
            if (timeoutTask != null)
                timeoutTask.cancel(false);

            timeoutTask = SCHEDULER.schedule(() -> {
                SessionState removed = SESSIONS.remove(sessionId);
                if (removed != null && removed.hook != null) {
                    removed.hook.editOriginalComponents(Collections.emptyList()).queue(null, ignored -> {
                    });
                }
            }, TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        private void close() {
            if (timeoutTask != null)
                timeoutTask.cancel(false);
        }
    }
}