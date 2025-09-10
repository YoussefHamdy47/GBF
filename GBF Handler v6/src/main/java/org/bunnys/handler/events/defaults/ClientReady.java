package org.bunnys.handler.events.defaults;

import com.github.lalyos.jfiglet.FigletFont;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.Presence;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.spi.Event;
import org.bunnys.handler.utils.handler.colors.ConsoleColors;
import org.bunnys.handler.utils.handler.logging.Logger;

import java.io.IOException;
import java.util.Arrays;

public class ClientReady extends ListenerAdapter implements Event {

    private final BunnyNexus client;
    private final long startTime;

    public ClientReady(BunnyNexus client) {
        this.client = client;
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void register(JDA jda) {
    }

    @Override
    public void onReady(ReadyEvent event) {
        JDA jda = event.getJDA();

        String clientName = jda.getSelfUser().getName();
        String asciiArt = renderASCII(clientName);

        System.out.println(ConsoleColors.RED + asciiArt + ConsoleColors.RESET);
        System.out.println(ConsoleColors.RED + "_".repeat(longestLine(asciiArt)) + ConsoleColors.RESET);
        System.out.println(ConsoleColors.RED + clientName + " is now online! v" +
                this.client.getConfig().version() + ConsoleColors.RESET);

        long durationMillis = System.currentTimeMillis() - startTime;
        double seconds = durationMillis / 1000.0;

        long totalUsers = jda.getGuilds().stream()
                .mapToLong(Guild::getMemberCount)
                .sum();
        int totalServers = jda.getGuilds().size();

        Presence presence = jda.getPresence();
        String status = presence.getStatus().name();

        Activity activity = presence.getActivity();
        String activityName = (activity != null) ? activity.getName() : "No Status";

        System.out.println(ConsoleColors.CYAN +
                String.format("> Total app users: %,d%n> Total Servers: %,d%n" +
                        "---------------------------------%n> Presence: %s%n> Activity: %s",
                        totalUsers, totalServers, status, activityName)
                + ConsoleColors.RESET);

        System.out.println(ConsoleColors.GREEN +
                String.format("> Startup Time: %.2f seconds", seconds)
                + ConsoleColors.RESET);

        jda.getPresence().setPresence(
                net.dv8tion.jda.api.OnlineStatus.ONLINE,
                Activity.playing("BunnyNexus"));

        Logger.flushStartupBuffer();
    }

    /** Render ASCII safely; fallback to plain text */
    private String renderASCII(String text) {
        try {
            return FigletFont.convertOneLine(text);
        } catch (IOException e) {
            Logger.warning(
                    "[ClientReady] Failed to render ASCII art. Falling back to plain text\nError: " + e.getMessage());
            return text;
        }
    }

    /** Find the longest line in ASCII art for underline */
    private int longestLine(String asciiArt) {
        return Arrays.stream(asciiArt.split("\n"))
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }
}
