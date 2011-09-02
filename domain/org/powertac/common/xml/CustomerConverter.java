package org.powertac.common.xml;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import org.powertac.common.CustomerInfo;
import org.powertac.common.repo.CustomerRepo;
import org.springframework.beans.factory.annotation.Autowired;

public class CustomerConverter implements SingleValueConverter
{
  @Autowired
  CustomerRepo customerRepo;
  
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
