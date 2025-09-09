import { Emojis } from "../../Handler";
import { ITimerData, Semester } from "../../Models/Timer/TimerTypes";
import { GBFUser } from "../../Models/User/UserTypes";
import { calculateGPA } from "./GradeEngine";
import { hoursRequired, rpRequired, xpRequired } from "./LevelEngine";

export class TimerStats {
  public timerData: ITimerData;
  public userData: GBFUser;

  constructor(timerData: ITimerData, userData: GBFUser) {
    this.timerData = timerData;
    this.userData = userData;
  }

  /// Account Details

  public getTotalStudyTime(): number {
    // Assuming timerData.account.lifetimeTime is stored in seconds,
    // we multiply by 1000 to convert to milliseconds.
    return this.timerData.account.lifetimeTime > 0
      ? this.timerData.account.lifetimeTime * 1000
      : 0;
  }

  public getSemesterTime(): number {
    return this.timerData.currentSemester.semesterTime
      ? this.timerData.currentSemester.semesterTime * 1000
      : 0;
  }

  public getSessionCount(): number {
    return this.timerData.currentSemester.sessionStartTimes.length || 0;
  }

  public getAverageSessionTime(): number {
    return this.timerData.currentSemester.semesterTime > 0 &&
      this.getSessionCount() > 0
      ? (this.timerData.currentSemester.semesterTime / this.getSessionCount()) *
          1000
      : 0;
  }

  /// Record Details

  public getLongestSessionTime(): number {
    return this.timerData.currentSemester.longestSession !== null
      ? this.timerData.currentSemester.longestSession * 1000
      : 0;
  }

  public getLongestSemester(): Semester | null {
    return this.timerData.account.longestSemester
      ? this.timerData.account.longestSemester
      : null;
  }

  /// Semester Break Details

  public getBreakTime(): number {
    return this.timerData.currentSemester.totalBreakTime > 0
      ? this.timerData.currentSemester.totalBreakTime * 1000
      : 0;
  }

  public getBreakCount(): number {
    return this.timerData.currentSemester.breakCount || 0;
  }

  public getAverageBreakTime(): number {
    return this.timerData.currentSemester.totalBreakTime > 0 &&
      this.getBreakCount() > 0
      ? (this.timerData.currentSemester.totalBreakTime / this.getBreakCount()) *
          1000
      : 0;
  }

  public getAverageTimeBetweenBreaks(): number {
    return this.timerData.currentSemester.semesterTime > 0 &&
      this.getBreakCount() > 0
      ? (this.timerData.currentSemester.semesterTime /
          this.timerData.currentSemester.breakCount) *
          1000
      : 0;
  }

  /// Subject Details

  public getSubjectCount(): number {
    return this.timerData.currentSemester.semesterSubjects.length || 0;
  }

  public getSubjectsInOrder(): { subjectName: string; timesStudied: number }[] {
    return this.timerData.currentSemester.semesterSubjects.length > 0
      ? [...this.timerData.currentSemester.semesterSubjects].sort(
          (a, b) => b.timesStudied - a.timesStudied
        )
      : [];
  }

  /*   public getMostStudiedSubject(): string {
    return this.timerData.currentSemester.semesterSubjects.length > 0
      ? this.timerData.currentSemester.semesterSubjects.reduce(
          (prev, current) =>
            prev.timesStudied > current.timesStudied ? prev : current
        ).subjectName
      : "No Data";
  }

  public getMostStudiedCount(): number {
    return this.timerData.currentSemester.semesterSubjects.length > 0
      ? this.timerData.currentSemester.semesterSubjects.reduce(
          (prev, current) =>
            prev.timesStudied > current.timesStudied ? prev : current
        ).timesStudied
      : 0;
  }

  public getLeastStudiedSubject(): string {
    return this.timerData.currentSemester.semesterSubjects.length > 0
      ? this.timerData.currentSemester.semesterSubjects.reduce(
          (prev, current) =>
            prev.timesStudied < current.timesStudied ? prev : current
        ).subjectName
      : "No Data";
  }

  public getLeastStudiedCount(): number {
    return this.timerData.currentSemester.semesterSubjects.length > 0
      ? this.timerData.currentSemester.semesterSubjects.reduce(
          (prev, current) =>
            prev.timesStudied < current.timesStudied ? prev : current
        ).timesStudied
      : 0;
  } */

