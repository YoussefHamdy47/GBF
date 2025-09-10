package org.bunnys;

import org.bunnys.handler.BunnyNexus;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(Main.class);

        app.setLogStartupInfo(false);
        app.setBannerMode(org.springframework.boot.Banner.Mode.OFF);

        ApplicationContext context = app.run(args);

        context.getBean(BunnyNexus.class);
    }
}
