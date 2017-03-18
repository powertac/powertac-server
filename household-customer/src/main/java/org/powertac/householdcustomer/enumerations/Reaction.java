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
 * This enumerator defines the four different reaction patterns of washing
 * machine By loading the machine in the morning, a task that might have been
 * done at another time of the day and selecting shifting interval the household
 * resident shows a strong reaction.
 * 
 * Loading the machine at the same time as before but still selecting a shifting
 * interval is a responsive reaction.
 * 
 * Loading and starting the machine at the same time as before is an
 * unresponsive reaction.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */

public enum Reaction
{
  Strong, Responsive, Unresponsive
};