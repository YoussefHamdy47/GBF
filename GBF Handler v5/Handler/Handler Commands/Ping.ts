import { EmbedBuilder } from "discord.js";
import { SlashCommand } from "../Command Handlers/Slash Handler";
import { GBF, ColorCodes } from "../GBF";

export class PingCommand extends SlashCommand {
  constructor(client: GBF) {
    super(client, {
      name: "ping",
      category: "General",
      development: false,
      description: `Check ${client.user.username}'s ping! 🏓`,
      cooldown: 5,
      async execute({ client, interaction }) {
        const PingEmbed = new EmbedBuilder()
          .setTitle(`Pong 🏓`)
          .setDescription(
            `API Latency: \`${Math.round(client.ws.ping).toLocaleString(
              "en-US"
            )}\` ms`
          )
          .setColor(ColorCodes.Default);

        return interaction.reply({
          embeds: [PingEmbed],
        });
      },
    });
  }
}
