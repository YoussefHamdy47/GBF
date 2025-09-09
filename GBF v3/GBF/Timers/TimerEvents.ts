import {
  ButtonInteraction,
  CommandInteraction,
  GuildTextBasedChannel,
} from "discord.js";
import { ITimerData } from "../../Models/Timer/TimerTypes";
import { GBFUser } from "../../Models/User/UserTypes";
import CommandIDs from "../Data/CommandIDs.json";
import { TimerModel } from "../../Models/Timer/TimerModel";
import { Document } from "mongoose";
import { CustomEvents } from "../Data/ClientEvents";
import { msToTime, getTimestamp, GBF } from "../../Handler";
import {
  ButtonFixerOptions,
  LevelUpOptions,
  RecordBrokenOptions,
  TimerButtonID,
  TimerEventsReturns,
} from "./TimerHelper";
import { calculateXP, checkLevel, checkRank } from "./LevelEngine";

export class TimerEvents {
  public timerData: ITimerData & Document;
  public userData: GBFUser & Document;
  public interaction: ButtonInteraction | CommandInteraction;
  public client: GBF;

  constructor(
    client: GBF,
    interaction: ButtonInteraction,
    timerData: ITimerData & Document,
    userData: GBFUser & Document
  ) {
    this.client = client;
    this.timerData = timerData;
    this.userData = userData;
    this.interaction = interaction;
  }

  /// Helpers

  public async originalMessage() {
    return (
      this.client.guilds.cache
        .get(this.timerData.sessionData.guildID)
        ?.channels.cache.get(
          this.timerData.sessionData.channelID
        ) as GuildTextBasedChannel
    )?.messages.fetch(this.timerData.sessionData.messageID);
  }

  public checkID(): boolean {
    return !Object.values(TimerButtonID).includes(
      (this.interaction as ButtonInteraction).customId as TimerButtonID
    );
  }

  public async checkMessageOwner() {
    const originalMessage = await this.originalMessage();

    if (!originalMessage) {
      this.timerData.sessionData.messageID = null;

      await this.timerData!.save();

      throw new Error(
        `I couldn't find the session start message, use ${CommandIDs.timer_start} to start a new session or re-make the session start message.`
      );
    }

    const messageOwner = await TimerModel.findOne({
      "sessionData.messageID": (this.interaction as ButtonInteraction).message
        .id,
    });

    if (
      !messageOwner ||
      (messageOwner && this.interaction.user.id !== messageOwner.account.userID)
    )
      throw new Error(
        `You can't use this button, create your own using ${CommandIDs.timer_start}.`
      );
  }

  public async handleBreak() {
    await this.checkMessageOwner();

    if (
      this.timerData.sessionData.sessionBreaks.sessionBreakStart &&
      (this.interaction as ButtonInteraction).customId !==
        TimerButtonID.Unpause &&
      (this.interaction as ButtonInteraction).customId !== TimerButtonID.Info
    )
      this.client.emit(
        CustomEvents.FixTimerButtons,
        this.client,
        this.interaction
      );
  }

  /// Event Handlers

  public async handleStart() {
    await this.checkMessageOwner();

    if (this.timerData.sessionData.sessionStartTime) {
      const fixerButtons: ButtonFixerOptions = {
        enabledButtons: [
          TimerButtonID.Info,
          TimerButtonID.Pause,
          TimerButtonID.Stop,
        ],
      };
      this.client.emit(
        CustomEvents.FixTimerButtons,
        this.client,
        this.interaction,
        fixerButtons
      );

      return TimerEventsReturns.TimerAlreadyRunning;
    }

    const currentTimestamp = Date.now();

    this.timerData.currentSemester.sessionStartTimes.push(currentTimestamp);

    this.timerData.sessionData.sessionStartTime = new Date(currentTimestamp);
    this.timerData.sessionData.lastSessionDate = new Date(currentTimestamp);
    this.timerData.sessionData.sessionBreaks = {
      sessionBreakStart: null,
      sessionBreakTime: 0,
    };
    this.timerData.sessionData.numberOfBreaks = 0;
    this.timerData.sessionData.sessionTime = 0;

    let sessionTopic = this.timerData.currentSemester.semesterSubjects.find(
      (subject) =>
        subject.subjectCode.trim().toLowerCase() ===
        this.timerData.sessionData.sessionTopic
          ?.split(" - ")[0]
          .trim()
          .toLowerCase()
    );

    if (sessionTopic) {
      this.timerData.sessionData.subjectsStudied.push(sessionTopic.subjectCode);
      sessionTopic.timesStudied++;
    }

    await this.timerData!.save();
  }

