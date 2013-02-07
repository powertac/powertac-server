package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.primefaces.context.RequestContext;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


public class ChatBean implements Serializable {

	
	private static final long serialVersionUID = 1L;
	private String name="";
	private String msg="";
	
	private ChatGlobal chatGlobal;
	
	@Autowired
	public ChatBean(ChatGlobal chatGlobal) {
	this.chatGlobal=chatGlobal;
	}
	public void setMsg(String msg) {
		this.msg = msg;
	}
	public String getMsg() {
		return msg;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void sendMsg(){
		chatGlobal.addMsg(""+name+": "+msg);
		msg="";
	}
		
	

	
}
