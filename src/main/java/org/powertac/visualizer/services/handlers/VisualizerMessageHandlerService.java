package org.powertac.visualizer.services.handlers;

import org.apache.log4j.Logger;
import org.powertac.common.BankTransaction;
import org.powertac.common.Competition;
import org.powertac.common.MarketPosition;
import org.powertac.common.MarketTransaction;
import org.powertac.common.msg.*;
import org.powertac.visualizer.MessageDispatcher;
import org.powertac.visualizer.VisualizerApplicationContext;
import org.powertac.visualizer.beans.VisualizerBean;
import org.powertac.visualizer.interfaces.Initializable;
import org.powertac.visualizer.interfaces.TimeslotCompleteActivation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class VisualizerMessageHandlerService implements Initializable
{
  private Logger log = Logger.getLogger(VisualizerMessageHandlerService.class);
  @Autowired
  private VisualizerBean visualizerBean;
  @Autowired
  private VisualizerMessageHandlerHelperService helper;
  @Autowired
  private MessageDispatcher router;

  public void handleMessage (Competition competition)
  {
    visualizerBean.setRunning(true);
    visualizerBean.setCompetition(competition);
  }

  public void handleMessage (TimeslotUpdate timeslotUpdate)
  {
    visualizerBean.setTimeslotUpdate(timeslotUpdate);
    // millis: should include timezone in the future.
    visualizerBean.setCurrentMillis(timeslotUpdate.getPostedTime().getMillis());

    // for the first timeslot:
    if (visualizerBean.getFirstTimeslotInstant() == null)
      visualizerBean.setFirstTimeslotInstant(timeslotUpdate.getPostedTime());

    StringBuilder builder = new StringBuilder();
    builder.append("Enabled timeslots:");
    for (int i = timeslotUpdate.getFirstEnabled(); i < timeslotUpdate
            .getLastEnabled(); i++) {

      builder.append(" ").append(i);
    }

    log.debug(builder + "\n");

    int relativeTimeslotIndex =
      helper.computeRelativeTimeslotIndex(timeslotUpdate.getPostedTime());
    // for all brokers and gencos:
    // helper.updateTimeslotIndex(relativeTimeslotIndex);
    // update for visualizerBean:
    visualizerBean.setRelativeTimeslotIndex(relativeTimeslotIndex);

    log.debug("\nTimeslot index: " + relativeTimeslotIndex + "\nPostedtime:"
              + timeslotUpdate.getPostedTime());

    Competition comp = visualizerBean.getCompetition();
    // timeslot serial number:
    int timeslotSerialNumber =
      timeslotUpdate.getFirstEnabled() - comp.getDeactivateTimeslotsAhead();
    visualizerBean.setCurrentTimeslotSerialNumber(timeslotSerialNumber);

    visualizerBean.setWeek(timeslotSerialNumber / (24 * 7));
    visualizerBean.setDay(timeslotSerialNumber / 24);
    visualizerBean.setHour(timeslotSerialNumber % 24);
  }

  public void handleMessage (TimeslotComplete complete)
  {
    if (visualizerBean.getFirstTimeslotIndex() == -1) {
      visualizerBean.setFirstTimeslotIndex(complete.getTimeslotIndex());
    }

    // activate beans that implement timeslotcompleteactivation interface:
    List<TimeslotCompleteActivation> activators =
      VisualizerApplicationContext
              .listBeansOfType(TimeslotCompleteActivation.class);
    for (TimeslotCompleteActivation active: activators) {
      log.debug("activating..." + active.getClass().getSimpleName());
      active.activate(complete.getTimeslotIndex(), visualizerBean
              .getTimeslotUpdate().getPostedTime());
    }

    // new day? if so, make new day overview:
    int relativeTimeslotIndex =
      complete.getTimeslotIndex() - visualizerBean.getFirstTimeslotIndex();

    if (relativeTimeslotIndex % 23 == 0
        && visualizerBean.getFirstTimeslotIndex() != complete
                .getTimeslotIndex()) {
      helper.buildDayOverview();
    }
  }

  public void handleMessage (SimStart simStart)
  {

  }

  public void handleMessage (SimEnd simEnd)
  {
    visualizerBean.setRunning(false);
    visualizerBean.setFinished(true);
  }

  public void handleMessage (SimPause simPause)
  {
    // TODO
  }

  public void handleMessage (BankTransaction bankTransaction)
  {
    // TODO
  }

  public void handleMessage (MarketTransaction marketTransaction)
  {
    log.debug("\nBroker: "
              + marketTransaction.getBroker().getUsername()
              + " MWh: "
              + marketTransaction.getMWh()
              + "\n Price: "
              + marketTransaction.getPrice()
              + " Postedtime: "
              + marketTransaction.getPostedTime()
              + " Timeslot\n Serial Number: "
              + marketTransaction.getTimeslot().getSerialNumber()
              + " Index: "
              + helper.computeRelativeTimeslotIndex(marketTransaction
                      .getTimeslot().getStartInstant()));

    // TODO
  }

  public void handleMessage (MarketPosition marketPosition)
  {
    log.debug("\nBroker: "
              + marketPosition.getBroker()
              + " Overall Balance: "
              + marketPosition.getOverallBalance()
              + "\n Timeslot:\n serialnumber: "
              + marketPosition.getTimeslot().getSerialNumber()
              + " Timeslot\n Serial Number: "
              + marketPosition.getTimeslot().getSerialNumber()
              + " Index: "
              + helper.computeRelativeTimeslotIndex(marketPosition
                      .getTimeslot().getStartInstant()));
    // TODO
  }

  public void handleMessage (SimResume simResume)
  {
    // TODO
  }

  public void handleMessage (DistributionReport report)
  {
    log.debug("DIST REPORT: " + "PROD " + report.getTotalProduction() + "CONS "
              + report.getTotalConsumption());
  }

  public void handleMessage (TariffExpire msg)
  {
    // TODO
  }

  public void handleMessage (TariffRevoke msg)
  {
    // TODO
  }

  public void handleMessage (TariffStatus msg)
  {
    // TODO
  }

  public void handleMessage (TariffUpdate msg)
  {
    // TODO
  }

  public void initialize ()
  {
    for (Class<?> clazz: Arrays.asList(DistributionReport.class,
                                       SimResume.class, SimEnd.class,
                                       MarketPosition.class,
                                       MarketTransaction.class,
                                       BankTransaction.class, SimPause.class,
                                       SimStart.class, TimeslotComplete.class,
                                       TimeslotUpdate.class, Competition.class,
                                       TariffExpire.class, TariffRevoke.class,
                                       TariffStatus.class, TariffUpdate.class)) {
      router.registerMessageHandler(this, clazz);
    }
  }

}
