import { EmbedBuilder } from "discord.js";
import { CustomEvents } from "../../GBF/Data/ClientEvents";
import { LevelUpOptions } from "../../GBF/Timers/TimerHelper";
import { ColorCodes, GBF } from "../../Handler";
import { rankUpEmoji, rpRequired } from "../../GBF/Timers/LevelEngine";

export function LevelUpAccount(client: GBF) {
  client.on(CustomEvents.AccountLevelUp, async (options: LevelUpOptions) => {
    const newRank =
      options.levelUps !== 0
        ? options.userData.Rank + options.levelUps
        : options.userData.Rank + 1;

    const rankUpEmbed = new EmbedBuilder()
      .setTitle(`${rankUpEmoji(newRank)} Ranked Up`)
      .setColor(ColorCodes.Default)
      .setDescription(
        `• New Account Rank: ${newRank.toLocaleString(
          "en-US"
        )}\n• RP Required to reach rank ${newRank + 1}: ${
          options.carryOverXP ? options.carryOverXP.toLocaleString("en-US") : 0
        } / ${rpRequired(newRank + 1).toLocaleString(
          "en-US"
        )}\n• Number of Rank Ups: ${options.levelUps.toLocaleString("en-US")}`
      )
      .setTimestamp();

    return options.interaction.channel.send({
      content: `<@${options.interaction.user.id}>`,
      embeds: [rankUpEmbed],
    });
  });
}
