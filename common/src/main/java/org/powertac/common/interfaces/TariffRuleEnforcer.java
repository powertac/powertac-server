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
public class TariffRuleEnforcer implements MessageSelector  {

    @Override
    public boolean accept(Message<?> message) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

}
