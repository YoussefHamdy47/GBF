import { Events, GuildMember, Interaction } from "discord.js";
import { Document } from "mongoose";
import { redBright } from "chalk";
import { GBF } from "../GBF";
import { IBotBan, BotBanModel } from "../Handler Models/Bot Ban Schema";
import {
  IGuildData,
  BotGuildModel,
} from "../Handler Models/Bot Settings Schema";
import { HandlerChecks } from "../Utils/Handler Features";
import { SlashCommand } from "../Command Handlers/Slash Handler";
import { ContextCommand } from "../Command Handlers/Context Handler";
import { Engine } from "../../Utils/Engine";

export async function GBFInteractionCreate(client: GBF) {
  client.on(Events.InteractionCreate, async (interaction: Interaction) => {
    if (interaction.user.bot) return;

    const BanData: (IBotBan & Document<any, any, IBotBan>) | null =
      client.DatabaseInteractions
        ? await BotBanModel.findOne({ userId: interaction.user.id })
        : null;

    const GuildSettings: (IGuildData & Document<any, any, IGuildData>) | null =
      !interaction.inGuild() || !client.DatabaseInteractions
        ? null
        : (await BotGuildModel.findOne({ guildID: interaction.guildId })) ||
          (await new BotGuildModel({ guildID: interaction.guildId }).save());

    let SlashCommand: SlashCommand | ContextCommand;

    if (interaction.isAutocomplete()) {
      SlashCommand = client.SlashCommands.get(interaction.commandName);

      if (!SlashCommand) return;

      const SubGroup = interaction.options.getSubcommandGroup(false);
      const SubCommand = interaction.options.getSubcommand(false);

      if (SubGroup || SubCommand) {
        const SubCMD = SlashCommand.CommandOptions.groups
          ? SlashCommand.CommandOptions.groups[SubGroup]?.subcommands[
              SubCommand
            ]
          : SlashCommand.CommandOptions.subcommands[SubCommand];

        if (SubCMD && SubCMD.autocomplete) {
          return await Engine.HandleAutoComplete(interaction, SubCMD);
        }
      } else {
        if (SlashCommand.CommandOptions.autocomplete) {
          return await Engine.HandleAutoComplete(interaction, SlashCommand);
        }
      }
    }

    if (
      (interaction.isCommand() && interaction.isUserContextMenuCommand()) ||
      interaction.isMessageContextMenuCommand()
    ) {
      SlashCommand = client.ContextCommands.get(interaction.commandName);

      if (!SlashCommand) return;

      const HandlerCheck = await new HandlerChecks(
        client,
        interaction.user,
        interaction.member as GuildMember,
        BanData,
        GuildSettings,
        SlashCommand,
        interaction
      ).RunChecks();

      if (!HandlerCheck[1]) {
        interaction.reply({
          embeds: [HandlerCheck[0]],
          ephemeral: true,
        });
        return;
      }

      try {
        await SlashCommand.CommandOptions.execute({ client, interaction });
      } catch (ExecuteError) {
        console.log(
          redBright(
            `• Error in "${SlashCommand.CommandOptions.name}"\n${ExecuteError}`
          )
        );
      }
    }

    if (interaction.isChatInputCommand()) {
      SlashCommand = client.SlashCommands.get(interaction.commandName);

      if (!SlashCommand) return;

      const HandlerCheck = await new HandlerChecks(
        client,
        interaction.user,
        interaction.member as GuildMember,
        BanData,
        GuildSettings,
        SlashCommand,
        interaction
      ).RunChecks();

      if (!HandlerCheck[1]) {
        interaction.reply({
          embeds: [HandlerCheck[0]],
          ephemeral: true,
        });
        return;
      }

      const SubGroup = interaction.options.getSubcommandGroup(false);
      const SubCommand = interaction.options.getSubcommand(false);

      try {
        if (
          SlashCommand.CommandOptions.groups ||
          SlashCommand.CommandOptions.subcommands
        ) {
          const SubCMD = SlashCommand.CommandOptions.groups
            ? SlashCommand.CommandOptions.groups[SubGroup]?.subcommands[
                SubCommand
              ]
            : SlashCommand.CommandOptions.subcommands[SubCommand];

          if (SubCMD && SubCMD.execute) {
            return await SubCMD.execute({
              client,
              interaction,
              SubGroup,
              SubCommand,
            });
          }
        }
        await SlashCommand.CommandOptions.execute({
          client,
          interaction,
          //@ts-ignore
          SubGroup,
          SubCommand,
        });
      } catch (ExecuteError) {
        console.log(
          redBright(
            `• Error in "${SlashCommand.CommandOptions.name}"\n${ExecuteError}`
          )
        );
      }
    }
  });
}