  public async handleTimerInfo(): Promise<string> {
    await this.checkMessageOwner();

    let summarizedStats = "";

    if (this.timerData.sessionData.sessionBreaks.sessionBreakStart) {
      let activeBreakTime = Math.abs(
        Number(
          (
            (Date.now() -
              this.timerData.sessionData.sessionBreaks.sessionBreakStart.getTime()) /
            1000
          ).toFixed(3)
        ) * 1000
      );

      summarizedStats += `• Active Break Time: ${
        activeBreakTime > 0 ? msToTime(activeBreakTime) : "0s"
      }\n\n`;
    }

    /**ms */
    const breakTimeRaw = this.timerData.sessionData.sessionBreaks
      .sessionBreakTime
      ? this.timerData.sessionData.sessionBreaks.sessionBreakTime * 1000
      : 0;

    const breakTime =
      breakTimeRaw > this.timerData.sessionData.sessionStartTime.getTime()
        ? breakTimeRaw
        : 0; // If breakTimeRaw is greater than sessionStartTime, use breakTimeRaw, otherwise use 0

    const timeElapsed = Math.abs(
      Number(
        (
          (Date.now() - this.timerData.sessionData.sessionStartTime.getTime()) /
            1000 -
          breakTime
        ).toFixed(3)
      )
    );

    summarizedStats += `• Time Elapsed: ${
      timeElapsed > 0 ? msToTime(timeElapsed * 1000) : "0s"
    }\n• Break Time: ${
      breakTimeRaw > 0 ? msToTime(breakTimeRaw) : "0s"
    }\n• Number of Breaks: ${
      this.timerData.sessionData.numberOfBreaks
    }\n\n• Start Time: ${getTimestamp(
      this.timerData.sessionData.sessionStartTime,
      "F"
    )}`;

    return summarizedStats;
  }

  public async handlePause() {
    await this.checkMessageOwner();

    const fixerOptions: ButtonFixerOptions = {
      enabledButtons: [TimerButtonID.Unpause, TimerButtonID.Info],
      isPaused: true,
    };

    if (!this.timerData.sessionData.sessionStartTime) {
      const disabledButtons: ButtonFixerOptions = {
        enabledButtons: [],
      };
      this.client.emit(
        CustomEvents.FixTimerButtons,
        this.client,
        this.interaction,
        disabledButtons
      );

      return TimerEventsReturns.TimerNotStarted;
    }

    if (this.timerData.sessionData.sessionBreaks.sessionBreakStart) {
      this.client.emit(
        CustomEvents.FixTimerButtons,
        this.client,
        this.interaction,
        fixerOptions
      );

      return TimerEventsReturns.TimerAlreadyPaused;
    }

    this.timerData.sessionData.sessionBreaks.sessionBreakStart = new Date(
      Date.now()
    );
    this.timerData.sessionData.numberOfBreaks++;
    this.timerData.currentSemester.breakCount++;

    this.client.emit(
      CustomEvents.FixTimerButtons,
      this.client,
      this.interaction,
      fixerOptions
    );

    await this.timerData!.save();
  }

