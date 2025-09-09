import {
  ApplicationCommandData,
  ApplicationCommandType,
  BitFieldResolvable,
  Client,
  ClientOptions,
  Collection,
  ColorResolvable,
  GatewayIntentBits,
  Guild,
  REST,
  Routes,
  Snowflake,
} from "discord.js";

import {
  redBright,
  greenBright,
  blueBright,
  magentaBright,
  yellowBright,
} from "chalk";

import { AppConfig, IGBF, IgnoreEvents } from "./types";
import { Engine } from "../Utils/Engine";
import { MessageCommand } from "./Command Handlers/Message Handler";
import { RegisterCommands } from "./Command Handlers/Command Registry";
import path from "path";
import { connect } from "mongoose";
import { SlashCommand } from "./Command Handlers/Slash Handler";
import { IsValidURL } from "../Utils/Utils";
import { ContextCommand } from "./Command Handlers/Context Handler";
import { GBFError } from "../Utils/GBF Errors";

export enum BuiltInEvents {
  "Ready" = "GBFReady",
}

export enum BuiltInCommands {
  "All" = "AllCommands",
  "Set Presence" = "set_presence",
  "Bot Ban" = "bot_ban",
  "Uptime" = "uptime",
  "Ping" = "ping",
}

export class Emojis {
  static Verify = "<:verified:821419611438317638>";
  static Error = "<:error:822091680605011978>";
  static readonly Spotify = "<:Spotify:962905037649096815>";
  static readonly GBFLogo = "<:LogoTransparent:838994085527945266>";
  static readonly progressBarLeftEmpty = "<:leftEmpty:1068143435095220265>";
  static readonly progressBarLeftFull = "<:leftFull:1068143511179894804>";
  static readonly progressBarRightEmpty = "<:rightEmpty:1068143887010517032>";
  static readonly progressBarRightFull = "<:rightFull:1068143806622470244>";
  static readonly progressBarMiddleEmpty = "<:middleEmpty:1068143614804377681>";
  static readonly progressBarMiddleFull = "<:middleFull:1068143723080319038>";
  static readonly diamondSpin = "<a:DiamondGIF:954506603426635837>";
  static readonly crownAnimated = "<a:Crown:1335252412394377250>";
  static readonly blackHeartSpin = "<a:blackSpin:1025851052442005594>";
  static readonly whiteHeartSpin = "<a:whiteSpin:1025851168720687174>";
  static readonly redHeartSpin = "<a:redSpin:1025851361583173773>";
  static readonly pinkHeartSpin = "<a:pinkSpin:1025851222068052101>";
  static readonly donutSpin = "<a:donutSpin:1025851417421955204>";
}

export class ColorCodes {
  static Default: ColorResolvable = "#e91e63";
  static readonly ErrorRed = "#FF0000";
  static readonly SuccessGreen = "#33a532";
  static readonly SalmonPink = "#ff91a4";
  static readonly CardinalRed = "#C41E3A";
  static readonly Cherry = "#D2042D";
  static readonly PastelRed = "#FAA0A0";
  static readonly Cyan = "#00FFFF";
}

export class GBF extends Client implements IGBF {

  private startTime?: number;

  public readonly VerifyEmoji?: Snowflake | string;
  public readonly ErrorEmoji?: Snowflake | string;
  public readonly DefaultColor?: ColorResolvable;
  private HandlerVersion: string;
  public readonly LogActions?: boolean;
  private LogActionsMessage: string = "";
  public readonly HandlerEvents: Collection<string, Function> =
    new Collection();
  public readonly Aliases: Collection<string, string> = new Collection();

  public readonly EventsFolder?: string;
  public readonly IgnoredEvents?: IgnoreEvents;

  public readonly CommandsFolder?: string;
  public readonly MessageCommands: Collection<string, MessageCommand> =
    new Collection();
  public readonly SlashCommands: Collection<string, SlashCommand> =
    new Collection();
  public readonly ContextCommands: Collection<string, ContextCommand> =
    new Collection();

  public readonly BotConfig?: AppConfig;
  public readonly intents:
    | GatewayIntentBits[]
    | BitFieldResolvable<string, number>;
  public readonly Prefix?: string;
  public readonly Prefixes?: string[];
  public readonly DMEnabled?: boolean;
  public readonly Developers?: string[];
  public readonly TestServers?: string[];
  public readonly SupportServer?: string;
  public readonly AppealURL?: string;
  public readonly Version?: string;
  public readonly LogsChannel?: Snowflake[];
  public readonly DisabledHandlerEvents?: BuiltInEvents[];
  public readonly DisabledHandlerCommands?: BuiltInCommands[];
  public readonly DisabledCommands?: string[];
  public DatabaseInteractions?: boolean;
  private BuiltInHandlerCommands: string[];

