/*
 * Copyright (c) 2018 by John Collins
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
package org.powertac.samplebroker.interfaces;

/**
 * Implementations are intended to allow out-of-process broker implementation
 * through an inter-process communication path. The obvious example is a
 * pair of character streams such as pipes or FIFOs.
 * 
 * @author John Collins
 */
public interface IpcAdapter
{
  /**
   * Opens output and input streams.
   */
  default void openStreams ()
  {
    // in the default case, they are already open.
  }

  /**
   * Sends an incoming server message, in text form, to an out-of-process
   * broker implementation.
   */
  public void exportMessage (String message);

  /**
   * Starts a listener for incoming messages, which will be forwarded
   * to the server.
   */
  public void startMessageImport ();
}
