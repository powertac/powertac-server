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
package org.powertac.common.config;

import java.util.List;

/**
 * Interface for recording configuration information.
 * @author John Collins
 */
public interface ConfigurationRecorder
{
  /**
   * Records a single configuration item
   */
  public void recordItem (String key, Object value);

  /**
   * Records metadata for an item. Default implementation just returns
   */
  default public void recordMetadata (String key, String description,
                                      String valueType,
                                      boolean publish, boolean bootstrapState)
  {
    return;
  }

  /**
   * Records an instance list for a class. Default implementation does nothing.
   */
  default public void recordInstanceList (String key, List<String> names)
  {
    return;
  }
}
