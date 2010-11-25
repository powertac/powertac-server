package org.powertac.server.core.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.powertac.server.core.command.RevokeTariffCommand;

public class AccountingService {

  private final Log log = LogFactory.getLog(AccountingService.class);

  public Boolean revokeTariff(RevokeTariffCommand rtc) {
    log.error ("Revoke tariff " + rtc.getTariffId() + " of broker " + rtc.getAuthToken());
    return true;
  }
}
