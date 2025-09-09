import { type Snowflake } from "discord.js";
import { Subject } from "../../GBF/Timers/GradeEngine";

/**
 * Represents an account with user-specific study data
 */
interface Account {
  /**
   * The unique Discord user ID
   * @type {Snowflake}
   */
  userID: Snowflake;

  /**
   * The total time the user has spent studying, in seconds
   * @type {number}
   */
  lifetimeTime: number;

  /**
   * The longest semester the user had
   * @type {Semester}
   */
  longestSemester: Semester;
}

/**
 * Represents a semester with related data such as level, XP, and study time
 */
interface Semester {
  /**
   * The name of the semester
   * @type {string}
   */
  semesterName: string;

  /**
   * The level of the semester (e.g., 1 for first year, 2 for second year, etc.)
   * @type {number}
   */
  semesterLevel: number;

  /**
   * The total XP earned in the semester
   * @type {number}
   */
  semesterXP: number;

  /**
   * The total study time in the semester, in seconds
   * @type {number}
   */
  semesterTime: number;

  /**
   * An array of subjects that are part of the semester
   * @type {Subject[]}
   */
  semesterSubjects: Subject[];

  /**
   * An array of timestamps representing the start times of sessions during the semester
   * @type {number[]}
   */
  sessionStartTimes: number[];

  /**
   * The total break time taken during the semester, in seconds
   * @type {number}
   */
  totalBreakTime: number;

  /**
   * The total number of breaks taken during the semester
   * @type {number}
   */
  breakCount: number;

  /**
   * The longest session in the current semester
   * @type {number}
   */
  longestSession: number;

  /**
   * The longest streak in the current semester
   * @type {number}
   */
  longestStreak: number;

  /**
   * The user's current study streak
   * @type {number}
   */
  streak: number;

  /**
   * Last time the streak was updated
   * @type {Date}
   */
  lastStreakUpdate: Date;
}

/**
 * Represents a break taken during a study session
 */
interface SessionBreak {
  /**
   * The duration of the session break, in seconds
   * @type {number}
   */
  sessionBreakTime: number;

  /**
   * The start time of the session break
   * @type {Date | null}
   */
  sessionBreakStart: Date | null;
}

interface Session {
  /**
   * The Discord guild (server) ID associated with the session
   * @type {Snowflake}
   */
  guildID: Snowflake;

  /**
   * The channel's ID
   * @type {Snowflake}
   */
  channelID: Snowflake;

  /**
   * The unique Discord message ID for the session's message
   * @type {Snowflake}
   */
  messageID: Snowflake;

  /**
   * The start time of the session
   * @type {Date | null}
   */
  sessionStartTime: Date | null;

  /**
   * The topic or focus of the session
   * @type {string}
   */
  sessionTopic: string;

  /**
   * The duration of the session, in seconds
   * @type {number}
   */
  sessionTime: number;

  /**
   * An array of session breaks taken during the session
   * @type {SessionBreak}
   */
  sessionBreaks: SessionBreak;

  /**
   * The total number of breaks taken during the session
   * @type {number}
   */
  numberOfBreaks: number;

  /**
   * The topic of the last session
   * @type {string}
   */
  lastSessionTopic: string;
  /**
   * The date of the last session
   * @type {Date}
   */
  lastSessionDate: Date;

  /**
   * An array that contains the subjects studied in the session, this is added to avoid increasing the times studied for a subject more than once in a single session, stores using subject code.
   * @type {string[]}
   */
  subjectsStudied: string[];
}

/**
 * Represents the timer data for a user, including account information, current semester, and session data
 * Extends the Mongoose Document interface to interact with MongoDB
 */
interface ITimerData extends Document {
  /**
   * The account information for the user
   * @type {Account}
   */
  account: Account;

  /**
   * The current semester data for the user
   * @type {Semester}
   */
  currentSemester: Semester;

  /**
   * The active session
   * @type {Session}
   */
  sessionData: Session;
}

export { Account, Semester, SessionBreak, Session, ITimerData };
