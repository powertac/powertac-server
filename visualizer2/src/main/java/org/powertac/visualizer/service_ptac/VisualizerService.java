package org.powertac.visualizer.service_ptac;

import org.powertac.visualizer.config.ApplicationProperties;
import org.powertac.visualizer.config.Constants;
import org.powertac.visualizer.domain.Broker;
import org.powertac.visualizer.domain.Customer;
import org.powertac.visualizer.domain.Tariff;
import org.powertac.visualizer.repository_ptac.RecycleRepository;
import org.powertac.visualizer.web.websocket.Pusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Collection;

import jakarta.annotation.PostConstruct;

/**
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@Service
public class VisualizerService {

    private Logger log = LoggerFactory.getLogger(VisualizerService.class);

    public enum VisualizerState {
        IDLE, WAITING, RUNNING, FINISHED, FAILED
    }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Pusher pusher;

    @SuppressWarnings("rawtypes")
    private Collection<RecycleRepository> repositories;

    private VisualizerState state;

    @Autowired
    private ApplicationProperties applicationProperties;

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        String mode = applicationProperties.getMode().trim();
        if (mode == null || mode.isEmpty()) {
            log.debug("No mode provided, falling back to 'research'");
            mode = Constants.MODE_RESEARCH;
            applicationProperties.setMode(mode);
        }
        // TODO RuntimeException is probably not the right class here...?
        if (mode.equals(Constants.MODE_TOURNAMENT)) {
            if (applicationProperties.getConnect().getServerUrl().isEmpty()) {
                throw new RuntimeException(
                        "In tournament mode, a 'serverUrl' is required!");
            }
        } else if (!mode.equals(Constants.MODE_RESEARCH)) {
            throw new RuntimeException("Unsupported mode '" + mode + "'");
        }
        state = VisualizerState.IDLE;
        registerAllRecyclables();
    }

    public String getMode() {
        return applicationProperties.getMode();
    }

    public int getTimeslotPause() {
        return applicationProperties.getTimeslotPause();
    }

    public String getMachineName() {
        return applicationProperties.getConnect().getMachineName();
    }

    public String getServerUrl() {
        return applicationProperties.getConnect().getServerUrl();
    }

    public String getTournamentUrl() {
        return applicationProperties.getConnect().getTournamentUrl();
    }

    public String getTournamentPath() {
        return applicationProperties.getConnect().getTournamentPath();
    }

    public void newRun() {
        init();
        // TODO Reset current competition
    }

    public void init() {
        recycleAll();
    }

    public void registerAllRecyclables () {
      repositories = context.getBeansOfType(RecycleRepository.class).values();
    }

    public void recycleAll() {
        for (RecycleRepository<?> repository: repositories) {
            repository.recycle();
        }
        Broker.recycle();
        Customer.recycle();
        Tariff.recycle();
    }

    public VisualizerState getState() {
        return state;
    }

    public void setState(VisualizerState state) {
        this.state = state;
        pusher.sendGameStatusMessage(state);
    }

}
