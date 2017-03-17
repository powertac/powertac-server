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
package org.powertac.common;

/**
 * Classes of this type may implement the isValid() method to perform
 * a local validation of the contents of a message. Messages sent by
 * brokers to the simulation server are expected to override this method
 * to check local constraints on field values.
 * @author John Collins
 */
public interface ValidatableMessage
{
  /**
   * Implementation should return true just in case the message is
   * internally valid.
   */
  public boolean isValid ();
}
