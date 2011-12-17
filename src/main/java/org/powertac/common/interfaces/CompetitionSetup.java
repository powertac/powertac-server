/*
 * Copyright (c) 2011 by the original author
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
package org.powertac.common.interfaces;

/**
 * Handles the pre-game competition setup process. To start a simulation,
 * one must call preGame(), followed by CompetitionControl.runOnce().
 * @author John Collins
 */
public interface CompetitionSetup
{  
  /**
   * Runs the pre-game cycle of the simulator, which sets all plugin components
   * to their default state.
   */
  public void preGame ();

}
