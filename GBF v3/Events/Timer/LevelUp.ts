import { EmbedBuilder } from "discord.js";
import { CustomEvents } from "../../GBF/Data/ClientEvents";
import { LevelUpOptions } from "../../GBF/Timers/TimerHelper";
import { ColorCodes, GBF } from "../../Handler";
import { rankUpEmoji, xpRequired } from "../../GBF/Timers/LevelEngine";

export function LevelUpSemester(client: GBF) {
  client.on(CustomEvents.SemesterLevelUp, async (options: LevelUpOptions) => {
    const newLevel =
      options.timerData.currentSemester.semesterLevel +
      (options.levelUps != 0 ? options.levelUps : 1);

    const levelUpEmbed = new EmbedBuilder()
      .setTitle(`${rankUpEmoji(newLevel)} Leveled Up`)
      .setColor(ColorCodes.Default)
      .setDescription(
        `• New Semester Level: ${newLevel.toLocaleString(
          "en-US"
        )}\n• XP Required to reach level ${newLevel + 1}: ${
          options.carryOverXP ? options.carryOverXP.toLocaleString("en-US") : 0
        } / ${xpRequired(newLevel + 1).toLocaleString(
          "en-US"
        )}\n• Number of Level Ups: ${options.levelUps.toLocaleString("en-US")}`
      )
      .setTimestamp();

    return options.interaction.channel.send({
      content: `<@${options.interaction.user.id}>`,
      embeds: [levelUpEmbed],
    });
  });
}
