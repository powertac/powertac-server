/*
 * Copyright 2011 the original author or authors.
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
package org.powertac.common.interfaces;

import java.util.List;

import org.powertac.common.BrokerTransaction;

/**
 * Interface for a service that processes transactions. 
 * @author John Collins
 */
public interface TransactionProcessor
{
  /**
   * Processes a transaction. Presumably there will be a method
   * for each subclass of BrokerTransaction. If as a result of processing
   * the transaction there is a message to be sent to the broker, then
   * that message shall be added to the messages list.
   */
  public void processTransaction(BrokerTransaction tx, List messages);
}
