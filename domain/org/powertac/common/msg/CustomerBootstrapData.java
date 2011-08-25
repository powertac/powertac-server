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

import java.util.HashMap;
import java.util.Map;

import org.powertac.common.Constants;
import org.powertac.common.CustomerInfo;

import com.thoughtworks.xstream.annotations.*;

/**
 * This message is used to send the bootstrap of every customer in the competition to the brokers
 * in order to see what kind of customers they are.
 * @author Anthony Chrysopoulos
 */
@XStreamAlias("customer-bootstrap-data")
public class CustomerBootstrapData 
{
  private CustomerInfo customer;

  // JEC - what's the point of the Map? Why not just store the array?
  private HashMap<String, HashMap<String, Double> > bootstrapData = new HashMap<String, HashMap<String, Double> >();

  public void fillBootstrapData(double[][] bootstrap) {
    for (int i=0;i < bootstrap.length; i++) {
      HashMap<String, Double> day = new HashMap<String, Double>();
      for (int j = 0; j < bootstrap[i].length; j++) {
        day.put("Hour " + j, bootstrap[i][j]);
      }
      bootstrapData.put("Day "+ i, day);
    }
  }

  public double[][] getBootstrapData(){

    double[][] bootstrap = new double[Constants.DAYS_OF_BOOTSTRAP][Constants.HOURS_OF_DAY];

    for (int i = 0; i < Constants.DAYS_OF_BOOTSTRAP; i++) {
      Map<String, Double> temp = bootstrapData.get("Day " +i);
      for (int j = 0; j < Constants.HOURS_OF_DAY; j++) {
        bootstrap[i][j] = temp.get("Hour " +j);
      }
    }
    return bootstrap;
  }
  
  public CustomerInfo getCustomer ()
  {
    return customer;
  }

  public void setCustomer (CustomerInfo customer)
  {
    this.customer = customer;
  }
}