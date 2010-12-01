package org.powertac.common.commands;

import org.powertac.common.enumerations.CustomerType;

import java.util.Set;

public class TariffPublishedCommand extends AbstractTariffCommand {
  Set<CustomerType> permittedCustomerTypes;
}
