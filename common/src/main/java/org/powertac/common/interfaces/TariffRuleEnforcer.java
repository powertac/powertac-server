package org.powertac.common.interfaces;

import org.springframework.integration.Message;
import org.springframework.integration.core.MessageSelector;

/**
 * Created by IntelliJ IDEA.
 * User: ddauer
 * Date: 12/1/10
 * Time: 2:34 PM
 * To change this template use File | Settings | File Templates.
 */
public interface TariffRuleEnforcer extends MessageSelector  {

  @Override
  public boolean accept(Message<?> message);
}
