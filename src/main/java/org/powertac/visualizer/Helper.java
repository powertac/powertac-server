package org.powertac.visualizer;

import java.math.BigDecimal;


import org.apache.log4j.Logger;
import org.powertac.common.TariffTransaction;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONException;

public class Helper {
	
	private static Logger log=Logger.getLogger(Helper.class);

	/**
	 * @param tariffTransaction
	 * @return positive number for new customers, negative number for ex customers. Otherwise, zero is returned.
	 */
	public static int getCustomerCount(TariffTransaction tariffTransaction){
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
	
	public static void updateJSON(JSONArray array, int x, double y) {
		int decimal_points = 2;

		JSONArray point = new JSONArray();
		BigDecimal bd = new BigDecimal(y);
		bd.setScale(decimal_points, BigDecimal.ROUND_HALF_UP);
		try {
			point.put(x).put(bd.doubleValue());
			array.put(x, point);
		} catch (JSONException e) {
			log.warn("Problems with JSON");
		}

	
	}
}
