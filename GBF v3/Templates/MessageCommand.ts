import { MessageCommand, GBF } from "../Handler";

export class MessageCommandTemplate extends MessageCommand {
  constructor(client: GBF) {
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
}
