/*
 * Copyright (c) 2016 by John Collins
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

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.CustomerInfo.CustomerClass;
import org.powertac.common.WeatherReport;
import org.powertac.common.XMLMessageConverter;
import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.common.msg.MarketBootstrapData;
import org.springframework.test.util.ReflectionTestUtils;

import com.thoughtworks.xstream.XStream;

/**
 * @author John Collins
 *
 */
public class BootstrapDataRepoTest
{
  static private Logger log =
          LogManager.getLogger(BootstrapDataRepoTest.class.getName());

  BootstrapDataRepo uut;
  XMLMessageConverter xmc;

  /**
   * 
   */
  @Before
  public void setUp () throws Exception
  {
    uut = new BootstrapDataRepo();
    xmc = new XMLMessageConverter();
    xmc.afterPropertiesSet();
    ReflectionTestUtils.setField(uut, "messageConverter", xmc);
  }

  /**
   * Test method for {@link org.powertac.common.repo.BootstrapDataRepo#add(java.lang.Object)}.
   */
  @Test
  public void testAddObject ()
  {
    String s1 = "test string";
    uut.add(s1);
    Integer i1 = 42;
    uut.add(i1);
    
    List<?> result = uut.getData();
    assertEquals("two items", 2, result.size());
    String r1;
    if (result.get(0) instanceof String)
      r1 = (String)result.get(0);
    else
      r1 = (String)result.get(1);
    assertNotNull("found a String", r1);
    assertEquals("correct String", "test string", r1);
    Integer r2;
    if (result.get(0) instanceof Integer)
      r2 = (Integer)result.get(0);
    else
      r2 = (Integer)result.get(1);
    assertNotNull("found a String", r2);
    assertEquals("correct Integer", r2, new Integer(42));
  }

  /**
   * Test method for {@link org.powertac.common.repo.BootstrapDataRepo#add(java.util.List)}.
   */
  @Test
  public void testAddListOfObject ()
  {
    ArrayList<Object> data = new ArrayList();
    String s1 = "test string";
    data.add(s1);
    Integer i1 = 42;
    data.add(i1);
    uut.add(data);
    
    List<?> result = uut.getData();
    assertEquals("two items", 2, result.size());
    String r1;
    if (result.get(0) instanceof String)
      r1 = (String)result.get(0);
    else
      r1 = (String)result.get(1);
    assertNotNull("found a String", r1);
    assertEquals("correct String", "test string", r1);
    Integer r2;
    if (result.get(0) instanceof Integer)
      r2 = (Integer)result.get(0);
    else
      r2 = (Integer)result.get(1);
    assertNotNull("found a String", r2);
    assertEquals("correct Integer", r2, new Integer(42));
  }

  /**
   * Test method for {@link org.powertac.common.repo.BootstrapDataRepo#getData(java.lang.Class)}.
   */
  @Test
  public void testGetDataClassQ ()
  {
    uut.add("test1");
    uut.add("test2");
    uut.add(new Integer(42));
    uut.add(new Integer(43));
    uut.add(new Integer(44));
    
    List<?> result = uut.getData();
    assertEquals("five items", 5, result.size());
    
    TreeSet<String> strings = new TreeSet<>();
    for (Object r: uut.getData(String.class)) {
      strings.add((String)r);
    }
    String[] ar = new String[3];
    ar = strings.toArray(ar);
    assertEquals("first string", "test1", ar[0]);
    assertEquals("second string", "test2", ar[1]);
    assertNull("no third string", ar[2]);
    
    TreeSet<Integer> nums= new TreeSet<>();
    for (Object r: uut.getData(Integer.class)) {
      nums.add((Integer)r);
    }
    Integer[] an = new Integer[4];
    an = nums.toArray(an);
    assertEquals("first int", new Integer(42), an[0]);
    assertEquals("second int", new Integer(43), an[1]);
    assertEquals("third int", new Integer(44), an[2]);
    assertNull("no fourth int", an[3]);
  }

  /**
   * Test method for {@link org.powertac.common.repo.BootstrapDataRepo#recycle()}.
   */
  @Test
  public void testRecycle ()
  {
    uut.add("test1");
    uut.add("test2");
    uut.add(new Integer(42));
    uut.add(new Integer(43));
    uut.add(new Integer(44));
    List<?> result = uut.getData();
    assertEquals("five", 5, result.size());
    uut.recycle();
    result = uut.getData();
    assertEquals("zero", 0, result.size());
  }

  /**
   * Test method for {@link org.powertac.common.repo.BootstrapDataRepo#readBootRecord(java.net.URL)}.
   */
  @Test
  public void testReadBootRecord ()
  {
    try {
      String cwd = System.getProperty("user.dir");
      //System.out.println("file://" + cwd);
      URL url = new URL("file:src/test/resources/boot.xml");
      uut.readBootRecord(url);;
    }
    catch (MalformedURLException e) {
      fail(e.toString());
    }
    List<Object> items;
    Competition bc = uut.getBootstrapCompetition();
    assertNotNull("found bootstrap competition", bc);
    items = uut.getData(CustomerInfo.class);
    assertEquals("11 customers", 11, items.size());
    assertEquals("24 weather reports", 336,
                 uut.getData(WeatherReport.class).size());
  }
}
