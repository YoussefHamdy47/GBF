import { Model, Schema, model } from "mongoose";

interface IGuildData {
  GuildID: string;
  Prefix: string;
}

const GBFGuildDataSchema = new Schema<IGuildData>(
  {
    GuildID: String,
    Prefix: {
      type: String,
      default: "!!",
    },
  },
  {
    collection: "Bot Server Data",
  }
);

const BotGuildModel: Model<IGuildData> = model<IGuildData>(
  "Bot Server Data",
  GBFGuildDataSchema
);

export { IGuildData, BotGuildModel };