  constructor(HandlerOptions: ClientOptions & IGBF) {
    super(HandlerOptions);

    const ConfigPath = HandlerOptions.BotConfig;
    const Config = Engine.LoadConfig(ConfigPath as string);

    this.BotConfig = Config;
    if (HandlerOptions.AutoLogin) this.login();

    this.intents = (HandlerOptions.intents as Array<GatewayIntentBits>)
      ?.length || [GatewayIntentBits.Guilds];

    this.EventsFolder = HandlerOptions.EventsFolder;
    this.CommandsFolder = HandlerOptions.CommandsFolder;
    this.IgnoredEvents = HandlerOptions.IgnoredEvents ?? [];

    this.Prefix = HandlerOptions.Prefix ?? "!!";
    this.Prefixes = HandlerOptions.Prefixes ?? [this.Prefix];
    this.HandlerVersion = "5.0.0";
    this.LogActions = HandlerOptions.LogActions;
    this.DMEnabled = HandlerOptions.DMEnabled ?? false;
    this.TestServers = HandlerOptions.TestServers ?? [];
    this.Developers = HandlerOptions.Developers ?? [];
    this.SupportServer = HandlerOptions.SupportServer ?? "No Support Server.";
    this.AppealURL = HandlerOptions.AppealURL ?? undefined;
    this.Version = HandlerOptions.Version ?? "1.0.0";
    this.LogsChannel = HandlerOptions.LogsChannel ?? [];
    this.DisabledHandlerEvents = HandlerOptions.DisabledHandlerEvents ?? [];
    this.DisabledHandlerCommands = HandlerOptions.DisabledHandlerCommands ?? [];
    this.DisabledCommands = HandlerOptions.DisabledCommands ?? [];
    this.DatabaseInteractions = HandlerOptions.DatabaseInteractions ?? true;
    this.BuiltInHandlerCommands = [
      BuiltInCommands["Bot Ban"],
      BuiltInCommands.Ping,
      BuiltInCommands["Set Presence"],
      BuiltInCommands.Uptime,
    ];
    this.DefaultColor =
      (HandlerOptions.DefaultColor as ColorResolvable) ??
      (ColorCodes.Default as ColorResolvable);

    ColorCodes.Default = this.DefaultColor;

    this.VerifyEmoji = HandlerOptions.VerifyEmoji ?? Emojis.Verify;
    this.ErrorEmoji = HandlerOptions.ErrorEmoji ?? Emojis.Error;

    if (this.DisabledCommands.length)
      this.DisabledCommands = this.DisabledCommands.map((Command) =>
        Command.toLowerCase()
      );

    if (this.AppealURL && !IsValidURL(this.AppealURL)) {
      process.emitWarning(
        yellowBright(`• Warning: The Appeal URL provided is not a valid URL.`),
        {
          code: "InvalidURL",
          detail:
            "Our validation system has decided that the Appeal URL you provided in the handler options is not a valid URL and will be set to undefined.",
        }
      );
      this.AppealURL = undefined;
    }

    this.LogActionsMessage += greenBright(
      `• GBF Handler v${this.HandlerVersion} is now online!`
    );
  }

