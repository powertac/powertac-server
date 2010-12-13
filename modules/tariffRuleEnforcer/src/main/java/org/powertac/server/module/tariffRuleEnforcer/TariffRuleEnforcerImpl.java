package org.powertac.server.module.tariffRuleEnforcer;

import org.powertac.common.commands.TariffReply;
import org.powertac.common.interfaces.TariffRuleEnforcer;

public class TariffRuleEnforcerImpl implements TariffRuleEnforcer {

    @Override
    public boolean accept(TariffReply tariffReply) {
        // Default implementation simply returns true
        return true;
    }
}
