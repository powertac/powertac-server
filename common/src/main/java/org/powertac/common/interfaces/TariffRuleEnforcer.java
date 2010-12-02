package org.powertac.common.interfaces;

import org.powertac.common.commands.TariffReplyCommand;

public interface TariffRuleEnforcer  {

    /**
     * Evaluates a given tariff
     * @param tariffReplyCommand
     * @return whether tariffReplyCommand is accepted or not
     */
  public boolean accept(TariffReplyCommand tariffReplyCommand);

}
