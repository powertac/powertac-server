package org.powertac.visualizer.service_ptac;

import java.time.Instant;
import org.powertac.common.CashPosition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.MarketTransaction;
import org.powertac.common.TariffSpecification;
import org.powertac.common.TariffTransaction;
import org.powertac.common.TariffTransaction.Type;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.common.msg.SimEnd;
import org.powertac.common.msg.SimPause;
import org.powertac.common.msg.SimResume;
import org.powertac.common.msg.SimStart;
import org.powertac.common.msg.TariffRevoke;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.visualizer.domain.RetailKPIHolder;
import org.powertac.visualizer.domain.Broker;
import org.powertac.visualizer.domain.Customer;
import org.powertac.visualizer.domain.Tariff;
import org.powertac.visualizer.domain.TickSnapshot;
import org.powertac.visualizer.domain.WholesaleKPIHolder;
import org.powertac.visualizer.repository_ptac.BrokerRepository;
import org.powertac.visualizer.repository_ptac.CustomerRepository;
import org.powertac.visualizer.repository_ptac.TariffRepository;
import org.powertac.visualizer.repository_ptac.TickSnapshotRepository;
import org.powertac.visualizer.service_ptac.VisualizerService.VisualizerState;
import org.powertac.visualizer.web.dto.InitMessage;
import org.powertac.visualizer.web.dto.TickValueBroker;
import org.powertac.visualizer.web.dto.TickValueCustomer;
import org.powertac.visualizer.web.websocket.Pusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * This is an implementation of the Initializable interface. It will be
 * initialized upon startup and registered for handling Power TAC messages in
 * case there are proper method signatures, e.g.,
 * "handleMessage(VizCompetition competition)". intended for the Visualizer.
 *
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@Service
public class MessageHandler {

    static private Logger log = LoggerFactory.getLogger(MessageHandler.class);

    @Autowired
    private BrokerRepository brokerRepo;

    @Autowired
    private CustomerRepository customerRepo;

    @Autowired
    private TickSnapshotRepository tickSnapshotRepo;

    @Autowired
    private TariffRepository tariffRepo;

    @Autowired
    private VisualizerService visualizerService;

    @Autowired
    public Pusher pusher;

    @Autowired
    private CompetitionService currentCompetition;

    // TODO Should be in VisualizerService?
    // Or in current competition?
    private int currentTimeslot = 0;
    private Instant currentInstant;

    public void initialize() {
        // TODO not used?
    }

    public synchronized void handleMessage(org.powertac.common.Competition c) {
        // create vizCompetition
        org.powertac.common.Competition.setCurrent(c);
        currentCompetition.setCurrent(c);

        // create brokers
        for (String n : c.getBrokers()) {
            Broker broker = new Broker(n);
            brokerRepo.save(broker);
        }

        // create customers:
        for (CustomerInfo ci : c.getCustomers()) {
            Customer customer = new Customer(ci);
            customerRepo.save(customer);
        }

        currentInstant = null;
        currentTimeslot = c.getBootstrapTimeslotCount() + c.getBootstrapDiscardedTimeslots() - 1;

        log.info("VizCompetition received");
    }

    /**
     * Receives the SimPause message, used to pause the clock. While the clock
     * is paused, the broker needs to ignore the local clock.
     */
    public synchronized void handleMessage(SimPause sp) {
        // local brokers can ignore this.
        // log.debug("Paused at " +
        // timeService.getCurrentDateTime().toString());
        // pausedAt = timeslotRepo.currentSerialNumber();
    }

    /**
     * Receives the SimResume message, used to update the clock.
     */
    public synchronized void handleMessage(SimResume sr) {
        // local brokers don't need to handle this
        log.trace("SimResume received");
        // pausedAt = 0;
        // timeService.setStart(sr.getStart().toInstant().toEpochMilli() - serverClockOffset);
        // timeService.updateTime();
    }

    /**
     * Receives the SimStart message, used to start the clock. The server's
     * clock offset is subtracted from the start time indicated by the server.
     */
    public synchronized void handleMessage(SimStart ss) {
        log.debug("SimStart received - start time is " + ss.getStart().toString());
        visualizerService.setState(VisualizerState.RUNNING);
    }

