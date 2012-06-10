package org.powertac.visualizer.user;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.powertac.visualizer.domain.broker.TariffInfo;
import org.springframework.context.annotation.Scope;


public class BrokersBean implements Serializable {

	private static final long serialVersionUID = 1L;
	private TariffInfo selectedTariffInfo;
	
	public TariffInfo getSelectedTariffInfo() {
		return selectedTariffInfo;
	}
	public void setSelectedTariffInfo(TariffInfo selectedTariffInfo) {
		this.selectedTariffInfo = selectedTariffInfo;
	}
}
