package org.powertac.logtool.common;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"org.powertac.common"})
public class LogtoolCoreConfig {
    // This configuration ensures that beans in org.powertac.common are scanned
}