    /**
     * Receives the SimEnd message, which ends the broker session.
     */
    public synchronized void handleMessage(SimEnd se) {
        log.info("SimEnd received");
        visualizerService.setState(VisualizerState.FINISHED);
    }

    /**
     * Updates the sim clock on receipt of the TimeslotUpdate message, which
     * should be the first to arrive in each timeslot.
     */
    public synchronized void handleMessage(TimeslotUpdate tu) {
        if (currentInstant == null) {
            // skip reporting on a very first timeslot update
            // but send a control message so the front-end can be initialized:
            currentInstant = tu.getPostedTime();
            InitMessage initMessage = new InitMessage(
                    visualizerService.getState(), currentCompetition,
                    brokerRepo.findAll(), customerRepo.findAll(),
                    tickSnapshotRepo.findAll());

            log.trace("handleMessage(TimeslotUpdate), about to make a call to "
                    + "pusher.sendInitMessage ");
            pusher.sendInitMessage(initMessage);

            return;
        }
        currentInstant = tu.getPostedTime();
        currentTimeslot++;
        perTimeslotUpdate();
    }

    public synchronized void handleMessage(CustomerBootstrapData cbd) {
        Customer customer = customerRepo.findByName(cbd.getCustomerName());
        customer.setBootstrapNetUsage(Arrays.stream(cbd.getNetUsage()).boxed().collect(Collectors.toList()));
    }

    /**
     * CashPosition is the last message sent by Accounting. This is normally
     * when any broker would submit its bids, so that's when this VizBroker will
     * do it.
     */
    public synchronized void handleMessage(CashPosition cp) {
        org.powertac.common.Broker ptacBroker = cp.getBroker();

        // we only care about standard (retail+wholesale) brokers
        if (!ptacBroker.isWholesale()) {
            Broker broker = brokerRepo.findByName(ptacBroker.getUsername());
            if (broker != null) {
                broker.setCash(cp.getBalance());
            }
        }
    }

    /**
     * Handles a TariffSpecification. These are sent by the server when new
     * tariffs are published. If it's not ours, then it's a competitor's tariff.
     * We keep track of competing tariffs locally, and we also store them in the
     * tariffRepo.
     */
    public synchronized void handleMessage(TariffSpecification spec) {
        Broker broker = brokerRepo.findByName(spec.getBroker().getUsername());
        if (broker != null) {
            Tariff tariff = new Tariff(broker, spec);
            tariffRepo.save(tariff);
            broker.getRetail().incrementPublishedTariffs();
        } else {
            log.error("VizBroker " + spec.getBroker() + " cannot be found.");
        }
    }

