import { Schema, model } from "mongoose";

interface IBotBan {
  UserID: string;
  Reason: string;
  Timestamp: Date;
  Developer: string;
}

const GBFBotBanSchema = new Schema<IBotBan>(
  {
    UserID: String,
    Reason: String,
    Timestamp: Date,
    Developer: String,
  },
  {
    collection: "Bot Ban Docs",
  }
);

const BotBanModel = model<IBotBan>("Bot Ban Docs", GBFBotBanSchema);

export { IBotBan, BotBanModel };