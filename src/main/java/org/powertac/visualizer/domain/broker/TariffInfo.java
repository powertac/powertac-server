package org.powertac.visualizer.domain.broker;

import org.apache.log4j.Logger;
import org.powertac.common.Rate;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.visualizer.Helper;
import org.powertac.visualizer.interfaces.TimeslotModelUpdate;
import org.powertac.visualizer.json.TariffInfoJSON;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * TariffInfo tracks a tariff offered by a broker to customers.
 * 
 * @author Jurica Babic
 * 
 */
public class TariffInfo implements TimeslotModelUpdate {

	private Logger log = Logger.getLogger(TariffInfo.class);

	private TariffSpecification tariffSpecification;
	private ArrayList<TariffTransaction> tariffTransactions = new ArrayList<TariffTransaction>();
	private ArrayList<String> tariffLifecycle = new ArrayList<String>();
	private ArrayList<RateInfo> rateInfos;
	private TariffInfoJSON json = new TariffInfoJSON();

	private int subscribedPopulation;

	private double totalRevenue;
	// private double totalRevenuePerUnit;
	private double hourlyRevenue;

	private double totalKWh;
	// private double totalKWhPerUnit;
	private double hourlyKWh;

	public TariffInfo(TariffSpecification tariffSpecification) {
		this.tariffSpecification = tariffSpecification;
		// Rates:
		List<Rate> rates = tariffSpecification.getRates();
		rateInfos = new ArrayList<RateInfo>(rates.size());

		if (rates.size() == 1) {
			RateInfo rateInfo = new RateInfo(rates.get(0));
			rateInfos.add(rateInfo);
			json.setRatesLineChartMaxValue(rateInfo.getJson().getRateLineChartMaxValue());
			json.setRatesLineChartMinValue(rateInfo.getJson().getRateLineChartMinValue());
		} else {
			
			JSONArray ratesJsonMax = new JSONArray();
			JSONArray ratesJsonMin = new JSONArray();

      for (Rate rate: rates) {
        RateInfo rateInfo = new RateInfo(rate);
        rateInfos.add(rateInfo);

        JSONArray maxArray = rateInfo.getJson().getRateLineChartMaxValue();
        JSONArray minArray = rateInfo.getJson().getRateLineChartMinValue();

        for (int i = 0; i < maxArray.length(); i++) {
          try {
            ratesJsonMax.put(maxArray.get(i));
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }

        for (int i = 0; i < minArray.length(); i++) {
          try {
            ratesJsonMin.put(minArray.get(i));
          } catch (JSONException e) {
            e.printStackTrace();
          }
        }
      }
			
			json.setRatesLineChartMaxValue(ratesJsonMax);
			json.setRatesLineChartMinValue(ratesJsonMin);
		}
		
		tariffLifecycle.add(tariffSpecification.toString());
	}

	public void addTariffMessage(String msg) {
		tariffLifecycle.add(msg);
	}

	public void addTariffTransaction(TariffTransaction tx) {
		tariffTransactions.add(tx);
		subscribedPopulation += Helper.getCustomerCount(tx);

		hourlyRevenue += tx.getCharge();
		hourlyKWh += tx.getKWh();

	}

	private void resetHourlyValues() {
		hourlyRevenue = 0;
		hourlyKWh = 0;
	}

	public void update(int timeslotIndex, org.joda.time.Instant postedTime) {

		// double hourlyKWhPerUnit = 0;
		// double hourlyRevenuePerUnit = 0;
		//
		// if (subscribedPopulation != 0) {
		// hourlyKWhPerUnit = hourlyKWh / subscribedPopulation;
		// hourlyRevenuePerUnit = hourlyRevenue / subscribedPopulation;
		// }

		totalKWh += hourlyKWh;
		// totalKWhPerUnit += hourlyKWhPerUnit;
		totalRevenue += hourlyRevenue;
		// totalRevenuePerUnit += hourlyRevenuePerUnit;

		try {

			JSONArray totalRevenuePoint = new JSONArray();
			totalRevenuePoint.put(timeslotIndex).put(totalRevenue);
			json.getTotalRevenueLineChart().put(totalRevenuePoint);

			// JSONArray totalRevenuePerUnitPoint = new JSONArray();
			// totalRevenuePerUnitPoint.put(timeslotIndex).put(totalRevenuePerUnit);
			// json.getTotalRevenuePerUnitLineChart().put(totalRevenuePerUnitPoint);

			JSONArray hourlyRevenuePoint = new JSONArray();
			hourlyRevenuePoint.put(timeslotIndex).put(hourlyRevenue);
			json.getHourlyRevenueLineChart().put(hourlyRevenuePoint);

			// JSONArray hourlyRevenuePerUnitPoint = new JSONArray();
			// hourlyRevenuePerUnitPoint.put(timeslotIndex).put(hourlyRevenuePerUnit);
			// json.getHourlyRevenuePerUnitLineChart().put(hourlyRevenuePerUnitPoint);

			JSONArray totalKWhPoint = new JSONArray();
			totalKWhPoint.put(timeslotIndex).put(totalKWh);
			json.getTotalKWhLineChart().put(totalKWhPoint);

			// JSONArray totalKWhPerUnitPoint = new JSONArray();
			// totalKWhPerUnitPoint.put(timeslotIndex).put(totalKWhPerUnit);
			// json.getTotalKWhPerUnitLineChart().put(totalKWhPerUnitPoint);

			JSONArray hourlyKWhPoint = new JSONArray();
			hourlyKWhPoint.put(timeslotIndex).put(hourlyKWh);
			json.getHourlyKWhLineChart().put(hourlyKWhPoint);

			// JSONArray hourlyKWhPerUnitPoint = new JSONArray();
			// hourlyKWhPerUnitPoint.put(timeslotIndex).put(hourlyKWhPerUnit);
			// json.getHourlyKWhPerUnitLineChart().put(hourlyKWhPerUnitPoint);

			JSONArray subscribedPopulationPoint = new JSONArray();
			subscribedPopulationPoint.put(timeslotIndex).put(subscribedPopulation);
			json.getSubscribedPopulationLineChart().put(subscribedPopulationPoint);

		} catch (JSONException e) {
			log.warn("JSON update for TariffInfo is not working.");
		}

		resetHourlyValues();
	}

	public Logger getLog() {
		return log;
	}

	public TariffSpecification getTariffSpecification() {
		return tariffSpecification;
	}

	public ArrayList<TariffTransaction> getTariffTransactions() {
		return (ArrayList<TariffTransaction>) tariffTransactions.clone();
	}

	public ArrayList<String> getTariffLifecycle() {
		return (ArrayList<String>) tariffLifecycle.clone();
	}

	public ArrayList<RateInfo> getRateInfos() {
		return (ArrayList<RateInfo>) rateInfos.clone();
	}

	public TariffInfoJSON getJson() {
		return json;
	}

	public int getSubscribedPopulation() {
		return subscribedPopulation;
	}

	public double getTotalRevenue() {
		return totalRevenue;
	}

	// public double getTotalRevenuePerUnit() {
	// return totalRevenuePerUnit;
	// }

	public double getHourlyRevenue() {
		return hourlyRevenue;
	}

	public double getTotalKWh() {
		return totalKWh;
	}

	// public double getTotalKWhPerUnit() {
	// return totalKWhPerUnit;
	// }

	public double getHourlyKWh() {
		return hourlyKWh;
	}

}
