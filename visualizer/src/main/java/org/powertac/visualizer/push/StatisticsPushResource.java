package org.powertac.visualizer.push;

import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.PushEndpoint;
import org.primefaces.push.impl.JSONEncoder;

@PushEndpoint("/statisticspush")
public class StatisticsPushResource {

	@OnMessage(encoders = { JSONEncoder.class })
	public String onMessage(String data) {
		return data;
	}
}
