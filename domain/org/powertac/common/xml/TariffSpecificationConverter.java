package org.powertac.common.xml;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import org.powertac.common.TariffSpecification;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;

public class TariffSpecificationConverter implements SingleValueConverter
{
  private TariffRepo tariffRepo;
  
  public TariffSpecificationConverter ()
  {
    super();
    tariffRepo = (TariffRepo) SpringApplicationContext.getBean("tariffRepo");
  }
  
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
