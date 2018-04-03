package org.powertac.visualizer.logtool;

import java.io.InputStream;

import org.powertac.common.Competition;
import org.powertac.common.msg.TimeslotUpdate;
import org.powertac.logtool.common.NewObjectListener;
import org.powertac.logtool.common.NoopAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogtoolExecutor extends NoopAnalyzer {

    static private Logger log = LoggerFactory.getLogger(LogtoolExecutor.class);

    private NewObjectListener objListener;
    private ObjectHandler objectHandler;
    private String logName;

    /**
     * Constructor does nothing. Call setup() before reading a file to get this
     * to work.
     */
    public LogtoolExecutor() {
        super();
    }

    public String readLog(InputStream logStream, NewObjectListener listener, int timeslotPause) {
        this.logName = "(stream)";
        objListener = listener;
        // TODO - magic number here
        getCore().setPerTimeslotPause(timeslotPause);
        return getCore().readStateLog(logStream, this);
    }

    /**
     * Creates data structures, opens output file. It would be nice to dump the
     * broker names at this point, but they are not known until we hit the first
     * timeslotUpdate while reading the file.
     */
    @Override
    public void setup ()
    {
      log.info("Starting replay of " + logName);
      if (objectHandler == null) {
        objectHandler = new ObjectHandler();
        registerNewObjectListener(objectHandler, null);
      }
    }

    @Override
    public void report() {
        log.info("Finished replay of " + logName);
        objectHandler.ignore = true;
    }

    public void interrupt() {
      getCore().interrupt();
      log.info("Interrupted replay of " + logName);
      objectHandler.ignore = true;
    }


    // This suppresses all events until the first TimeslotUpdate. At that point
    // it sends the current competition object, and commences forwarding all
    // events from there on out.
    protected class ObjectHandler
    extends NoopAnalyzer.ObjectHandler {

        private boolean ignore = true;

        @Override
        public void handleNewObject(Object thing) {
            if (ignore) {
                if (thing instanceof TimeslotUpdate) {
                    // send competition
                    objListener.handleNewObject(Competition.currentCompetition());
                    ignore = false;
                } else {
                    log.warn("Ignoring message " + thing.getClass().getName());
                    return;
                }
            }
            objListener.handleNewObject(thing);
        }
    }
}
