/*
* Copyright 2009-2012 the original author or authors.
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

package org.powertac.householdcustomer.enumerations;

/**
 * This enumerator defines the four different modes of semi-automatic appliances
 * Mode one: the washing has to finish within five hours from the loading point.
 * Mode two: finish within five to ten hours. Mode three: finish in more than
 * ten hours, but at the same day. Mode four: finish laundry between loading
 * point and end of day.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */

public enum Mode
{
  One, Two, Three, Four
}
