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

import java.time.Instant;
import org.powertac.common.state.Domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * This message is used to communicate a revised simulation start time
 * prior to the end of a simulation pause. This
 * allows all parties to synchronize their simulation clocks.
 * @author John Collins
 */
@Domain
@XStreamAlias("sim-resume")
public class SimResume 
{
  private Instant start;
  
  public SimResume (Instant newStart)
  {
    super();
    start = newStart;
  }

  public Instant getStart ()
  {
    return start;
  }
}
