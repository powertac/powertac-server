package org.powertac.visualizer.user;

import java.util.ArrayList;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.primefaces.context.RequestContext;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;
import org.springframework.stereotype.Service;

//@Service
public class ChatGlobal {

	private final PushContext pushContext = PushContextFactory.getDefault()
			.getPushContext();
	private final static String CHANNEL = "/chat"; 

	private ArrayList<String> msgs = new ArrayList<String>();
	
	public ArrayList<String> getMsgs() {
		return msgs;
	}
	
	public void addMsg(String msg){
		msgs.add(msg);
		pushContext.push(CHANNEL, msg);  
	}
	
}
