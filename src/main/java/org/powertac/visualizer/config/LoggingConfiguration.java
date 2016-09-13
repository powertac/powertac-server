package org.powertac.visualizer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

@Configuration
public class LoggingConfiguration {

    private final Logger log = LoggerFactory.getLogger(LoggingConfiguration.class);

    @Value("${spring.application.name}")
    private String appName;

    @Value("${server.port}")
    private String serverPort;

    @Inject
    private JHipsterProperties jHipsterProperties;

    @PostConstruct
    private void init() {
        if (jHipsterProperties.getLogging().getLogstash().isEnabled()) {
            addLogstashAppender();
        }
    }

    public void addLogstashAppender() {
        log.error("Usupported: Logstash logging");
    }
}
