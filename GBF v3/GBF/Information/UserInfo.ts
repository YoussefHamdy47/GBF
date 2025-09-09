import {
  Client,
  ColorResolvable,
  EmbedBuilder,
  Guild,
  GuildMember,
  hyperlink,
  ImageURLOptions,
  RoleMention,
  type Snowflake,
  User,
  UserFlags,
} from "discord.js";
import {
  chooseRandomFromArray,
  ColorCodes,
  KeyPerms,
  trimArray,
  type GBF,
} from "../../Handler";
import { UserActivity } from "./Activity RPC";

const TipMessages = [
  "The user color is clickable",
  "You can see which song the user is listening to",
  "If you click the song name it will take you to the song on Spotfiy",
  "Blue text means you can click it",
  `This command also works for user's outside of this server`,
  `If the user has the "Administrator" permission it will only show that in Key Permissions`,
  `This command works best for server members`,
  `The joined server and Discord timestamps are in UNIX which means they will update forever`,
  `We use a "smart" system, that means *some* fields will not show if empty to avoid clutter`,
  `If the user doesn't have any roles that field will not show`,
  `If you click the user ID it will take you to their profile`,
  `This command was made using GBF's powerful handler`,
];

const displayTipMessage = chooseRandomFromArray(TipMessages);

const badgesMap = {
  "": "None",
  [UserFlags[UserFlags.ActiveDeveloper]]: "<:ActiveDev:1325638276123590717>",
  [UserFlags[UserFlags.Staff]]: "<:D_Staff:826951574088581140>",
  [UserFlags[UserFlags.Partner]]: "<:D_Partner:826951266357608558>",
  [UserFlags[UserFlags.BugHunterLevel1]]: "<:D_BugHunter:826950777176195123>",
  [UserFlags[UserFlags.Hypesquad]]: "<:D_HypeSquadEvents:826950445695500358>",
  [UserFlags[UserFlags.HypeSquadOnlineHouse1]]:
    "<:D_Bravery:826950151444103178>",
  [UserFlags[UserFlags.HypeSquadOnlineHouse2]]:
    "<:D_Brilliance:826949675038801981>",
  [UserFlags[UserFlags.HypeSquadOnlineHouse3]]:
    "<:D_Balance:826948943178760212>",
  [UserFlags[UserFlags.PremiumEarlySupporter]]:
    "<:D_Earlysupporter:826949248607977474>",
  [UserFlags[UserFlags.VerifiedBot]]:
    "Verified Bot <:verifiedbot:972938255291015248>",
  [UserFlags[UserFlags.VerifiedDeveloper]]:
    "<:D_VerifBotDev:826952303042101288>",
  [UserFlags[UserFlags.BotHTTPInteractions]]: "",
  [UserFlags[UserFlags.TeamPseudoUser]]: "",
  [UserFlags[UserFlags.BugHunterLevel2]]: "<:bugHunter2:973146470494662677>",
  [UserFlags[UserFlags.CertifiedModerator]]:
    "<:certifiedMod:973146823814418492>",
};

const statusesMap = {
  "": "None",
  online: "<:online:934570010901377025> Online",
  idle: "<:idle:934569895474114611> Idle",
  dnd: "<:dnd:934569539776155718> Do Not Disturb",
  streaming: "<:streaming:934569539054731324> Streaming",
  offline: "<:invs:934569539197341717> Offline",
};

const devicesMap = {
  "": "None",
  web: "🌐",
  desktop: "💻",
  mobile: "📱",
};

export class UserInfo {
  public readonly client: GBF | Client;
  public readonly targetUser: User;
  public readonly userID: Snowflake;
  public readonly guildID?: Snowflake;
  public readonly targetMember: GuildMember;
  public readonly guild: Guild;

  public readonly embedColor: ColorResolvable;

  public readonly imageSettings: ImageURLOptions = {
    extension: "jpeg",
    size: 1024,
  };

  public userCreatedTimestamp: number;
  public userJoinedTimestamp: number;
  public statusText: string;
  public userDevice: string;
  public highestRole: string;
  public userRoles: RoleMention[];
  public userBadges: string;

  constructor(
    client: GBF | Client,
    userID: Snowflake,
    guildID: Snowflake,
    embedColor: ColorResolvable = ColorCodes.Default
  ) {
    this.client = client;
    this.userID = userID;
    this.guildID = guildID;

    this.guild = this.getGuild();
    this.targetUser = this.getUser();
    this.targetMember = this.getMember();

    this.embedColor = embedColor;

    this.userCreatedTimestamp = Math.round(
      this.targetUser.createdTimestamp / 1000
    );
  }

  private getGuild() {
    return this.client.guilds.cache.get(this.guildID);
  }

  private getUser() {
    return this.client.users.cache.get(this.userID);
  }

  private getMember() {
    return this.guild.members.cache.get(this.userID);
  }

