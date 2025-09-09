import {
  ApplicationCommandOptionData,
  ApplicationCommandType,
  AutocompleteInteraction,
  BitFieldResolvable,
  ChatInputCommandInteraction,
  ColorResolvable,
  CommandInteraction,
  GatewayIntentBits,
  Message,
  MessageContextMenuCommandInteraction,
  Snowflake,
  UserContextMenuCommandInteraction,
} from "discord.js";
import { BuiltInCommands, BuiltInEvents, GBF } from "./GBF";

export type AppConfig = {
  /**
   * The bot's authentication token
   * @type {string}
   */
  TOKEN: string;
  /**
   * The bot's mongo connection UR
   * @type {string}
   */
  MongoURI?: string;
};

/**
 * If all is specified, every event will be ignored, the array must contain the names of the event functions
 */
export type IgnoreEvents = string[] | "All";

export interface IGBF {
  /**
   * The default success / verify emoji
   * @type {Snowflake | string}
   */
  VerifyEmoji?: Snowflake | string;
  /**
   * The default error emoji
   * @type {Snowflake | string}
   */
  ErrorEmoji?: Snowflake | string;
  /**
   * The default color that will be used for arrays
   * @type {ColorResolvable}
   */
  DefaultColor?: ColorResolvable;
  /**
   * An array of Test Server IDs, those are where development commands will register.
   * @type {Snowflake[]}
   */
  TestServers?: Snowflake[];
  /**
   * An array of Developer IDs, those are the users who can use DeveloperOnly commands.
   * @type {Snowflake[]}
   */
  Developers?: Snowflake[];
  /**
   * The path to your commands folder, Eg. path.join(__dirname, "./commands").
   * @type {string}
   */
  CommandsFolder?: string;
  /**
   * The path to your commands folder, Eg. path.join(__dirname, "./events").
   * @type {string}
   */
  EventsFolder?: string;
  /**
   * Your bot's default prefix.
   * @type {string}
   */
  Prefix?: string;
  /**
   * An array of prefixes that your bot will respond to, the Prefix is already added to this array.
   * @type {string}
   */
  Prefixes?: string[];
  /**
   * Whether to log handler actions like the number of commands registered etc.
   * @type {boolean}
   */
  LogActions?: boolean;
  /**
   * Whether to automatically login using the TOKEN and MongoURI found in the config, without having to use the login method.
   * @type {boolean}
   */
  AutoLogin?: boolean;
  /**
   * Whether commands are usable in Direct Messages by default
   * @type {boolean}
   */
  DMEnabled?: boolean;
  /**
   * The URL to your bot's ban appeal page, if it exists.
   * @type {string}
   */
  AppealURL?: string;
  /**
   * An array of strings that contains all of the event functions that will be disabled on runtime.
   * @type {IgnoreEvents}
   */
  IgnoredEvents?: IgnoreEvents;
  /**
   * The path to your bot's JSON Config path, if you want to use .env, don't specify this.
   * @type {AppConfig | string}
   */
  BotConfig?: AppConfig | string;
  /**
   * The URL to your bot's support server.
   * @type {string}
   */
  SupportServer?: string;
  /**
   * Your bot's version.
   * @type {string}
   */
  Version?: string;
  /**
   * An array of the Handler Events that you don't want to register.
   * @type {BuiltInEvents[]}
   */
  DisabledHandlerEvents?: BuiltInEvents[];
  /**
   * An array of the Handler Commands that you don't want to register.
   * @type {BuiltInCommands[]}
   */
  DisabledHandlerCommands?: BuiltInCommands[];
  /**
   * An array of command names that you don't want to register.
   * @type {string[]}
   */
  DisabledCommands?: string[];
  /**
   * An array of logs channels to log any important events that happen to your bot's system like a user gets banned from using the bot.
   * @type {Snowflake[]}
   */
  LogsChannel?: Snowflake[];
  /**
   * Whether to have any database interactions, this is recommended to have since most of the bot's features use a Database.
   * @type {boolean}
   */
  DatabaseInteractions?: boolean;
  /**
   * The intents that your bot will use, don't use more than you need to preserve memory.
   * @type {GatewayIntentBits[] | BitFieldResolvable<string, number>}
   */
  intents: GatewayIntentBits[] | BitFieldResolvable<string, number>;
}

