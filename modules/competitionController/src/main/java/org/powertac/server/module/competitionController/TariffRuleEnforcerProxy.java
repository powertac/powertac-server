/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.powertac.server.module.competitionController;

import org.powertac.common.commands.TariffReply;
import org.powertac.common.interfaces.TariffRuleEnforcer;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageSelector;

/**
 * This class acts as a proxy for the actual TariffRuleEnforcer module so that the module
 * is not required to use Spring Integration
 *
 * @author David Dauer
 * @version 0.0.1
 */

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
            return tariffRuleEnforcer.accept((TariffReply) message.getPayload());
        }
        return false;
    }
}