  public getAverageStudyTimePerSubject(): number {
    return this.getTotalTimesStudied() > 0
      ? (this.timerData.currentSemester.semesterTime /
          this.getTotalTimesStudied()) *
          1000
      : 0;
  }

  public getTotalTimesStudied(): number {
    return this.timerData.currentSemester.semesterSubjects.length > 0
      ? this.timerData.currentSemester.semesterSubjects.reduce(
          (total, subject) => total + subject.timesStudied,
          0
        )
      : 0;
  }

  /// Last Session Details

  public getLastSessionTopic(): string {
    return this.timerData?.sessionData?.lastSessionTopic || "No Data";
  }

  public getLastSessionTime(): number {
    return (this.timerData?.sessionData?.sessionTime || 0) * 1000;
  }

  public getLastSessionDateUNIX(): number | null {
    const lastDate = this.timerData?.sessionData?.lastSessionDate;
    return lastDate ? Math.round(lastDate.getTime() / 1000) : null;
  }

  /// Average Session Details

  public getAverageStartTimeUNIX(): number | null {
    if (this.timerData.currentSemester.sessionStartTimes.length === 0)
      return null;

    // Calculate the average timestamp correctly
    const avgTimestamp = Math.round(
      this.timerData.currentSemester.sessionStartTimes.reduce(
        (prev, current) => prev + current,
        0
      ) / this.timerData.currentSemester.sessionStartTimes.length
    );

    // Convert to seconds for Discord's timestamp format
    return Math.floor(avgTimestamp / 1000);
  }

  public getAverageTimePerWeek(): number {
    return this.timerData.currentSemester.sessionStartTimes.length > 0
      ? (this.timerData.currentSemester.semesterTime /
          this.timerData.currentSemester.sessionStartTimes.length) *
          1000
      : 0;
  }

  /// Level Details

  public getSemesterLevel(): number {
    return this.timerData.currentSemester.semesterLevel || 0;
  }

  public getSemesterXP(): number {
    return this.timerData.currentSemester.semesterXP || 0;
  }

  public getAccountLevel(): number {
    return this.userData.Rank || 0;
  }

  public getAccountRP(): number {
    return this.userData.RP || 0;
  }

  public percentageToNextRank(): number {
    return this.userData.Rank
      ? Math.round((this.userData.RP / rpRequired(this.userData.Rank)) * 100)
      : 0;
  }

  public percentageToNextLevel(): number {
    const currentXP = this.timerData.currentSemester.semesterXP;
    const requiredXP = xpRequired(
      this.timerData.currentSemester.semesterLevel + 1
    );
    return Math.round((currentXP / requiredXP) * 100);
  }

  public getMsToNextLevel(): number {
    const xpLeft =
      xpRequired(this.timerData.currentSemester.semesterLevel + 1) -
      this.timerData.currentSemester.semesterXP;
    return hoursRequired(xpLeft) * 60 * 60 * 1000; // Convert hours to milliseconds
  }

  public getMsToNextRank(): number {
    const rpLeft = rpRequired(this.userData.Rank + 1) - this.userData.RP;
    return hoursRequired(rpLeft) * 60 * 60 * 1000; // Convert hours to milliseconds
  }

  public generateProgressBar(
    percentageComplete: number,
    totalSegments: number = 3
  ): string {
    const clampedPercentage = Math.min(Math.max(percentageComplete, 0), 100);
    const filledSegments = Math.round(
      (clampedPercentage / 100) * totalSegments
    );

    let progressSegments = [
      filledSegments >= 1
        ? Emojis.progressBarLeftFull
        : Emojis.progressBarLeftEmpty,
      filledSegments >= 2
        ? Emojis.progressBarMiddleFull
        : Emojis.progressBarMiddleEmpty,
      filledSegments >= 3
        ? Emojis.progressBarRightFull
        : Emojis.progressBarRightEmpty,
    ];

    return progressSegments.join("");
  }

  public GPA(): number {
    return calculateGPA(this.userData.Subjects);
  }

  /// Streak details

  public getCurrentStreak(): number {
    return this.timerData.currentSemester.streak;
  }

  public getLongestStreak(): number {
    return this.timerData.currentSemester.longestStreak;
  }
}
