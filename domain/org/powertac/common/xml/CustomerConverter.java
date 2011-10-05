/*
 * Copyright (c) 2011 by the original author
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
package org.powertac.common.xml;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import org.powertac.common.CustomerInfo;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

public class CustomerConverter implements SingleValueConverter
{
  CustomerRepo customerRepo;
  
  public CustomerConverter ()
  {
    super();
    customerRepo = (CustomerRepo) SpringApplicationContext.getBean("customerRepo");
  }
  
  public boolean canConvert (Class type)
  {
    return CustomerInfo.class.isAssignableFrom(type);
  }

  public Object fromString (String id)
  {
    return customerRepo.findById(Long.parseLong(id));
  }

  public String toString (Object customer)
  {
    return Long.toString(((CustomerInfo)customer).getId());
  }
}
