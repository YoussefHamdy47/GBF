package org.bunnys.handler;

import net.dv8tion.jda.api.requests.GatewayIntent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BunnyHubBuilder {
    // Default feature states
    private boolean logActions = true;
    private boolean autoLogin = true;
    private String tokenKey = "DISCORD_TOKEN";

    private String eventPackage = null;
    private String commandPackage = null;
    private String buttonPackage = null;
    private String modalPackage = null;

    // Explicitly empty by default
    private final List<GatewayIntent> intents = new ArrayList<>();
    private final List<String> developerIds = new ArrayList<>();
    private final List<String> testServerIds = new ArrayList<>();

    public BunnyHubBuilder setLogActions(boolean logActions) {
        this.logActions = logActions;
        return this;
    }

    public BunnyHubBuilder setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
        return this;
    }

    public BunnyHubBuilder setTokenKey(String tokenKey) {
        this.tokenKey = tokenKey;
        return this;
    }

    public BunnyHubBuilder addIntents(GatewayIntent... gatewayIntents) {
        this.intents.addAll(Arrays.asList(gatewayIntents));
        return this;
    }

    public BunnyHubBuilder setEventPackage(String packageName) {
        this.eventPackage = packageName;
        return this;
    }

    public BunnyHubBuilder setCommandPackage(String packageName) {
        this.commandPackage = packageName;
        return this;
    }

    public BunnyHubBuilder setButtonPackage(String packageName) {
        this.buttonPackage = packageName;
        return this;
    }

    public BunnyHubBuilder setModalPackage(String packageName) {
        this.modalPackage = packageName;
        return this;
    }

    public BunnyHubBuilder addDeveloperIds(String... ids) {
        this.developerIds.addAll(Arrays.asList(ids));
        return this;
    }

    public BunnyHubBuilder addTestServerIds(String... ids) {
        this.testServerIds.addAll(Arrays.asList(ids));
        return this;
    }

    // Getters so BunnyHub can read the configuration safely
    public boolean isLogActions() {
        return logActions;
    }

    public boolean isAutoLogin() {
        return autoLogin;
    }

    public String getTokenKey() {
        return tokenKey;
    }

    public List<GatewayIntent> getIntents() {
        return intents;
    }

    public String getEventPackage() {
        return eventPackage;
    }

    public List<String> getDeveloperIds() {
        return developerIds;
    }

    public List<String> getTestServerIds() {
        return testServerIds;
    }

    public String getCommandPackage() {
        return commandPackage;
    }

    public String getButtonPackage() {
        return buttonPackage;
    }

    public String getModalPackage() {
        return modalPackage;
    }

    /**
     * Builds and returns the BunnyHub instance based on this configuration.
     */
    public BunnyHub build() {
        return new BunnyHub(this);
    }
}