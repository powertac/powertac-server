package org.powertac.visualizer.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.powertac.visualizer.domain.broker.TariffData;
import org.springframework.beans.factory.annotation.Autowired;

public class TariffHelperBean implements Serializable
{

  @Autowired
  TariffMarketBean tariffMarketBean;

  private TariffData selectedTariff;
  private List<TariffData> filteredValue;
  private ArrayList<TariffData> allTarifs;

  public TariffHelperBean ()
  {
  }

  @PostConstruct
  private void doSth ()
  {
    allTarifs = tariffMarketBean.getAllTarifs();
  }

  public TariffData getSelectedTariff ()
  {
    return selectedTariff;
  }

  public void setSelectedTariff (TariffData selectedTariff)
  {
    this.selectedTariff = selectedTariff;
  }

  public ArrayList<TariffData> getAllTarifs ()
  {
    return allTarifs;
  }

  public List<TariffData> getFilteredValue ()
  {
    return filteredValue;
  }

  public void setFilteredValue (List<TariffData> filteredValue)
  {
    this.filteredValue = filteredValue;
  }

}
