/*
 * Copyright (c) 2012 by the original author
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
package org.powertac.visualizer.services;

/**
 * State handlers for the remote visualizer service.
 * @author John Collins
 */
public interface VisualizerState
{
  /** possible event types */
  public static enum Event {tick, noTm, accept, vsr, simEnd, quit};

  /**
   * Come here on state entry
   */
  public void entry();

  /**
   * Handle an event
   */
  public void handleEvent(Event event);
}
