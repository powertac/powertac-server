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
 * This enumerator defines the state of a person. The available states are
 * Normal: Person is in the house Working: Person is out of house and at work
 * Leisure: Person is out of house and doing a leisure activity Vacation: Person
 * is out of house and in vacation Sleeping: Person is in the house and sleeping
 * Sick: Person is in the house and sick
 * 
 * @author Antonios Chrysopoulos
 * @version 1.5, Date: 2.25.12
 */

public enum Status
{
  Normal, Working, Leisure, Vacation, Sleeping, Sick
};