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
 * This enumerator defines the two different kinds of Water Heater appliances
 * Instant Heater: Utilized when in need from the house tenants and closed
 * afterwards Storage Heater: Always having warm water inside. In the beginning
 * is working to boil the water and then works on stand-by to keep it warm.
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */

public enum HeaterType
{
  InstantHeater, StorageHeater
}
