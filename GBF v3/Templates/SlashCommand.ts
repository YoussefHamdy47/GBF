import { ApplicationCommandOptionType } from "discord.js";
import { SlashCommand, GBF } from "../Handler";

export class SlashCommandTemplate extends SlashCommand {
  constructor(client: GBF) {
    super(client, {
      name: "",
      description: "",
      UserPermissions: [],
      BotPermissions: [],
      category: "",
      cooldown: 5,
      options: [
        {
          name: "",
          description: "",
          type: ApplicationCommandOptionType.String,
        },
      ],
      async execute({ client, interaction }) {},
    });
  }
}
