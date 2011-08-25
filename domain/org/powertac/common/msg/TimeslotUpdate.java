/*
 * Copyright (c) 2011 by the original author or authors.
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
package org.powertac.common.msg;

import java.util.ArrayList;

import org.powertac.common.Timeslot;
import com.thoughtworks.xstream.annotations.*;

/**
 * Message used to communicate enable and disable events on timeslots.
 * @author John Collins
 */
@XStreamAlias("timeslot-update")
public class TimeslotUpdate 
{  
  private ArrayList<Timeslot> enabled;
  
  private ArrayList<Timeslot> disabled;

  public TimeslotUpdate (ArrayList enabled, ArrayList disabled)
  {
    super();
    this.enabled = enabled;
    this.disabled = disabled;
  }
  
  public TimeslotUpdate (Timeslot enabled, Timeslot disabled)
  {
    super();
    this.enabled = new ArrayList();
    this.enabled.add(enabled);
    this.disabled = new ArrayList();
    this.disabled.add(disabled);
  }
}
