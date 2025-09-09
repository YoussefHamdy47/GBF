import { ApplicationCommandType } from "discord.js";
import { ColorCodes, ContextCommand, GBF, UserAvatar } from "../../Handler";

export class AvatarContext extends ContextCommand {
  constructor(client: GBF) {
    super(client, {
      name: "Show Avatar",
      ContextType: ApplicationCommandType.User,
      category: "General Context",
      cooldown: 5,
      async execute({ client, interaction }) {
        const Avatar = new UserAvatar(
          client,
          interaction.user.id,
          interaction.guildId,
          ColorCodes.Default,
          "Global"
        );

        return await interaction.reply({
          embeds: [Avatar.GenerateEmbed()],
          components: [Avatar.GetAvatarButtons()],
        });
      },
    });
  }
}
