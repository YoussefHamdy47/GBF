package org.bunnys.executors.timer;

import org.bunnys.executors.timer.engine.TimerHelper.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * <p>
 * A dedicated Spring component for publishing timer-related events. This class
 * acts as a centralized event bus client, abstracting the details of Spring's
 * {@link ApplicationEventPublisher}. It provides a clean, type-safe API for
 * different parts of the application to publish events related to the timer
 * subsystem, such as UI state changes, level-ups, and new records.
 * </p>
 *
 * <p>
 * By using an event-driven architecture, this class promotes a decoupled
 * design.
 * Components that are interested in timer events (e.g., a Discord message
 * listener
 * that updates the UI) can simply subscribe to these events without having any
 * direct dependency on the {@link TimerEvents} class. This makes the system
 * more
 * modular, scalable, and easier to maintain.
 * </p>
 */
@Component
public class TimerEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs a new {@code TimerEventPublisher} instance. Spring automatically
     * injects the {@link ApplicationEventPublisher} instance.
     *
     * @param eventPublisher The Spring {@link ApplicationEventPublisher} instance.
     */
    public TimerEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    /**
     * Publishes a {@link ButtonStateChangeEvent} to notify listeners that the
     * timer's
     * interactive buttons need to be updated. This is typically used to enable or
     * disable buttons based on the current timer state (e.g., starting a session
     * disables the "Start" button and enables "Pause" and "Stop").
     *
     * @param context        The {@link TimerEvents.TimerContext} containing the
     *                       user and message details.
     * @param enabledButtons A list of {@link TimerButtonID}s that should be
     *                       enabled.
     * @param isPaused       A boolean indicating if the timer is in a paused state,
     *                       which affects button display.
     * @throws NullPointerException if the provided context is null.
     */
    public void publishButtonStateChange(TimerEvents.TimerContext context,
            List<TimerButtonID> enabledButtons,
            boolean isPaused) {
        Objects.requireNonNull(context, "context cannot be null");
        ButtonFixerOptions options = new ButtonFixerOptions(enabledButtons, isPaused);
        eventPublisher.publishEvent(new ButtonStateChangeEvent(context, options));
    }

    /**
     * Publishes a {@link LevelUpEvent} when a user gains a new semester level.
     * This event carries all the necessary information for a listener to handle
     * the event, such as sending a celebratory message to the user.
     *
     * @param context     The {@link TimerEvents.TimerContext} for the event.
     * @param levelUps    The number of levels gained in this event.
     * @param carryOverXP The remaining XP after the level-up calculation.
     * @throws NullPointerException if the provided context is null.
     */
    public void publishLevelUp(TimerEvents.TimerContext context, int levelUps, int carryOverXP) {
        Objects.requireNonNull(context, "context cannot be null");
        LevelUpOptions options = new LevelUpOptions(null, levelUps, carryOverXP,
                context.timerData(), context.userData());
        eventPublisher.publishEvent(new LevelUpEvent(context, options));
    }

    /**
     * Publishes a {@link RankUpEvent} when a user gains a new account rank. This
     * event is distinct from a level-up, signaling a more significant, long-term
     * progression milestone.
     *
     * @param context     The {@link TimerEvents.TimerContext} for the event.
     * @param rankUps     The number of ranks gained.
     * @param carryOverRP The remaining RP after the rank-up calculation.
     * @throws NullPointerException if the provided context is null.
     */
    public void publishRankUp(TimerEvents.TimerContext context, int rankUps, int carryOverRP) {
        Objects.requireNonNull(context, "context cannot be null");
        LevelUpOptions options = new LevelUpOptions(null, rankUps, carryOverRP,
                context.timerData(), context.userData());
        eventPublisher.publishEvent(new RankUpEvent(context, options));
    }

    /**
     * Publishes a {@link RecordBrokenEvent} when a user surpasses a previous
     * record,
     * such as their longest study session. This event can be used by listeners to
     * trigger a special celebratory message or achievement notification.
     *
     * @param context     The {@link TimerEvents.TimerContext} for the event.
     * @param type        The {@link ActivityType} of the record broken (e.g.,
     *                    SESSION).
     * @param sessionTime The new record value (e.g., the new longest session time).
     * @throws NullPointerException if the provided context is null.
     */
    public void publishRecordBroken(TimerEvents.TimerContext context, ActivityType type, long sessionTime) {
        Objects.requireNonNull(context, "context cannot be null");
        RecordBrokenOptions options = new RecordBrokenOptions(type, null, sessionTime,
                context.timerData().getCurrentSemester());
        eventPublisher.publishEvent(new RecordBrokenEvent(context, options));
    }

    // Event classes - these are simple record-based DTOs to carry the event
    // payload.
    // They are public so other components can subscribe to them.

    /**
     * An event that signals a change in the state of the timer UI buttons.
     *
     * @param context The {@link TimerEvents.TimerContext} for the event.
     * @param options The {@link ButtonFixerOptions} containing the state details.
     */
    public record ButtonStateChangeEvent(TimerEvents.TimerContext context, ButtonFixerOptions options) {
    }

    /**
     * An event triggered when a user gains one or more semester levels.
     *
     * @param context The {@link TimerEvents.TimerContext} for the event.
     * @param options The {@link LevelUpOptions} containing the level progression
     *                details.
     */
    public record LevelUpEvent(TimerEvents.TimerContext context, LevelUpOptions options) {
    }

    /**
     * An event triggered when a user gains one or more account ranks.
     *
     * @param context The {@link TimerEvents.TimerContext} for the event.
     * @param options The {@link LevelUpOptions} containing the rank progression
     *                details.
     */
    public record RankUpEvent(TimerEvents.TimerContext context, LevelUpOptions options) {
    }

    /**
     * An event triggered when a user breaks a personal record.
     *
     * @param context The {@link TimerEvents.TimerContext} for the event.
     * @param options The {@link RecordBrokenOptions} containing the record details.
     */
    public record RecordBrokenEvent(TimerEvents.TimerContext context, RecordBrokenOptions options) {
    }
}