package org.powertac.common.xml;

import com.thoughtworks.xstream.converters.SingleValueConverter;

public class DoubleArrayConverter implements SingleValueConverter
{

  @Override
  @SuppressWarnings("rawtypes")
  public boolean canConvert (Class type)
  {
    return double[].class.isAssignableFrom(type);
  }

  @Override
  public Object fromString (String xml)
  {
    String[] values = xml.split(",");
    double[] result = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      result[i] = Double.parseDouble(values[i]);
    }
    return result;
  }

  @Override
  public String toString (Object input)
  {
    // convert array to string
    StringBuffer buf = new StringBuffer();
    double[] values = (double[])input;
    //buf.append("[");
    String delim = "";
    for (double datum : values) {
      buf.append(delim).append(datum);
      delim = ",";
    }
    //buf.append("]");
    return buf.toString();
  }

}
