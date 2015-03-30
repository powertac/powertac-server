package org.powertac.common.interfaces;

import java.util.List;

import org.powertac.common.Tariff;

public interface SubscriptionRepoListener {
  /**
   * Called periodically with a list of newly-published Tariffs 
   */
  void updatedSubscriptionRepo();
}
