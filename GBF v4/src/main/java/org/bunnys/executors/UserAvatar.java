package org.bunnys.executors;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.bunnys.handler.BunnyNexus;
import org.bunnys.handler.utils.handler.colors.ColorCodes;

import java.awt.*;
import java.util.Objects;

@SuppressWarnings("unused")
public class UserAvatar {
    private final BunnyNexus client;
    private final String userID;
    private final String guildID;
    private final Color embedColor;
    private final AvatarPriority avatarPriority;

    // Configuration constants
    private static final String DEFAULT_EXTENSION = "png";
    private static final int DEFAULT_SIZE = 1024;
    private static final int MIN_SIZE = 16;
    private static final int MAX_SIZE = 4096;

    // Cached entities - lazy loaded
    private User user;
    private Member member;
    private Guild guild;

    // Cached computed values
    private String displayName;
    private Boolean isAvatarDifferent; // Use Boolean to detect uninitialized state

    public enum AvatarPriority {
        GLOBAL,
        GUILD
    }

    public static class Builder {
        private final BunnyNexus client;
        private final String userID;
        private String guildID;
        private Color embedColor = ColorCodes.DEFAULT;
        private AvatarPriority avatarPriority = AvatarPriority.GLOBAL;
        private String extension = DEFAULT_EXTENSION;
        private int size = DEFAULT_SIZE;

        public Builder(BunnyNexus client, String userID) {
            this.client = Objects.requireNonNull(client, "Client cannot be null");
            this.userID = Objects.requireNonNull(userID, "User ID cannot be null");
        }

        public Builder guild(String guildID) {
            this.guildID = guildID;
            return this;
        }

        public Builder embedColor(Color color) {
            this.embedColor = color != null ? color : ColorCodes.DEFAULT;
            return this;
        }

        public Builder avatarPriority(AvatarPriority priority) {
            this.avatarPriority = priority != null ? priority : AvatarPriority.GLOBAL;
            return this;
        }

        public Builder imageFormat(String extension, int size) {
            this.extension = validateExtension(extension);
            this.size = validateSize(size);
            return this;
        }

        private String validateExtension(String ext) {
            if (ext == null || ext.trim().isEmpty())
                return DEFAULT_EXTENSION;
            String lower = ext.toLowerCase().trim();
            return lower.matches("(png|jpg|jpeg|webp|gif)") ? lower : DEFAULT_EXTENSION;
        }

        private int validateSize(int size) {
            if (size < MIN_SIZE || size > MAX_SIZE)
                return DEFAULT_SIZE;
            // Ensure size is power of 2 for Discord API compatibility
            return Integer.highestOneBit(size);
        }

        public UserAvatar build() {
            return new UserAvatar(this);
        }
    }

    private final String extension;
    private final int size;

    private UserAvatar(Builder builder) {
        this.client = builder.client;
        this.userID = builder.userID;
        this.guildID = builder.guildID;
        this.embedColor = builder.embedColor;
        this.avatarPriority = builder.avatarPriority;
        this.extension = builder.extension;
        this.size = builder.size;
    }

    // Legacy constructor for backward compatibility
    @Deprecated
    public UserAvatar(BunnyNexus client, String userID, String guildID,
            Color embedColor, AvatarPriority avatarPriority) {
        this.client = Objects.requireNonNull(client, "Client cannot be null");
        this.userID = Objects.requireNonNull(userID, "User ID cannot be null");
        this.guildID = guildID;
        this.embedColor = embedColor != null ? embedColor : ColorCodes.DEFAULT;
        this.avatarPriority = avatarPriority != null ? avatarPriority : AvatarPriority.GLOBAL;
        this.extension = DEFAULT_EXTENSION;
        this.size = DEFAULT_SIZE;
    }

    // Lazy loading with caching
    private User getUser() {
        if (user == null) {
            user = client.getShardManager().getUserById(userID);
            if (user == null) {
                throw new IllegalArgumentException("User not found with ID: " + userID);
            }
        }
        return user;
    }

