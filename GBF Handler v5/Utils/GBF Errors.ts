import { yellowBright, redBright } from "chalk";
export class GBFError extends Error {
  constructor(ErrorMessage: string) {
    super(ErrorMessage);
    this.message = redBright(`\n• Handler Error: ${ErrorMessage}`);
    this.name = `GBF Handler Error`;
  }
}

export class GBFWarning {
  static PrintWarning(WarningMessage: string): void {
    console.warn(yellowBright(`• Warning: ${WarningMessage}`));
  }
}
