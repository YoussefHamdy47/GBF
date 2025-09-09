import { GBF } from "../GBF";
import { readdir } from "fs/promises";
import { join, resolve } from "path";
import { redBright } from "chalk";
import { MessageCommand } from "./Message Handler";
import { SlashCommand } from "./Slash Handler";
import { ContextCommand } from "./Context Handler";
import { GBFError } from "../../Utils/GBF Errors";

async function LoadCommand(client: GBF, FilePath: string) {
  try {
    const CommandModule = await import(FilePath);
    const CommandClass = CommandModule.default
      ? CommandModule.default
      : Object.values(CommandModule)[0];

    if (
      typeof CommandClass !== "function" ||
      (!(CommandClass.prototype instanceof MessageCommand) &&
        !(CommandClass.prototype instanceof SlashCommand) &&
        !(CommandClass.prototype instanceof ContextCommand))
    )
      throw new Error(
        redBright(
          `"${FilePath}" does not export a constructable class extending MessageCommand, SlashCommand or ContextCommand.`
        )
      );

    const CommandInstance: MessageCommand | SlashCommand | ContextCommand =
      new CommandClass(client);

    if (CommandClass.prototype instanceof MessageCommand) {
      const { name, aliases, description } = (CommandInstance as MessageCommand)
        .CommandOptions;

      if (!name)
        throw new Error(
          redBright(`"${FilePath}" does not have a name option.`)
        );

      if (!description)
        throw new Error(
          redBright(`"${FilePath}" does not have a description option.`)
        );

      if (client.MessageCommands.has(name))
        throw new Error(
          redBright(`The Message Command "${name}" exists twice.`)
        );

      if (aliases) {
        aliases.forEach((Alias) => {
          client.Aliases!.set(Alias, name);
        });
      }

      client.MessageCommands.set(name, CommandInstance as MessageCommand);
    } else if (CommandClass.prototype instanceof SlashCommand) {
      const { name } = (CommandInstance as SlashCommand).CommandOptions;

      if (!name) throw new Error(`"${FilePath}" does not have a name option.`);

      if (client.SlashCommands.has(name))
        throw new Error(redBright(`The Slash Command "${name}" exists twice.`));

      client.SlashCommands.set(name, CommandInstance as SlashCommand);
    } else {
      const { name } = (CommandInstance as ContextCommand).CommandOptions;

      if (!name) throw new Error(`"${FilePath}" does not have a name option.`);

      if (client.ContextCommands.has(name))
        throw new Error(
          redBright(`The Context Command "${name}" exists twice.`)
        );

      client.ContextCommands.set(name, CommandInstance as ContextCommand);
    }
  } catch (LoadError) {
    console.log(
      redBright(`• Command Load Error in "${FilePath}"\n${LoadError}`)
    );
  }
}
export async function RegisterCommands(
  client: GBF,
  ...dirs: string[]
): Promise<void> {
  await Promise.all(
    dirs.map(async (dir) => {
      try {
        const DirPath = resolve(__dirname, dir);
        const AvailableFiles = await readdir(DirPath, { withFileTypes: true });

        for (const File of AvailableFiles) {
          const FilePath = join(DirPath, File.name);

          if (File.name.includes("-ignore") || File.isDirectory()) {
            await RegisterCommands(client, FilePath);
          } else if (
            (File.name.endsWith(".ts") && !File.name.endsWith(".d.ts")) ||
            (File.name.endsWith(".js") && !File.name.endsWith(".js.map"))
          ) {
            await LoadCommand(client, FilePath);
          }
        }
      } catch (RegisterError) {
        throw new GBFError(
          `Encountered an error while attempting to access the directory "${dir}"\n${RegisterError}`
        );
      }
    })
  );
}
