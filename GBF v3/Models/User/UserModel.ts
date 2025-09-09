import { Schema, Document, model } from "mongoose";
import { z } from "zod";
import { GBFUser } from "./UserTypes";
import { Subject } from "../../GBF/Timers/GradeEngine";

const mongooseSubjectSchema = new Schema<Subject>(
  {
    subjectName: { type: String, required: true },
    grade: { type: String, required: true },
    creditHours: { type: Number, required: true, min: 1 },
    subjectCode: { type: String, required: true },
  },
  {
    _id: false,
  }
);

const subjectSchema = z.object({
  subjectName: z
    .string()
    .min(1, "Subject name is required")
    .max(100, "Subject name too long"),
  grade: z.string().min(1, "Grade is required"),
  subjectCode: z.string().min(1, "Subject code is required"),
  creditHours: z.number().int().min(1, "Credit hours must be at least 1"),
});

const userSchema = z.object({
  userID: z.string().min(1, "User ID is required"),
  friends: z.array(z.string()),
  privateProfile: z.boolean(),
  Rank: z.number().int().min(0, "Rank cannot be negative"),
  RP: z.number().int().min(0, "RP cannot be negative"),
  Subjects: z.array(subjectSchema),
});

const UserSchema = new Schema<GBFUser>(
  {
    userID: { type: String, required: true },
    Rank: { type: Number, default: 0, min: 0, max: 5000 },
    RP: { type: Number, default: 0, min: 0 },
    Subjects: { type: [mongooseSubjectSchema], default: [] },
  },
  {
    collection: "GBF Users",
  }
);

const UserModel = model<GBFUser & Document>("GBF Users", UserSchema);

export { UserModel, userSchema };
