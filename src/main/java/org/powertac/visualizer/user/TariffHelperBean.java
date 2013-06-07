package org.powertac.visualizer.user;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.powertac.visualizer.domain.broker.TariffData;
import org.springframework.beans.factory.annotation.Autowired;

public class TariffHelperBean {

	@Autowired
	TariffMarketBean tariffMarketBean;

	private TariffData selectedTariff;
	private List<TariffData> filteredValue;
	private ArrayList<TariffData> allTarifs;
	

	public TariffHelperBean() {
		//System.out.println("Inside Tariff Helper Bean constructor");
	}

	@PostConstruct
	private void doSth() {
		System.out.println("inside doSth()");
		allTarifs = tariffMarketBean.getAllTarifs();
	}

	public TariffData getSelectedTariff() {
		return selectedTariff;
	}

	public void setSelectedTariff(TariffData selectedTariff) {
		this.selectedTariff = selectedTariff;

	}

	public ArrayList<TariffData> getAllTarifs() {
		//System.out.println("In getAllTariffs");
		//for(TariffData td:allTarifs)System.out.println(td.getSpec().getId());
		return allTarifs;
	}

	public List<TariffData> getFilteredValue() {
		return filteredValue;
	}

	public void setFilteredValue(List<TariffData> filteredValue) {
		this.filteredValue = filteredValue;
	}

}
