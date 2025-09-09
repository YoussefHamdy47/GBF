package org.bunnys.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.spi.MessageCommand;
import org.bunnys.handler.commands.message.MessageCommandConfig;

@SuppressWarnings("unused")
public class PingCommand extends MessageCommand {
    @Override
    protected void commandOptions(MessageCommandConfig.Builder options) {
        options.name("ping")
                .description("Replies with Pong and gateway latency")
                .usage("ping")
                .aliases("p", "latency")
                .devOnly(true);
    }

    @Override
    public void execute(BunnyNexus client, MessageReceivedEvent message, String[] args) {
        long ping = message.getJDA().getGatewayPing();
        message.getMessage().reply("Ping: `" + ping + "ms`").queue();
    }
}
