package org.powertac.visualizer.web.websocket;

import org.powertac.visualizer.domain.TickSnapshot;
import org.powertac.visualizer.repository_ptac.BrokerRepository;
import org.powertac.visualizer.repository_ptac.CustomerRepository;
import org.powertac.visualizer.repository_ptac.TickSnapshotRepository;
import org.powertac.visualizer.service_ptac.CompetitionService;
import org.powertac.visualizer.service_ptac.VisualizerService;
import org.powertac.visualizer.service_ptac.VisualizerService.VisualizerState;
import org.powertac.visualizer.web.dto.InitMessage;
import org.powertac.visualizer.web.dto.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

/**
 * @author Jurica Babic, Govert Buijs, Erik Kemperman
 */
@Controller
public class Pusher {

    private static Logger log = LoggerFactory.getLogger(Pusher.class);

    private static final String TOPIC_MESSAGE = "/topic/push";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private VisualizerService visualizerService;

    @Autowired
    private BrokerRepository brokerRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TickSnapshotRepository tickSnapshotRepository;

    @Autowired
    private CompetitionService currentCompetition;

    @SubscribeMapping(TOPIC_MESSAGE)
    public Message pusherInit() {
        return new Message(Message.Type.INIT,
                new InitMessage(visualizerService.getState(),
                        currentCompetition, brokerRepository.findAll(),
                        customerRepository.findAll(),
                        tickSnapshotRepository.findAll()));
    }

    public void sendInitMessage(InitMessage initMessage) {
        messagingTemplate.convertAndSend(TOPIC_MESSAGE,
                new Message(Message.Type.INIT, initMessage));
    }

    public void sendTickSnapshotUpdates(TickSnapshot payload) {
        messagingTemplate.convertAndSend(TOPIC_MESSAGE,
                new Message(Message.Type.DATA, payload));
    }

    public void sendGameStatusMessage(VisualizerState status) {
        messagingTemplate.convertAndSend(TOPIC_MESSAGE,
                new Message(Message.Type.INFO, status));
    }

}
