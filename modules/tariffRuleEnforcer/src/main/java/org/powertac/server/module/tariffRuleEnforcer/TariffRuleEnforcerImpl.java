package org.powertac.server.module.tariffRuleEnforcer;

import org.powertac.common.commands.TariffReply;
import org.powertac.common.interfaces.TariffRuleEnforcer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TariffRuleEnforcerImpl implements TariffRuleEnforcer {

    final static Logger log = LoggerFactory.getLogger(TariffRuleEnforcerImpl.class);

    @Override
    public boolean accept(TariffReply tariffReply) {
        // Default implementation simply returns true
        return true;
    }
}
