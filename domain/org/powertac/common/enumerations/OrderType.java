/*
 * Copyright 2009-2010 the original author or authors.
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

package org.powertac.common.enumerations;

/**
 * Defines the type of an order (shout). Market orders do not carry a limit price
 * and are executed against any matching order. Limit orders carry a limit price
 * and are only executed if the limit price threshold is exceeded.
 *
 * @author Carsten Block
 * @since 0.5
 * @version 1
 * First created: 02.05.2010
 * Last Updated: 02.05.2010
 */
public enum OrderType {
    MARKET,
    LIMIT
}

