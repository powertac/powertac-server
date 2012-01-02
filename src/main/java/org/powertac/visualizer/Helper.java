package org.powertac.visualizer;

import org.powertac.common.TariffTransaction;

public class Helper {
	
	/**
	 * @param tariffTransaction
	 * @return positive number for new customers, negative number for ex customers.
	 */
	public static int getCustomerCount(TariffTransaction tariffTransaction){
		// add new customers
				if (tariffTransaction.getTxType().compareTo(TariffTransaction.Type.SIGNUP) == 0) {
					
					return tariffTransaction.getCustomerCount();

				}// remove customers that revoke or withdraw
				else if ((tariffTransaction.getTxType().compareTo(TariffTransaction.Type.WITHDRAW) == 0)
						|| (tariffTransaction.getTxType().compareTo(TariffTransaction.Type.REVOKE) == 0)) {
					return (-1) * tariffTransaction.getCustomerCount();
				}
				else return 0;
	}

}
