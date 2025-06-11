package org.powertac.common.xml;

import com.thoughtworks.xstream.converters.SingleValueConverter;
import org.powertac.common.TariffSpecification;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.spring.SpringApplicationContext;

public class TariffSpecificationConverter implements SingleValueConverter
{
  private TariffRepo tariffRepo;

  public TariffSpecificationConverter ()
  {
    super();
  }
  
  @Override
  @SuppressWarnings("rawtypes")
  public boolean canConvert (Class type)
  {
    return TariffSpecification.class.isAssignableFrom(type);
  }

  @Override
  public Object fromString (String id)
  {
    return getTariffRepo().findSpecificationById(Long.parseLong(id));
  }

  @Override
  public String toString (Object tariffSpec)
  {
    return Long.toString(((TariffSpecification)tariffSpec).getId());
  }

  private TariffRepo getTariffRepo ()
  {
    if (null == tariffRepo)
      tariffRepo = (TariffRepo) SpringApplicationContext.getBean("tariffRepo");
    return tariffRepo;
  }
}
