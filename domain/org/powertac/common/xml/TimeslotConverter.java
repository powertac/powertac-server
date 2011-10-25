package org.powertac.common.xml;

import org.powertac.common.Timeslot;
import org.powertac.common.repo.TimeslotRepo;
import org.springframework.beans.factory.annotation.Autowired;

import com.thoughtworks.xstream.converters.SingleValueConverter;

public class TimeslotConverter implements SingleValueConverter
{
  @Autowired
  private TimeslotRepo timeslotRepo;
  
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
