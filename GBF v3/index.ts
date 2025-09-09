import { GatewayIntentBits } from "discord.js";
import { GBF } from "./Handler";
import path from "path";

export const client = new GBF({
  intents: [GatewayIntentBits.Guilds],
  Version: "3.0.0",
  CommandsFolder: path.join(__dirname, "./Commands"),
  EventsFolder: path.join(__dirname, "./Events"),
  Prefix: "!!",
  LogActions: true,
  AutoLogin: true,
  Developers: ["333644367539470337"],
  TestServers: ["1187559385359200316"],
  SupportServer: "https://discord.gg/yrM7fhgNBW",
  AppealURL:
    "https://docs.google.com/forms/d/e/1FAIpQLScd5AyKpLT9AAFJQ3t1_pz_iqoCV8EyvxXh4-XowHnQ9wwGYg/viewform?usp=sf_link",
  DMEnabled: false,
  LogsChannel: ["1204097210913128468"],
});
