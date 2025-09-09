import {
  ActivityType,
  ApplicationCommandOptionType,
  CommandInteractionOptionResolver,
  PresenceStatusData,
} from "discord.js";
import { SlashCommand } from "../Command Handlers/Slash Handler";
import { GBF, Emojis } from "../GBF";

export class SetPresence extends SlashCommand {
  constructor(client: GBF) {
    super(client, {
      name: "set_presence",
      category: "Developer",
      development: false,
      description: `Set this bot's presence`,
      options: [
        {
          name: "text",
          description: "The text to add to the presence",
          type: ApplicationCommandOptionType.String,
          maxLength: 128,
          minLength: 1,
          required: true,
        },
        {
          name: "type",
          description: "The type of activity",
          type: ApplicationCommandOptionType.String,
          choices: [
            { name: "Playing", value: "Playing" },
            { name: "Watching", value: "Watching" },
            { name: "Listening", value: "Listening" },
          ],
        },
        {
          name: "status",
          description: "The bot's status",
          type: ApplicationCommandOptionType.String,
          choices: [
            {
              name: "Online",
              value: "Online",
            },
            {
              name: "Idle",
              value: "Idle",
            },
            {
              name: "Do Not Disturb",
              value: "DND",
            },
            {
              name: "Invisible",
              value: "Invisible",
            },
          ],
        },
      ],
      DeveloperOnly: true,
      DMEnabled: false,
      async execute({ client, interaction }) {
        const ProvidedText = (
          interaction.options as CommandInteractionOptionResolver
        ).getString("text");
        const ProvidedPresence =
          (interaction.options as CommandInteractionOptionResolver).getString(
            "type"
          ) ?? "Playing";
        const ProvidedStatus =
          (interaction.options as CommandInteractionOptionResolver).getString(
            "status"
          ) ?? "Online";

        const ActivityTypeStrings: { [key: string]: ActivityType } = {
          Playing: ActivityType.Playing,
          Watching: ActivityType.Watching,
          Listening: ActivityType.Listening,
        };

        const StatusTypeStrings: { [key: string]: PresenceStatusData } = {
          Online: "online",
          Idle: "idle",
          DND: "dnd",
          Invisible: "invisible",
        };

        client.user.setPresence({
          activities: [
            {
              name: ProvidedText,
              type: ActivityTypeStrings[ProvidedPresence],
            },
          ],
          status: StatusTypeStrings[ProvidedStatus],
        });

        return interaction.reply({
          content: `${Emojis.Verify} Presence Updated!\n• ${ProvidedText}\n• ${ProvidedPresence}\n• ${ProvidedStatus}`,
          ephemeral: true,
        });
      },
    });
  }
}