    /**
     * Handles a TariffTransaction. We only care about certain types: PRODUCE,
     * CONSUME, SIGNUP, and WITHDRAW.
     */
    public synchronized void handleMessage(TariffTransaction ttx) {
        try {
            // make sure we have this tariff
            TariffSpecification newSpec = ttx.getTariffSpec();
            if (newSpec == null) {
                log.error("TariffTransaction type={} for unknown spec",
                          ttx.getTxType());
            } else {
                Tariff oldSpec = tariffRepo.findById(newSpec.getId());
                if (oldSpec == null &&
                    !newSpec.getBroker().getUsername().equals("default broker")) {
                  if (!Type.PUBLISH.equals(ttx.getTxType())) {
                    log.error("Incoming spec {} for {} not matched in repo",
                              newSpec.getId(), ttx.getBroker().getUsername());
                  }
                }
            }

            TariffTransaction.Type txType = ttx.getTxType();
            ArrayList<RetailKPIHolder> retailKPIHolders = new ArrayList<>();

            Customer customer = null;
            if (ttx.getCustomerInfo() != null) {
                customer = customerRepo.findById(ttx.getCustomerInfo().getId());
            }
            if (customer != null) {
                retailKPIHolders.add(customer.getRetail());
            }
            Broker broker = brokerRepo.findByName(ttx.getBroker().getUsername());
            if (broker != null) {
                retailKPIHolders.add(broker.getRetail());
            }

            Tariff tariff = null;
            if (newSpec != null) {
                tariff = tariffRepo.findById(newSpec.getId());
            }
            if (tariff != null) {
                retailKPIHolders.add(tariff.getRetail());
            }

            for (RetailKPIHolder record : retailKPIHolders) {
                if (TariffTransaction.Type.SIGNUP == txType) {
                    log.debug("SIGNUP:" + ttx.toString() + "cnt_customers:" + ttx.getCustomerCount());
                    // keep track of customer counts
                    record.signup(ttx.getCustomerCount());
                } else if (TariffTransaction.Type.WITHDRAW == txType) {
                    log.debug("WITHDRAW:" + ttx.toString() + "cnt_customers:" + ttx.getCustomerCount());
                    // customers presumably found a better deal
                    record.withdraw(ttx.getCustomerCount());
                } else if (TariffTransaction.Type.PRODUCE == txType) {
                    record.produceConsume(ttx.getKWh(), ttx.getCharge());
                } else if (TariffTransaction.Type.CONSUME == txType) {
                    record.produceConsume(ttx.getKWh(), ttx.getCharge());
                } else if (TariffTransaction.Type.PERIODIC == txType) {
                  record.produceConsume(ttx.getKWh(), ttx.getCharge());
                }
            }
        } catch (NullPointerException npe) {
            StringBuilder stack = new StringBuilder();
            for (int i = 0; i < npe.getStackTrace().length; i++) {
                stack.append(npe.getStackTrace()[i]);
            }
            log.error("TariffTransaction NPE:" + npe.getMessage() + " " + stack);
        }
    }

    /**
     * Handles a TariffRevoke message from the server, indicating that some
     * tariff has been revoked.
     */
    public synchronized void handleMessage(TariffRevoke tr) {
        log.trace("Revoke tariff " + tr.getTariffId() + " from " + tr.getBroker().getUsername());

        Tariff tariff = tariffRepo.findById(tr.getTariffId());
        tariff.setActive(false);

        Broker broker = tariff.getBroker();
        broker.getRetail().incrementRevokedTariffs();
    }

    /** Handles a wholesale MarketTransaction */
    public synchronized void handleMessage(MarketTransaction mtx) {
        Broker broker = brokerRepo.findByName(mtx.getBroker().getUsername());
        if (broker != null) {
            broker.getWholesale().addTransaction(mtx);
        } /* else if (mtx.getPrice() * mtx.getMWh() >= 0) {
          System.out.print("Weird MarketTx for broker " + mtx.getBroker().getUsername() + ": ");
          System.out.println("  #" + mtx.getTimeslotIndex() + "\t " + mtx.getBroker().getUsername() + "\t " + mtx.getMWh() + " @ " + mtx.getPrice());
        } */
    }

    private void perTimeslotUpdate() {
        TickSnapshot ts = new TickSnapshot(currentInstant.toInstant().toEpochMilli(), currentTimeslot);
        tickSnapshotRepo.save(ts);

        // reset per time slot KPI values:
        for (Broker broker : brokerRepo.findAll()) {
            TickValueBroker tv = new TickValueBroker(broker,
                    new RetailKPIHolder(broker.getRetail()),
                    new WholesaleKPIHolder(broker.getWholesale(), currentTimeslot));
            ts.getTickValueBrokers().add(tv);
            broker.getRetail().resetCurrentValues();
            broker.getWholesale().resetCurrentValues();
        }

        for (Customer customer : customerRepo.findAll()) {
            TickValueCustomer tv = new TickValueCustomer(customer.getId(),
                    new RetailKPIHolder(customer.getRetail()));
            if (!tv.isEmpty()) {
                ts.getTickValueCustomers().add(tv);
            }
            customer.getRetail().resetCurrentValues();
        }

        for (Tariff tariff : tariffRepo.findAll()) {
            tariff.getRetail().resetCurrentValues();
        }

        log.trace("perTimeslotUpdate(), about to make a call to "
                + "pusher.sendTickSnapshotUpdates ");

        pusher.sendTickSnapshotUpdates(ts);
    }
}
