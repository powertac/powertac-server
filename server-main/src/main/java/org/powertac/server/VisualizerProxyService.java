package org.powertac.server;

import java.util.ArrayList;

import org.powertac.common.interfaces.VisualizerMessageListener;
import org.powertac.common.interfaces.VisualizerProxy;
import org.springframework.stereotype.Service;

@Service
public class VisualizerProxyService implements VisualizerProxy
{

  private ArrayList<VisualizerMessageListener> listeners =
      new ArrayList<VisualizerMessageListener>();
  
  public void registerVisualizerMessageListener (VisualizerMessageListener listener)
  {
    if (!listeners.contains(listener))
      listeners.add(listener);
  }

  public void forwardMessage (Object message)
  {
    for (VisualizerMessageListener listener : listeners)
      listener.receiveMessage(message);
  }
}
