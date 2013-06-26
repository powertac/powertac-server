package org.powertac.visualizer.user;

import com.google.gson.Gson;
import org.powertac.common.enumerations.PowerType;
import org.powertac.visualizer.display.BrokerSeriesTemplate;
import org.powertac.visualizer.display.CustomerStatisticsTemplate;
import org.powertac.visualizer.display.DrillDownTemplate;
import org.powertac.visualizer.display.DrillDownTemplate2;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.domain.broker.TariffData;
import org.powertac.visualizer.domain.broker.TariffDynamicData;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.handlers.VisualizerHelperService;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TariffMarketBean implements Serializable {

	private String tariffDynData;
	private String tariffDynDataOneTimeslot;
	private ArrayList<TariffData> allTarifs = new ArrayList<TariffData>();// tom
	private String customerStatictics;
	private List<TariffData> filteredValue;
	private TariffData selectedTariff;

	@Autowired
	public TariffMarketBean(BrokerService brokerService,
			VisualizerHelperService helper)
  {
		Gson gson = new Gson();

		// Data Array
		ArrayList<Object> tariffData = new ArrayList<Object>();
		ArrayList<Object> tariffDataOneTimeslot = new ArrayList<Object>();
		ArrayList<Object> customerStaticticsArray = new ArrayList<Object>();// tom
		int safetyTsIndex = helper.getSafetyWholesaleTimeslotIndex();

		// brokers:
    for (BrokerModel brokerModel : brokerService.getBrokersMap().values()) {
      Object customerStatisticsBroker;
      long[] customerType = new long[3];
      customerType[0] = 0; // consumption
      customerType[1] = 0; // production
      customerType[2] = 0; // storage
      HashMap<PowerType, Long> customerTypeSpecific = new HashMap<PowerType, Long>()
      {{
          put(PowerType.BATTERY_STORAGE, 0l);
          put(PowerType.CHP_PRODUCTION, 0l);
          put(PowerType.CONSUMPTION, 0l);
          put(PowerType.ELECTRIC_VEHICLE, 0l);
          put(PowerType.FOSSIL_PRODUCTION, 0l);
          put(PowerType.INTERRUPTIBLE_CONSUMPTION, 0l);
          put(PowerType.PRODUCTION, 0l);
          put(PowerType.PUMPED_STORAGE_PRODUCTION, 0l);
          put(PowerType.RUN_OF_RIVER_PRODUCTION, 0l);
          put(PowerType.SOLAR_PRODUCTION, 0l);
          put(PowerType.STORAGE, 0l);
          put(PowerType.THERMAL_STORAGE_CONSUMPTION, 0l);
          put(PowerType.WIND_PRODUCTION, 0l);
        }};
      ArrayList<Object> customerNumberData = new ArrayList<Object>();
      ArrayList<Object> profitData = new ArrayList<Object>();
      ArrayList<Object> netKWhData = new ArrayList<Object>();
      ArrayList<Object> drillDataBroker = new ArrayList<Object>();

      // calculating number of customers for each broker's tariff -- by
      // tom
      Collection<TariffData> allTariffsData = brokerModel
          .getTariffCategory().getTariffData().values();
      for (TariffData td : allTariffsData) {
        if (td.getCustomers() > 0) {
          // Object[] drillData = {
          // "Id: " + Long.toString(td.getSpec().getId()),
          // td.getCustomers() };
          // drillDataBroker.add(drillData);
          if (td.getSpec().getPowerType().isConsumption()) {
            customerType[0] += td.getCustomers();
          }
          if (td.getSpec().getPowerType().isProduction()) {
            customerType[1] += td.getCustomers();
          }
          if (td.getSpec().getPowerType().isStorage()) {
            customerType[2] += td.getCustomers();
          }

          customerTypeSpecific.put(
              td.getSpec().getPowerType(),
              customerTypeSpecific.get(td.getSpec()
                  .getPowerType()) + td.getCustomers());
        }
      }

      ArrayList<Object> data_c = new ArrayList<Object>();
      data_c.add(new PowerTypeTemplate("Consumption", customerTypeSpecific.get(PowerType.CONSUMPTION)));
      data_c.add(new PowerTypeTemplate("Interruptible consumption", customerTypeSpecific.get(PowerType.INTERRUPTIBLE_CONSUMPTION)));
      data_c.add(new PowerTypeTemplate("Thermal storage consumption", customerTypeSpecific.get(PowerType.THERMAL_STORAGE_CONSUMPTION)));

      ArrayList<Object> data_p = new ArrayList<Object>();
      data_p.add(new PowerTypeTemplate("Production", customerTypeSpecific.get(PowerType.PRODUCTION)));
      data_p.add(new PowerTypeTemplate("Chp production", customerTypeSpecific.get(PowerType.CHP_PRODUCTION)));
      data_p.add(new PowerTypeTemplate("Fossil production", customerTypeSpecific.get(PowerType.FOSSIL_PRODUCTION)));
      data_p.add(new PowerTypeTemplate("Run of river production", customerTypeSpecific.get(PowerType.RUN_OF_RIVER_PRODUCTION)));
      data_p.add(new PowerTypeTemplate("Solar production", customerTypeSpecific.get(PowerType.SOLAR_PRODUCTION)));
      data_p.add(new PowerTypeTemplate("Wind production", customerTypeSpecific.get(PowerType.WIND_PRODUCTION)));

      ArrayList<Object> data_s = new ArrayList<Object>();
      data_s.add(new PowerTypeTemplate("Storage", customerTypeSpecific.get(PowerType.STORAGE)));
      data_s.add(new PowerTypeTemplate("Battery storage", customerTypeSpecific.get(PowerType.BATTERY_STORAGE)));
      data_s.add(new PowerTypeTemplate("Electric vehicle", customerTypeSpecific.get(PowerType.ELECTRIC_VEHICLE)));
      data_s.add(new PowerTypeTemplate("Pumped storage production", customerTypeSpecific.get(PowerType.PUMPED_STORAGE_PRODUCTION)));

      DrillDownTemplate2 dt2_c = new DrillDownTemplate2("Consumers", customerType[0], new DrillDown("Specific", data_c));
      DrillDownTemplate2 dt2_p = new DrillDownTemplate2("Producers", customerType[1], new DrillDown("Specific", data_p));
      DrillDownTemplate2 dt2_s = new DrillDownTemplate2("Storage", customerType[2], new DrillDown("Specific", data_s));

      if (customerType[0] > 0) {
        drillDataBroker.add(dt2_c);
      }
      if (customerType[1] > 0) {
        drillDataBroker.add(dt2_p);
      }
      if (customerType[2] > 0) {
        drillDataBroker.add(dt2_s);
      }

      customerStatisticsBroker = new DrillDownTemplate(
          brokerModel.getName(), brokerModel.getAppearance()
          .getColorCode(), drillDataBroker);

      // one timeslot
      ArrayList<Object> customerNumberDataOneTimeslot = new ArrayList<Object>();
      ArrayList<Object> profitDataOneTimeslot = new ArrayList<Object>();
      ArrayList<Object> kwhDataOneTimeslot = new ArrayList<Object>();

      ConcurrentHashMap<Integer, TariffDynamicData> tariffDynData = brokerModel
          .getTariffCategory().getTariffDynamicDataMap();

      Set<Integer> keysTariffDynData = new TreeSet<Integer>(brokerModel
          .getTariffCategory().getTariffDynamicDataMap().keySet())
          .headSet(safetyTsIndex, true);

      // dynamic tariff data:
      for (Integer key : keysTariffDynData) {
        TariffDynamicData dynData = tariffDynData.get(key);
        Object[] timeCustomerCount = {helper.getMillisForIndex(key),
            dynData.getCustomerCount()};
        Object[] profit = {helper.getMillisForIndex(key),
            dynData.getDynamicData().getProfit()};
        Object[] netKWh = {helper.getMillisForIndex(key),
            dynData.getDynamicData().getEnergy()};

        customerNumberData.add(timeCustomerCount);
        profitData.add(profit);
        netKWhData.add(netKWh);

        // one timeslot:
        Object[] customerCountOneTimeslot = {
            helper.getMillisForIndex(key),
            dynData.getCustomerCountDelta()};
        Object[] profitOneTimeslot = {helper.getMillisForIndex(key),
            dynData.getDynamicData().getProfitDelta()};
        Object[] kWhOneTimeslot = {helper.getMillisForIndex(key),
            dynData.getDynamicData().getEnergyDelta()};

        customerNumberDataOneTimeslot.add(customerCountOneTimeslot);
        profitDataOneTimeslot.add(profitOneTimeslot);
        kwhDataOneTimeslot.add(kWhOneTimeslot);
      }
      if (keysTariffDynData.size() == 0) {
        // dummy:
        double[] dummy = {helper.getMillisForIndex(0), 0};
        customerNumberData.add(dummy);
        profitData.add(dummy);
        netKWhData.add(dummy);
        customerNumberDataOneTimeslot.add(dummy);
        profitDataOneTimeslot.add(dummy);
        kwhDataOneTimeslot.add(dummy);
      }
      tariffData.add(new BrokerSeriesTemplate(brokerModel.getName(),
          brokerModel.getAppearance().getColorCode(), 0, // + " PRICE"
          profitData, true));
      tariffData.add(new BrokerSeriesTemplate(brokerModel.getName(),
          brokerModel.getAppearance().getColorCode(), 1, // + " KWH"
          netKWhData, false));
      tariffData.add(new BrokerSeriesTemplate(brokerModel.getName(),
          brokerModel.getAppearance().getColorCode(), 2, // + " CUST"
          true, customerNumberData, false));

      // one timeslot:
      tariffDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
          .getName(), brokerModel.getAppearance() // + " PRICE"
          .getColorCode(), 0, profitDataOneTimeslot, true));
      tariffDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
          .getName(), brokerModel.getAppearance() // + " KWH"
          .getColorCode(), 1, kwhDataOneTimeslot, false));
      tariffDataOneTimeslot.add(new BrokerSeriesTemplate(brokerModel
          .getName(), brokerModel.getAppearance() // + " CUST"
          .getColorCode(), 2, true, customerNumberDataOneTimeslot,
          false));

      customerStaticticsArray.add(new CustomerStatisticsTemplate(
          brokerModel.getName(), brokerModel.getAppearance()
          .getColorCode(), brokerModel.getTariffCategory()
          .getCustomerCount(), customerStatisticsBroker));//
      // tom
      for (TariffData data : brokerModel.getTariffCategory()
          .getTariffData().values()) {
        allTarifs.add(data);
      }
      // -tom
    }// end BROKER for loop
		this.tariffDynData = gson.toJson(tariffData);
		this.tariffDynDataOneTimeslot = gson.toJson(tariffDataOneTimeslot);
		this.customerStatictics = gson.toJson(customerStaticticsArray);
		//log.info("*****" + customerStatictics);

	}

	public String getTariffDynData() {
		return tariffDynData;
	}

	public String getTariffDynDataOneTimeslot() {
		return tariffDynDataOneTimeslot;
	}

	public ArrayList<TariffData> getAllTarifs() {
		return allTarifs;
	}

	public List<TariffData> getFilteredValue() {
		return filteredValue;
	}

	public void setFilteredValue(List<TariffData> filteredValue) {
		this.filteredValue = filteredValue;
	}

	public String getCustomerStatictics() {
		return customerStatictics;
	}

	public TariffData getSelectedTariff() {
		return selectedTariff;
	}

	public void setSelectedTariff(TariffData selectedTariff) {
		this.selectedTariff = selectedTariff;

	}
	
	private class DrillDown{
		String name;
		ArrayList<Object> data;
		
		public DrillDown(String name, ArrayList<Object> data){
			this.name = name;
			this.data = data;
		}
		
	}
	
	private class PowerTypeTemplate{
		String name;
		long y;
		
		public PowerTypeTemplate(String name, long y){
			this.name = name;
			this.y = y;
		}
	}

}