export interface MessageCommandExecute {
  /**
   * The client instance
   * @type {GBF};
   */
  client: GBF;
  /**
   * The message object
   * @type {Message}
   */
  message: Message;
  /**
   * An array of the user's input argument split
   * @type {unknown[]}
   */
  args?: unknown[];
}
export interface MessageCommandOptions {
  /**
   * The Message Command's name.
   * @type {string}
   * @required True
   * @default undefined
   */
  name: string;
  /**
   * The Message Command's description.
   * @type {string}
   * @required True
   * @default undefined
   */
  description: string;
  /**
   * The aliases that can be used to trigger the command.
   * @type {string[]}
   * @default []
   */
  aliases?: string[];
  /**
   * The Message Command's category.
   * @type {string}
   * @default ""
   */
  category?: string;
  /**
   * If the Message Command can be used in non-NSFW channels or not.
   * @type {boolean}
   * @default False
   */
  NSFW?: boolean;
  /**
   * The Message Command's usage example.
   * @type {string}
   * @default "${client.Prefix}${Command.name}"
   */
  usage?: string;
  /**
   * The number of seconds the user must wait before they can re-use the command.
   * @type {number}
   * @default 0
   * @unit Seconds
   */
  cooldown?: number;
  /**
   * The permissions that the user requires to run the Message Command.
   * @type {bigint[]}
   * @default []
   */
  UserPermissions?: bigint[];
  /**
   * The permissions that the bot requires to run the Message Command.
   * @type {bigint[]}
   * @default []
   */
  BotPermissions?: bigint[];
  /**
   * If only developers can use this command.
   * @type {boolean}
   * @default False
   */
  DeveloperOnly?: boolean;
  /**
   * If developers can bypass the cooldown
   * @type {boolean}
   * @default False
   */
  DeveloperBypass?: boolean;
  /**
   * If enabled, the command will only register in Test Servers
   * @type {boolean}
   * @default False
   */
  development?: boolean;
  /**
   * If the command is usable in Direct Messages
   * @type {boolean}
   * @default Client.DMEnabled
   */
  DMEnabled?: boolean;
  /**
   * If the command will get ignored by the handler
   * @type {boolean}
   * @default False
   */
  IgnoreCommand?: boolean;
  /**
   * The execute function, this contains the command's code
   * @param {
   *  client: GBF,
   *  message: Message,
   *  args: unknown[]
   * }
   * @returns {Promise<Message | any> | Message | any}
   */
  execute?: ({
    client,
    message,
    args,
  }: MessageCommandExecute) => Promise<Message | any> | Message | any;
}

export interface SlashExecute {
  /**
   * The client instance
   * @type {GBF}
   */
  client: GBF;
  /**
   * The interaction object
   * @type {CommandInteraction | ChatInputCommandInteraction}
   */
  interaction: ChatInputCommandInteraction;
}

export interface ContextExecute {
  /**
   * The client instance
   * @type {GBF}
   */
  client: GBF;
  /**
   * The interaction object
   * @type {UserContextMenuCommandInteraction | MessageContextMenuCommandInteraction}
   */
  interaction:
    | UserContextMenuCommandInteraction
    | MessageContextMenuCommandInteraction;
}

export interface SubOptions {
  /**
   * The SubCommand's description.
   * @type {string}
   * @default undefined
   */
  description: string;
  /**
   * The options for the subcommand.
   * @type {ApplicationCommandOptionData[]}
   * @default []
   */
  SubCommandOptions?: ApplicationCommandOptionData[];
  /**
   * The permissions that the user requires to run the subcommand.
   * @type {bigint[]}
   * @default []
   */
  UserPermissions?: bigint[];
  /**
   * The permissions that the bot requires to run the subcommand.
   * @type {bigint[]}
   * @default []
   */
  BotPermissions?: bigint[];
  /**
   * If the command is usable in non-NSFW channels.
   * @type {boolean}
   * @default False
   */
  NSFW?: boolean;
  /**
   * If the command is usable in Direct Messages.
   * @type {boolean}
   * @default Client.DMEnabled
   */
  DMEnabled?: boolean;
  /**
   * The options for the autocomplete command
   * @param interaction [The autocomplete interaction object]
   * @param option [The option for the autocomplete interaction]
   * @returns {string[]}
   */
  autocomplete?: (
    interaction: AutocompleteInteraction,
    option: string
  ) => string[] | Promise<string[]>;
  /**
   *
   * @param {
   * client: GBF,
   * interaction: CommandInteraction
   * }
   * @returns {Promise<CommandInteraction | any> | any}
   */
  execute?: ({
    client,
    interaction,
  }: SlashExecute) =>
    | Promise<CommandInteraction | any>
    | CommandInteraction
    | any;
}

