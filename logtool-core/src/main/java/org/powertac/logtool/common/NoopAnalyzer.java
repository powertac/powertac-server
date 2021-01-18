package org.powertac.logtool.common;

import org.powertac.common.spring.SpringApplicationContext;
import org.powertac.logtool.LogtoolContext;
import org.powertac.logtool.ifc.Analyzer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoopAnalyzer extends LogtoolContext implements Analyzer
{

  static private Logger log = LoggerFactory.getLogger(NoopAnalyzer.class);

  public NoopAnalyzer ()
  {
    super();
    setContext(SpringApplicationContext.getContext());
  }

  @Override
  public void setup ()
  {
    log.info("Starting replay");
    registerNewObjectListener(new ObjectHandler(), null);
  }

  @Override
  public void report ()
  {
    log.info("Finished replay");
  }

  protected class ObjectHandler implements NewObjectListener
  {
    @Override
    public void handleNewObject (Object thing)
    {
      log.info("Received " + thing.getClass() + ": " + thing.toString());
    }
  }
}
