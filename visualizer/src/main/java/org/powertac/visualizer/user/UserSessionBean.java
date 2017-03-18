package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;

import javax.faces.event.AjaxBehaviorEvent;

import org.powertac.visualizer.domain.customer.Customer;
import org.powertac.visualizer.services.CustomerService;
import org.primefaces.event.ToggleEvent;
import org.primefaces.push.PushContext;
import org.primefaces.push.PushContextFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class UserSessionBean implements Serializable {

	private static final long serialVersionUID = 1L;

	private String nickname;
	private String message;
	private final PushContext pushContext = PushContextFactory.getDefault()
			.getPushContext();
	private final static String CHANNEL = "/chat/";
	

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
