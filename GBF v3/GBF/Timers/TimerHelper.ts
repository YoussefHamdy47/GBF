import {
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  CommandInteraction,
  type Snowflake,
} from "discord.js";
import { ITimerData, Semester } from "../../Models/Timer/TimerTypes";
import { GBFUser } from "../../Models/User/UserTypes";
import { Document } from "mongoose";

// Enum for Timer Button IDs
export enum TimerButtonID {
  Start = "startTimer",
  Pause = "pauseTimer",
  Info = "timerInfo",
  Stop = "stopTimer",
  Unpause = "unpauseTimer",
}

// Enum for Timer Event Returns
export enum TimerEventsReturns {
  TimerAlreadyRunning = "You have an active session.",
  TimerStarted = "Timer Started",
  TimerAlreadyPaused = "The timer is already paused, stop it before starting a new break.",
  TimerNotStarted = "You don't have an active session.",
  TimerNotPaused = "The timer is not paused.",
  CannotStopPaused = "You cannot end the session when the timer is paused.",
}

export type ButtonFixerOptions = {
  enabledButtons: TimerButtonID[];
  isPaused?: boolean;
};

export type RecordBrokenOptions = {
  type: "Session" | "Semester";
  interaction: CommandInteraction;
  sessionTime?: number;
  semester?: Semester;
};

export type LevelUpOptions = {
  interaction: CommandInteraction;
  levelUps: number;
  carryOverXP: number;
  timerData?: ITimerData & Document;
  userData?: GBFUser & Document;
};

// Utility function to format hours into a readable string
export function formatHours(hours: number): string {
  const minutes = Math.round(hours * 60);
  return minutes > 0
    ? `${minutes}m (${hours.toFixed(3)} hours)`
    : `${hours.toFixed(3)} hours`;
}

// Function to create a timer action row with dynamic button states
export function createTimerActionRow(
  disabledButtons: Partial<Record<TimerButtonID, boolean>>,
  isPaused = false
): ActionRowBuilder<ButtonBuilder> {
  const actionRow = new ActionRowBuilder<ButtonBuilder>().addComponents([
    // Start Button
    new ButtonBuilder()
      .setCustomId(TimerButtonID.Start)
      .setEmoji("🕛")
      .setLabel("Start Session")
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(disabledButtons[TimerButtonID.Start] ?? false),

    // Pause/Unpause Button
    new ButtonBuilder()
      .setCustomId(isPaused ? TimerButtonID.Unpause : TimerButtonID.Pause)
      .setEmoji(isPaused ? "▶️" : "⏰")
      .setLabel(isPaused ? "Unpause Timer" : "Pause Timer")
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(
        disabledButtons[
          isPaused ? TimerButtonID.Unpause : TimerButtonID.Pause
        ] ?? false
      ),

    // Info Button
    new ButtonBuilder()
      .setCustomId(TimerButtonID.Info)
      .setEmoji("ℹ️")
      .setLabel("Session Stats")
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(disabledButtons[TimerButtonID.Info] ?? false),

    // Stop Button
    new ButtonBuilder()
      .setCustomId(TimerButtonID.Stop)
      .setEmoji("🕧")
      .setLabel("End Session")
      .setStyle(ButtonStyle.Secondary)
      .setDisabled(disabledButtons[TimerButtonID.Stop] ?? false),
  ]);

  return actionRow;
}

// Function to generate a Discord message URL from IDs
export function messageURL(
  guildID: Snowflake,
  channelID: Snowflake,
  messageID: Snowflake
): string {
  return `https://discord.com/channels/${guildID}/${channelID}/${messageID}`;
}
