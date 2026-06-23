package org.bunnys;

import org.bunnys.handler.BunnyHub;

public class Main {
    public static void main(String[] args) {

        @SuppressWarnings("unused")
        BunnyHub client = BunnyHub.create()
                .setEventPackage("org.bunnys.events")
                .setCommandPackage("org.bunnys.commands")
                .setButtonPackage("org.bunnys.buttons")
                .setModalPackage("org.bunnys.modals")
                .setLogActions(false)
                .setAutoLogin(true)
                .addTestServerIds("1187559385359200316")
                .addDeveloperIds("333644367539470337")
                .setTokenKey("TOKEN")
                .addIntents()
                .build();

    }
}