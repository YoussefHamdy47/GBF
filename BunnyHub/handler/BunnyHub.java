package org.bunnys.handler;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.bunnys.handler.commands.CommandLoader;
import org.bunnys.handler.commands.CommandRegistry;
import org.bunnys.handler.database.DB;
import org.bunnys.handler.database.MongoManager;
import org.bunnys.handler.events.EventLoader;
import org.bunnys.handler.router.buttons.ButtonRouter;
import org.bunnys.handler.router.modals.ModalRouter;
import org.bunnys.handler.utils.TokenLoader;
import org.bunnys.utils.BunnyLog;

import java.util.Scanner;

public class BunnyHub {
    private JDA jda;
    private final BunnyHubBuilder config;
    private final long startTime;
    private volatile boolean isRunning = true;
    private final CommandRegistry commandRegistry;
    private final MongoManager mongoManager;

    BunnyHub(BunnyHubBuilder config) {
        this.config = config;
        this.startTime = System.currentTimeMillis();
        this.commandRegistry = new CommandRegistry(this, config.getDeveloperIds(), config.getTestServerIds());

        BunnyLog.setLogActions(config.isLogActions());

        String mongoURI = TokenLoader.getEnv("MongoURI");
        this.mongoManager = new MongoManager(mongoURI, "GBF");

        DB.init(this.mongoManager);

        if (!this.mongoManager.isConnected()) {
            BunnyLog.error("[BunnyHub] Aborting core startup loop: Database state offline.");
            return;
        }

        if (config.getDeveloperIds().isEmpty())
            BunnyLog.warning("No Developer IDs provided.");

        if (config.getTestServerIds().isEmpty())
            BunnyLog.warning("No Test Server IDs provided.");

        registerShutdownHook();

        if (config.isAutoLogin()) {
            login();
            startConsoleListener();
        }
    }

    public void login() {
        if (this.jda != null)
            return;

        try {
            String token = TokenLoader.getToken(config.getTokenKey());
            JDABuilder jdaBuilder = JDABuilder.createLight(token, config.getIntents());

            if (config.getButtonPackage() != null)
                ButtonRouter.loadButtons(config.getButtonPackage());

            if (config.getModalPackage() != null)
                ModalRouter.loadModals(config.getModalPackage());

            if (config.getEventPackage() != null)
                EventLoader.loadEvents(this, jdaBuilder, config.getEventPackage());

            if (config.getCommandPackage() != null)
                CommandLoader.loadCommands(this, this.commandRegistry, config.getCommandPackage());

            this.jda = jdaBuilder.build();

        } catch (Exception e) {
            BunnyLog.error("Critical failure during BunnyHub login process.\n" + e.getMessage());
        }
    }

    public void shutdown() {
        performShutdown(false);
        System.exit(0);
    }

    private synchronized void performShutdown(boolean emergency) {
        if (!isRunning)
            return;
        isRunning = false;

        String mode = emergency ? "emergency fallback" : "graceful";
        BunnyLog.info("[BunnyHub] Initiating " + mode + " shutdown...");

        try {
            int clearedCmds = this.commandRegistry.clearCommands();
            BunnyLog.info("[CommandRegistry] Cleared " + clearedCmds + " commands.");

            int clearedEvents = EventLoader.clearEvents(this.jda);
            BunnyLog.info("[EventRegistry] Unregistered " + clearedEvents + " active events.");

            if (this.jda != null) {
                if (emergency)
                    this.jda.shutdownNow();
                else
                    this.jda.shutdown();
                BunnyLog.info("[BunnyHub] ShardManager shutdown complete.");
            }

            if (this.mongoManager != null)
                this.mongoManager.disconnect();

            BunnyLog.info("[BunnyHub] Offline");

        } catch (Exception e) {
            BunnyLog.error("[ERROR] Error during " + mode + " shutdown: " + e.getMessage());
        } finally {
            System.out.flush();
            System.err.flush();
        }
    }

    private void startConsoleListener() {
        Thread consoleThread = new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                while (isRunning) {
                    if (scanner.hasNextLine()) {
                        String line = scanner.nextLine().trim().toLowerCase();
                        if (line.equals("stop") || line.equals("exit"))
                            shutdown();
                    }
                }
            }
        }, "BunnyConsoleListener");

        consoleThread.setDaemon(true);
        consoleThread.start();
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> performShutdown(true), "BunnyEmergencyShutdownHook"));
    }

    public JDA getJDA() {
        return this.jda;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public String getVersion() {
        return "4.0.0";
    }

    public CommandRegistry getCommandRegistry() {
        return this.commandRegistry;
    }

    public MongoManager getMongoManager() {
        return this.mongoManager;
    }

    public static BunnyHubBuilder create() {
        return new BunnyHubBuilder();
    }
}