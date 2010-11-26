package org.powertac.server.core.test;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

    public Logger() {
        System.out.println("logger");
    }

    public void log(int i) {
		System.out.println("log: " + i + " at " + new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
	}

}