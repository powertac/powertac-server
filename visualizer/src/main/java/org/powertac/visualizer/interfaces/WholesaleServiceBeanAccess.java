package org.powertac.visualizer.interfaces;

import java.util.concurrent.ConcurrentHashMap;

import org.powertac.common.ClearedTrade;

public interface WholesaleServiceBeanAccess {

	public ConcurrentHashMap<Long, ConcurrentHashMap<Long,ClearedTrade>> getFinalClearedTrades();
}
