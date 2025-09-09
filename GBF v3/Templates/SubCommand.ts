import { ApplicationCommandOptionType } from "discord.js";
import { SlashCommand, GBF } from "../Handler";

export class SubCommandTemplate extends SlashCommand {
  constructor(client: GBF) {
    super(client, {
      name: "",
      description: "",
      UserPermissions: [],
      BotPermissions: [],
      category: "",
      cooldown: 5,
      subcommands: {
        name: {
          description: "",
          SubCommandOptions: [
            {
              name: "",
              description: "",
              type: ApplicationCommandOptionType.String,
            },
          ],
          async execute({ client, interaction }) {},
        },
        ["command-name"]: {
          description: "",
          SubCommandOptions: [
            {
              name: "",
              description: "",
              type: ApplicationCommandOptionType.String,
            },
          ],
          async execute({ client, interaction }) {},
        },
      },
    });
  }
};
