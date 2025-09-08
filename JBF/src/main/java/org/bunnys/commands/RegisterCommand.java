package org.bunnys.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import org.bunnys.database.RepositoryFactory;
import org.bunnys.database.models.User;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.commands.slash.SlashCommandConfig;
import org.bunnys.handler.database.providers.MongoProvider;
import org.bunnys.handler.spi.SlashCommand;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class RegisterCommand extends SlashCommand {

    @Override
    protected void commandOptions(SlashCommandConfig.Builder options) {
        options.name("register")
                .description("Save your profile to the database");
    }

    @Override
    public void execute(BunnyNexus client, SlashCommandInteractionEvent event) {
        String userId = event.getUser().getId();
        String username = event.getUser().getName();

        MongoProvider mongo = client.getMongoProvider();
        if (mongo == null) {
            event.reply("❌ Database is not connected. Please try again later.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        RepositoryFactory repos = new RepositoryFactory(mongo.getConnection());

        try {
            Optional<User> existing = repos.users().findById(userId).get();
            if (existing.isPresent()) {
                event.reply("⚠️ You are already registered as **"
                                + existing.get().getUsername() + "** (`" + existing.get().getUserId() + "`)")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            User newUser = new User(userId, username);
            repos.users().save(newUser).get();

            event.reply("✅ Registered profile for **" + username + "** (`" + userId + "`)").queue();

        } catch (InterruptedException | ExecutionException e) {
            event.reply("❌ Failed to register. Please try again later.").setEphemeral(true).queue();
            e.printStackTrace();
        }
    }
}
