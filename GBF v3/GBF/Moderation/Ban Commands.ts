import {
  Client,
  EmbedBuilder,
  Guild,
  GuildMember,
  PermissionFlagsBits,
  Snowflake,
  User,
} from "discord.js";
import { ColorCodes, Emojis, GBF } from "../../Handler";

interface BanAddOptions {
  BanAdmins?: boolean;
  SelfBan?: boolean;
  DMUser?: boolean;
}

const DefaultBanOptions: BanAddOptions = {
  BanAdmins: false,
  DMUser: false,
  SelfBan: false,
};

/**
 * Delete the messages sent by the user in the previous days
 * @type {number}
 */
type DeleteDays = 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7;

export class GBFBan {
  /**
   * The client object
   * @type {GBF | Client}
   */
  public readonly client: GBF | Client;
  /**
   * The ID of the user to ban
   * @type {Snowflake}
   */
  public readonly UserID: Snowflake;
  /**
   * The ID of the guild where the ban is happening
   * @type {Snowflake}
   */
  public readonly GuildID: Snowflake;
  /**
   * The Guild where the ban is happening
   * @type {Guild}
   */
  private Guild: Guild;
  /**
   * The User Object to ban
   * @type {User}
   */
  public readonly TargetUser: User;
  /**
   * The GuildMember Object to ban
   * @type {GuildMember}
   */
  public readonly TargetMember: GuildMember;
  /**
   * The ID of the user who ran the command
   * @type {Snowflake}
   */
  public readonly CommandAuthorID: Snowflake;
  /**
   * The User Object for the command author
   * @type {User}
   */
  public readonly CommandAuthorUser: User;
  /**
   * The GuildMember Object for the command author
   * @type {GuildMember}
   */
  public readonly CommandAuthorMember: GuildMember;
  /**
   *
   * @param {GBF} client The client object
   * @param {Snowflake} UserID The ID of the target user to perform the actions on
   * @param {Snowflake} CommandAuthorID The ID of the user who ran the command
   * @param {Snowflake} GuildID The ID of the guild where the actions will be performed
   * @param {User} TargetUser The User object of the user to perform the actions on [Optional]
   * @param {Guild} TargetMember The GuildMember object of the member to perform the actions on [Optional]
   */
  constructor(
    client: GBF,
    UserID: Snowflake,
    CommandAuthorID: Snowflake,
    GuildID: Snowflake,
    TargetUser?: User,
    TargetMember?: GuildMember
  ) {
    this.client = client;
    this.UserID = UserID;
    this.CommandAuthorID = CommandAuthorID;
    this.GuildID = GuildID;

    this.Guild = this.client.guilds.cache.get(this.GuildID);

    if (!this.Guild)
      throw new Error(`[Ban Command] The provided Guild ID is invalid.`);

    if (!TargetUser) this.TargetUser = this.client.users.cache.get(UserID);
    else this.TargetUser = TargetUser;

    if (!this.TargetUser)
      throw new Error(`[Ban Command] The provided User is invalid.`);

    if (!TargetMember) this.TargetMember = this.Guild.members.cache.get(UserID);
    else this.TargetMember = TargetMember;

    this.CommandAuthorUser = this.client.users.cache.get(this.CommandAuthorID);

    if (!this.CommandAuthorUser)
      throw new Error(`[Ban Command] Invalid Command Author ID`);

    this.CommandAuthorMember = this.Guild.members.cache.get(
      this.CommandAuthorID
    );
  }

