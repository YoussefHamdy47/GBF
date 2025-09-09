const { MessageCommand } = require("../Handler");

module.exports = class MessageCommandTemplate extends MessageCommand {
  constructor(client) {
    super(client, {
      name: "",
      description: "",
      aliases: [""],
      UserPermissions: [],
      BotPermissions: [],
      category: "",
      cooldown: 5,
      usage: `${client.Prefix}`,
      async execute({ client, message, args }) {},
    });
  }
};
