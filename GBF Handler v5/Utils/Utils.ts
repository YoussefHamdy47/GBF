import {
  BaseMessageOptions,
  DMChannel,
  GuildMember,
  Interaction,
  Message,
  PermissionResolvable,
  TextBasedChannel,
} from "discord.js";

/**
 * Takes a string and lowercases it then uppercase the first word of each sentence you could say
 * @param {string}  str - The word to be like Custom Status instead of just CUSTOM STATUS
 * @returns {string} - Returns a beautified converted string
 * @example capitalize(string)
 */
export function capitalize(str: string): string {
  return str.replace(/(?<=\W)(\w)/g, (char) => char.toUpperCase());
}

export function GetRandomFromArray<T>(array: T[]): T {
  const randomIndex = Math.floor(Math.random() * array.length);
  return array[randomIndex];
}

export function IsValidURL(string: string): boolean {
  try {
    new URL(string);
    return true;
  } catch (_) {
    return false;
  }
}

const DEFAULT_FORMAT_OPTIONS: TimeFormatOptions = {
  format: "long",
  includeSpaces: false,
  joinDelimiter: " ",
  maxUnitCount: 100,
};

export function msToTime(
  duration: number,
  options: TimeFormatOptions = {}
): string | undefined {
  const { format, includeSpaces, joinDelimiter, maxUnitCount } = {
    ...DEFAULT_FORMAT_OPTIONS,
    ...options,
  };

  if (typeof duration !== "number" || duration < 0) return undefined;

  let formattedTime = "";
  let unitCount = 0;

  for (let i = Object.keys(TimeUnitValues).length - 1; i >= 0; i--) {
    const unitKey = Object.keys(TimeUnitValues)[i];
    if (unitKey === "year") continue;

    const unitValue = TimeUnitValues[unitKey];
    let unitDuration = duration / unitValue;
    if (unitDuration >= 1) {
      if ((maxUnitCount || 100) < ++unitCount) break;

      unitDuration = Math.floor(unitDuration);
      const unitName = FullTimeUnitNames[unitKey][format!];
      const pluralSuffix = unitDuration !== 1 && format !== "short" ? "s" : "";
      formattedTime += `${unitDuration} ${unitName}${pluralSuffix}${
        includeSpaces ? " " : joinDelimiter
      }`;
      duration -= unitDuration * unitValue;
    }
  }

  formattedTime = formattedTime.trim();
  return formattedTime || undefined;
}

export function MissingPermissions(
  TargetMember: GuildMember,
  RequiredPermissions: PermissionResolvable | PermissionResolvable[]
) {
  const missingPerms = TargetMember.permissions
    .missing(RequiredPermissions)
    .map(
      (str) =>
        `\`${str
          .replace(/_/g, " ")
          .toLowerCase()
          .replace(/\b(\w)/g, (char) => char.toUpperCase())}\``
    );

  return missingPerms;
}

export async function SendAndDelete(
  Channel: TextBasedChannel,
  MessageOptions: BaseMessageOptions,
  TimeInSeconds = 5
): Promise<Message<false>> {
  if (Channel instanceof DMChannel) return Channel.send(MessageOptions);
  //@ts-ignore
  const message: Message | Interaction = await Channel.send(MessageOptions);

  setTimeout(async () => {
    //@ts-ignore
    await message.delete();
  }, TimeInSeconds * 1000);
}

interface TimeFormatOptions {
  format?: "long" | "short";
  includeSpaces?: boolean;
  joinDelimiter?: string;
  maxUnitCount?: number;
}

const TimeUnitValues: Record<string, number> = {
  year: 31557600000,
  day: 86400000,
  hour: 3600000,
  minute: 60000,
  second: 1000,
};

const FullTimeUnitNames: Record<string, Record<string, string>> = {
  year: { long: "year", short: "yr" },
  day: { long: "day", short: "day" },
  hour: { long: "hour", short: "hr" },
  minute: { long: "minute", short: "min" },
  second: { long: "second", short: "sec" },
};
