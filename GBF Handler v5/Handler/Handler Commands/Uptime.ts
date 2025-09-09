import { EmbedBuilder } from "discord.js";
import { SlashCommand } from "../Command Handlers/Slash Handler";
import { GBF, ColorCodes } from "../GBF";
import { msToTime } from "../../Utils/Utils";

export class Uptime extends SlashCommand {
  constructor(client: GBF) {
    super(client, {
      name: "uptime",
      category: "General",
      development: false,
      description: `${client.user.username}'s uptime`,
      cooldown: 5,
      async execute({ client, interaction }) {
        const UptimeEmbed = new EmbedBuilder()
          .setTitle(`${client.user.username}'s Uptime`)
          .setDescription(`${msToTime(client.uptime)}`)
          .setColor(ColorCodes.Default);

        return interaction.reply({
          embeds: [UptimeEmbed],
        });
      },
    });
  }
}
