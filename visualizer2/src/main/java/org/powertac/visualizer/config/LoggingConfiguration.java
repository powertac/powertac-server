package org.powertac.visualizer.config;

import io.github.jhipster.config.JHipsterProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingConfiguration {

    private final Logger log = LoggerFactory.getLogger(LoggingConfiguration.class);

    public LoggingConfiguration(JHipsterProperties jHipsterProperties) {
        if (jHipsterProperties.getLogging().getLogstash().isEnabled()) {
            log.error("Usupported: Logstash logging");
        }
    }

}
