import {
  ButtonInteraction,
  InteractionReplyOptions,
  MessageFlags,
} from "discord.js";
import { CustomEvents } from "../../GBF/Data/ClientEvents";
import { GBF } from "../../Handler";
import {
  ButtonFixerOptions as ButtonChangeOptions,
  createTimerActionRow,
  TimerButtonID,
} from "../../GBF/Timers/TimerHelper";
import { Timers } from "../../GBF/Timers/Timers";

export function ButtonChanger(client: GBF) {
  client.on(
    CustomEvents.FixTimerButtons,
    async (
      client: GBF,
      interaction: ButtonInteraction,
      options: ButtonChangeOptions
    ) => {
      if (!options.isPaused) options.isPaused = false;
      
      try {
        const timers = await Timers.create(
          interaction.user.id,
          false,
          true,
          interaction,
          client
        );

        const sessionMessage = await timers.timerEvents.originalMessage();

        if (!sessionMessage) return;

        // Create a default state where all buttons are disabled
        const disabledButtons: Partial<Record<TimerButtonID, boolean>> = {
          [TimerButtonID.Start]: true,
          [TimerButtonID.Pause]: true,
          [TimerButtonID.Info]: true,
          [TimerButtonID.Stop]: true,
          [TimerButtonID.Unpause]: true,
        };

        // Enable the buttons based on the provided options
        options.enabledButtons.forEach((buttonId) => {
          disabledButtons[buttonId] = false;
        });

        // Create the action row with the updated button states
        const actionRow = createTimerActionRow(
          disabledButtons,
          options.isPaused
        );

        return sessionMessage.edit({
          components: [actionRow],
        });
      } catch (error) {
        const options: InteractionReplyOptions = {
          content: `I ran into an error\n\n${error.message}`,
          flags: MessageFlags.Ephemeral,
        };
        return interaction.replied
          ? interaction.followUp(options)
          : interaction.reply(options);
      }
    }
  );
}
