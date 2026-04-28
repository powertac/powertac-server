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
package org.powertac.common.msg;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Message sent by a remote visualizer to check status of a server.
 * If the server is alive, it will respond by returning the message 
 * (or another message indistinguishable from the original).
 * @author John Collins
 */
@XStreamAlias("visualizer-status")
public class VisualizerStatusRequest
{
  public VisualizerStatusRequest ()
  {
    super();
  }
}
