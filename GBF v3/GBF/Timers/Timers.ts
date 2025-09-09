import {
  ButtonInteraction,
  Client,
  CommandInteraction,
  type Snowflake,
} from "discord.js";
import { Semester, type ITimerData } from "../../Models/Timer/TimerTypes";
import { TimerModel } from "../../Models/Timer/TimerModel";
import { type GBFUser } from "../../Models/User/UserTypes";
import { UserModel } from "../../Models/User/UserModel";
import { TimerStats } from "./TimerStats";
import { GBF, msToTime, secondsToHours } from "../../Handler";
import {
  calculateTotalSeasonXP,
  checkRank,
  convertSeasonLevel,
  hoursRequired,
  rankUpEmoji,
  rpRequired,
  xpRequired,
} from "./LevelEngine";
import { gradeToGPA, Subject } from "./GradeEngine";
import { Document } from "mongoose";
import { CustomEvents } from "../Data/ClientEvents";
import { TimerEvents } from "./TimerEvents";
import { formatHours, RecordBrokenOptions } from "./TimerHelper";

export class Timers {
  public readonly userID: Snowflake;
  public timerData: (ITimerData & Document) | null = null;
  public userData: (GBFUser & Document) | null = null;
  public timerStats: TimerStats | null = null;
  public timerEvents: TimerEvents | null = null;
  public gbfClient: GBF | null = null;
  public interaction: CommandInteraction | null = null;
  private isDataLoaded: boolean = false;

  constructor(
    userID: Snowflake,
    client: GBF = undefined,
    interaction: CommandInteraction = undefined
  ) {
    this.userID = userID;
    this.gbfClient = client;
    this.interaction = interaction;
  }

  /// Functions to ensure the system is working properly

  /**
   * Creates a new instance of the Timers class
   *
   * @param userID - The unique identifier of the user
   * @param timerStats - Whether to initialize timer statistics. Defaults to false
   * @param timerEvents - Whether to initialize timer events. Defaults to false
   * @param interaction - The command interaction object. Optional
   * @param client - The GBF client object. Optional
   * @returns A promise that resolves to an instance of the Timers class
   * @throws Will throw an error if `timerEvents` is true and either `interaction` or `client` is not provided
   */
  public static async create(
    userID: Snowflake,
    timerStats = false,
    timerEvents = false,
    interaction: CommandInteraction | ButtonInteraction = undefined,
    client: GBF = undefined
  ): Promise<Timers> {
    const instance = new Timers(
      userID,
      client ? client : undefined,
      interaction ? (interaction as CommandInteraction) : undefined
    );
    await instance.setUser();

    if (timerStats)
      instance.timerStats = new TimerStats(
        instance.timerData,
        instance.userData
      );

    if (timerEvents) {
      if (!interaction || !client)
        throw new Error(
          "To run timerEvents you must provide the Interaction and Client object"
        );
      instance.timerEvents = new TimerEvents(
        instance.gbfClient,
        interaction as unknown as ButtonInteraction,
        instance.timerData,
        instance.userData
      );
    }
    return instance;
  }

  private async setUser() {
    this.timerData = await TimerModel.findOne({
      "account.userID": this.userID,
    });
    this.userData = await UserModel.findOne({
      userID: this.userID,
    });

    this.isDataLoaded = true;
  }

  private checkUser() {
    if (!this.isDataLoaded)
      throw new Error(
        `User data not loaded. Call 'create' to initialize Timers for userID: ${this.userID}`
      );

    if (!this.timerData && !this.userData)
      throw new Error(`You don't have a GBF Timers account.`);
  }

  private checkSemester() {
    this.checkUser();

    if (!this.timerData!.currentSemester.semesterName)
      throw new Error(`You don't have an active semester`);
  }

  /// Setters and Getters

  /**
   * Adds a new subject to the user's subjects list and saves the updated user data
   *
   * @param {Subject} subject - The subject to be added
   * @returns {Promise<void>} A promise that resolves when the user data has been saved
   * @throws Will throw an error if the subject already exists in the user's account or current semester
   */
  public async addSubjectAccount(subject: Subject): Promise<void> {
    this.checkUser();

    // Check if the subject already exists in the user's account
    if (
      this.userData!.Subjects.some((s) => s.subjectName === subject.subjectName)
    ) {
      throw new Error(
        `Subject '${subject.subjectName}' already exists for userID: ${this.userID} in the account subjects list`
      );
    }

    // Check if the subject exists in the current semester
    if (
      this.timerData?.currentSemester?.semesterSubjects.some(
        (s) => s.subjectName === subject.subjectName
      )
    ) {
      throw new Error(
        `Subject '${subject.subjectName}' already exists for userID: ${this.userID} in the current semester`
      );
    }

    this.userData.Subjects.push(subject);
    await this.userData!.save();
  }

