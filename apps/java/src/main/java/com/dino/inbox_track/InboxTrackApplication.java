package com.dino.inbox_track;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;

@SpringBootApplication
@Slf4j
public class InboxTrackApplication {


	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(InboxTrackApplication.class);
		app.addListeners((ApplicationEnvironmentPreparedEvent ev) -> {
			if (!log.isDebugEnabled()) {
				return; // no work when debug is off
			}
			var env = ev.getEnvironment();
			log.debug("spring.datasource.url = {}", env.getProperty("spring.datasource.url"));
		});
		app.run(args);
	}

}
