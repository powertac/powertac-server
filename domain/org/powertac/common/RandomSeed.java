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
package org.powertac.common;

import java.util.Random;
import java.util.UUID;

/**
 * RandomSeed is used to store generated random seed in the database in
 * order to be able to "replay" PowerTAC competitions later on with
 * exactly the same random seed settings as originally used.
 *
 * @author Carsten Block
 * @version 1.0 - January 01, 2011
 */
public class RandomSeed 
{
  UUID id = UUID.randomUUID();
  String requesterClass;
  long requesterId;
  String purpose = "unspecified";
  long value;
  
  /**
   * Constructor that creates a new seed with a random value.
   */
  public RandomSeed (String classname, long id, String purpose)
  {
    super();
    this.requesterClass = classname;
    this.requesterId = id;
    if (purpose != null)
      this.purpose = purpose;
    Random random = new Random();
    this.value = random.nextLong();
  }
  
  /**
   * Constructor to re-create a random seed with a given value.
   */
  public RandomSeed (UUID seedId, String classname, long requesterId,
                     String purpose, long value)
  {
    super();
    this.id = seedId;
    this.requesterClass = classname;
    this.requesterId = requesterId;
    this.purpose = purpose;
    this.value = value;
  }

  public UUID getId ()
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
