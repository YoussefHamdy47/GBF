import {
  ApplicationCommandOptionType,
  CommandInteractionOptionResolver,
  EmbedBuilder,
} from "discord.js";
import { SlashCommand, GBF, GBFFun, ColorCodes } from "../../Handler";

export class SubCommandTemplate extends SlashCommand {
  constructor(client: GBF) {
    super(client, {
      name: "fun",
      description: 'All GBF commands under the title "fun"',
      category: "Fun",
      cooldown: 3,
      subcommands: {
        topic: {
          description: "Returns a random topic to keep the chat alive",
          async execute({ client, interaction }) {
            const topicCMD = GBFFun.TopicGenerate();

            return interaction.reply({
              embeds: [topicCMD],
            });
          },
        },
        kill: {
          description: "You murderer! 🔪",
          SubCommandOptions: [
            {
              name: "victim",
              description: "Your victim",
              type: ApplicationCommandOptionType.User,
            },
          ],
          async execute({ client, interaction }) {
            const targetUser =
              (interaction.options as CommandInteractionOptionResolver).getUser(
                "victim",
                false
              ) || interaction.user;

            const killCMD = GBFFun.Kill(
              targetUser,
              interaction.user,
              interaction.guild
            );

            return interaction.reply({
              content: killCMD,
            });
          },
        },
      ["8-ball"]: {
          description: "Ask the magic 8-ball 🎱",
          SubCommandOptions: [
            {
              name: "question",
              description: "Your question",
              type: ApplicationCommandOptionType.String,
              required: true,
            },
          ],
          async execute({ client, interaction }) {
            const userQuestion = (
              interaction.options as CommandInteractionOptionResolver
            ).getString("question", true);

            const eightBallCMD = GBFFun.EightBall(
              userQuestion,
              true,
              ColorCodes.Default
            );

            return interaction.reply({
              embeds: [eightBallCMD as EmbedBuilder],
            });
          },
        },
      },
    });
  }
}