  /**
   * Ban a user in the provided guild
   * @param BanReason The reason for the ban
   * @param MessageDeleteDays Delete the messages the user sent in the previous days
   * @param BanOptions An object of options to customize your experience with this built-in command!
   * @returns {EmbedBuilder} Message Embed
   */
  async BanAdd(
    BanReason: string = "No Reason Provided",
    MessageDeleteDays: DeleteDays = 0,
    BanOptions: BanAddOptions = DefaultBanOptions
  ): Promise<EmbedBuilder> {
    if (MessageDeleteDays > 0) MessageDeleteDays = MessageDeleteDays * 86400;

    const GuildBans = await this.Guild.bans.fetch();

    const ErrorEmbed = new EmbedBuilder()
      .setTitle(`${Emojis.Error} You can't do that`)
      .setColor(ColorCodes.ErrorRed);

    ErrorEmbed.setDescription(
      `${this.TargetUser.username} is already banned in ${this.Guild.name}`
    );

    if (GuildBans && GuildBans.get(this.UserID)) return ErrorEmbed;

    if (this.CommandAuthorID === this.UserID && BanOptions.SelfBan) {
      ErrorEmbed.setDescription(`You cannot ban yourself.`);
      return ErrorEmbed;
    }

    if (this.TargetMember) {
      if (this.Guild.ownerId === this.UserID) {
        ErrorEmbed.setDescription(`You cannot ban the server's owner.`);
        return ErrorEmbed;
      }

      if (this.UserID === this.client.user.id) {
        ErrorEmbed.setDescription(`I cannot ban myself.`);
        return ErrorEmbed;
      }

      if (
        BanOptions.BanAdmins &&
        this.TargetMember.permissions.has(PermissionFlagsBits.Administrator) &&
        this.CommandAuthorID !== this.Guild.ownerId
      ) {
        ErrorEmbed.setDescription(`I cannot ban server admins.`);
        return ErrorEmbed;
      }

      const BotHierarchy = this.Guild.members.me.roles.highest.position;
      const TargetHierarchy = this.TargetMember.roles.highest.position;

      if (!this.CommandAuthorMember)
        throw new Error(
          `[Ban Command] The specified Command Author ID is invalid.`
        );

      const AuthorHierarchy = this.CommandAuthorMember.roles.highest.position;

      if (this.CommandAuthorID !== this.Guild.ownerId) {
        if (AuthorHierarchy <= TargetHierarchy) {
          ErrorEmbed.setDescription(
            `${this.TargetUser.username}'s role hierarchy is higher or equal to yours.`
          );
          return ErrorEmbed;
        }

        if (BotHierarchy <= TargetHierarchy) {
          ErrorEmbed.setDescription(
            `${this.TargetUser.username}'s role hierarchy is higher or equal to mine.`
          );
          return ErrorEmbed;
        }
      }
    }

    const UserBanned = new EmbedBuilder()
      .setTitle(`${Emojis.Verify} User Banned`)
      .setColor(ColorCodes.Default)
      .addFields(
        {
          name: "Moderator:",
          value: `${this.CommandAuthorUser} [${this.CommandAuthorUser.id}]`,
          inline: true,
        },
        {
          name: "User",
          value: `${this.TargetUser} [${this.TargetUser.id}]`,
          inline: true,
        },
        {
          name: "Reason",
          value: BanReason,
        },
        {
          name: "Messages Sent in the past days will be deleted",
          value: `${MessageDeleteDays} Days`,
        }
      );

    const BannedMessage = new EmbedBuilder()
      .setTitle(`📩 Message Received`)
      .setColor(ColorCodes.Default)
      .setDescription(`You have been banned from ${this.Guild.name}`)
      .addFields({
        name: "Reason",
        value: BanReason,
      });

    try {
      await this.TargetUser.send({
        embeds: [BannedMessage],
      });
    } catch (DMError) {}

    await this.Guild.bans.create(this.UserID, {
      deleteMessageSeconds: MessageDeleteDays,
      reason: BanReason,
    });

    return UserBanned;
  }

  async BanRemove(
    UnBanReason: string = "No Reason Provided"
  ): Promise<EmbedBuilder> {
    const ErrorEmbed = new EmbedBuilder()
      .setTitle(`${Emojis.Error} You can't do that`)
      .setColor(ColorCodes.ErrorRed);
    if (this.TargetMember) return ErrorEmbed;
  }
}
