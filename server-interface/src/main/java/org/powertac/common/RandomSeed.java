/*
 * Copyright 2009-2013 the original author or authors.
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
package org.powertac.common;

import org.powertac.common.state.StateChange;

import java.io.Serial;

/**
 * RandomSeed is used to store generated random seed in the database in
 * order to be able to "replay" PowerTAC competitions later on with
 * exactly the same random seed settings as originally used.
 * <p>
 * <b>Note</b> that server code is not intended to create instances of
 * RandomSeed directly. Instead, please request seeds through 
 * RandomSeedRepo.getRandomSeed(). This way, your code will work the same
 * whether using new seeds or replaying a previous simulation.</p>
 * <p>State log entry format:<br/>
 * <code>requesterClass::requesterId::purpose::value</code></p>
 *
 * @author Carsten Block, John Collins
 * @version 1.0 - January 01, 2011
 */
public class RandomSeed extends java.util.Random
{
  // needed because Random is serializable
  @Serial
  private static final long serialVersionUID = 1L;
  
  long id = IdGenerator.createId();
  String requesterClass;
  long requesterId;
  String purpose = "unspecified";
  long value;
  
  /**
   * Constructor that creates a new seed with a random value.
   * To keep the logfile simple, constructors are not logged in this
   * class; only the init() method is logged.
   */
  public RandomSeed (String classname, long requesterId, String purpose)
  {
    super();
    this.value = this.nextLong();
    init(classname, requesterId, purpose, value);
  }
  
  /**
   * Constructor to re-create a random seed with a given value.
   */
  public RandomSeed (String classname, long requesterId,
                     String purpose, long value)
  {
    super();
    init(classname, requesterId, purpose, value);
  }
  
  @StateChange
  private void init (String classname, long requesterId,
                     String purpose, long value)
  {
    this.requesterClass = classname;
    this.requesterId = requesterId;
    if (purpose != null)
      this.purpose = purpose;
    this.value = value;
    this.setSeed(this.value);
  }

  public long getId ()
  {
    return id;
  }

  public String getRequesterClass ()
  {
    return requesterClass;
  }

  public long getRequesterId ()
  {
    return requesterId;
  }

  public String getPurpose ()
  {
    return purpose;
  }

  public long getValue ()
  {
    return value;
  }
}
