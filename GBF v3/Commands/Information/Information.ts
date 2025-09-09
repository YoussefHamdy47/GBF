import {
  ApplicationCommandOptionType,
  ChannelType,
  CommandInteractionOptionResolver,
  EmbedBuilder,
  GuildNSFWLevel,
  GuildPremiumTier,
  GuildVerificationLevel,
} from "discord.js";

import {
  SlashCommand,
  GBF,
  AvatarPriority,
  ImageData,
  UserAvatar,
  ColorCodes,
  msToTime,
} from "../../Handler";
import { UserInfo } from "../../GBF/Information/UserInfo";

export class Information extends SlashCommand {
  constructor(client: GBF) {
    super(client, {
      name: "info",
      description: "User & Server Information Command",
      category: "Information",
      cooldown: 5,
      subcommands: {
        avatar: {
          description: "Show a user's avatar",
          SubCommandOptions: [
            {
              name: "user",
              description: "The user to display their avatar",
              type: ApplicationCommandOptionType.User,
            },
            {
              name: "priority",
              description:
                "Show the Server or Global avatar as the main avatar",
              type: ApplicationCommandOptionType.String,
              choices: [
                {
                  name: "Global",
                  value: "Global",
                },
                {
                  name: "Server",
                  value: "Guild",
                },
              ],
            },
          ],
          DMEnabled: true,
          async execute({ client, interaction }) {
            const TargetUser =
              (interaction.options as CommandInteractionOptionResolver).getUser(
                "user",
                false
              ) || interaction.user;
            const AvatarPriority: AvatarPriority | string =
              (
                interaction.options as CommandInteractionOptionResolver
              ).getString("priority", false) || "Global";

            const GBFAvatar = new UserAvatar(
              client,
              TargetUser.id,
              interaction.guild ? interaction.guildId : null,
              ColorCodes.Default,
              AvatarPriority as AvatarPriority
            );

            return await interaction.reply({
              embeds: [GBFAvatar.GenerateEmbed()],
              components: [GBFAvatar.GetAvatarButtons()],
            });
          },
        },
        ["server-info"]: {
          description: "Get information about the current server",
          async execute({ client, interaction }) {
            const boostTierMap = {
              [GuildPremiumTier.None]: "Tier 0",
              [GuildPremiumTier.Tier1]: "Tier 1",
              [GuildPremiumTier.Tier2]: "Tier 2",
              [GuildPremiumTier.Tier3]: "Tier 3",
            };

            const NSFWMap = {
              [GuildNSFWLevel.Default]: "Default",
              [GuildNSFWLevel.Safe]: "Safe",
              [GuildNSFWLevel.Explicit]: "Explicit",
              [GuildNSFWLevel.AgeRestricted]: "Age Restricted",
            };

            const verificationMap = {
              [GuildVerificationLevel.None]: "Unrestricted",
              [GuildVerificationLevel.Low]:
                "Must have verified email on account",
              [GuildVerificationLevel.Medium]:
                "Must be registered on Discord for longer than 5 minutes",
              [GuildVerificationLevel.High]:
                "Must be a member of the server for longer than 10 minutes",
              [GuildVerificationLevel.VeryHigh]:
                "Must have a verified phone number",
            };

            const serverInfoEmbed = new EmbedBuilder()
              .setTitle(`${interaction.guild.name}`)
              .setThumbnail(
                interaction.guild.iconURL(ImageData) ??
                  "https://i.imgur.com/AWGDmiu.png"
              )
              .setDescription(
                `${interaction.guild.name} was created on ${`<t:${Math.round(
                  Math.round(interaction.guild.createdTimestamp) / 1000
                )}:F>`}`
              )
              .setColor(ColorCodes.Default)
              .addFields(
                {
                  name: "Total Members",
                  value: `${interaction.guild.memberCount.toLocaleString()}`,
                  inline: true,
                },
                {
                  name: "Total Humans",
                  value: `${interaction.guild.members.cache
                    .filter((member) => !member.user.bot)
                    .size.toLocaleString()}`,
                  inline: true,
                },
                {
                  name: "Total Bots",
                  value: `${interaction.guild.members.cache
                    .filter((member) => member.user.bot)
                    .size.toLocaleString()}`,
                  inline: true,
                },
                {
                  name: "Categories",
                  value: `${
                    interaction.guild.channels.cache.filter(
                      (c) => c.type == ChannelType.GuildCategory
                    ).size
                  }`,
                  inline: true,
                },
                {
                  name: "Text Channels",
                  value: `${
                    interaction.guild.channels.cache.filter(
                      (c) => c.type === ChannelType.GuildText
                    ).size
                  }`,
                  inline: true,
                },
                {
                  name: "Voice Channels",
                  value: `${
                    interaction.guild.channels.cache.filter(
                      (c) => c.type === ChannelType.GuildVoice
                    ).size
                  }`,
                  inline: true,
                },
                {
                  name: "Role Count",
                  value: `${interaction.guild.roles.cache.size.toLocaleString()}`,
                  inline: true,
                },
                {
                  name: "Boosts",
                  value: `${interaction.guild.premiumSubscriptionCount}`,
                  inline: true,
                },
                {
                  name: "Boost Tier",
                  value: `${boostTierMap[interaction.guild.premiumTier]}`,
                  inline: true,
                },
                {
                  name: "Explicit Content Filter",
                  value: `${NSFWMap[interaction.guild.nsfwLevel]}`,
                  inline: true,
                },
                {
                  name: "Verification Level",
                  value: `${
                    verificationMap[interaction.guild.verificationLevel]
                  }`,
                  inline: true,
                },
                {
                  name: "AFK Channel",
                  value: `${interaction.guild.afkChannel ?? "None"}`,
                  inline: true,
                },
                {
                  name: "AFK Timeout",
                  value: interaction.guild.afkChannel
                    ? `${msToTime(interaction.guild.afkTimeout * 1000)}`
                    : "None",
                  inline: true,
                },
                {
                  name: "Owner",
                  value: `<@${interaction.guild.ownerId}>`,
                  inline: true,
                },
                {
                  name: "Region",
                  value: `${interaction.guild.preferredLocale}`,
                  inline: true,
                }
              )
              .setFooter({
                text: `Server ID: ${interaction.guild.id}`,
                iconURL: interaction.guild.iconURL(ImageData),
              });

            if (interaction.guild.description)
              serverInfoEmbed.addFields({
                name: "Description",
                value: `${interaction.guild.description ?? "No description"}`,
                inline: true,
              });

            if (interaction.guild.bannerURL())
              serverInfoEmbed
                .addFields({
                  name: "Banner",
                  value: `[Banner URL](${interaction.guild.bannerURL()})`,
                  inline: true,
                })
                .setImage(interaction.guild.bannerURL(ImageData));

            function formatFeatures(arr: string[]): string {
              return arr
                .map((str) => {
                  const lowercased = str.toLowerCase();
                  const replaced = lowercased.replace(/_/g, " ");
                  const words = replaced.split(" ");
                  const formattedWords = words.map((word) => {
                    return word.charAt(0).toUpperCase() + word.slice(1);
                  });
                  return formattedWords.join(" ");
                })
                .join(", ");
            }

            if (interaction.guild.features.length > 0) {
              const GuildFeatures = formatFeatures(interaction.guild.features);
              serverInfoEmbed.addFields({
                name: "Features",
                value: `${GuildFeatures}`,
              });
            }
            return interaction.reply({
              embeds: [serverInfoEmbed],
            });
          },
        },
        userinfo: {
          description: "Get information about a user",
          SubCommandOptions: [
            {
              name: "user",
              description: "The user to get information about",
              type: ApplicationCommandOptionType.User,
            },
            {
              name: "secret",
              description: "Make the command only visible to you",
              type: ApplicationCommandOptionType.Boolean,
            },
          ],
          async execute({ client, interaction }) {
            const targetUser =
              (interaction.options as CommandInteractionOptionResolver).getUser(
                "user",
                false
              ) || interaction.user;

            const secret = (
              interaction.options as CommandInteractionOptionResolver
            ).getBoolean("secret", false);

            const userInfo = new UserInfo(
              client,
              targetUser.id,
              interaction.guild.id,
              ColorCodes.Default
            );

            return interaction.reply({
              embeds: [await userInfo.getUserInfo()],
              ephemeral: secret,
            });
          },
        },
      },
    });
  }
}
