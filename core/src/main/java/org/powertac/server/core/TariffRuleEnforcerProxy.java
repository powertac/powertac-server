package org.powertac.server.core;

import org.powertac.common.commands.TariffReplyCommand;
import org.powertac.common.interfaces.TariffRuleEnforcer;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageSelector;

public class TariffRuleEnforcerProxy implements MessageSelector {

    TariffRuleEnforcer tariffRuleEnforcer;

    public TariffRuleEnforcer getTariffRuleEnforcer() {
        return tariffRuleEnforcer;
    }

    public void setTariffRuleEnforcer(TariffRuleEnforcer tariffRuleEnforcer) {
        this.tariffRuleEnforcer = tariffRuleEnforcer;
    }

    @Override
    public boolean accept(Message<?> message) {
        if (tariffRuleEnforcer != null) {
            return tariffRuleEnforcer.accept((TariffReplyCommand) message.getPayload());
        }
        return false;
    }
}
