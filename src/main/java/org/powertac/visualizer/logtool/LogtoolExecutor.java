package org.powertac.visualizer.logtool;

import org.powertac.common.Competition;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.logtool.LogtoolContext;
import org.powertac.logtool.common.DomainObjectReader;
import org.powertac.logtool.common.NewObjectListener;
import org.powertac.logtool.ifc.Analyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogtoolExecutor extends LogtoolContext implements Analyzer {

    static private Logger log = LoggerFactory.getLogger(LogtoolExecutor.class.getName());

    private NewObjectListener objListener;
    private String logName;

    /**
     * Constructor does nothing. Call setup() before reading a file to get this
     * to work.
     */
    public LogtoolExecutor() {
        super();
        setContext(SpringApplicationContext.getContext());
    }

    public void readLog(String logName, NewObjectListener listener) {
        log.info("Starting the replay of : " + logName);

        this.logName = logName;
        objListener = listener;
        super.cli(logName, this);
    }

    /**
     * Creates data structures, opens output file. It would be nice to dump the
     * broker names at this point, but they are not known until we hit the first
     * timeslotUpdate while reading the file.
     */
    @Override
    public void setup() {
        DomainObjectReader dor = (DomainObjectReader) getContext().getBean("domainObjectReader");
        dor.registerNewObjectListener(new ObjectHandler(), null);
    }

    @Override
    public void report() {
        // TODO

        log.info("Finished the replay of : " + logName);
    }

    private class ObjectHandler implements NewObjectListener {
        private boolean ignore = true;

        @Override
        public void handleNewObject(Object thing) {
            if (ignore) {
                if (thing instanceof TimeslotUpdate) {
                    // send competition
                    objListener.handleNewObject(Competition.currentCompetition());
                    ignore = false;
                } else {
                    return;
                }
            }
            objListener.handleNewObject(thing);
        }
    }
}
