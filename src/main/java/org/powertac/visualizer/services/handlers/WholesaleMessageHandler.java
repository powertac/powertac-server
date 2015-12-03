package org.powertac.visualizer.services.handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.LogManager;
import org.powertac.common.ClearedTrade;
import org.powertac.common.MarketPosition;
import org.powertac.common.MarketTransaction;
import org.powertac.common.Order;
import org.powertac.common.Orderbook;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.domain.broker.BrokerModel;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.services.BrokerService;
import org.powertac.visualizer.services.GradingService;
import org.powertac.visualizer.services.WholesaleService;
import org.powertac.visualizer.statistical.WholesaleCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WholesaleMessageHandler implements Initializable
{

  private Logger log = LogManager.getLogger(WholesaleMessageHandler.class);

  @Autowired
  private VisualizerBean visualizerBean;
  @Autowired
  private MessageDispatcher router;
  @Autowired
  private WholesaleService wholesaleService;
  @Autowired
  private BrokerService brokerService;
  @Autowired
  private VisualizerHelperService helper;
  @Autowired
  private GradingService gradingBean;

  public void initialize ()
  {
    for (Class<?> clazz: Arrays.asList(Order.class, Orderbook.class,
                                       ClearedTrade.class,
                                       MarketPosition.class,
                                       MarketTransaction.class)) {
      router.registerMessageHandler(this, clazz);
    }

  }

  /**
   * Handles message received when wholesale market transaction has been made.
   * Updates timeslot in marketTxs with received data, and adds received
   * transaction to list of transactions.
   * */
  public void handleMessage (MarketTransaction msg)
  {

    BrokerModel broker =
      brokerService.getBrokersMap().get(msg.getBroker().getUsername());

    if (broker != null) {
      WholesaleCategory wc = broker.getWholesaleCategory();

      int tsIndex = msg.getTimeslot().getSerialNumber();
     // important: price and MWh have different signs
      wc.update(tsIndex, msg.getMWh(), msg.getPrice() * Math.abs(msg.getMWh()));

      wc.getMarketTxs().putIfAbsent(msg.getTimeslot().getSerialNumber(),
                                    new ArrayList<MarketTransaction>(24));
      wc.getMarketTxs().get(msg.getTimeslot().getSerialNumber()).add(msg);
      if (msg.getMWh() > 0){
       // positive value:
    	  wc.addEnergyBought(msg.getMWh());
    	  // negative value:
        wc.addCostFromBuying(-1.0f*Math.abs(msg.getPrice()* msg.getMWh()));
        // positive value:
        gradingBean.addBoughtEnergyWholesaleMarket(msg.getMWh());
        // negative value
        gradingBean.addMoneyFromBuyingWholesaleMarket(-1.0f*Math.abs(msg.getPrice()* msg.getMWh()));
      }
    
      else{
       //negative value:
    	  wc.addEnergySold(msg.getMWh());
        // positive value:
        wc.addRevenueFromSelling(Math.abs(msg.getPrice() * msg.getMWh()));
       //negative value
        gradingBean.addSoldEnergyWholesaleMarket(msg.getMWh());
        //positive value:
        gradingBean.addMoneyFromSellingWholesaleMarket(Math.abs(msg.getPrice() * msg.getMWh()));
      }
    }
  }

  public void handleMessage (MarketPosition marketPosition)
  {

  }

  public void handleMessage (Order order)
  {
    log.info("+++ Order in ts:" + helper.getSafetyTimeslotIndex() + "-- From: "
             + order.getBroker() + ", for TS: " + order.getTimeslotIndex());

    BrokerModel broker =
      brokerService.getBrokersMap().get(order.getBroker().getUsername());

    if (broker != null) {
      WholesaleCategory cat = broker.getWholesaleCategory();
      int tsIndex = order.getTimeslotIndex();
      cat.getOrders().putIfAbsent(tsIndex, new ArrayList<Order>(24));
      cat.getOrders().get(tsIndex).add(order);
    }

  }

  public void handleMessage (Orderbook orderbook)
  {
  }

  public void handleMessage (ClearedTrade ct)
  {
    ConcurrentHashMap<Long, ConcurrentHashMap<Long, ClearedTrade>> map =
      wholesaleService.getClearedTrades();
    // if there is a new key:
    map.putIfAbsent(ct.getTimeslot().getStartInstant().getMillis(),
                    new ConcurrentHashMap<Long, ClearedTrade>(24, 0.75f, 1));

    TimeslotUpdate old = visualizerBean.getOldTimeslotUpdate();

    if (old != null) {
      map.get(ct.getTimeslot().getStartInstant().getMillis())
              .put(old.getPostedTime().getMillis(), ct);
    }
    else {
      log.warn("The old timeslot update does not exist.");
    }

  }

}
