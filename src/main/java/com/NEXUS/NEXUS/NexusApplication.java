package com.NEXUS.NEXUS;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.NEXUS.NEXUS.config.RenderDatabaseUrlAdapter;

@SpringBootApplication
@EnableScheduling
public class NexusApplication {

    public static void main(String[] args) {
        RenderDatabaseUrlAdapter.configure();
        SpringApplication.run(
                NexusApplication.class,
                args
        );
    }
}
