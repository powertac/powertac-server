package org.powertac.common.xml;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import org.powertac.common.TariffSpecification;

class TariffConverter implements SingleValueConverter
{

  public boolean canConvert (Class type)
  {
    return TariffSpecification.class.isAssignableFrom(type);
  }

  public Object fromString (String id)
  {
    return TariffSpecification.get(id);
  }

  public String toString (Object tariffSpec)
  {
    return tariffSpec.id;
  }

}
