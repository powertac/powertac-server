package org.powertac.common.xml;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import org.powertac.common.CustomerInfo;

class CustomerConverter implements SingleValueConverter
{

  public boolean canConvert (Class type)
  {
    return CustomerInfo.class.isAssignableFrom(type);
  }

  public Object fromString (String id)
  {
    return CustomerInfo.get(id);
  }

  public String toString (Object customer)
  {
    return Integer.toString(((CustomerInfo)customer).getId());
  }
}
