package dev.pagewatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PageWatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(PageWatchApplication.class, args);
    }
}
