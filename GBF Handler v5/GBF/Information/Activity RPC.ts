import {
  ActivityType,
  Client,
  ColorResolvable,
  EmbedBuilder,
  EmbedField,
  Guild,
  GuildMember,
  type Snowflake,
} from "discord.js";
import { ColorCodes, Emojis, GBF, msToTime } from "../../Handler";

/**
 * A utility class to handle and format a Discord user's activities
 * @class UserActivity
 * @example
 * // Create a new instance
 * const activity = new UserActivity(client, userId, guildId);
 *
 * // Get formatted embed
 * const embed = activity.getActivity();
 * channel.send({ embeds: [embed] });
 *
 * // Get raw data
 * const rawData = activity.getRawActivityData();
 */
export class UserActivity {
  /** Discord client instance */
  public readonly client: GBF | Client;
  /** Target user's Discord ID */
  public readonly userID: Snowflake;
  /** Guild/Server ID */
  public readonly guildID?: Snowflake;
  /** Custom color for embeds */
  public readonly embedColor?: ColorResolvable;

  /** Guild member instance of target user */
  private targetMember?: GuildMember;
  /** Guild instance */
  private userGuild?: Guild;
  /** Whether the target user is a bot */
  private userBot?: boolean;

  /**
   * Creates an instance of UserActivity
   * @param {GBF | Client} client - Discord client instance
   * @param {Snowflake} userID - ID of the user to fetch activities for
   * @param {Snowflake} guildID - ID of the guild/server
   * @param {ColorResolvable} [embedColor] - Custom color for the embeds
   * @throws {Error} When user ID is invalid
   * @throws {Error} When user has no presence data
   */
  constructor(
    client: GBF | Client,
    userID: Snowflake,
    guildID: Snowflake,
    embedColor?: ColorResolvable
  ) {
    this.client = client;
    this.userID = userID;
    this.guildID = guildID;
    this.embedColor = embedColor;

    this.userGuild = this.client.guilds.cache.get(this.guildID);

    this.targetMember = this.userGuild.members.cache.get(this.userID);

    if (!this.targetMember) throw new Error(`Invalid user ID Snowflake.`);

    if (this.targetMember.user.bot) this.userBot = true;

    if (!this.targetMember.presence)
      throw new Error(
        `${this.targetMember.user.username} is not doing anything.`
      );
  }

  /**
   * Handles activity data for bot users
   * @private
   * @returns {EmbedField[] | null | string} Formatted activity fields for bots or status message
   */
  private handleBot(): EmbedField[] | null | string {
    if (!this.userBot) return null;

    const activities = this.targetMember.presence.activities;

    if (!activities.length)
      return `${this.targetMember.user.username} is not doing anything.`;

    const activityDetails: EmbedField[] = activities.map((activity) => ({
      name: `${activityType[activity.type]}:`,
      value: activity.name || "No details",
      inline: true,
    }));

    return activityDetails;
  }