    private Guild getGuild() {
        if (guild == null && guildID != null) {
            guild = client.getShardManager().getGuildById(guildID);
            if (guild == null) {
                throw new IllegalArgumentException("Guild not found with ID: " + guildID);
            }
        }
        return guild;
    }

    private Member getMember() {
        if (member == null && getGuild() != null) {
            member = getGuild().getMemberById(userID);
        }
        return member;
    }

    private String getDisplayName() {
        if (displayName == null) {
            Member m = getMember();
            displayName = m != null ? m.getEffectiveName() : getUser().getName();
        }
        return displayName;
    }

    private boolean isAvatarDifferent() {
        if (isAvatarDifferent == null) {
            Member m = getMember();
            isAvatarDifferent = m != null &&
                    !Objects.equals(m.getEffectiveAvatarUrl(), getUser().getEffectiveAvatarUrl());
        }
        return isAvatarDifferent;
    }

    private String formatAvatarUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            return null;
        }

        StringBuilder url = new StringBuilder(baseUrl);

        // Replace .webp extension if present
        int webpIndex = url.lastIndexOf(".webp");
        if (webpIndex != -1) {
            url.replace(webpIndex, webpIndex + 5, "." + extension);
        }

        // Add size parameter
        char separator = url.indexOf("?") != -1 ? '&' : '?';
        url.append(separator).append("size=").append(size);

        return url.toString();
    }

    public String getAvatarString() {
        StringBuilder avatarString = new StringBuilder();
        avatarString.append("[Global Avatar](")
                .append(formatAvatarUrl(getUser().getEffectiveAvatarUrl()))
                .append(")");

        if (isAvatarDifferent()) {
            avatarString.append(" | [Server-Only Avatar](")
                    .append(formatAvatarUrl(getMember().getEffectiveAvatarUrl()))
                    .append(")");
        }

        return avatarString.toString();
    }

    public MessageEmbed generateEmbed(boolean showBothAvatars) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getDisplayName() + "'s Avatar")
                .setDescription(getAvatarString())
                .setColor(embedColor);

        if (showBothAvatars && isAvatarDifferent()) {
            if (avatarPriority == AvatarPriority.GUILD) {
                embed.setImage(getAvatar(true));
                embed.setThumbnail(getAvatar(false));
            } else {
                embed.setImage(getAvatar(false));
                embed.setThumbnail(getAvatar(true));
            }
        } else {
            // Show primary avatar based on priority, fallback to global if no guild avatar
            boolean useGuildAvatar = avatarPriority == AvatarPriority.GUILD && isAvatarDifferent();
            embed.setImage(getAvatar(useGuildAvatar));
        }

        return embed.build();
    }

    public String getAvatar(boolean useGuildAvatar) {
        if (useGuildAvatar && isAvatarDifferent()) {
            return formatAvatarUrl(getMember().getEffectiveAvatarUrl());
        }
        return formatAvatarUrl(getUser().getEffectiveAvatarUrl());
    }

    public Button[] getAvatarButtons() {
        Button globalButton = Button.link(
                formatAvatarUrl(getUser().getEffectiveAvatarUrl()),
                "Global Avatar");

        if (isAvatarDifferent()) {
            Button serverButton = Button.link(
                    formatAvatarUrl(getMember().getEffectiveAvatarUrl()),
                    "Server Avatar");
            return new Button[] { globalButton, serverButton };
        }

        return new Button[] { globalButton };
    }

    // Utility methods for external access
    public boolean hasGuildContext() {
        return guildID != null;
    }

    public boolean hasDifferentAvatars() {
        return isAvatarDifferent();
    }

    public String getUserID() {
        return userID;
    }

    public String getGuildID() {
        return guildID;
    }

    // Clear cached data if entities might have changed
    public void invalidateCache() {
        this.user = null;
        this.member = null;
        this.guild = null;
        this.displayName = null;
        this.isAvatarDifferent = null;
    }
}