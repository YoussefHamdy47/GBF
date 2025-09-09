const { ApplicationCommandType } = require("discord.js");
const { ContextCommand } = require("../Handler");

module.exports = class ContextCommandTemplate extends ContextCommand {
  constructor(client) {
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
};
