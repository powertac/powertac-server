package org.powertac.visualizer;

import org.powertac.common.TariffTransaction;

public class Helper {
	
	/**
	 * @param tariffTransaction
	 * @return positive number for new customers, negative number for ex customers.
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

}