export interface SlashOptions {
  /**
   * The name of the Slash Command, must be lowercase with no spaces.
   * @type {string}
   * @default undefined
   */
  name: string;
  /**
   * The Slash Command's description.
   * @type {string}
   * @default undefined
   */
  description: string;
  /**
   * The Slash Command's category.
   * @type {string}
   * @default ""
   */
  category?: string;
  /**
   * If the Slash Command will be registered and is usable in non-NSFW channels.
   * @type {boolean}
   * @default False
   */
  NSFW?: boolean;
  /**
   * The Slash Command's usage example
   * @type {string}
   * @default "/Command.name"
   */
  usage?: string;
  /**
   * The Slash Command's options
   * @type {ApplicationCommandOptionData[]}
   * @default []
   */
  options?: ApplicationCommandOptionData[];
  /**
   * The permissions that the user requires to run the Slash Command.
   * @type {(bigint)[]}
   * @default []
   */
  UserPermissions?: bigint[];
  /**
   * The permissions that the bot requires to run the Slash Command.
   * @type {(bigint )[]}
   * @default []
   */
  BotPermissions?: bigint[];
  /**
   * The number of seconds the user must wait before re-using the Slash Command.
   * @type {number}
   * @default 0
   * @unit Seconds
   */
  cooldown?: number;
  /**
   * If true, the command will only be registered in Test Servers
   * @type {boolean}
   * @default false
   */
  development?: boolean;
  /**
   * If true, the command can be used and will be registered in Direct Messages
   * @type {boolean}
   * @default Client.DMEnabled
   */
  DMEnabled?: boolean;
  /**
   * If the command can only be used by developers
   * @type {boolean}
   * @default false
   */
  DeveloperOnly?: boolean;
  /**
   * If developers can bypass the command cooldown
   * @type {boolean}
   * @default false
   */
  DeveloperBypass?: boolean;
  /**
   * If the command should be ignored by the handler
   * @type {boolean}
   * @default false
   */
  IgnoreCommand?: boolean;
  /**
   * Ignore
   */
  groups?: any;
  /**
   * The subcommands object
   * @type {Record<string, SubOptions>}
   * @default null
   */
  subcommands?: Record<string, SubOptions>;
  /**
   * The options for the autocomplete command
   * @param interaction [The autocomplete interaction object]
   * @param option [The option for the autocomplete interaction]
   * @returns {string[]}
   */
  autocomplete?: (
    interaction: AutocompleteInteraction,
    option: string
  ) => string[] | Promise<string[]>;
  execute?: ({
    client,
    interaction,
  }: SlashExecute) =>
    | Promise<CommandInteraction | any>
    | CommandInteraction
    | any;
}

export interface ContextOptions {
  /**
   * The Context Command name.
   * @type {string}
   * @default undefined
   */
  name: string;
  /**
   * The Context Command category
   * @type {string}
   * @default ""
   */
  category?: string;
  /**
   * The type of the Context Command
   * @type {ApplicationCommandType.Message | ApplicationCommandType.User}
   * @default undefined
   * @required true
   */
  ContextType: ApplicationCommandType.Message | ApplicationCommandType.User;
  /**
   * If the Context Command will be registered and is usable in non-NSFW channels.
   * @type {boolean}
   * @default False
   */
  NSFW?: boolean;
  /**
   * The permissions that the user requires to run the Context Command.
   * @type {bigint[]}
   * @default []
   */
  UserPermissions?: bigint[];
  /**
   * The permissions that the bot requires to run the Context Command.
   * @type {bigint[]}
   * @default []
   */
  BotPermissions?: bigint[];
  /**
   * The number of seconds the user must wait before re-using the command.
   * @type {number}
   * @default 0
   * @unit Seconds
   */
  cooldown?: number;
  /**
   * If the command will be registered only in Test Servers
   * @type {boolean}
   * @default false
   */
  development?: boolean;
  /**
   * If the command is usable and is registered in Direct Messages
   * @type {boolean}
   * @default false
   */
  DMEnabled?: boolean;
  /**
   * If only developers can use the command
   * @type {boolean}
   * @default false
   */
  DeveloperOnly?: boolean;
  /**
   * If the handler should ignore the command
   * @type {boolean}
   * @default false
   */
  IgnoreCommand?: boolean;
  /**
   * If developers can bypass the command cooldown
   * @type {boolean}
   * @default false
   */
  DeveloperBypass?: boolean;
  execute?: ({
    client,
    interaction,
  }: ContextExecute) => Promise<CommandInteraction | any> | any;
}

export interface Config {
  /**
   * The bot's authentication token
   * @type {string}
   */
  TOKEN: string;
  /**
   * The bot's mongo connection UR
   * @type {string}
   */
  MongoURI?: string;
}
