package org.bunnys.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.spi.SlashCommand;
import org.bunnys.handler.commands.slash.SlashCommandConfig;

public class PingSlash extends SlashCommand {
    @Override
    protected void commandOptions(SlashCommandConfig.Builder options) {
        options.name("ping")
                .description("Replies with Pong and gateway latency [Slash Test]")
                .devOnly(true)
                .cooldown(20)
                .botPermissions(Permission.ADMINISTRATOR)
                .userPermissions(Permission.ADMINISTRATOR)
                .NSFW(true)
                .testOnly(true);
    }

    @Override
    public void execute(BunnyNexus client, SlashCommandInteractionEvent event) {
        long ping = event.getJDA().getGatewayPing();
        event.reply("🏓 Pong! Gateway ping: `" + ping + "ms`").queue();
    }
}
