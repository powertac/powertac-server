package org.powertac.common.xml;

import org.powertac.common.Timeslot;
import org.powertac.common.repo.TimeslotRepo;
import org.powertac.common.spring.SpringApplicationContext;

import com.thoughtworks.xstream.converters.SingleValueConverter;

public class TimeslotConverter implements SingleValueConverter
{
  private TimeslotRepo timeslotRepo;
  
  public TimeslotConverter ()
  {
    super();
    timeslotRepo = (TimeslotRepo) SpringApplicationContext.getBean("timeslotRepo");
  }

  @SuppressWarnings("rawtypes")
  public boolean canConvert (Class type)
  {
    return Timeslot.class.isAssignableFrom(type);
  }

  public Object fromString (String serial)
  {
    return timeslotRepo.findBySerialNumber(Integer.valueOf(serial));
  }

  public String toString (Object timeslot)
  {
    return Integer.toString(((Timeslot)timeslot).getSerialNumber());
  }

}
