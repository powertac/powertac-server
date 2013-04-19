package org.powertac.visualizer.services;

import java.util.ArrayList;

import org.powertac.visualizer.push.GlobalPusher;
import org.powertac.visualizer.push.InfoPush;
import org.powertac.visualizer.push.StatisticsPusher;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

@Service
public class PushService {
	private Gson gson = new Gson();
	
	public void pushInfoMessage(InfoPush infoPush) {
		PushContext pushContext = PushContextFactory.getDefault()
				.getPushContext();
		
		pushContext.push("/infopush", gson.toJson(infoPush));
	}
	
	public void pushGlobal(GlobalPusher globalPusher) {
		PushContext pushContext = PushContextFactory.getDefault()
				.getPushContext();
		
		pushContext.push("/globalpush", gson.toJson(globalPusher));
	}
	
	public void pushWholesaleAvg(ArrayList<StatisticsPusher> statisticsPusher) {
		PushContext pushContext = PushContextFactory.getDefault()
				.getPushContext();
		
		pushContext.push("/statisticspush", gson.toJson(statisticsPusher));
	}
}
