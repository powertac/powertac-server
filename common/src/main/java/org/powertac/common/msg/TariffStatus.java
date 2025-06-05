/*
 * Copyright 2009-2011 the original author or authors.
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
package org.powertac.common.msg;

import org.powertac.common.Broker;
import org.powertac.common.TariffMessage;
import org.powertac.common.state.Domain;
import org.powertac.common.state.StateChange;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

/**
 * Represents a response from server to broker to publication or update
 * of a tariff.
 * @author jcollins
 */
@Domain(fields = {"broker", "tariffId", "updateId", "status"})
@XStreamAlias("tariff-status")
public class TariffStatus extends TariffMessage
{
  public enum Status {success, noSuchTariff, noSuchUpdate, illegalOperation,
    invalidTariff, invalidUpdate, duplicateId, invalidPowerType, unsupported}

  @XStreamAsAttribute
  private long tariffId;
  
  @XStreamAsAttribute
  private long updateId;
  
  private String message;
  
  @XStreamAsAttribute
  private Status status = Status.success;
  
  public TariffStatus (Broker broker, long tariffId, 
                       long updateId, Status status)
  {
    super(broker);
    this.tariffId = tariffId;
    this.updateId = updateId;
    this.status = status;
  }

  public String getMessage ()
  {
    return message;
  }

  @StateChange
  public TariffStatus withMessage (String message)
  {
    this.message = message;
    return this;
  }

  public long getTariffId ()
  {
    return tariffId;
  }

  public long getUpdateId ()
  {
    return updateId;
  }

  public Status getStatus ()
  {
    return status;
  }

  public void setStatus (Status status)
  {
    this.status = status;
  }
}
