import {
  ActionRowBuilder,
  ButtonBuilder,
  ButtonStyle,
  Client,
  ColorResolvable,
  EmbedBuilder,
  Guild,
  GuildMember,
  ImageURLOptions,
  type Snowflake,
  User,
  hyperlink,
} from "discord.js";
import { GBF } from "../../Handler/GBF";

export const ImageData: ImageURLOptions = {
  extension: "jpeg",
  size: 1024,
};

export type AvatarPriority = "Global" | "Guild";

const DefaultColor: ColorResolvable = "Blurple";

export class UserAvatar {
  public readonly client: GBF | Client;
  public readonly UserID: Snowflake;
  public readonly GuildID?: Snowflake;

  public readonly EmbedColor?: ColorResolvable;

  private TargetUser?: User;
  private TargetMember?: GuildMember;
  private UserGuild?: Guild;

  private DisplayName: string;

  private readonly IsAvatarDifferent: boolean;

  private readonly AvatarPriority?: AvatarPriority;

  constructor(
    client: Client | GBF,
    UserID: Snowflake,
    GuildID: Snowflake,
    EmbedColor: ColorResolvable = DefaultColor,
    AvatarPriority: AvatarPriority = "Global"
  ) {
    this.UserID = UserID;
    this.GuildID = GuildID;
    this.EmbedColor = EmbedColor;
    this.AvatarPriority = AvatarPriority;

    if (this.GuildID) {
      const CachedGuild = client.guilds.cache.get(this.GuildID);

      if (CachedGuild) this.UserGuild = CachedGuild;
      else throw new Error("Provided Guild ID is invalid.");
    }

    const CachedUser = client.users.cache.get(this.UserID);

    if (CachedUser) this.TargetUser = CachedUser;
    else throw new Error("Provided User ID is invalid.");

    let CachedMember: GuildMember;

    if (this.GuildID) {
      CachedMember = this.UserGuild?.members.cache.get(this.UserID);

      if (CachedMember) this.TargetMember = CachedMember;
    } else CachedMember = null;

    if (
      this.TargetMember &&
      this.TargetMember.displayAvatarURL() !==
        this.TargetUser.displayAvatarURL()
    )
      this.IsAvatarDifferent = true;
    else this.IsAvatarDifferent = false;

    this.DisplayName = this.TargetMember
      ? this.TargetMember.displayName
      : this.TargetUser.displayName;
  }

  /**
   * Returns a string with Avatar URLs
   *
   * @returns {string} - The string that contains the user's Avatar URLs
   * @example GetAvatarURL()
   * //Output: Global Avatar | Server Avatar => Users with 2 different avatars
   * //Output: Global Avatar => Users with only a global avatar
   */
  public GetAvatarString(): string {
    let AvatarString = `${hyperlink(
      "Global Avatar",
      this.TargetUser.displayAvatarURL(ImageData)
    )}`;

    if (this.IsAvatarDifferent)
      AvatarString += ` | ${hyperlink(
        "Server Avatar",
        this.TargetMember.displayAvatarURL(ImageData)
      )}`;

    return AvatarString;
  }

  /**
   * Returns the Embed with the user's Avatar
   * Tip: You can update it by accessing the embed
   *
   * @param {boolean} ShowBothAvatars - Default: False, If true, it's going to return the Embed with both Image and Thumbnail fields
   * @returns {EmbedBuilder} - The Embed that contains the Data
   * @example
   * //To Edit the Embed:
   * const Embed = GenerateEmbed();
   *
   * Embed.setTitle("Updated Title").setFooter({ text: "Hello, World!" });
   */
  public GenerateEmbed(ShowBothAvatars: boolean = false): EmbedBuilder {
    const UserAvatarEmbed = new EmbedBuilder()
      .setTitle(`${this.DisplayName}'s Avatar`)
      .setDescription(this.GetAvatarString())
      .setColor(this.EmbedColor);

    if (ShowBothAvatars && this.AvatarPriority === "Guild") {
      UserAvatarEmbed.setImage(this.GetAvatar(ShowBothAvatars));
      UserAvatarEmbed.setThumbnail(this.GetAvatar(false));
    } else if (ShowBothAvatars && this.AvatarPriority === "Global") {
      UserAvatarEmbed.setImage(this.GetAvatar(false));
      UserAvatarEmbed.setThumbnail(this.GetAvatar(ShowBothAvatars));
    } else UserAvatarEmbed.setImage(this.GetAvatar(false));

    return UserAvatarEmbed;
  }

  /**
   * Returns the user's Avatar string
   *
   * @param {boolean} GetGuild - To return the user's Guild Avatar
   * @returns {string} - The avatar URL
   */
  public GetAvatar(GetGuild: boolean = false): string {
    if (GetGuild && this.IsAvatarDifferent)
      return this.TargetMember.displayAvatarURL(ImageData);
    else return this.TargetUser.displayAvatarURL(ImageData);
  }

  /**
   * Generates a row of buttons linking to the user's avatar(s).
   *
   * @returns {ActionRowBuilder<ButtonBuilder>} - An ActionRow containing button(s) for the user's avatar(s).
   * @example
   * const avatarButtons = userAvatar.GetAvatarButtons();
   * // Returns an ActionRowBuilder with buttons linking to the user's global and possibly server avatars.
   */
  public GetAvatarButtons(): ActionRowBuilder<ButtonBuilder> {
    const AvatarButtons = new ButtonBuilder()
      .setLabel("Global Avatar")
      .setStyle(ButtonStyle.Link)
      .setURL(this.TargetUser.displayAvatarURL(ImageData));

    const AvatarButtonsRow: ActionRowBuilder<ButtonBuilder> =
      new ActionRowBuilder<ButtonBuilder>().addComponents(AvatarButtons);

    if (this.TargetMember && this.IsAvatarDifferent) {
      AvatarButtonsRow.addComponents(
        new ButtonBuilder()
          .setLabel("Server Avatar")
          .setStyle(ButtonStyle.Link)
          .setURL(this.TargetMember.displayAvatarURL(ImageData))
      );
    }

    return AvatarButtonsRow;
  }
}
