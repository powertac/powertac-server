package org.powertac.visualizer.json;

import org.powertac.common.Rate;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;

/**
 * Object that visualizes rate from tariff specification. Each line chart can
 * contain one or more line segments. Each line segment represent rate value for
 * one day.
 * 
 * @author Jurica Babic
 * 
 */
public class RateInfoJSON {

	private JSONArray rateLineChartMinValue = new JSONArray();
	private JSONArray rateLineChartMaxValue = new JSONArray();

	public RateInfoJSON(Rate rate) {
		buildRateLineCharts(rate);
	}

	private void buildRateLineCharts(Rate rate) {
		int startDayIndex = rate.getWeeklyBegin() == -1 ? 1 : rate.getWeeklyBegin();
		int endDayIndex = rate.getWeeklyEnd() == -1 ? 7 : rate.getWeeklyEnd();

		int startHourIndex = (rate.getDailyBegin() == -1 ? 0 : rate.getDailyBegin());
		int endHourIndex = (rate.getDailyEnd() == -1 ? 23 : rate.getDailyEnd());

		// check if start day is greater than end day.
		if (startDayIndex > endDayIndex) {
			for (int i = 1; i <= endDayIndex; i++) {
				createSingleSegment(i, rate, startHourIndex, endHourIndex);
			}
			for (int i = startDayIndex; i <= 7; i++) {
				createSingleSegment(i, rate, startHourIndex, endHourIndex);
			}

		} else {
			for (int i = startDayIndex; i <= endDayIndex; i++) {
				createSingleSegment(i, rate, startHourIndex, endHourIndex);
			}
		}

	}

	private void createSingleSegment(int i, Rate rate, int startHourIndex, int endHourIndex) {
		try {
			JSONArray lineSegmentMinValue = new JSONArray();
			JSONArray startOfSegmentMinValue = new JSONArray();
			JSONArray endOfSegmentMinValue = new JSONArray();

			JSONArray lineSegmentMaxValue = new JSONArray();
			JSONArray startOfSegmentMaxValue = new JSONArray();
			JSONArray endOfSegmentMaxValue = new JSONArray();

			int currentDayStartIndex = (i - 1) * 24;

			startOfSegmentMinValue.put(currentDayStartIndex + startHourIndex).put(rate.getMinValue());
			endOfSegmentMinValue.put(currentDayStartIndex + endHourIndex).put(rate.getMinValue());
			lineSegmentMinValue.put(startOfSegmentMinValue).put(endOfSegmentMinValue);
			rateLineChartMinValue.put(lineSegmentMinValue);

			startOfSegmentMaxValue.put(currentDayStartIndex + startHourIndex).put(rate.getMaxValue());
			endOfSegmentMaxValue.put(currentDayStartIndex + endHourIndex).put(rate.getMaxValue());
			lineSegmentMaxValue.put(startOfSegmentMaxValue).put(endOfSegmentMaxValue);
			rateLineChartMaxValue.put(lineSegmentMaxValue);
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	public JSONArray getRateLineChartMaxValue() {
		return rateLineChartMaxValue;
	}

	public JSONArray getRateLineChartMinValue() {
		return rateLineChartMinValue;
	}

}
