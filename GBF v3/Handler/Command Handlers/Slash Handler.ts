import {
  ApplicationCommandOptionType,
  ApplicationCommandSubGroup,
  ApplicationCommandType,
} from "discord.js";
import { GBF } from "../GBF";
import { SlashOptions, SubOptions } from "../types";

export abstract class SlashCommand {
  constructor(
    public readonly client: GBF,
    public readonly CommandOptions: SlashOptions = {
      name: undefined,
      description: undefined,
      options: [],
      category: "",
      NSFW: false,
      usage: `/${CommandOptions.name}`,
      cooldown: 0,
      UserPermissions: [],
      BotPermissions: [],
      DeveloperOnly: false,
      DeveloperBypass: false,
      development: false,
      DMEnabled: client.DMEnabled ?? false,
      IgnoreCommand: false,
      groups: null,
      subcommands: null,
      autocomplete: null,
      async execute({ client, interaction }) {},
    }
  ) {
    this.CommandOptions.name = this.CommandOptions.name ?? undefined;
    this.CommandOptions.description =
      this.CommandOptions.description ?? undefined;
    this.CommandOptions.options = this.CommandOptions.options ?? [];
    this.CommandOptions.category = this.CommandOptions.category ?? "";
    this.CommandOptions.NSFW = this.CommandOptions.NSFW ?? false;
    this.CommandOptions.usage =
      this.CommandOptions.usage ?? `/${CommandOptions.name}`;
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
    this.CommandOptions.groups = this.CommandOptions.groups ?? null;
    this.CommandOptions.subcommands = this.CommandOptions.subcommands ?? null;
    this.CommandOptions.IgnoreCommand =
      this.CommandOptions.IgnoreCommand ?? false;

    if (!this.CommandOptions?.options.length) {
      if (this.CommandOptions?.groups != null)
        this.CommandOptions.options = this.GetSubCommandGroupOptions(
          this.CommandOptions.groups
        );
      else if (this.CommandOptions.subcommands != null)
        this.CommandOptions.options = this.GetSubCommandOptions(
          this.CommandOptions.subcommands
        );
    }
  }
  private GetSubCommandOptions(SubCommands: Record<string, SubOptions>) {
    const CommandNames = Object.keys(SubCommands);
    const CommandOptions = [];
    for (const name of CommandNames) {
      const CommandOption = {
        name,
        description: SubCommands[name].description,
        options: SubCommands[name].SubCommandOptions,
        userPermission: SubCommands[name].UserPermissions,
        botPermission: SubCommands[name].BotPermissions,
        type: ApplicationCommandOptionType.Subcommand,
        nsfw: SubCommands[name].NSFW,
        dmPermission: SubCommands[name].DMEnabled,
        autocomplete: SubCommands[name].autocomplete,
      };

      CommandOptions.push(CommandOption);
    }
    return CommandOptions;
  }
  private GetSubCommandGroupOptions(Groups: ApplicationCommandSubGroup) {
    const GroupNames = Object.keys(Groups);
    const GroupOptions = [];
    for (const name of GroupNames) {
      const GroupOption = {
        name,
        description: Groups[name].description,
        options: this.GetSubCommandGroupOptions(Groups[name].subcommands),
        userPermission: Groups[name].userPermission,
        botPermission: Groups[name].botPermission,
        type: ApplicationCommandOptionType.SubcommandGroup,
        nsfw: Groups[name].NSFW,
        dmPermission: Groups[name].dmEnabled,
      };
      GroupOptions.push(GroupOption);
    }
    return GroupOptions;
  }
}
