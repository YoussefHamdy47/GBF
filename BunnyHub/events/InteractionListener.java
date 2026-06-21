package org.bunnys.events;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.attribute.IAgeRestrictedChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import org.bunnys.handler.BunnyHub;
import org.bunnys.handler.commands.BunnyCommand;
import org.bunnys.handler.commands.BunnySubcommand;
import org.bunnys.handler.commands.CommandRegistry;
import org.bunnys.handler.router.buttons.ButtonRouter;
import org.bunnys.handler.events.BunnyEvent;
import org.bunnys.handler.router.modals.ModalRouter;
import org.bunnys.utils.AppDesign.ColorCodes;
import org.bunnys.utils.AppDesign.Emojis;
import org.bunnys.utils.BunnyLog;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused")
public class InteractionListener extends BunnyEvent {
    private static final Map<String, Map<String, Long>> commandCooldowns = new ConcurrentHashMap<>();

    public InteractionListener(BunnyHub client) {
        super(client);
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getUser().isBot())
            return;

        ButtonRouter.handle(client, event);
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        ModalRouter.handle(client, event);
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        CommandRegistry registry = client.getCommandRegistry();
        BunnyCommand command = registry.getCommands().get(event.getName());

        if (command == null)
            return;

        BunnySubcommand subcommand = null;
        if (event.getSubcommandName() != null)
            subcommand = command.getSubcommands().get(event.getSubcommandName());

        try {
            List<String> rawChoices = (subcommand != null)
                    ? subcommand.autocomplete(client, event)
                    : command.autocomplete(client, event);

            if (rawChoices == null || rawChoices.isEmpty()) {
                event.replyChoiceStrings(Collections.emptyList()).queue();
                return;
            }

            String userInput = event.getFocusedOption().getValue().toLowerCase();

            List<String> filteredChoices = rawChoices.stream()
                    .filter(choice -> choice.toLowerCase().contains(userInput))
                    .limit(25)
                    .toList();

            event.replyChoiceStrings(filteredChoices).queue();

        } catch (Exception err) {
            BunnyLog.error("Autocomplete Exception (" + event.getName() + "): " + err.getMessage());
            event.replyChoiceStrings(Collections.emptyList()).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getUser().isBot())
            return;

        CommandRegistry registry = client.getCommandRegistry();
        BunnyCommand command = registry.getCommands().get(event.getName());

        if (command == null)
            return;

        BunnySubcommand subcommand = null;
        if (event.getSubcommandName() != null)
            subcommand = command.getSubcommands().get(event.getSubcommandName());

        boolean isDeveloper = registry.getDeveloperIds().contains(event.getUser().getId());
        boolean bypassChecks = command.isDeveloperBypass() && isDeveloper;

        if (!bypassChecks) {
            EmbedBuilder errorEmbed = new EmbedBuilder()
                    .setColor(ColorCodes.ERROR_RED)
                    .setTimestamp(Instant.now());

            boolean isDevOnly = (subcommand != null && subcommand.isDeveloperOnly()) || command.isDeveloperOnly();
            if (isDevOnly && !isDeveloper) {
                errorEmbed.setTitle(Emojis.ERROR + " Access Denied")
                        .setDescription(
                                "This configuration path is strictly locked to utility application developers.");
                event.replyEmbeds(errorEmbed.build()).setEphemeral(true).queue();
                return;
            }

            if (!command.isDmEnabled() && !event.isFromGuild()) {
                errorEmbed.setTitle(Emojis.ERROR + " Context Execution Error")
                        .setDescription("The execution profile for `" + command.getName()
                                + "` is disabled in direct messages.");
                event.replyEmbeds(errorEmbed.build()).setEphemeral(true).queue();
                return;
            }

            if (event.isFromGuild()) {

                boolean isNsfwActive = (subcommand != null && subcommand.isNsfw()) || command.isNsfw();
                if (isNsfwActive) {
                    boolean isNsfwChannel = (event.getChannel() instanceof IAgeRestrictedChannel arc) && arc.isNSFW();
                    if (!isNsfwChannel) {
                        errorEmbed.setTitle(Emojis.ERROR + " Age-Gate Restriction")
                                .setDescription(
                                        "You cannot execute NSFW marked parameters outside of designated age-restricted channels.");
                        event.replyEmbeds(errorEmbed.build()).setEphemeral(true).queue();
                        return;
                    }
                }

                for (Permission perm : command.getUserPermissions()) {
                    if (!Objects.requireNonNull(event.getMember()).hasPermission(event.getGuildChannel(), perm)) {
                        errorEmbed.setTitle(Emojis.ERROR + " Missing Authority")
                                .setDescription(event.getUser().getName()
                                        + ", you are missing the required permission: `" + perm.getName() + "`");
                        event.replyEmbeds(errorEmbed.build()).setEphemeral(true).queue();
                        return;
                    }
                }

                for (Permission perm : command.getAppPermissions()) {
                    if (!Objects.requireNonNull(event.getGuild()).getSelfMember().hasPermission(event.getGuildChannel(),
                            perm)) {
                        errorEmbed.setTitle(Emojis.ERROR + " System Operation Error")
                                .setDescription(
                                        "I am missing internal authorization requirements: `" + perm.getName() + "`");
                        event.replyEmbeds(errorEmbed.build()).setEphemeral(true).queue();
                        return;
                    }
                }
            }

            int targetCooldown = (subcommand != null && subcommand.getCooldown() > 0) ? subcommand.getCooldown()
                    : command.getCooldown();
            if (targetCooldown > 0) {
                String trackingKey = (subcommand != null) ? command.getName() + "." + subcommand.getName()
                        : command.getName();

                commandCooldowns.computeIfAbsent(trackingKey, k -> new ConcurrentHashMap<>());
                Map<String, Long> userTimestamps = commandCooldowns.get(trackingKey);

                long currentUnixMs = System.currentTimeMillis();

                userTimestamps.entrySet().removeIf(entry -> entry.getValue() < currentUnixMs);

                String targetUserId = event.getUser().getId();

                if (userTimestamps.containsKey(targetUserId)) {
                    long expirationUnixMs = userTimestamps.get(targetUserId);
                    long displayUnixSec = expirationUnixMs / 1000L;
                    errorEmbed.setTitle(Emojis.ERROR + " Rate Limit Active")
                            .setDescription(event.getUser().getAsMention()
                                    + ", you can process this runtime tracking path again <t:" + displayUnixSec
                                    + ":R>.");
                    event.replyEmbeds(errorEmbed.build()).setEphemeral(true).queue();
                    return;
                }

                long computedLimitMs = TimeUnit.SECONDS.toMillis(targetCooldown);
                userTimestamps.put(targetUserId, currentUnixMs + computedLimitMs);
            }
        }

        try {
            if (subcommand != null)
                subcommand.execute(client, event);
            else
                command.execute(client, event);
        } catch (Exception err) {
            BunnyLog.error("Command Processing Exception (" + event.getName() + "): " + err.getMessage());
            err.printStackTrace();

            if (!event.isAcknowledged()) {
                EmbedBuilder internalError = new EmbedBuilder()
                        .setColor(ColorCodes.ERROR_RED)
                        .setTitle(Emojis.ERROR + " Process Aborted")
                        .setDescription("An unhandled code runtime engine exception disrupted processing operations.");
                event.replyEmbeds(internalError.build()).setEphemeral(true).queue();
            }
        }
    }
}