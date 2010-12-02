/*
 * Copyright 2009-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.powertac.common.commands;

import org.powertac.common.interfaces.Broker;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Command object that represents an abstract cash transaction
 * (add / deduce money) to be executed on a specific
 * broker cash account for a given reason.
 *
 * @author Carsten Block
 * @version 1.0, Date: 02.12.10
 */
public class AbstractCashCommand implements Serializable {

    private static final long serialVersionUID = 9051838018541279797L;

    Broker broker;
    BigDecimal moneyChange;
    String reason;
    String origin;

    public AbstractCashCommand(Broker broker, BigDecimal moneyChange, String reason, String origin) {
        this.broker = broker;
        this.moneyChange = moneyChange;
        this.reason = reason;
        this.origin = origin;
    }

    public AbstractCashCommand(Broker broker, Double moneyChange, String reason, String origin) {
        this.broker = broker;
        BigDecimal value = new BigDecimal(moneyChange);
        value = value.setScale(2);
        this.moneyChange = value;
        this.reason = reason;
        this.origin = origin;
    }

    /**
     * The broker (or more precisely the broker's cash account)
     * this cash transaction is targeted at.
     *
     * @return Broker cash account on which to execute the transaction
     */
    public Broker getBroker() {
        return broker;
    }

    /**
     * The amount of money that should be
     * added (positive value) or deduced from
     * the cash account
     *
     * @return BigDecimal the money change
     */
    public BigDecimal getMoneyChange() {
        return moneyChange;
    }

    /**
     * Describes the reason for this transaction, e.g.
     * "deduction of balancing power cost".
     *
     * @return String reason for this transaction
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns a String describing the originator of this
     * transaction, i.e. the customer, tax authority, or other
     * power tac entity who required this booking.
     *
     * @return String the origin power tac entitiy who required this transaction
     */
    public String getOrigin() {
        return origin;
    }
}
