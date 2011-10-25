package org.powertac.common.xml;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import org.powertac.common.TariffSpecification;
import org.powertac.common.repo.TariffRepo;
import org.springframework.beans.factory.annotation.Autowired;

public class TariffSpecificationConverter implements SingleValueConverter
{
  @Autowired
  private TariffRepo tariffRepo;
  
  @SuppressWarnings("rawtypes")
  public boolean canConvert (Class type)
  {
    return TariffSpecification.class.isAssignableFrom(type);
  }

  public Object fromString (String id)
  {
    return tariffRepo.findSpecificationById(Long.parseLong(id));
  }

  public String toString (Object tariffSpec)
  {
    return Long.toString(((TariffSpecification)tariffSpec).getId());
  }

}