  /**
   * Adds a subject to the current semester's list of subjects
   *
   * @param {Subject} subject - The subject to be added to the current semester
   * @returns {Promise<void>} A promise that resolves when the subject has been added and the data has been saved
   * @throws Will throw an error if the subject already exists in the current semester or user's account
   */
  public async addSubjectSemester(subject: Subject): Promise<void> {
    this.checkSemester();

    // Check if the subject already exists in the current semester
    if (
      this.timerData!.currentSemester.semesterSubjects.some(
        (s) => s.subjectCode === subject.subjectCode
      )
    ) {
      throw new Error(
        `Subject '${subject.subjectCode}' already in the current semester`
      );
    }

    // Check if the subject exists in the user's account
    if (
      this.userData?.Subjects.some((s) => s.subjectName === subject.subjectName)
    ) {
      throw new Error(
        `Subject '${subject.subjectName}' already exists in the account subjects list`
      );
    }

    this.timerData!.currentSemester.semesterSubjects.push(subject);
    await this.timerData!.save();
  }

  /**
   * Removes a subject from the user's subjects list by name and saves the updated user data
   *
   * @param {string} subjectCode - The code of the subject to be removed
   * @returns {Promise<void>} A promise that resolves when the user data has been saved
   * @throws Will throw an error if the subject is not found or if the user is not authenticated
   */
  public async removeSubjectAccount(subjectCode: string): Promise<void> {
    this.checkUser();

    const index = this.userData!.Subjects.findIndex(
      (s) =>
        s.subjectCode.trim().toLowerCase() === subjectCode.trim().toLowerCase()
    );

    if (index === -1) {
      throw new Error(
        `You haven't registered '${subjectCode}' in your account.`
      );
    }

    this.userData.Subjects.splice(index, 1);

    await this.userData!.save();
  }

  /**
   * Removes a subject from the user's subjects list by name and saves the updated user data
   *
   * @param {string} subjectCode - The code of the subject to be removed
   * @returns {Promise<void>} A promise that resolves when the user data has been saved
   * @throws Will throw an error if the subject is not found or if the user is not authenticated
   */
  public async removeSubjectSemester(subjectCode: string): Promise<void> {
    this.checkSemester();

    const index = this.timerData.currentSemester!.semesterSubjects.findIndex(
      (s) => s.subjectCode.toLowerCase() === subjectCode.toLowerCase()
    );

    if (index === -1) {
      throw new Error(
        `You haven't registered '${subjectCode}' for this semester`
      );
    }

    this.timerData.currentSemester.semesterSubjects.splice(index, 1);

    await this.timerData!.save();
  }

  public getSemesterName(): string {
    this.checkSemester();
    return this.timerData!.currentSemester.semesterName;
  }

  /// Command functions

  public async register(semesterName?: string): Promise<boolean> {
    if (semesterName && semesterName.trim() === "")
      throw new Error("Semester name cannot be empty");

    try {
      if (!this.userData) await this.registerUser();

      if (semesterName) await this.registerSemester(semesterName);

      return true;
    } catch (error) {
      throw new Error(error instanceof Error ? error.message : String(error));
    }
  }

  private async registerUser() {
    if (this.userData)
      throw new Error(
        `An existing account already exists for user ID '${this.userID}'`
      );

    this.userData = new UserModel({
      userID: this.userID,
      Subjects: [],
    });

    if (!this.timerData) {
      const newData = new TimerModel({
        "account.userID": this.userID,
      });

      this.timerData = newData as unknown as ITimerData & Document;

      await this.timerData!.save();
    }

    await this.userData.save();
  }

