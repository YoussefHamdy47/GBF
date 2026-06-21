package org.bunnys.modals;

import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import org.bunnys.handler.BunnyHub;
import org.bunnys.handler.router.modals.BunnyModal;
import org.bunnys.nexus.timers.Timers;
import org.bunnys.utils.AppDesign;
import org.bunnys.utils.BunnyLog;

public class SemesterEndModal extends BunnyModal {

    @Override
    public String getPrefix() {
        return "semester_end_modal";
    }

    @Override
    public void execute(BunnyHub client, ModalInteractionEvent event, String[] args) {
        // Prevent silent failure: Acknowledge the event if arguments are malformed
        if (args.length < 3) {
            event.reply("> " + AppDesign.Emojis.ERROR + " **System Error:** Malformed modal data received.").setEphemeral(true).queue();
            return;
        }

        String targetId = args[1];
        long requestTime;

        try {
            requestTime = Long.parseLong(args[2]);
        } catch (NumberFormatException e) {
            event.reply("> " + AppDesign.Emojis.ERROR + " **Error:** Invalid request timestamp.").setEphemeral(true).queue();
            return;
        }

        if (!event.getUser().getId().equals(targetId)) {
            event.reply("> " + AppDesign.Emojis.ERROR + " **Access Denied:** This modal belongs to someone else.").setEphemeral(true).queue();
            return;
        }

        try {
            Timers timerSystem = new Timers(targetId, event);
            MessageEmbed finalRecapEmbed = timerSystem.processEndSemesterModal(event, requestTime);

            // By setting the content to the ping, it happens completely outside the embed
            event.editMessageEmbeds(finalRecapEmbed)
                    .setContent("<@" + targetId + ">")
                    .setComponents() // This clears the confirmation buttons
                    .queue();

        } catch (IllegalArgumentException | IllegalStateException e) {
            // Catches invalid confirmation phrases, 5-minute timeouts, or missing semesters
            event.reply("> ❌ **Termination aborted:** " + e.getMessage())
                    .setEphemeral(true).queue();
        } catch (Exception e) {
            // Log to console AND alert the user so Discord doesn't timeout
            BunnyLog.error("[SemesterEndModal] Critical failure during execution for user: " + targetId + " - " + e.getMessage());
            event.reply("> " + AppDesign.Emojis.ERROR + " **System Error:** Could not finalize semester archival.")
                    .setEphemeral(true).queue();
        }
    }
}