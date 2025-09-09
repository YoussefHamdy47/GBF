import dotenv from "dotenv";
import path from "path";
import fs from "fs";
import { redBright, yellowBright } from "chalk";
import { Config } from "../Handler/types";
import {
  AutocompleteInteraction,
  CommandInteractionOptionResolver,
} from "discord.js";
import { SlashCommand } from "../Handler";

dotenv.config();

export class Engine {
  static ReadFiles(Dir: string, FileExtensions: string[] = []): string[] {
    const Files: string[] = [];

    function ReadDir(CurrentDir: string) {
      const Entries = fs.readdirSync(CurrentDir, {
        withFileTypes: true,
      });

      Entries.forEach((Entry) => {
        const FilePath = path.join(CurrentDir, Entry.name);

        if (Entry.isDirectory()) ReadDir(FilePath);
        else {
          const FileExtension = path.extname(FilePath).toLowerCase();

          if (
            !FileExtensions ||
            (FileExtensions.includes(FileExtension) &&
              !Entry.name.includes("d.ts"))
          )
            Files.push(FilePath);
        }
      });
    }

    ReadDir(Dir);
    return Files;
  }

  static LoadConfig(JSONConfigPath?: string): Config {
    const JSONPath =
      JSONConfigPath || path.join(__dirname, "config/config.json");

    try {
      const JSONConfig = require(JSONPath);
      return {
        ...JSONConfig,
        MongoURI: JSONConfig.MongoURI,
        TOKEN: JSONConfig.TOKEN,
      };
    } catch (JSONError) {
      if (JSONConfigPath) {
        console.log(
          redBright(`• Unable to load config from "${JSONConfigPath}"`)
        );
        process.exit(1);
      }
      return {
        MongoURI: process.env.MongoURI,
        TOKEN: process.env.TOKEN,
      };
    }
  }

  static async HandleAutoComplete(
    interaction: AutocompleteInteraction,
    command: SlashCommand | any
  ) {
    const autocomplete = command?.autocomplete;

    if (!autocomplete) {
      process.emitWarning(
        yellowBright(
          `• Warning: The autocomplete command ${command?.name} does not have an autocomplete method.`
        ),
        {
          code: "AutoCompleteData",
          detail: "Invalid input for the autocomplete method",
        }
      );
      return;
    }

    const FocusedOption = (
      interaction.options as CommandInteractionOptionResolver
    ).getFocused(true);

    const AvailableChoices: string[] = await autocomplete(
      interaction,
      FocusedOption.name
    );

    if (AvailableChoices.length > 25) {
      process.emitWarning(
        `• Warning: The provided autocomplete options exceed 25 elements, please reduce them or they will automatically be reduced, this might delete some crucial data.`,
        {
          code: "AutoCompleteExcess",
          detail: "Exceeded autocomplete options limit",
        }
      );
    }

    const FilteredChoices = AvailableChoices.filter((choice: string) => {
      return choice.toLowerCase().startsWith(FocusedOption.value.toLowerCase());
    }).slice(0, 25);

    await interaction.respond(
      FilteredChoices.map((choice: string) => ({
        name: choice,
        value: choice,
      }))
    );
  }
}