  private async LoadCommands(
    CommandsFolder: string,
    HandlerCommands: boolean = false
  ): Promise<void> {
    try {
      if (!this.application?.owner) await this.application.fetch();

      await RegisterCommands(this, CommandsFolder);

      if (this.MessageCommands.size >= 1 && !HandlerCommands)
        this.LogActionsMessage += blueBright(
          `\n• Registered ${this.MessageCommands.size.toLocaleString(
            "en-US"
          )} Message Command${this.MessageCommands.size > 1 ? "s." : "."}`
        );

      const FilteredGuildSlashCommands = this.SlashCommands.filter(
        (Command) => {
          return (
            !Command.CommandOptions.IgnoreCommand &&
            Command.CommandOptions.development &&
            !this.DisabledCommands.includes(
              Command.CommandOptions.name.toLowerCase()
            ) &&
            (this.DisabledHandlerCommands.includes(BuiltInCommands.All)
              ? !this.BuiltInHandlerCommands.includes(
                  Command.CommandOptions.name
                )
              : !this.DisabledHandlerCommands.includes(
                  Command.CommandOptions.name as BuiltInCommands
                )) &&
            !this.DisabledCommands.includes(
              Command.CommandOptions.name.toLowerCase()
            )
          );
        }
      );

      const FilteredGuildContextCommands = this.ContextCommands.filter(
        (Command) => {
          return (
            !Command.CommandOptions.IgnoreCommand &&
            Command.CommandOptions.development &&
            !this.DisabledCommands.includes(
              Command.CommandOptions.name.toLowerCase()
            ) &&
            (this.DisabledHandlerCommands.includes(BuiltInCommands.All)
              ? !this.BuiltInHandlerCommands.includes(
                  Command.CommandOptions.name
                )
              : !this.DisabledHandlerCommands.includes(
                  Command.CommandOptions.name as BuiltInCommands
                )) &&
            !this.DisabledCommands.includes(
              Command.CommandOptions.name.toLowerCase()
            )
          );
        }
      );

      const FilteredGlobalSlashCommands = this.SlashCommands.filter(
        (Command) => {
          return (
            !Command.CommandOptions.IgnoreCommand &&
            !Command.CommandOptions.development &&
            !this.DisabledCommands.includes(
              Command.CommandOptions.name.toLowerCase()
            ) &&
            (this.DisabledHandlerCommands.includes(BuiltInCommands.All)
              ? !this.BuiltInHandlerCommands.includes(
                  Command.CommandOptions.name
                )
              : !this.DisabledHandlerCommands.includes(
                  Command.CommandOptions.name as BuiltInCommands
                )) &&
            !this.DisabledCommands.includes(
              Command.CommandOptions.name.toLowerCase()
            )
          );
        }
      );

      const FilteredGlobalContextCommands = this.ContextCommands.filter(
        (Command) => {
          return (
            !Command.CommandOptions.IgnoreCommand &&
            !Command.CommandOptions.development &&
            !this.DisabledCommands.includes(
              Command.CommandOptions.name.toLowerCase()
            ) &&
            (this.DisabledHandlerCommands.includes(BuiltInCommands.All)
              ? !this.BuiltInHandlerCommands.includes(
                  Command.CommandOptions.name
                )
              : !this.DisabledHandlerCommands.includes(
                  Command.CommandOptions.name as BuiltInCommands
                )) &&
            !this.DisabledCommands.includes(
              Command.CommandOptions.name.toLowerCase()
            )
          );
        }
      );

      const GuildCommands: ApplicationCommandData[] = [
        ...(this.ToApplicationCommand(
          FilteredGuildSlashCommands
        ) as ApplicationCommandData[]),
        ...(this.ToApplicationCommand(
          FilteredGuildContextCommands
        ) as ApplicationCommandData[]),
      ];

      const GlobalCommands: ApplicationCommandData[] = [
        ...(this.ToApplicationCommand(
          FilteredGlobalSlashCommands
        ) as ApplicationCommandData[]),
        ...(this.ToApplicationCommand(
          FilteredGlobalContextCommands
        ) as ApplicationCommandData[]),
      ];

      const rest = new REST().setToken(this.BotConfig.TOKEN);

      if (
        GuildCommands &&
        GuildCommands.length &&
        this.TestServers.length > 0
      ) {
        await Promise.all(
          this.TestServers.map(async (ServerID) => {
            const TestServer = await this.guilds.fetch(ServerID);
            if (TestServer && TestServer instanceof Guild) {
              const GuildID: Snowflake = TestServer.id;
              await rest
                .put(Routes.applicationGuildCommands(this.user.id, GuildID), {
                  body: GuildCommands,
                })
                .then(() => {
                  if (!HandlerCommands)
                    this.LogActionsMessage += magentaBright(
                      `\n• Registering Guild Only Commands in: ${TestServer.name}`
                    );
                  else
                    this.LogActionsMessage += magentaBright(
                      `\n• Registering Guild Only Handler Commands in: ${TestServer.name}`
                    );
                });
            }
          })
        );
      }

      if (GlobalCommands && GlobalCommands.length) {
        await rest.put(Routes.applicationCommands(this.user.id), {
          body: GlobalCommands,
        });
        if (!HandlerCommands)
          this.LogActionsMessage += magentaBright(
            `\n• Registered ${GlobalCommands.length} Global Command${
              GlobalCommands.length > 1 ? "s." : "."
            }`
          );
        else
          this.LogActionsMessage += magentaBright(
            `\n• Registered ${GlobalCommands.length} Global Handler Command${
              GlobalCommands.length > 1 ? "s." : "."
            }`
          );
      }
    } catch (CommandSetError) {
      throw new Error(redBright(`• Error Setting Commands\n${CommandSetError}`))
        .message;
    }
  }

