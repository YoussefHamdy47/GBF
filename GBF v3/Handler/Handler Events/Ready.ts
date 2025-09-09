import { Events } from "discord.js";
import { GBF } from "../GBF";
import { redBright, whiteBright, underline } from "chalk";

import { textSync } from "figlet";

export function GBFReady(client: GBF) {
  client.on(Events.ClientReady, async () => {
    const TotalUsers = client.guilds.cache.reduce(
      (Acc, Guild) => Acc + Guild.memberCount,
      0
    );

    console.log(
      redBright(
        textSync(`${client.user.username}`, {
          horizontalLayout: "full",
        })
      )
    );

    console.log(
      redBright(
        underline(`${client.user.username} is now online! v${client.Version}`)
      )
    );

    console.log(
      whiteBright(
        `> Total app users: ${TotalUsers.toLocaleString()}\n> Total Servers: ${client.guilds.cache.size.toLocaleString()}\n---------------------------------\n` +
          `> Presence: ${client.user.presence.status}\n> Status: ${
            client.user.presence.activities.length
              ? client.user.presence.activities[0].name
              : "No Status"
          }`
      )
    );

    process.on("unhandledRejection", (reason: any, promise: Promise<any>) => {
      console.log(
        "[ERROR] Unhandled Rejection at: Promise ",
        promise,
        " reason: ",
        reason.message
      );
    });
  });

  process.on("uncaughtException", (err: Error, origin: string) => {
    console.log(`Caught uncaughtException: ${err}`);
  });
}
