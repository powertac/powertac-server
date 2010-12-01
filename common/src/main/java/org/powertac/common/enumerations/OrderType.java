package org.powertac.common.enumerations;

/**
 * Defines the type of an order (shout). Market orders do not carry a limit price
 * and are executed against any matching order. Limit orders carry a limit price
 * and are only executed if the limit price threshold is exceeded.
 *
 * @author Carsten Block
 * @since 0.5
 * @version 1
 * First created: 02.05.2010
 * Last Updated: 02.05.2010
 */
public enum OrderType {
    MARKET,
    LIMIT
}