  private async LoadEvents(
    EventsFolder: string,
    SkippedEvents: string[] | "All" = this.IgnoredEvents
  ): Promise<void> {
    const EventFiles = Engine.ReadFiles(EventsFolder, [".ts", ".js"]);

    const IgnoredNamesLower =
      Array.isArray(SkippedEvents) &&
      SkippedEvents.map((EventName) => EventName.toLowerCase());

    for (const File of EventFiles) {
      try {
        const EventModule = await import(File);

        type EventHandler = (
          arg: typeof this,
          config: typeof this.BotConfig
        ) => void;

        let EventFunction: EventHandler = EventModule.default
          ? EventModule.default
          : Object.values(EventModule)[0];

        if (typeof EventFunction !== "function")
          throw new Error(
            redBright(`• "${File}" does not have a callable export.`)
          );

        const LowerFunctionName = EventFunction.name.toLowerCase();

        if (SkippedEvents === "All") {
          this.LogActionsMessage += blueBright("\n• Will not register events.");
          return;
        }

        if (
          Array.isArray(IgnoredNamesLower) &&
          IgnoredNamesLower.includes(LowerFunctionName)
        ) {
          if (
            !Object.values(BuiltInEvents)
              .map((Event) => Event.toLowerCase())
              .some((Event) => IgnoredNamesLower.includes(Event))
          ) {
            this.LogActionsMessage += blueBright(
              `\n• Ignoring event: ${EventFunction.name}`
            );
          }

          continue;
        }

        this.HandlerEvents.set(EventFunction.name, EventFunction);

        EventFunction(this, this.BotConfig);
      } catch (EventLoadError) {
        throw new GBFError(
          `Error Loading Events in "${File}"\n${EventLoadError}`
        );
      }
    }
  }

  async login(
    ProvidedToken?: string,
    ProvidedMongoURI?: string
  ): Promise<string> {
    this.startTime = performance.now();
    
    const BotToken = ProvidedToken ?? this.BotConfig.TOKEN;
    const MongoURI = ProvidedMongoURI ?? this.BotConfig.MongoURI;
    try {
      await super.login(BotToken);
    } catch (LoginError) {
      throw new GBFError(`Error Logging In\n${LoginError}`);
    }

    if (this.EventsFolder)
      try {
        await this.LoadEvents(this.EventsFolder);
      } catch (EventsError) {
        throw new GBFError(
          `Error Registering Custom Event Files\n${EventsError}`
        );
      }

    try {
      await this.LoadEvents(
        path.join(__dirname, "./Handler Events"),
        this.DisabledHandlerEvents
      );
    } catch (BuiltInEventsError) {
      throw new GBFError(
        `Error Registering Built-In Events\n${BuiltInEventsError}`
      );
    }

    if (!MongoURI) this.DatabaseInteractions = false;

    if (MongoURI && this.DatabaseInteractions) {
      try {
        await connect(this.BotConfig.MongoURI, {
          bufferCommands: true,
          autoCreate: false,
          autoIndex: true,
          serverSelectionTimeoutMS: 30000,
        });

        this.LogActionsMessage += greenBright(
          "\n• Connected to MongoDB successfully."
        );
      } catch (MongoError) {
        if (this.DatabaseInteractions) this.DatabaseInteractions = false;
        throw new GBFError(`Couldn't connect to MongoDB\n${MongoError}`);
      }
    } else {
      process.emitWarning(
        yellowBright(
          "• Warning: No MongoURI provided, ensure MongoURI exists in your config, no database interactions will occur."
        ),
        {
          code: "DataBaseWarn",
          detail:
            "No MongoURI was provided in the bot config, ensure that the MongoURI is set to the name MongoURI in your .env or JSON file, this instance will not use DataBase interactions until a valid URI is provided.",
        }
      );
    }

    if (!this.DisabledHandlerCommands.includes(BuiltInCommands.All))
      try {
        await this.LoadCommands(
          path.join(__dirname, "./Handler Commands"),
          true
        );
      } catch (BuiltInCommandsError) {
        throw new GBFError(
          `Error Registering Built-In Commands\n${BuiltInCommandsError}`
        );
      }

    try {
      if (this.CommandsFolder) await this.LoadCommands(this.CommandsFolder);
    } catch (CommandsError) {
      console.log(CommandsError);
      process.exit(1);
    }

    if (this.LogActions) console.log(this.LogActionsMessage);

    return "Logged In";
  }

  private ToApplicationCommand(
    SlashCommands: Collection<string, SlashCommand | ContextCommand>
  ) {
    return SlashCommands.map((command) => {
      const CommandData = {
        name: command.CommandOptions.name,
        description:
          (command instanceof SlashCommand
            ? command.CommandOptions.description
            : null) ?? null,
        options:
          (command instanceof SlashCommand
            ? command.CommandOptions.options
            : null) ?? [],
        defaultMemberPermissions: command.CommandOptions.UserPermissions || [],
        defaultPermission: command.CommandOptions.development || false,
        dmPermission: command.CommandOptions.DMEnabled || this.DMEnabled,
        nsfw: command.CommandOptions.NSFW || false,
        type:
          (command instanceof ContextCommand
            ? command.CommandOptions.ContextType
            : ApplicationCommandType.ChatInput) ??
          ApplicationCommandType.ChatInput,
      };

      return CommandData;
    });
  }
}
