import { IFreebie } from "./FreebieTypes";
import axios from "axios";

import { EGSRapidAPIHost, EGSRapidAPIKey } from "../../Config/GBF Config.json";

export class Freebie {
  public readonly freebie: IFreebie;
  public readonly numberOfFreebies: number = 1;

  constructor(numberOfFreebies: number) {
    if (numberOfFreebies < 1)
      throw new Error("Number of freebies must be at least 1.");

    this.numberOfFreebies = numberOfFreebies;
  }

  public async fetchFreebies() {
    const options = {
      method: "GET",
      url: "https://free-epic-games.p.rapidapi.com/free",
      headers: {
        "x-rapidapi-key": EGSRapidAPIKey,
        "x-rapidapi-host": EGSRapidAPIHost,
      },
    };

    try {
      const response = await axios.request(options);
      console.log(JSON.stringify(response.data, null, 2));
    } catch (error) {
      console.error(error);
    }
  }
}