  private async registerSemester(semesterName: string) {
    this.checkUser();

    if (this.timerData) {
      if (this.timerData.currentSemester.semesterName)
        throw new Error(
          `Semester '${this.timerData.currentSemester.semesterName}' is already active, end it before you can start a new one.`
        );
      else {
        // Resetting the data just in-case
        const resetSemester: Semester = {
          breakCount: 0,
          longestSession: 0,
          semesterLevel: 1,
          semesterName: semesterName,
          semesterSubjects: [],
          semesterTime: 0,
          semesterXP: 0,
          sessionStartTimes: [],
          totalBreakTime: 0,
          longestStreak: 0,
          streak: 0,
          lastStreakUpdate: null,
        };

        this.timerData.currentSemester = resetSemester;

        await this.timerData!.save();
      }
    } else {
      const newData = new TimerModel({
        "account.userID": this.userID,
        "currentSemester.semesterName": semesterName,
      });

      this.timerData = newData as unknown as ITimerData & Document;

      await this.timerData!.save();
    }
  }

  public statDisplay() {
    this.checkUser();

    const timerStats = new TimerStats(this.timerData, this.userData);

    let gap = "\n";

    let stats = "";

    let accountDetails = "";

    accountDetails += `• Lifetime Study Time: ${
      timerStats.getTotalStudyTime() !== 0
        ? msToTime(timerStats.getTotalStudyTime()) +
          ` [${Number(
            secondsToHours(timerStats.getTotalStudyTime() / 1000).split(" ")[0]
          ).toLocaleString("en-US")} hours]`
        : "0s"
    }\n`;

    if (this.timerData.currentSemester.semesterName) {
      accountDetails += `• Semester Study Time: ${
        timerStats.getSemesterTime() !== 0
          ? msToTime(timerStats.getSemesterTime()) +
            ` [${secondsToHours(timerStats.getSemesterTime() / 1000)}]`
          : "0s"
      }\n`;

      // Assuming the user only does 1 session a day, another way to do this is to get the start time for the semester but this will be added later
      const numberOfWeeks = Math.floor(timerStats.getSessionCount() / 7);

      const averageTimePerWeek = Number(
        (timerStats.getSemesterTime() / numberOfWeeks).toFixed(3)
      );

      accountDetails += `• Average Session Time / 7 Sessions: ${
        averageTimePerWeek !== 0
          ? msToTime(averageTimePerWeek) +
            ` [${secondsToHours(averageTimePerWeek / 1000)}]`
          : "0s"
      }\n`;

      accountDetails += `• Average Session Time: ${
        timerStats.getAverageSessionTime() !== 0
          ? msToTime(timerStats.getAverageSessionTime())
          : "0s"
      }\n`;

      accountDetails += `• Total Sessions: ${timerStats.getSessionCount()}\n`;

      stats += accountDetails + gap;
    }

    let streakDetails = "";

    streakDetails += `• Study Streak: ${timerStats.getCurrentStreak()} 🔥\n`;
    streakDetails += `• Longest Study Streak: ${timerStats.getLongestStreak()} 🔥\n`;

    stats += streakDetails + gap;

    let recordDetails = "";

    if (this.timerData.currentSemester.semesterName)
      recordDetails += `• Longest Session: ${
        timerStats.getLongestSessionTime() !== 0
          ? msToTime(timerStats.getLongestSessionTime())
          : "0s"
      }\n`;

    const longestSemester = timerStats.getLongestSemester();

    if (longestSemester)
      recordDetails += `• Longest Semester: ${msToTime(
        longestSemester.semesterTime * 1000
      )} [${longestSemester.semesterTime / 3600} hours] - [${
        longestSemester.semesterName
      }]\n`;

    stats += recordDetails + gap;

    if (this.timerData.currentSemester.semesterName) {
      let semesterBreakDetails = "";

      semesterBreakDetails += `• Semester Break Time: ${
        timerStats.getBreakTime() !== 0
          ? msToTime(timerStats.getBreakTime())
          : "0s"
      }\n`;

      semesterBreakDetails += `• Total Breaks: ${timerStats.getBreakCount()}\n`;

      semesterBreakDetails += `• Average Break Time: ${
        timerStats.getAverageBreakTime() !== 0
          ? msToTime(timerStats.getAverageBreakTime())
          : "0s"
      }\n`;

      semesterBreakDetails += `• Average Time Between Breaks: ${
        timerStats.getAverageTimeBetweenBreaks() !== 0
          ? msToTime(timerStats.getAverageTimeBetweenBreaks())
          : "0s"
      }\n`;

      stats += semesterBreakDetails + gap;

      let averageDetails = "";

      averageDetails += `• Average Start Time: ${
        timerStats.getAverageStartTimeUNIX() !== null
          ? `<t:${timerStats.getAverageStartTimeUNIX()}:t>`
          : "N/A"
      }\n`;

      // Will re-add later when I add a semester start time

      // averageDetails += `• Average Session Time / Week: ${
      //   timerStats.getAverageTimePerWeek() !== 0
      //     ? msToTime(timerStats.getAverageTimePerWeek())
      //     : "0s"
      // }\n`;

      stats += averageDetails + gap;
    }
    let subjectDetails = "";

    if (this.timerData.currentSemester.semesterName) {
      subjectDetails += `• Total Subjects: ${timerStats.getSubjectCount()}\n`;

      subjectDetails += `• Total study instances across all subjects: ${timerStats.getTotalTimesStudied()}\n`;

      /*   subjectDetails += `• Most Studied Subject: ${
        timerStats.getMostStudiedSubject() !== "No Data"
          ? timerStats.getMostStudiedSubject()
          : "N/A"
      } [${timerStats.getMostStudiedCount()}]\n`;

      subjectDetails += `• Least Studied Subject: ${
        timerStats.getLeastStudiedSubject() !== "No Data"
          ? timerStats.getLeastStudiedSubject()
          : "N/A"
      } [${timerStats.getLeastStudiedCount()}]\n`; */

      const orderedSubjects = timerStats.getSubjectsInOrder();

      if (orderedSubjects.length > 0) {
        subjectDetails += "**\nSubject Stats**\n";
        orderedSubjects.forEach(
          (subject) =>
            (subjectDetails += `• ${subject.subjectName} [${subject.timesStudied}]\n`)
        );
      } else {
        subjectDetails += "**Subject Stats**\nN/A\n";
      }

      subjectDetails += `• Average Study Time Per Subject: ${
        timerStats.getAverageStudyTimePerSubject() !== 0
          ? msToTime(timerStats.getAverageStudyTimePerSubject())
          : "N/A"
      }\n`;

      stats += subjectDetails + gap;
    }

    let levelDetails = "";

    let xpToNextLevel = 0;

    if (this.timerData.currentSemester.semesterName)
      xpToNextLevel = xpRequired(timerStats.getSemesterLevel() + 1);

    const rpToNextLevel = rpRequired(timerStats.getAccountLevel() + 1);

    if (this.timerData.currentSemester.semesterName) {
      levelDetails += `${rankUpEmoji(
        timerStats.getSemesterLevel()
      )} Semester Level: ${timerStats.getSemesterLevel()}\n`;

      levelDetails += `• XP to reach level ${
        timerStats.getSemesterLevel() + 1
      }: ${
        timerStats.getSemesterXP().toLocaleString("en-US") +
        "/" +
        xpToNextLevel.toLocaleString("en-US")
      }\n`;

      const percentageCompleteSemester = timerStats.percentageToNextLevel();
      levelDetails += `${timerStats.generateProgressBar(
        percentageCompleteSemester
      )} [${percentageCompleteSemester}%]\n`;

      levelDetails += `• Time until level ${
        timerStats.getSemesterLevel() + 1
      }: ${msToTime(timerStats.getMsToNextLevel())}\n`;

      levelDetails += `${rankUpEmoji(
        timerStats.getAccountLevel()
      )} Account Level: ${timerStats.getAccountLevel()}\n`;

      levelDetails += `• RP to reach level ${
        timerStats.getAccountLevel() + 1
      }: ${timerStats
        .getAccountRP()
        .toLocaleString("en-US")}/${rpToNextLevel.toLocaleString("en-US")}\n`;

      const percentageCompleteAccount = timerStats.percentageToNextRank();

      levelDetails += `${timerStats.generateProgressBar(
        percentageCompleteAccount
      )} [${percentageCompleteAccount}%]\n`;

      levelDetails += `• Time until level ${
        timerStats.getAccountLevel() + 1
      }: ${msToTime(timerStats.getMsToNextRank())}\n`;

      stats += levelDetails + gap;

      levelDetails += `• GPA: ${timerStats.GPA()}`;

      return stats;
    }
  }

