import {
  ApplicationCommandOptionType,
  CommandInteractionOptionResolver,
  EmbedBuilder,
  TextChannel,
  hyperlink,
} from "discord.js";
import { SlashCommand } from "../Command Handlers/Slash Handler";
import { ColorCodes, Emojis, GBF } from "../GBF";
import { BotBanModel } from "../Handler Models/Bot Ban Schema";

export class BotBan extends SlashCommand {
  constructor(client: GBF) {
    super(client, {
      name: "bot_ban",
      description: `Ban / Un-ban a user from using ${client.user.username}`,
      category: "Developer",
      development: false,
      DMEnabled: false,
      DeveloperOnly: true,
      subcommands: {
        add: {
          description: `Ban a user from using ${client.user.username}`,
          SubCommandOptions: [
            {
              name: "user",
              description: "The user to ban",
              type: ApplicationCommandOptionType.User,
              required: true,
            },
            {
              name: "reason",
              description: "The reason for the ban",
              type: ApplicationCommandOptionType.String,
            },
          ],
          async execute({ client, interaction }) {
            if (!client.DatabaseInteractions)
              return interaction.reply({
                content: `This command cannot function without a database.`,
                ephemeral: true,
              });

            const TargetUser = (
              interaction.options as CommandInteractionOptionResolver
            ).getUser("user");
            const BanReason = (
              interaction.options as CommandInteractionOptionResolver
            ).getString("reason");

            const BanData = await BotBanModel.findOne({
              UserID: TargetUser.id,
            });

            if (BanData)
              return interaction.reply({
                content: `${TargetUser.username} is already banned.`,
                ephemeral: true,
              });

            const BanDoc = new BotBanModel({
              UserID: TargetUser.id,
              Reason: BanReason || "No reason provided.",
              Developer: interaction.user.id,
              Timestamp: Date.now(),
            });

            await BanDoc.save();

            const BanEmbed = new EmbedBuilder()
              .setTitle(`${Emojis.Verify} Success`)
              .setColor(ColorCodes.Default)
              .addFields(
                {
                  name: "User",
                  value: `${TargetUser.username} [${TargetUser.id}]`,
                  inline: true,
                },
                {
                  name: "Developer",
                  value: `${interaction.user.username} [${interaction.user.id}]`,
                  inline: true,
                },
                {
                  name: "Reason",
                  value: `${BanReason}`,
                  inline: true,
                }
              )
              .setTimestamp();

            let AppealMessage = "";

            if (client.AppealURL.length)
              AppealMessage = `If you believe this decision was made in error or wish to appeal, you may do so ${hyperlink(
                "here",
                client.AppealURL
              )}.`;
            else
              AppealMessage = `This action is final, and no appeals will be considered.`;

            const DMEmbed = new EmbedBuilder()
              .setTitle(`📩 You have received a new message`)
              .setDescription(
                `You have been permanently banned from using ${
                  client.user.username
                }.\n• Reason: ${BanReason}\nDate: <t:${Math.floor(
                  Date.now() / 1000
                )}:F>\n${AppealMessage}`
              )
              .setColor(ColorCodes.Default)
              .setTimestamp();

            try {
              await TargetUser.send({
                embeds: [DMEmbed],
              });
            } catch (DMError) {
              console.log(`• I couldn't DM ${TargetUser.username}\n${DMError}`);
            }

            await interaction.reply({
              embeds: [BanData],
            });

            if (client.LogsChannel) {
              if (client.LogsChannel.length >= 1) {
                for (const ChannelID of client.LogsChannel) {
                  const LogsChannel = client.channels.cache.get(
                    ChannelID
                  ) as TextChannel;

                  await LogsChannel.send({
                    embeds: [BanEmbed],
                  });
                }
              }
            }
          },
        },
      },
    });
  }
}
