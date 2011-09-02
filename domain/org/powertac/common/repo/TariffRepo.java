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
package org.powertac.common.repo;

import java.util.HashMap;

import org.apache.log4j.Logger;
import org.powertac.common.TariffSpecification;
import org.springframework.stereotype.Repository;

/**
 * Repository for TariffSpecifications, Tariffs, Rates, and other related types.
 * @author John Collins
 */
@Repository
public class TariffRepo implements DomainRepo
{
  static private Logger log = Logger.getLogger(TariffRepo.class.getName());

  private HashMap<Long,TariffSpecification> specs;
  
  public TariffRepo ()
  {
    super();
    specs = new HashMap<Long,TariffSpecification>();
  }
  
  public TariffSpecification findSpecificationById (long id)
  {
    return specs.get(id);
  }
  
  public void recycle ()
  {
    specs.clear();
  }

}