  public GPAMenu() {
    this.checkUser();
    const timerStats = new TimerStats(this.timerData, this.userData);

    const gradeOrder: Record<string, number> = {
      "A+": 10,
      A: 9,
      "A-": 8,
      "B+": 7,
      B: 6,
      "B-": 5,
      "C+": 4,
      C: 3,
      "C-": 2,
      "D+": 1,
      D: 0,
      F: -1,
    };

    let subjectsList = "";
    const gradeCounts: Record<string, number> = {};

    // Sort subjects by grade descending (higher first)
    const sortedSubjects = this.userData.Subjects.sort(
      (a, b) => (gradeOrder[b.grade] || -1) - (gradeOrder[a.grade] || -1)
    );

    sortedSubjects.forEach((subject) => {
      subjectsList += `• ${subject.subjectName} - ${subject.grade ?? "N/A"}\n`;
      if (subject.grade) {
        const normalizedGrade = subject.grade.trim();
        gradeCounts[normalizedGrade] = (gradeCounts[normalizedGrade] || 0) + 1;
      }
    });

    const formattedGPA = parseFloat(timerStats.GPA().toPrecision(3));
    subjectsList += `• GPA: ${formattedGPA}\n`;

    // Sort grade counts in descending order
    const sortedGradeSummary = Object.entries(gradeCounts)
      .sort(
        ([gradeA], [gradeB]) =>
          (gradeOrder[gradeB] || -1) - (gradeOrder[gradeA] || -1)
      )
      .map(([grade, count]) => `${count} ${grade}`)
      .join(", ");

    if (sortedGradeSummary) {
      subjectsList += `• ${sortedGradeSummary}`;
    }

    return subjectsList;
  }

