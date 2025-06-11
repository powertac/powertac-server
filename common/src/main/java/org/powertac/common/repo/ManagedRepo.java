/**
 * Copyright (c) 2019 by John Collins
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
package org.powertac.common.repo;

import java.time.Instant;
import org.powertac.common.RepeatingTimedAction;
import org.powertac.common.TimeService;
import org.powertac.common.TimedAction;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Domain repos that do active memory management. This works by waking
 * up every few timeslots and running a cleanup method.
 * @author John Collins
 */
public abstract class ManagedRepo implements DomainRepo
{
  @Autowired
  protected TimeService timeService;
  
  // True if setup has completed
  private boolean setupComplete = false;

  // Run the cleanup every 12 hours
  protected long interval = TimeService.HOUR * 12;
  
  // Offset by 3h to avoid adding work during tariff eval
  protected long offset = TimeService.HOUR * 3;

  // Sets up a cleanup action every 12h in the sim. This method must be
  // called as the last action in the repo's recycle method.
  protected void setup ()
  {
    Instant now = timeService.getCurrentTime();
    if (null == now) {
      // time service has not been initialized
      setupComplete = false;
    }
    else if (!setupComplete) {
      RepeatingTimedAction rta =
              new RepeatingTimedAction(new TimedAction() {
                @Override
                public void perform (Instant time) {
                  doCleanup();
                }
              }, interval);
      timeService.addAction(timeService.getCurrentTime().plusMillis(offset),
                            rta);
      setupComplete = true;
    }
  }

  // recycling just clears the setupComplete flag
  public void recycle () {
    setupComplete = false;
  }
  
  // Implementations of this method need to clean up memory and log
  // their actions
  protected abstract void doCleanup ();
}
