import {
  User,
  GuildMember,
  Message,
  Interaction,
  EmbedBuilder,
  DMChannel,
  TextChannel,
  PermissionResolvable,
  Collection,
} from "discord.js";
import { Document } from "mongoose";
import { MessageCommand } from "../Command Handlers/Message Handler";
import { ColorCodes, Emojis, GBF } from "../GBF";
import { IBotBan } from "../Handler Models/Bot Ban Schema";
import { IGuildData } from "../Handler Models/Bot Settings Schema";
import { MissingPermissions } from "../../Utils/Utils";
import { SlashCommand } from "../Command Handlers/Slash Handler";
import { ContextCommand } from "../Command Handlers/Context Handler";

/** Represents a collection to store command cooldowns. */
const CommandCooldowns = new Collection();

export class HandlerChecks {
  private client: GBF;
  private CommandUser: User;
  private CommandMember: GuildMember;
  private UserBlacklistData: (IBotBan & Document<any, any, IBotBan>) | null;
  private GuildSettings: (IGuildData & Document<any, any, IGuildData>) | null;
  private Command: SlashCommand | MessageCommand | ContextCommand;
  private ActionType: Message | Interaction;

  constructor(
    client: GBF,
    commandUser: User,
    commandMember: GuildMember,
    userBlacklistData: (IBotBan & Document<any, any, IBotBan>) | null,
    guildSettings: (IGuildData & Document<any, any, IGuildData>) | null,
    command: SlashCommand | MessageCommand | ContextCommand,
    actionType: Interaction | Message
  ) {
    this.client = client;
    this.CommandUser = commandUser;
    this.CommandMember = commandMember;
    this.UserBlacklistData = userBlacklistData;
    this.GuildSettings = guildSettings;
    this.Command = command;
    this.ActionType = actionType;
  }

  public async RunChecks(): Promise<[EmbedBuilder, boolean]> {
    const ErrorEmbed = new EmbedBuilder()
      .setColor(ColorCodes.ErrorRed)
      .setTimestamp();

    const InGuild = !(this.ActionType.channel instanceof DMChannel);

    if (this.UserBlacklistData) {
      ErrorEmbed.setTitle(`${Emojis.Error} Account Ban Notice`).setDescription(
        `Your account has been banned from ${this.client.user.username} due to a violation of ${this.client.user.username}'s terms of service`
      );
      return [ErrorEmbed, false];
    }

    if (
      this.Command.CommandOptions.development &&
      !this.client.TestServers.includes(this.ActionType.guildId)
    ) {
      ErrorEmbed.setTitle(`${Emojis.Error} You can't use that`).setDescription(
        `${this.Command.CommandOptions.name} is disabled globally.`
      );
      return [ErrorEmbed, false];
    }

    if (!(this.Command instanceof ContextCommand))
      if (!this.Command.CommandOptions.DMEnabled && !InGuild) {
        ErrorEmbed.setTitle(
          `${Emojis.Error} You can't use that here`
        ).setDescription(
          `${this.Command.CommandOptions.name} is disabled in DMs.`
        );
        return [ErrorEmbed, false];
      }

    if (
      this.Command.CommandOptions.DeveloperOnly &&
      !this.client.Developers.includes(this.CommandUser.id)
    ) {
      ErrorEmbed.setTitle(`${Emojis.Error} You can't use that`).setDescription(
        `${this.Command.CommandOptions.name} is a developer only command.`
      );
      return [ErrorEmbed, false];
    }

    if (
      this.Command.CommandOptions.NSFW &&
      InGuild &&
      !(this.ActionType.channel as TextChannel).nsfw
    ) {
      ErrorEmbed.setTitle(`${Emojis.Error} You can't use that`).setDescription(
        `You cannot use NSFW commands in non-NSFW channels.`
      );
      return [ErrorEmbed, false];
    }

    if (InGuild) {
      if (
        this.Command.CommandOptions.UserPermissions &&
        !this.CommandMember.permissions.has(
          this.Command.CommandOptions.UserPermissions as PermissionResolvable,
          true
        )
      ) {
        ErrorEmbed.setTitle(
          `${Emojis.Error} You can't use that`
        ).setDescription(
          `${
            this.CommandUser.username
          }, You are missing the following permissions: ${MissingPermissions(
            this.CommandMember,
            this.Command.CommandOptions.UserPermissions
          )}`
        );

        return [ErrorEmbed, false];
      }

      if (
        this.Command.CommandOptions.BotPermissions &&
        !(this.ActionType.channel as TextChannel)
          .permissionsFor(this.ActionType.guild.members.me)
          .has(
            this.Command.CommandOptions.BotPermissions as PermissionResolvable,
            true
          )
      ) {
        ErrorEmbed.setTitle(`${Emojis.Error} You can't do that`).setDescription(
          `${
            this.CommandUser.username
          }, I am missing the following permissions: ${MissingPermissions(
            this.ActionType.guild.members.me,
            this.Command.CommandOptions.BotPermissions
          )}`
        );

        return [ErrorEmbed, false];
      }
    }
    if (
      !this.Command.CommandOptions.DeveloperBypass ||
      (this.Command.CommandOptions.DeveloperBypass &&
        !this.client.Developers.includes(this.CommandUser.id))
    ) {
      /**@unit Seconds */
      const ProvidedCooldownTime: number = this.Command.CommandOptions.cooldown;

      if (ProvidedCooldownTime) {
        if (!CommandCooldowns.has(this.Command.CommandOptions.name))
          CommandCooldowns.set(
            this.Command.CommandOptions.name,
            new Map<string, number>()
          );

        const CurrentTime = Date.now();
        const Timestamps: Map<string, number> = CommandCooldowns.get(
          this.Command.CommandOptions.name
        ) as Map<string, number>;
        /**@unit Miliseconds */
        const CooldownTime = ProvidedCooldownTime * 1000;

        if (Timestamps.has(this.CommandUser.id)) {
          const ExpirationTime =
            Timestamps.get(this.CommandUser.id)! + CooldownTime;

          if (CurrentTime < ExpirationTime) {
            const RemainingTime = Number(
              (ExpirationTime - CurrentTime).toFixed(1)
            );
            const UnixFormat = Math.round(
              Date.now() / 1000 + RemainingTime / 1000
            );

            ErrorEmbed.setTitle(
              `${Emojis.Error} You can't use that yet`
            ).setDescription(
              `${this.CommandUser}, You can use "${this.Command.CommandOptions.name}" <t:${UnixFormat}:R>`
            );
          }
        }
        Timestamps.set(this.CommandUser.id, CurrentTime);
        setTimeout(
          () => Timestamps.delete(this.CommandUser.id),
          CooldownTime
        ).unref();
      }
    }
    return [ErrorEmbed, true];
  }
}
