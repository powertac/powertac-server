/**
 * Copyright 2019, John Collins.
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

import java.time.Instant;

/**
 * A timedAction
 * @author John Collins
 */
public class RepeatingTimedAction implements TimedAction
{

  TimeService ts;
  long interval = 0;
  TimedAction core = null;
  
  public RepeatingTimedAction (TimedAction act, long interval)
  {
    ts = TimeService.getInstance();
    this.interval = interval;
    this.core = act;
  }

  @Override
  public void perform (Instant theTime)
  {
    ts.addAction(ts.getCurrentTime().plusMillis(interval),
                 this);
    core.perform(theTime);
  }
}