  public async handleUnpause() {
    await this.checkMessageOwner();

    const fixerOptions: ButtonFixerOptions = {
      enabledButtons: [
        TimerButtonID.Pause,
        TimerButtonID.Info,
        TimerButtonID.Stop,
      ],
    };

    if (!this.timerData.sessionData.sessionStartTime) {
      const disabledButtons: ButtonFixerOptions = {
        enabledButtons: [],
      };
      this.client.emit(
        CustomEvents.FixTimerButtons,
        this.client,
        this.interaction,
        disabledButtons
      );

      return TimerEventsReturns.TimerNotStarted;
    }

    if (!this.timerData.sessionData.sessionBreaks.sessionBreakStart) {
      this.client.emit(
        CustomEvents.FixTimerButtons,
        this.client,
        this.interaction,
        fixerOptions
      );

      return TimerEventsReturns.TimerNotPaused;
    }

    const timeElapsed = Math.round(
      Number(
        Math.abs(
          (Date.now() -
            this.timerData.sessionData.sessionBreaks.sessionBreakStart.getTime()) /
            1000
        )
      )
    );

    this.timerData.sessionData.sessionBreaks.sessionBreakTime += timeElapsed;
    this.timerData.sessionData.sessionBreaks.sessionBreakStart = null;

    this.client.emit(
      CustomEvents.FixTimerButtons,
      this.client,
      this.interaction,
      fixerOptions
    );

    await this.timerData!.save();

    return timeElapsed;
  }

