/*
 * Copyright (c) 2011, 2012 by the original author or authors.
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
package org.powertac.common.xml;

import org.powertac.common.Timeslot;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;

import com.thoughtworks.xstream.converters.SingleValueConverter;

/**
 * Converts Timeslot instances by serial number.
 * @author John Collins
 */
public class TimeslotConverter implements SingleValueConverter
{
  private TimeslotRepo timeslotRepo;
  
  public TimeslotConverter ()
  {
    super();
    timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
  }

  @Override
  @SuppressWarnings("rawtypes")
  public boolean canConvert (Class type)
  {
    return Timeslot.class.isAssignableFrom(type);
  }

  @Override
  public Object fromString (String serial)
  {
    return timeslotRepo.findOrCreateBySerialNumber(Integer.valueOf(serial));
  }

  @Override
  public String toString (Object timeslot)
  {
    return Integer.toString(((Timeslot)timeslot).getSerialNumber());
  }

}
