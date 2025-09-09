import { Schema, model, Document } from "mongoose";
import { Subject } from "../../GBF/Timers/GradeEngine";
import { Account, Semester, Session, SessionBreak } from "./TimerTypes";

interface ITimerData extends Document {
  account: Account;
  currentSemester: Semester;
  sessionData: Session;
}

const roundToThree = (num: number) => parseFloat(num.toFixed(3));

const SubjectSchema = new Schema<Subject>(
  {
    subjectName: { type: String, default: null },
    subjectCode: { type: String, default: null, unique: true, trim: true },
    timesStudied: { type: Number, default: 0, min: 0 },
    creditHours: { type: Number, default: null, min: 1 },
    grade: {
      type: String,
      default: null,
      enum: ["A+", "A", "A-", "B+", "B", "B-", "C+", "C", "C-", "D+", "D", "F"],
    },
    marksLost: { type: Number, default: 0, min: 0 },
  },
  {
    _id: false,
  }
);

const SemesterSchema = new Schema<Semester>(
  {
    semesterName: { type: String, default: null },
    semesterLevel: { type: Number, default: 1, min: 1, max: 5000 },
    semesterXP: { type: Number, default: 0, min: 0 },
    semesterTime: { type: Number, default: 0, min: 0, set: roundToThree },
    semesterSubjects: { type: [SubjectSchema], default: [] },
    sessionStartTimes: { type: [Number], default: [] },
    totalBreakTime: { type: Number, default: 0, min: 0, set: roundToThree },
    breakCount: { type: Number, default: 0, min: 0 },
    longestSession: { type: Number, default: 0, set: roundToThree },
    longestStreak: { type: Number, default: 0 },
    streak: { type: Number, default: 0 },
    lastStreakUpdate: { type: Date, default: null },
  },
  {
    _id: false,
  }
);

const SessionBreakSchema = new Schema<SessionBreak>(
  {
    sessionBreakTime: { type: Number, default: 0, set: roundToThree },
    sessionBreakStart: { type: Date, default: null },
  },
  {
    _id: false,
  }
);

const SessionSchema = new Schema<Session>(
  {
    guildID: { type: String },
    channelID: { type: String },
    messageID: { type: String },
    sessionStartTime: { type: Date, default: null },
    sessionTopic: { type: String },
    sessionTime: { type: Number, default: 0, set: roundToThree },
    numberOfBreaks: { type: Number, default: 0 },
    sessionBreaks: { type: SessionBreakSchema, default: () => ({}) },
    lastSessionTopic: { type: String, default: null },
    lastSessionDate: { type: Date, default: null },
    subjectsStudied: { type: [String], default: [] },
  },
  {
    _id: false,
  }
);

const AccountSchema = new Schema<Account>(
  {
    userID: { type: String, required: true },
    lifetimeTime: { type: Number, default: 0, set: roundToThree },
    longestSemester: { type: SemesterSchema, default: null },
  },
  {
    _id: false,
  }
);

const ITimerDataSchema = new Schema<ITimerData>(
  {
    account: { type: AccountSchema, required: true },
    currentSemester: { type: SemesterSchema, default: () => ({}) },
    sessionData: { type: SessionSchema, default: () => ({}) },
  },
  {
    collection: "Timer Data",
  }
);

const TimerModel = model<ITimerData>("Timer Data", ITimerDataSchema);

export {
  TimerModel,
  ITimerData,
  Account,
  Semester,
  Subject,
  Session,
  SessionBreak,
};
