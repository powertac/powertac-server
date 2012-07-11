package org.powertac.visualizer;

import java.math.BigDecimal;

import org.apache.log4j.Logger;
import org.joda.time.Chronology;
import org.joda.time.Instant;
import org.powertac.common.TariffTransaction;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.log4j.Logger;

public class Helper {

	private static Logger log = Logger.getLogger(Helper.class);

	/**
	 * @param tariffTransaction
	 * @return positive number for new customers, negative number for ex
	 *         customers. Otherwise, zero is returned.
	 */
	public static int getCustomerCount(TariffTransaction tariffTransaction) {
		// add new customers

		switch (tariffTransaction.getTxType()) {
		case SIGNUP:
			return tariffTransaction.getCustomerCount();
		case REVOKE:
		case WITHDRAW:
			return (-1) * tariffTransaction.getCustomerCount();
		case CONSUME:
		case PERIODIC:
		case PRODUCE:
		case PUBLISH:
		default:
			return 0;
		}
	}

	public static JSONArray pointJSON(int x, double y) {
		int decimal_points = 2;

		JSONArray point = new JSONArray();
		BigDecimal bd = new BigDecimal(y);
		bd.setScale(decimal_points, BigDecimal.ROUND_HALF_UP);
		try {
			point.put(x).put(bd.doubleValue());
			return point;
		} catch (JSONException e) {
			log.warn("Problems with JSON");
		}
		return null;

	}

	public static double roundNumberTwoDecimal(double number) {
		return new BigDecimal(number).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
	}
	
	public static String getJsonDate(Instant time){
		
		int year = time.get(time.getChronology().year());
		int month = time.get(time.getChronology().monthOfYear());
		month--;
		int day = time.get(time.getChronology().dayOfMonth());
		int hour = time.get(time.getChronology().hourOfDay());
		
		String jsonDate = "Date.UTC("+year+","+month+","+day+","+hour+")";
		
		return jsonDate;
		
	}

}
