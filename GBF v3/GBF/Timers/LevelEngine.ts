import { Emojis } from "../../Handler";
/**
 *
 * @param {number} level The user's level + 1
 * @returns {number} The amount of XP the user is required to reach the next level
 * @example
 * //Returns 100
 * console.log(xpRequired(1))
 */
export function xpRequired(level: number): number {
  return level * 400 + (level - 1) * 200 - 300;
}

/**
 *
 * @param {number} level The user's level + 1
 * @returns {number} The amount of XP the user is required to reach the next level
 * @example
 * //Returns 300
 * console.log(xpRequired(1))
 */
export function rpRequired(level: number): number {
  return level * 800 + (level - 1) * 400 - 500;
}

/**
 * @param {number} XP [The required XP to reach the next level]
 * @description [For every 5 minutes spent, 180 XP is given, we use that information to calculate the number of hours]
 * @returns {Hours} [The hours required to gain enough XP to reach the next level]
 */

export function hoursRequired(XP: number): number {
  let minutesFunction: number = (5 * XP) / 180;
  return minutesFunction / 60;
}

/**
 * Calculate the amount of XP rewarded for the time elapsed
 *
 * @param {number} time - Time elapsed in minutes, where each 5-minute block awards 180 XP
 * @returns {number} - The amount of XP rewarded for the time elapsed, rounded down to nearest integer
 */
export function calculateXP(time: number): number {
  return Math.floor(time / 5) * 180;
}

interface LevelResult {
  hasLeveledUp: boolean;
  addedLevels: number;
  remainingXP: number;
}

/**
 * Checks the level of a user based on their current rank, current RP (Rank Points),
 * and the RP to be added. It calculates the number of levels gained and the remaining RP.
 *
 * @param currentRank - The current rank of the user.
 * @param currentRP - The current RP of the user.
 * @param addedRP - The RP to be added to the user's current RP.
 * @returns An object containing:
 *   - `hasRankedUp`: A boolean indicating if the user has ranked up.
 *   - `addedLevels`: The number of levels the user has gained.
 *   - `remainingXP`: The remaining RP after leveling up.
 */
export function checkLevel(
  currentRank: number,
  currentRP: number,
  addedRP: number
): LevelResult {
  const MAX_RANK = 5000;
  let addedLevels = 0;
  let userXP = currentRP + addedRP;
  let requiredXP = xpRequired(currentRank + addedLevels);

  // Level up while there's enough XP to cover the requirement
  while (userXP >= requiredXP) {
    // Deduct the required XP for this level
    userXP -= requiredXP;
    addedLevels++;

    // Prevent leveling up past the maximum rank
    if (currentRank + addedLevels >= MAX_RANK) {
      addedLevels--;
      // If leveling up is not allowed, return the XP before this level up
      userXP += requiredXP;
      break;
    }

    // Update the required XP for the next level
    requiredXP = xpRequired(currentRank + addedLevels);
  }

  return {
    hasLeveledUp: addedLevels > 0,
    addedLevels,
    remainingXP: userXP,
  };
}

interface RankResult {
  hasRankedUp: boolean;
  addedLevels: number;
  remainingRP: number;
}

/**
 * Checks and calculates the new rank and remaining RP after adding a certain amount of RP.
 *
 * @param currentRank - The current rank of the user.
 * @param currentRP - The current RP of the user.
 * @param addedRP - The amount of RP to be added.
 * @returns An object containing the result of the rank check:
 * - `hasRankedUp`: A boolean indicating if the user has ranked up.
 * - `addedLevels`: The number of levels the user has gained.
 * - `remainingRP`: The remaining RP after leveling up.
 */
export function checkRank(
  currentRank: number,
  currentRP: number,
  addedRP: number
): RankResult {
  const MAX_RANK = 5000;
  let addedLevels = 0;
  let userRP = currentRP + addedRP;
  let requiredRP = rpRequired(currentRank + addedLevels);

  // Continue leveling up as long as the user has enough RP.
  while (userRP >= requiredRP) {
    // Subtract the required RP for the current level.
    userRP -= requiredRP;
    addedLevels++;

    // Stop if the maximum rank is reached.
    if (currentRank + addedLevels >= MAX_RANK) {
      addedLevels--; // Undo the last level-up if it goes over the limit.
      // Add back the RP subtracted, since the level-up didn't occur.
      userRP += requiredRP;
      break;
    }

    // Recalculate the RP required for the new level.
    requiredRP = rpRequired(currentRank + addedLevels);
  }

  return {
    hasRankedUp: addedLevels > 0,
    addedLevels,
    remainingRP: userRP,
  };
}

export function convertSeasonLevel(level: number): number {
  return level * 500 + Math.round(level / 5) * 5;
}

export function calculateTotalSeasonXP(SeasonLevel: number): number {
  let totalXP = 0;
  for (let level = 1; level <= SeasonLevel; level++) {
    totalXP += level * 400 + (level - 1) * 200 - 300;
  }
  return totalXP;
}

export function calculateTotalAccountRP(AccountRank: number): number {
  let totalXP = 0;
  for (let level = 1; level <= AccountRank; level++) {
    totalXP += level * 800 + (level - 1) * 400 - 500;
  }
  return totalXP;
}

interface EmojiRange {
  min: number;
  max: number;
  emoji: string;
}

const emojiRanges: EmojiRange[] = [
  { min: 0, max: 25, emoji: Emojis.Verify },
  { min: 26, max: 50, emoji: Emojis.blackHeartSpin },
  { min: 51, max: 100, emoji: Emojis.whiteHeartSpin },
  { min: 101, max: 150, emoji: Emojis.pinkHeartSpin },
  { min: 151, max: 250, emoji: Emojis.redHeartSpin },
  { min: 251, max: Infinity, emoji: Emojis.donutSpin },
];

export function rankUpEmoji(level: number): string {
  const range = emojiRanges.find(
    ({ min, max }) => level >= min && level <= max
  );
  return range ? range.emoji : Emojis.Verify;
}
