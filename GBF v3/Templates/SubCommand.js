const { ApplicationCommandOptionType } = require("discord.js");
const { SlashCommand } = require("../Handler");

module.exports = class SubCommandTemplate extends SlashCommand {
  constructor(client) {
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