  private handleNonMember(): EmbedBuilder | null {
    if (this.targetMember) return null;

    const userEmbed = new EmbedBuilder()
      .setAuthor({
        name: `${this.targetUser.username}`,
        iconURL: this.targetUser.displayAvatarURL(this.imageSettings),
      })
      .setThumbnail(this.targetUser.displayAvatarURL(this.imageSettings))
      .addFields(
        {
          name: "Account Created:",
          value: `<t:${this.userCreatedTimestamp}:F>, <t:${this.userCreatedTimestamp}:R>`,
          inline: true,
        },
        {
          name: "Username:",
          value: `${this.targetUser.username}`,
          inline: true,
        },
        {
          name: "Account Type:",
          value: `${this.targetUser.bot ? "Bot" : "Human"}`,
          inline: true,
        },
        {
          name: "ID:",
          value: `${this.targetUser.id}`,
          inline: true,
        },
        {
          name: "\u200b",
          value: "\u200b",
          inline: true,
        },
        {
          name: "Profile URL:",
          value: `${hyperlink(
            this.targetUser.username,
            `https://discord.com/users/${this.targetUser.id}`
          )}`,
          inline: true,
        }
      )
      .setColor(this.embedColor)
      .setFooter({
        text: `TIP: This command shows more information about server members`,
        iconURL: `https://emoji.gg/assets/emoji/2487-badge-serverbooster9.png`,
      });

    return userEmbed;
  }

  private async updateValues(): Promise<void> {
    this.userJoinedTimestamp = Math.round(
      this.targetMember.joinedTimestamp / 1000
    );

    this.statusText = this.targetMember.presence
      ? statusesMap[this.targetMember.presence.status]
      : "<:invs:934569539197341717> Offline";

    this.userDevice =
      this.targetMember.presence &&
      !this.targetUser.bot &&
      Object.keys(this.targetMember.presence.clientStatus).length > 0
        ? devicesMap[Object.keys(this.targetMember.presence.clientStatus)[0]]
        : "";

    this.highestRole = this.targetMember.roles.highest.id
      ? this.targetMember.roles.highest.id === this.guildID
        ? "@everyone"
        : `<@&${this.targetMember.roles.highest.id}>`
      : "User Has No Roles";

    this.userRoles = trimArray(
      this.targetMember.roles.cache
        .sort((a, b) => b.position - a.position)
        .map((role) => role.toString())
        .slice(0, -1)
    );
    this.userBadges =
      (await this.targetUser.fetchFlags())
        .toArray()
        .map((flag) => badgesMap[flag])
        .join(" ") || "None";
  }

  public async getUserInfo(): Promise<EmbedBuilder> {
    await this.updateValues();

    if (!this.targetMember) return this.handleNonMember();

    const userInfoEmbed = new EmbedBuilder()
      .setAuthor({
        name: `${this.targetUser.username} ${this.userDevice}`,
        iconURL: this.targetUser.displayAvatarURL(this.imageSettings),
      })
      .setColor(this.targetMember.displayHexColor ?? ColorCodes.Default)
      .setThumbnail(this.targetUser.displayAvatarURL(this.imageSettings))
      .setFooter({
        text: `TIP: ${displayTipMessage}`,
        iconURL: `https://emoji.gg/assets/emoji/2487-badge-serverbooster9.png`,
      })
      .addFields(
        {
          name: "User Badges:",
          value: `${this.userBadges}`,
          inline: false,
        },
        {
          name: "Joined Discord:",
          value: `<t:${this.userCreatedTimestamp}:F>, <t:${this.userCreatedTimestamp}:R>`,
          inline: true,
        },
        {
          name: "Joined Server:",
          value: `<t:${this.userJoinedTimestamp}:F>, <t:${this.userJoinedTimestamp}:R>`,
          inline: true,
        },
        {
          name: "Nickname:",
          value: `${
            this.targetMember.nickname
              ? this.targetMember.nickname
              : this.targetUser.username
          }`,
          inline: true,
        },
        {
          name: "Account Type:",
          value: `${this.targetUser.bot ? "Bot" : "Human"}`,
          inline: true,
        },
        {
          name: "Color:",
          value: `[${
            this.targetMember.displayHexColor
          }](${`https://www.color-hex.com/color/${this.targetMember.displayHexColor.slice(
            1
          )}`})`,
          inline: true,
        },
        {
          name: "ID:",
          value: `[${
            this.targetMember.id
          }](${`https://discord.com/users/${this.targetMember.id}`})`,
          inline: true,
        },
        {
          name: "Highest Role:",
          value: `${this.highestRole}`,
          inline: true,
        },
        {
          name: "Status:",
          value: `${this.statusText}`,
          inline: true,
        }
      );

    if (this.userRoles.length > 0)
      userInfoEmbed.addFields({
        name: `Roles [${this.userRoles.length}]:`,
        value: `${this.userRoles}`,
        inline: false,
      });

    const PublicAvatar = `[Public Avatar](${this.targetUser.displayAvatarURL(
      this.imageSettings
    )})`;
    const ServerAvatar = `[Server Avatar](${this.targetMember.displayAvatarURL(
      this.imageSettings
    )})`;

    const AvatarLinks =
      this.targetMember.displayAvatarURL() !==
      this.targetUser.displayAvatarURL()
        ? `${PublicAvatar} | ${ServerAvatar}`
        : PublicAvatar;

    userInfoEmbed.setDescription(AvatarLinks);

    if (this.targetMember.premiumSinceTimestamp !== null)
      userInfoEmbed.addFields({
        name: `Boosting "${this.targetMember.guild.name}" Since:`,
        value: `<t:${Math.round(
          this.targetMember.premiumSinceTimestamp / 1000
        )}:F>, <t:${Math.round(
          this.targetMember.premiumSinceTimestamp / 1000
        )}:R>`,
      });

    if ((KeyPerms(this.targetMember)[1] as number) > 0) {
      userInfoEmbed.addFields({
        name: `Key Permissions [${KeyPerms(this.targetMember)[1]}]:`,
        value: `${KeyPerms(this.targetMember)[0]}`,
        inline: false,
      });
    }

    try {
      const userPresence = new UserActivity(
        this.client,
        this.targetUser.id,
        this.guildID
      );

      const userActivity = userPresence.getActivityFields();

      if (userActivity.length) userInfoEmbed.addFields(userActivity);
    } catch (error) {
      userInfoEmbed.addFields({
        name: "Presence:",
        value: `${error.message}`,
      });
    }

    return userInfoEmbed;
  }
}
