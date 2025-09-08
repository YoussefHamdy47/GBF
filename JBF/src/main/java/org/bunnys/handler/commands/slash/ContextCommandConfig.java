package org.bunnys.handler.commands.slash;

import net.dv8tion.jda.api.interactions.commands.Command;

import java.util.Objects;

/**
 * Immutable configuration for Context Menu Commands (User & Message)
 */
public final class ContextCommandConfig {
    private final String name;
    private final String description;
    private final Command.Type type; // USER or MESSAGE
    private final boolean testOnly;
    private final boolean devOnly;

    private ContextCommandConfig(Builder b) {
        this.name = Objects.requireNonNull(b.name, "Context command name is required.");
        this.description = b.description;
        this.type = Objects.requireNonNull(b.type, "Context command type is required.");
        this.testOnly = b.testOnly;
        this.devOnly = b.devOnly;
    }

    public String name() { return name; }
    public String description() { return description; }
    public Command.Type type() { return type; }
    public boolean testOnly() { return testOnly; }
    public boolean devOnly() { return devOnly; }

    // Builder
    public static class Builder {
        private String name;
        private String description;
        private Command.Type type;
        private boolean testOnly = false;
        private boolean devOnly = false;

        public Builder name(String name) {
            if (name == null || name.isBlank())
                throw new IllegalArgumentException("Context command name cannot be blank");
            this.name = name.trim();
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder type(Command.Type type) {
            this.type = type;
            return this;
        }

        public Builder testOnly(boolean testOnly) {
            this.testOnly = testOnly;
            return this;
        }

        public Builder devOnly(boolean devOnly) {
            this.devOnly = devOnly;
            return this;
        }

        public ContextCommandConfig build() {
            return new ContextCommandConfig(this);
        }
    }
}
