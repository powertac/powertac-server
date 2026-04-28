/*
 * Copyright (c) 2012 by the original author or authors.
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

import org.powertac.common.enumerations.PowerType;

import com.thoughtworks.xstream.converters.SingleValueConverter;

/**
 * Converts Timeslot instances by serial number.
 * @author John Collins
 */
public class PowerTypeConverter implements SingleValueConverter
{
  public PowerTypeConverter ()
  {
    super();
  }

  @Override
  @SuppressWarnings("rawtypes")
  public boolean canConvert (Class type)
  {
    return PowerType.class.isAssignableFrom(type);
  }

  @Override
  public Object fromString (String label)
  {
    return PowerType.valueOf(label);
  }

  @Override
  public String toString (Object powerType)
  {
    return ((PowerType)powerType).toString();
  }

}
