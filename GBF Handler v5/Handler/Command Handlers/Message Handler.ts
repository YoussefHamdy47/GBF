import { GBF } from "../GBF";
import { MessageCommandOptions } from "../types";

export abstract class MessageCommand {
  constructor(
    protected readonly client: GBF,
    public readonly CommandOptions: MessageCommandOptions = {
      name: undefined,
      description: undefined,
      category: "",
      aliases: [],
      NSFW: false,
      usage: `${client.Prefix}${CommandOptions.name}`,
      cooldown: 0,
      UserPermissions: [],
      BotPermissions: [],
      DeveloperOnly: false,
      DeveloperBypass: false,
      development: false,
      DMEnabled: client.DMEnabled ?? false,
      IgnoreCommand: false,
      async execute({ client, message, args }) {},
    }
  ) {
    this.CommandOptions.name = this.CommandOptions.name ?? undefined;
    this.CommandOptions.description =
      this.CommandOptions.description ?? undefined;
    this.CommandOptions.category = this.CommandOptions.category ?? "";
    this.CommandOptions.aliases = this.CommandOptions.aliases ?? [];
    this.CommandOptions.NSFW = this.CommandOptions.NSFW ?? false;
    this.CommandOptions.usage =
      this.CommandOptions.usage ?? `${client.Prefix}${CommandOptions.name}`;
    this.CommandOptions.cooldown = this.CommandOptions.cooldown ?? 0;
    this.CommandOptions.UserPermissions =
      this.CommandOptions.UserPermissions ?? [];
    this.CommandOptions.BotPermissions =
      this.CommandOptions.BotPermissions ?? [];
    this.CommandOptions.DeveloperOnly =
      this.CommandOptions.DeveloperOnly ?? false;
    this.CommandOptions.DeveloperBypass =
      this.CommandOptions.DeveloperBypass ?? false;
    this.CommandOptions.development = this.CommandOptions.development ?? false;
    this.CommandOptions.DMEnabled =
      this.CommandOptions.DMEnabled ?? client.DMEnabled ?? false;
    this.CommandOptions.IgnoreCommand =
      this.CommandOptions.IgnoreCommand ?? false;
  }
}
