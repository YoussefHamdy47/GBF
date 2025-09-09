import { ApplicationCommandType } from "discord.js";
import { GBF } from "../GBF";
import { ContextOptions } from "../types";

export abstract class ContextCommand {
  constructor(
    public readonly client: GBF,
    public readonly CommandOptions: ContextOptions = {
      name: undefined,
      category: "",
      NSFW: false,
      ContextType: ApplicationCommandType.Message,
      UserPermissions: [],
      BotPermissions: [],
      cooldown: 0,
      DeveloperOnly: false,
      DeveloperBypass: false,
      DMEnabled: false,
      development: false,
      IgnoreCommand: false,
      execute({ client, interaction }) {},
    }
  ) {
    this.CommandOptions.name = this.CommandOptions.name ?? undefined;
    this.CommandOptions.category = this.CommandOptions.category ?? "";
    this.CommandOptions.NSFW = this.CommandOptions.NSFW ?? false;
    this.CommandOptions.ContextType =
      this.CommandOptions.ContextType ?? ApplicationCommandType.Message;
    this.CommandOptions.UserPermissions =
      this.CommandOptions.UserPermissions ?? [];
    this.CommandOptions.BotPermissions =
      this.CommandOptions.BotPermissions ?? [];
    this.CommandOptions.cooldown = this.CommandOptions.cooldown ?? 0;
    this.CommandOptions.DeveloperOnly =
      this.CommandOptions.DeveloperOnly ?? false;
    this.CommandOptions.DeveloperBypass =
      this.CommandOptions.DeveloperBypass ?? false;
    this.CommandOptions.DMEnabled =
      this.CommandOptions.DMEnabled ?? client.DMEnabled ?? false;
    this.CommandOptions.development = this.CommandOptions.development ?? false;
    this.CommandOptions.IgnoreCommand =
      this.CommandOptions.IgnoreCommand ?? false;
  }
}