  public async endSemester() {
    this.checkSemester();

    // These are needed incase the user resets the semester before ever starting a session

    const semester = this.timerData.currentSemester;

    const longestSession = semester.longestSession
      ? msToTime(semester.longestSession * 1000)
      : "0s";

    const totalTime = semester.semesterTime
      ? msToTime(semester.semesterTime * 1000)
      : "0s";

    const totalBreakTime = semester.totalBreakTime
      ? msToTime(semester.totalBreakTime * 1000)
      : "0s";

    const sessionCount = Array.isArray(semester.sessionStartTimes)
      ? semester.sessionStartTimes.length
      : 0;

    const convertedXP =
      convertSeasonLevel(semester.semesterLevel) + (semester.semesterXP ?? 0);

    const semesterXP = semester.semesterXP ?? 0;
    const totalSemesterXP = calculateTotalSeasonXP(semesterXP) + semesterXP;

    let semesterRecap = `
      **• Total Time:** ${totalTime}\n
      **• Number of Sessions:** ${sessionCount}\n
      **• Total Break Time:** ${totalBreakTime}\n
      **• Longest Session:** ${longestSession}\n
      **• Semester Level:** ${semester.semesterLevel ?? 0}\n
      **• Semester XP:** ${totalSemesterXP.toLocaleString("en-US")}\n
      **• Account XP Converted:** ${convertedXP.toLocaleString("en-US")}`;

    if (
      this.timerData.account.longestSemester &&
      this.timerData.account.longestSemester.semesterTime <
        this.timerData.currentSemester.semesterTime
    ) {
      this.timerData.account.longestSemester = this.timerData.currentSemester;

      const recordBroken: RecordBrokenOptions = {
        interaction: this.interaction,
        type: "Semester",
        semester: this.timerData.currentSemester,
      };

      this.gbfClient.emit(CustomEvents.RecordBroken, recordBroken);
    }

    this.timerData.account.lifetimeTime +=
      this.timerData.currentSemester.semesterTime;

    const rankUpCheck = checkRank(
      this.userData.Rank,
      this.userData.RP,
      convertedXP
    );

    if (rankUpCheck.hasRankedUp) {
      this.gbfClient.emit(
        CustomEvents.AccountLevelUp,
        this.timerData.account.userID,
        rankUpCheck.addedLevels,
        rankUpCheck.remainingRP
      );
    } else {
      this.userData.RP += convertedXP;
    }

    const resetSemester: Semester = {
      breakCount: 0,
      longestSession: 0,
      semesterLevel: 1,
      semesterName: null,
      semesterSubjects: [],
      semesterTime: 0,
      semesterXP: 0,
      sessionStartTimes: [],
      totalBreakTime: 0,
      longestStreak: 0,
      streak: 0,
      lastStreakUpdate: null,
    };

    this.timerData.currentSemester = resetSemester;

    await this.timerData!.save();

    return semesterRecap;
  }
}
