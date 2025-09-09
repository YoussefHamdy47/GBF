import {
  ActionRowBuilder,
  ButtonBuilder,
  ButtonInteraction,
  ButtonStyle,
  CommandInteraction,
  EmbedBuilder,
  Events,
  InteractionReplyOptions,
  MessageFlags,
} from "discord.js";
import { ColorCodes, Emojis, GBF, msToTime } from "../../Handler";
import {
  ButtonFixerOptions,
  TimerButtonID,
  TimerEventsReturns,
  messageURL,
} from "../../GBF/Timers/TimerHelper";
import { Timers } from "../../GBF/Timers/Timers";
import { CustomEvents } from "../../GBF/Data/ClientEvents";

export function TimerEngine(client: GBF) {
  client.on(
    Events.InteractionCreate,
    async (interaction: ButtonInteraction) => {
      if (!interaction.isButton()) return;

      try {
        const timers = await Timers.create(
          interaction.user.id,
          false,
          true,
          interaction,
          client
        );

        const timerEvents = timers.timerEvents;

        if (timerEvents.checkID()) return;

        const timerEmbed = new EmbedBuilder();

        if (interaction.customId === TimerButtonID.Start) {
          const startHandler = await timers.timerEvents.handleStart();

          if (startHandler === TimerEventsReturns.TimerAlreadyRunning) {
            timerEmbed
              .setTitle("You can't use that")
              .setColor(ColorCodes.ErrorRed)
              .setDescription(startHandler)
              .setTimestamp();

            const fixerOptions: ButtonFixerOptions = {
              enabledButtons: [
                TimerButtonID.Stop,
                TimerButtonID.Pause,
                TimerButtonID.Info,
              ],
              isPaused: false,
            };

            client.emit(
              CustomEvents.FixTimerButtons,
              client,
              interaction,
              fixerOptions
            );

            return interaction.reply({
              embeds: [timerEmbed],
              flags: MessageFlags.Ephemeral,
            });
          }

          timerEmbed
            .setTitle(
              `${Emojis.Verify} ${timers.timerData.sessionData.sessionTopic}`
            )
            .setColor(ColorCodes.Default)
            .setDescription(
              `Timer started ${
                interaction.user.username
              }, user the buttons on the [original message](<${messageURL(
                timers.timerData.sessionData.guildID,
                timers.timerData.sessionData.channelID,
                timers.timerData.sessionData.messageID
              )}>), best of luck.`
            )
            .setTimestamp();

          const fixerOptions: ButtonFixerOptions = {
            enabledButtons: [
              TimerButtonID.Stop,
              TimerButtonID.Pause,
              TimerButtonID.Info,
            ],
            isPaused: false,
          };

          client.emit(
            CustomEvents.FixTimerButtons,
            client,
            interaction,
            fixerOptions
          );

          return interaction.reply({
            embeds: [timerEmbed],
          });
        }

        if (interaction.customId === TimerButtonID.Info) {
          const infoHandler = await timers.timerEvents.handleTimerInfo();

          timerEmbed
            .setTitle(
              `Session Stats | ${timers.timerData.sessionData.sessionTopic}`
            )
            .setDescription(infoHandler)
            .setColor(ColorCodes.Default)
            .setFooter({
              text: "Active Break Time is not counted in the total break time.",
            })
            .setTimestamp();

          return interaction.reply({
            embeds: [timerEmbed],
          });
        }

        if (interaction.customId === TimerButtonID.Pause) {
          const pauseHandler = await timers.timerEvents.handlePause();

          if (pauseHandler === TimerEventsReturns.TimerAlreadyPaused) {
            timerEmbed
              .setTitle("You can't do that")
              .setColor(ColorCodes.ErrorRed).setDescription(`${pauseHandler}
              \nTo unpause use the buttons on the [original message](<${messageURL(
                timers.timerData.sessionData.guildID,
                timers.timerData.sessionData.channelID,
                timers.timerData.sessionData.messageID
              )}>)`);

            return interaction.reply({
              embeds: [timerEmbed],
              flags: MessageFlags.Ephemeral,
            });
          }

          if (pauseHandler === TimerEventsReturns.TimerNotStarted) {
            timerEmbed
              .setTitle("You can't do that")
              .setColor(ColorCodes.ErrorRed)
              .setDescription(pauseHandler);

            return interaction.reply({
              embeds: [timerEmbed],
              flags: MessageFlags.Ephemeral,
            });
          }

          timerEmbed
            .setTitle(`${Emojis.Verify} Timer Paused`)
            .setColor(ColorCodes.Default)
            .setDescription(
              `The timer has been paused, time spent from now till unpause won't be counted.
          \nTo unpause use the buttons on the [original message](<${messageURL(
            timers.timerData.sessionData.guildID,
            timers.timerData.sessionData.channelID,
            timers.timerData.sessionData.messageID
          )}>).`
            )
            .setTimestamp();

          return interaction.reply({
            embeds: [timerEmbed],
          });
        }

        if (interaction.customId === TimerButtonID.Unpause) {
          const unpauseHandler = await timers.timerEvents.handleUnpause();

          if (unpauseHandler === TimerEventsReturns.TimerNotPaused) {
            timerEmbed
              .setTitle("You can't do that")
              .setColor(ColorCodes.ErrorRed)
              .setDescription(`${unpauseHandler}`);

            return interaction.reply({
              embeds: [timerEmbed],
              flags: MessageFlags.Ephemeral,
            });
          }

          if (unpauseHandler === TimerEventsReturns.TimerNotStarted) {
            timerEmbed
              .setTitle("You can't do that")
              .setColor(ColorCodes.ErrorRed)
              .setDescription(unpauseHandler);

            return interaction.reply({
              embeds: [timerEmbed],
              flags: MessageFlags.Ephemeral,
            });
          }

          timerEmbed
            .setTitle(`${Emojis.Verify} Timer Unpaused`)
            .setColor(ColorCodes.Default)
            .setDescription(
              `• Break Time: ${msToTime(
                unpauseHandler * 1000
              )}\n• Break Number: ${
                timers.timerData.sessionData.numberOfBreaks
              }`
            )
            .setTimestamp();

          return interaction.reply({
            embeds: [timerEmbed],
          });
        }

        if (interaction.customId === TimerButtonID.Stop) {
          const endHandler = await timerEvents.handleStop();

          if (endHandler === TimerEventsReturns.TimerNotStarted) {
            timerEmbed
              .setTitle("You can't use that")
              .setColor(ColorCodes.ErrorRed)
              .setDescription(endHandler)
              .setTimestamp();

            return interaction.reply({
              embeds: [timerEmbed],
              flags: MessageFlags.Ephemeral,
            });
          }

          if (endHandler === TimerEventsReturns.CannotStopPaused) {
            timerEmbed
              .setTitle("You can't use that")
              .setColor(ColorCodes.ErrorRed)
              .setDescription(endHandler)
              .setTimestamp();

            return interaction.reply({
              embeds: [timerEmbed],
              flags: MessageFlags.Ephemeral,
            });
          }

          timerEmbed
            .setTitle(
              `${Emojis.Verify} Session Ended | ${timers.timerData.sessionData.sessionTopic}`
            )
            .setDescription(endHandler)
            .setColor(ColorCodes.Default)
            .setTimestamp();

          return interaction.reply({
            embeds: [timerEmbed],
          });
        }
      } catch (error) {
        const errorEmbed = new EmbedBuilder()
          .setTitle("An error occurred")
          .setColor(ColorCodes.ErrorRed)
          .setDescription(
            `I ran into an error :(\n\n\`\`\`js\n${error.message}\`\`\``
          )
          .setTimestamp();

        const replyOptions: InteractionReplyOptions = {
          embeds: [errorEmbed],
          flags: MessageFlags.Ephemeral,
        };

        return interaction.replied || interaction.deferred
          ? interaction.followUp(replyOptions)
          : interaction.reply(replyOptions);
      }
    }
  );
}