  /**
   * Gets formatted activity fields for the embed
   * @private
   * @returns {EmbedField[]} Array of formatted embed fields
   */
  private handleUser(): EmbedField[] | null | string {
    if (this.userBot) return null;

    const activities = this.targetMember.presence.activities;

    if (!activities.length)
      return `${this.targetMember.user.username} is not doing anything.`;

    // Stores all of the user's activities since they can have multiple
    const fields: EmbedField[] = [];

    for (const activity of activities) {
      // Data for the embed
      let name: string;
      let value: string;
      let inline = true;

      switch (activity.type) {
        // The user's custom status, the message that they write in their status
        case ActivityType.Custom: {
          name = activityType[activity?.type] ?? "Custom Status";
          const emoji = activity.emoji?.toString() ?? ""; //Emoji will only work if the bot is in the same server that the emoji exists in
          const state = activity.state ?? "";
          value = `${emoji} ${state}`;
          break;
        }
        // What the user is listening to, the song, artist, album, and how much time is left in the song
        case ActivityType.Listening:
          // activity.name here is the name of the app, this was only tested with spotify giving "Spotify"
          name = `${
            activity.name
              ? `Listening on ${activity.name} 🎧`
              : "Listening to Music 🎧"
          }`;

          // The song name, if it's not available  for some reason, it will say "Unknown Track"
          const trackName = activity.details ?? "Unknown Track";
          // When testing, the artist was separated by a semicolon, so I replaced it with a comma
          const artist = activity.state?.replace(";", ",") ?? "Unknown Artist";
          const album = activity.assets?.largeText ?? "Unknown Album";

          // Calculating the remaining time of the song
          const songEndTimestamp = activity.timestamps?.end?.getTime();

          const RemainingTime = songEndTimestamp
            ? songEndTimestamp - Date.now()
            : null;

          const songEndedTimeStamp = RemainingTime
            ? msToTime(Math.abs(RemainingTime))
            : "Just Ended";

          value = `**[${trackName}](${`https://open.spotify.com/search/${encodeURIComponent(
            trackName
          )}`})**\nby [${artist}](${`https://open.spotify.com/search/${encodeURIComponent(
            artist
          )}`})\non [${album}](${`https://open.spotify.com/search/${encodeURIComponent(
            album
          )}`})\nTrack ends in: ${songEndedTimeStamp}`;

          break;
        // What the user is playing i.e. Minecraft, Valorant, etc.
        case ActivityType.Playing: {
          let gameName: string = activity.name ?? "Unknown Game"; // Name of the game
          let gameDetails: string = activity.details ?? "No Details"; // Details i.e. in Customs etc.
          let gameState: string = activity.state ?? "No State"; // State i.e. in a match, in the lobby etc.
          let startedPlaying: string;

          let playTimeText: string;
          const { timestamps } = activity;

          // Handling play time text
          switch (true) {
            case timestamps?.start !== null: {
              const startedPlayingTS = new Date(timestamps.start).getTime();

              const currentTime = Date.now();
              startedPlaying = msToTime(
                Math.abs(startedPlayingTS - currentTime)
              );
              playTimeText = `Current Session:`;
              break;
            }
            case timestamps?.start === null || timestamps?.end !== null: {
              startedPlaying = "";
              playTimeText = "In-Game";
              break;
            }
            default: {
              startedPlaying = "Just Started";
              playTimeText = `Current Session:`;
              break;
            }
          }

          name = `Playing ${gameName}`;
          value =
            gameDetails !== "" && gameState !== ""
              ? `🕑 ${playTimeText} ${startedPlaying}\n\n${gameDetails}\n${gameState}`
              : `${playTimeText} ${startedPlaying}`;
        }
      }
      fields.push({ name, value, inline });
    }
    return fields;
  }

  /**
   * Gets formatted activity fields for the embed
   * @private
   * @returns {EmbedField[]} Array of formatted embed fields
   */
  private getActivityFields(): EmbedField[] {
    const botActivities = this.handleBot();
    if (Array.isArray(botActivities)) return botActivities;

    const userActivities = this.handleUser();
    if (Array.isArray(userActivities)) return userActivities;

    return [];
  }

  /**
   * Gets raw activity data without Discord formatting
   * @returns {ActivityData[]} Array of activity data objects
   * @example
   * const activity = new UserActivity(client, userId, guildId);
   * const rawData = activity.getRawActivityData();
   * // Filter gaming activities
   * const games = rawData.filter(act => act.type === 'Playing');
   */
  public getRawActivityData(): ActivityData[] {
    if (!this.targetMember?.presence?.activities.length) {
      return [];
    }

    return this.targetMember.presence.activities.map((activity) => {
      const baseData: ActivityData = {
        type: activityType[activity.type] || "Unknown",
        name: activity.name,
        details: activity.details,
        state: activity.state,
        emoji: activity.emoji?.toString(),
        timestamps: {
          start: activity.timestamps?.start
            ? new Date(activity.timestamps.start)
            : undefined,
          end: activity.timestamps?.end
            ? new Date(activity.timestamps.end)
            : undefined,
        },
        url: activity.url,
      };

      return Object.fromEntries(
        Object.entries(baseData).filter(([_, v]) => v !== undefined)
      ) as ActivityData;
    });
  }

  /**
   * Gets a fully formatted Discord embed with all user activities
   * @returns {EmbedBuilder} Discord embed containing activity information
   * @example
   * const activity = new UserActivity(client, userId, guildId);
   * const embed = activity.getActivity();
   * channel.send({ embeds: [embed] });
   */
  public getActivity() {
    const userActivity = new EmbedBuilder()
      .setColor(this.embedColor || ColorCodes.Default)
      .setTitle(`${this.targetMember.user.username}'s Activity`);

    const fields = this.getActivityFields();

    if (fields.length === 0)
      userActivity.setDescription(
        `${this.targetMember.user.username} is not doing anything.`
      );
    else userActivity.addFields(fields);

    return userActivity;
  }
}

/**
 * Represents raw activity data
 * @interface ActivityData
 */
interface ActivityData {
  /** Activity type (Playing, Listening, etc.) */
  type: string;
  /** Name of the activity */
  name?: string;
  /** Additional activity details */
  details?: string;
  /** Current activity state */
  state?: string;
  /** Custom status emoji */
  emoji?: string;
  /** Activity timestamps */
  timestamps?: {
    /** When the activity started */
    start?: Date;
    /** When the activity ends/ended */
    end?: Date;
  };
  /** Associated URL if any */
  url?: string;
}

/**
 * Mapping of activity type numbers to readable strings
 * @constant
 */
const activityType = {
  0: "Playing",
  1: "Streaming",
  2: "Listening",
  3: "Watching",
  4: "Custom",
  5: "Competing",
};
