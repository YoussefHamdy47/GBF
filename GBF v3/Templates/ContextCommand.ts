import { ApplicationCommandType } from "discord.js";
import { ContextCommand, GBF } from "../Handler";

export class ContextCommandTemplate extends ContextCommand {
  constructor(client: GBF) {
    super(client, {
      name: "",
      ContextType: ApplicationCommandType.User,
      UserPermissions: [],
      BotPermissions: [],
      category: "",
      cooldown: 5,
      async execute({ client, interaction }) {},
    });
  }
}
