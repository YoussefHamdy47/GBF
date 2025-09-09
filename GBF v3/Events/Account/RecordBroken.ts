import { EmbedBuilder } from "discord.js";
import { CustomEvents } from "../../GBF/Data/ClientEvents";
import { RecordBrokenOptions } from "../../GBF/Timers/TimerHelper";
import { ColorCodes, Emojis, GBF, msToTime } from "../../Handler";

export function recordBroken(client: GBF) {
  client.on(CustomEvents.RecordBroken, async (options: RecordBrokenOptions) => {
    const recordEmbed = new EmbedBuilder();

    if (options.type === "Semester") {
      recordEmbed
        .setTitle(
          `${Emojis.crownAnimated} Record Broken ${Emojis.crownAnimated}`
        )
        .setColor(ColorCodes.Cherry)
        .setDescription(
          `You broke your current longest semester record! This semester's time was ${msToTime(
            options.semester.semesterTime * 1000
          )}`
        )
        .setTimestamp();
    }

    if (options.type === "Session") {
      recordEmbed
        .setTitle(`${Emojis.diamondSpin} Record Broken`)
        .setColor(ColorCodes.PastelRed)
        .setDescription(
          `You broke your current longest session record! This sessions's time was ${msToTime(
            options.sessionTime * 1000
          )}`
        )
        .setTimestamp();
    }

    return options.interaction.channel.send({
      content: `<@${options.interaction.user.id}>`,
      embeds: [recordEmbed],
    });
  });
}
