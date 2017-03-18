package org.powertac.visualizer.services.handlers;

import org.powertac.common.Broker;
import org.powertac.common.CashPosition;
import org.powertac.common.MarketPosition;
import org.powertac.common.MarketTransaction;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.domain.genco.Genco;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.GencoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
public class GencoMessageHandler implements Initializable {

	@Autowired
	private MessageDispatcher router;

	@Autowired
	private GencoService gencoService;

	@Autowired
	private BrokerService brokerService;

  private Boolean tournamentMode = false;

	public void initialize() {
		for (Class<?> clazz : Arrays.asList(CashPosition.class, MarketTransaction.class, MarketPosition.class)) {
			router.registerMessageHandler(this, clazz);
		}
	}

	/**
	 * Responsible for updating genco's cash balance. This is the only method
	 * that will create genco if it can't be found.
	 * 
	 * @param position
	 */
	public void handleMessage(CashPosition position) {
		Broker broker = position.getBroker();
		
		// only care about the wholesale broker
		if (isWholesaleBroker(broker)) {
			Genco genco = gencoService.findGencoByUsername(broker.getUsername());
			if (genco == null) {
				genco = gencoService.addGenco(broker);
			}
			genco.addCashPosition(position);
		}
	}

	public void handleMessage(MarketTransaction transaction) {
		Broker broker = transaction.getBroker();

		// only care about the wholesale broker
    if (isWholesaleBroker(broker)) {
			Genco genco = gencoService.findGencoByUsername(broker.getUsername());
			genco.findWholesaleDataByTimeslot(
          transaction.getTimeslot()).addMarketTransaction(transaction);
		}
	}

	public void handleMessage(MarketPosition position) {
		Broker broker = position.getBroker();

		// only care about the wholesale broker
    if (isWholesaleBroker(broker)) {
			Genco genco = gencoService.findGencoByUsername(broker.getUsername());
			genco.findWholesaleDataByTimeslot(
          position.getTimeslot()).addMarketPosition(position);
		}
	}

  private Boolean isWholesaleBroker(Broker broker)
  {
    if (tournamentMode) {
      // hard-coding for JMS: broker is a wholesale broker if it is not
      // listed in broker service collection...
      return brokerService.findBrokerByName(broker.getUsername()) == null;
    }
    else {
      return broker.isWholesale();
    }
  }

  public Boolean getTournamentMode() {
    return tournamentMode;
  }
  public void setTournamentMode(Boolean tournamentMode) {
    this.tournamentMode = tournamentMode;
  }
}
