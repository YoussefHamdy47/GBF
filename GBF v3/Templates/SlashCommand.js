const { ApplicationCommandOptionType } = require("discord.js");
const { SlashCommand } = require("../Handler");

module.exports = class SlashCommandTemplate extends SlashCommand {
  constructor(client) {
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
};
