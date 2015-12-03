package org.powertac.visualizer.services;

import java.util.ArrayList;

import org.powertac.visualizer.push.GlobalPusher;
import org.powertac.visualizer.push.InfoPush;
import org.powertac.visualizer.push.StatisticsPusher;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

@Service
public class PushService {
	private Gson gson = new Gson();
	
	public void pushInfoMessage(InfoPush infoPush) {
		EventBus pushContext = EventBusFactory.getDefault().eventBus();
		
		pushContext.publish("/infopush", gson.toJson(infoPush));
	}
	
	public void pushGlobal(GlobalPusher globalPusher) {
		EventBus pushContext = EventBusFactory.getDefault().eventBus();
		
		pushContext.publish("/globalpush", gson.toJson(globalPusher));
	}
	
	public void pushWholesaleAvg(ArrayList<StatisticsPusher> statisticsPusher) {
		EventBus pushContext = EventBusFactory.getDefault().eventBus();
		
		pushContext.publish("/statisticspush", gson.toJson(statisticsPusher));
	}
}
