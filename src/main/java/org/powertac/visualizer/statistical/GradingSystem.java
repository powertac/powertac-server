package org.powertac.visualizer.statistical;

/**
 * This class will compute broker's grade for a particular category. Each category will have the same scale (e.g., 0-100, where 0 is the worst performance while 100 means a broker performs excellent in an observed category.)
 * 
 * @author Jurica Babic
 * 
 */
public class GradingSystem {

	/**
	 * Calculates a grade for broker's balancing performance.
	 * 
	 * @param brokerKWh Total kWh of imbalance from a broker.
	 * @param brokerDistributedKWh Total distributed kWh for a broker. 
	 * @return Returns a grade.
	 */
	public static double getBalancingGrade(double brokerKWh, double brokerDistributedKWh) {
		if (brokerDistributedKWh == 0) {
			return 0;
		} else {
			// we only care about absolute values:
			brokerKWh = Math.abs(brokerKWh);
			brokerDistributedKWh = Math.abs(brokerDistributedKWh);
			return brokerKWh/brokerDistributedKWh;
	
		}

	}

	/**
	 * Calculates how well did broker perform in the wholesale market. It currently depends on ratio between the number of successful market transactions and the total number of broker's published orders. 
	 * @param noOrders
	 * @param noMarketTransactions
	 * @return
	 */
	public static double getWholesaleMarketGrade(int noOrders,
			int noMarketTransactions) {
		if (noOrders == 0) {
			return 0;
		} else {
			return 1.0*noMarketTransactions / noOrders;
			

		}
		
	}
}
