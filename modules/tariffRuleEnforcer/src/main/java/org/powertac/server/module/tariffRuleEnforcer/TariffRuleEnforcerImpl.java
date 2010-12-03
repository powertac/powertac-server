package org.powertac.server.module.tariffRuleEnforcer;

import org.powertac.common.commands.TariffReplyCommand;
import org.powertac.common.interfaces.TariffRuleEnforcer;

public class TariffRuleEnforcerImpl implements TariffRuleEnforcer {

    @Override
    public boolean accept(TariffReplyCommand tariffReplyCommand) {
        // Default implementation simply returns true
        return true;
    }
}
