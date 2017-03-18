package org.powertac.visualizer.user;

import java.util.ArrayList;

import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;
import org.springframework.stereotype.Service;

//@Service
public class ChatGlobal {

	private final EventBus pushContext = EventBusFactory.getDefault().eventBus();
	private final static String CHANNEL = "/chat"; 

	private ArrayList<String> msgs = new ArrayList<String>();
	
	public ArrayList<String> getMsgs() {
		return msgs;
	}
	
	public void addMsg(String msg){
		msgs.add(msg);
		pushContext.publish(CHANNEL, msg);  
	}
	
}
