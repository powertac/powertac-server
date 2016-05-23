package org.powertac.visualizer.service_ptac;

import org.powertac.visualizer.config.Constants;
import org.powertac.visualizer.config.VisualizerProperties;
import org.powertac.visualizer.domain.Broker;
import org.powertac.visualizer.domain.Customer;
import org.powertac.visualizer.domain.Tariff;
import org.powertac.visualizer.repository_ptac.RecycleRepository;
import org.powertac.visualizer.web.websocket.Pusher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.Collection;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@Service
public class VisualizerService {

    private Logger log = LoggerFactory.getLogger(VisualizerService.class);

    public enum VisualizerState {
        IDLE, WAITING, RUNNING, FINISHED, FAILED
    }

    @Inject
    private ApplicationContext context;

    @Inject
    private Pusher pusher;

    private Collection<RecycleRepository> repositories;

    private VisualizerState state;

    @Inject
    private VisualizerProperties visualizerProperties;

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        String mode = visualizerProperties.getMode().trim();
        if (mode == null || mode.isEmpty()) {
            log.debug("No mode provided, falling back to 'research'");
            mode = Constants.MODE_RESEARCH;
            visualizerProperties.setMode(mode);
        }
        // TODO RuntimeException is probably not the right class here...?
        if (mode.equals(Constants.MODE_TOURNAMENT)) {
            if (visualizerProperties.getConnect().getServerUrl().isEmpty()) {
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
        return visualizerProperties.getMode();
    }

    public String getMachineName() {
        return visualizerProperties.getConnect().getMachineName();
    }

    public String getJmsUrl() {
        return visualizerProperties.getConnect().getJmsUrl();
    }

    public String getServerUrl() {
        return visualizerProperties.getConnect().getServerUrl();
    }

    public String getTournamentUrl() {
        return visualizerProperties.getConnect().getTournamentUrl();
    }

    public String getTournamentPath() {
        return visualizerProperties.getConnect().getTournamentPath();
    }

    public void newRun() {
        init();
        // TODO Reset current competion
    }

    public void init() {
        recycleAll();
    }

    public void registerAllRecyclables () {
      repositories = context.getBeansOfType(RecycleRepository.class).values();
    }

    public void recycleAll() {
        for (RecycleRepository repository: repositories) {
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