  public async handleStop() {
    await this.checkMessageOwner();

    if (!this.timerData.sessionData.sessionStartTime) {
      const fixerButtons: ButtonFixerOptions = {
        enabledButtons: [],
      };
      this.client.emit(
        CustomEvents.FixTimerButtons,
        this.client,
        this.interaction,
        fixerButtons
      );

      return TimerEventsReturns.TimerNotStarted;
    }

    if (this.timerData.sessionData.sessionBreaks.sessionBreakStart) {
      const fixerOptions: ButtonFixerOptions = {
        enabledButtons: [TimerButtonID.Unpause, TimerButtonID.Info],
        isPaused: true,
      };

      this.client.emit(
        CustomEvents.FixTimerButtons,
        this.client,
        this.interaction,
        fixerOptions
      );

      return TimerEventsReturns.CannotStopPaused;
    }

    let endMessage = `• Start Time: ${getTimestamp(
      this.timerData.sessionData.sessionStartTime,
      "F"
    )}\n`;

    const breakTimeRaw = this.timerData.sessionData.sessionBreaks
      .sessionBreakTime
      ? this.timerData.sessionData.sessionBreaks.sessionBreakTime * 1000
      : 0;

    const breakTime =
      breakTimeRaw > this.timerData.sessionData.sessionStartTime.getTime()
        ? breakTimeRaw
        : 0;

    let timeElapsed = Math.abs(
      Number(
        (
          (Date.now() - this.timerData.sessionData.sessionStartTime.getTime()) /
            1000 -
          breakTime
        ).toFixed(3)
      )
    );

    if (timeElapsed <= 0) timeElapsed = timeElapsed + breakTime;

    const totalTime = timeElapsed + breakTimeRaw / 1000;

    endMessage += `• Time Elapsed: ${msToTime(
      totalTime * 1000
    )}\n• Session Time: ${msToTime(timeElapsed * 1000)}`;

    const averageBreakTime =
      breakTimeRaw > 0 && this.timerData.sessionData.numberOfBreaks > 0
        ? breakTimeRaw / this.timerData.sessionData.numberOfBreaks
        : 0;

    endMessage += `\n\n• Average Break Time: ${
      averageBreakTime !== 0 ? msToTime(averageBreakTime) : "No Breaks Taken"
    }\n• Total Break Time: ${
      breakTimeRaw > 0 ? msToTime(breakTimeRaw) : "No Breaks Taken"
    }\n• Number of Breaks: ${this.timerData.sessionData.numberOfBreaks}`;

    // Add studied subjects summary
    const studiedSubjects =
      this.timerData.sessionData.subjectsStudied.length > 0
        ? `\n• Studied Subjects: ${this.timerData.sessionData.subjectsStudied.join(
            ", "
          )}`
        : "\n• No subjects studied this session.";

    endMessage += studiedSubjects;

    const disabledButtons: ButtonFixerOptions = {
      enabledButtons: [],
    };

    this.client.emit(
      CustomEvents.FixTimerButtons,
      this.client,
      this.interaction,
      disabledButtons
    );

    // Updating the total break time
    this.timerData.currentSemester.totalBreakTime += breakTimeRaw / 1000;
    this.timerData.currentSemester.semesterTime += timeElapsed;
    this.timerData.currentSemester.breakCount +=
      this.timerData.sessionData.numberOfBreaks;

    if (this.timerData.currentSemester.longestSession < timeElapsed) {
      this.timerData.currentSemester.longestSession = timeElapsed;

      const recordBroken: RecordBrokenOptions = {
        interaction: this.interaction as unknown as CommandInteraction,
        type: "Session",
        sessionTime: timeElapsed,
      };

      this.client.emit(CustomEvents.RecordBroken, recordBroken);
    }

    const xpEarned = Math.round(calculateXP(timeElapsed / 60));

    endMessage += `\n• XP & RP Earned: ${xpEarned.toLocaleString("en-US")}`;

    const hasRankedUp = checkRank(
      this.userData.Rank,
      this.userData.RP,
      xpEarned
    );

    const hasLeveledUp = checkLevel(
      this.timerData.currentSemester.semesterLevel,
      this.timerData.currentSemester.semesterXP,
      xpEarned
    );

    if (hasRankedUp.hasRankedUp) {
      const rankUpOptions: LevelUpOptions = {
        interaction: this.interaction as unknown as CommandInteraction,
        levelUps: hasRankedUp.addedLevels,
        carryOverXP: hasRankedUp.remainingRP,
        userData: this.userData,
      };

      this.userData.Rank +=
        hasRankedUp.addedLevels > 0 ? hasRankedUp.addedLevels : 1;
      this.userData.RP = hasRankedUp.remainingRP;

      this.client.emit(CustomEvents.AccountLevelUp, rankUpOptions);
    } else this.userData.RP += xpEarned;

    if (hasLeveledUp.hasLeveledUp) {
      const levelUpOptions: LevelUpOptions = {
        interaction: this.interaction as unknown as CommandInteraction,
        levelUps: hasLeveledUp.addedLevels,
        carryOverXP: hasLeveledUp.remainingXP,
        timerData: this.timerData,
      };

      this.timerData.currentSemester.semesterLevel +=
        hasLeveledUp.addedLevels > 0 ? hasLeveledUp.addedLevels : 1;
      this.timerData.currentSemester.semesterXP = hasLeveledUp.remainingXP;

      this.client.emit(CustomEvents.SemesterLevelUp, levelUpOptions);
    } else this.timerData.currentSemester.semesterXP += xpEarned;

    // Reset subjects studied array
    this.timerData.sessionData.subjectsStudied = [];

    // Resetting the session
    this.timerData.sessionData.sessionStartTime = null;
    this.timerData.sessionData.channelID = null;
    this.timerData.sessionData.messageID = null;
    this.timerData.sessionData.guildID = null;
    this.timerData.sessionData.numberOfBreaks = 0;
    this.timerData.sessionData.sessionTime = 0;
    this.timerData.sessionData.sessionBreaks.sessionBreakTime = 0;
    this.timerData.sessionData.sessionBreaks.sessionBreakStart = null;
    this.timerData.sessionData.lastSessionTopic =
      this.timerData.sessionData.sessionTopic;

    this.timerData.account.lifetimeTime += timeElapsed;

    const oneDayMS = 24 * 60 * 60 * 1000;
    const now = Date.now();

    if (
      this.timerData.currentSemester.lastStreakUpdate === null ||
      now - this.timerData.currentSemester.lastStreakUpdate.getTime() >=
        oneDayMS
    ) {
      this.timerData.currentSemester.streak++;
      this.timerData.currentSemester.lastStreakUpdate = new Date();

      if (
        this.timerData.currentSemester.streak >
        this.timerData.currentSemester.longestStreak
      )
        this.timerData.currentSemester.longestStreak =
          this.timerData.currentSemester.streak;
    }

    await this.timerData!.save();
    await this.userData!.save();

    return endMessage;
  }
}
