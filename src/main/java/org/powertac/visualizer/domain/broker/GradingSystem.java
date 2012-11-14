package org.powertac.visualizer.domain.broker;

/**
 * This class will compute broker's grade for a particular category.
 * 
 * @author Jurica Babic
 * 
 */
public class GradingSystem {

	/**
	 * Calculates a grade for broker's balancing performance.
	 * 
	 * @param brokerKWh Total kWh of imbalance from a broker.
	 * @param totalKWh Total kWh of imbalance for all the brokers.
	 * @return Returns a grade.
	 */
	public static Enum<Grade> getBalancingGrade(double brokerKWh, double totalKWh) {
		if (totalKWh == 0) {
			return Grade.X;
		} else {
			// we only care about absolute values:
			brokerKWh = Math.abs(brokerKWh);

			double percentage = brokerKWh / totalKWh;

			if (percentage <= 0.05) {
				return Grade.S;
			} else if (percentage > 0.05 && percentage <= 0.15) {
				return Grade.A;
			} else if (percentage > 0.15 && percentage <= 0.25) {
				return Grade.B;
			} else if (percentage > 0.25 && percentage <= 0.35) {
				return Grade.C;
			} else if (percentage > 0.35 && percentage <= 0.5) {
				return Grade.D;
			} else if (percentage > 0.5 && percentage <= 1) {
				return Grade.F;
			}
			return Grade.X;

		}

	}
}
