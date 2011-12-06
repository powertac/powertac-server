package org.powertac.visualizer.beans.backing;

import java.io.Serializable;

import javax.faces.context.FacesContext;

import org.powertac.visualizer.beans.VisualizerBean;
import org.primefaces.component.carousel.Carousel;
import org.primefaces.component.commandbutton.CommandButton;
import org.primefaces.component.outputpanel.OutputPanel;
import org.primefaces.component.poll.Poll;
import org.springframework.beans.factory.annotation.Autowired;

public class CustomerBackingBean implements Serializable {
	private static final long serialVersionUID = 1L;

	private boolean allowCarouselRendering;
	private boolean stopPoller;

	@Autowired
	private VisualizerBean visualizerBean;

	public CustomerBackingBean() {
		allowCarouselRendering = false;
		stopPoller = false;
	}

	public void verifyRendering() {
		if (visualizerBean.getCustomers() == null) {
			allowCarouselRendering = false;
			stopPoller = false;

		} else {
			allowCarouselRendering = true;

		}
	}
	
		
	public boolean isAllowCarouselRendering() {
		return allowCarouselRendering;
	}

	public boolean isStopPoller() {
		
		
		return stopPoller;
	}
	
	
}
