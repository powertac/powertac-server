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

import static org.junit.jupiter.api.Assertions.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.WeatherReport;
import org.powertac.common.XMLMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * @author John Collins
 *
 */
public class BootstrapDataRepoTest
{
  // static private Logger log = LogManager.getLogger(BootstrapDataRepoTest.class.getName());

  BootstrapDataRepo uut;
  XMLMessageConverter xmc;

  /**
   * 
   */
  @BeforeEach
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
    assertEquals(2, result.size(), "two items");
    String r1;
    if (result.get(0) instanceof String)
      r1 = (String)result.get(0);
    else
      r1 = (String)result.get(1);
    assertNotNull(r1, "found a String");
    assertEquals(r1, "test string", "correct String");
    Integer r2;
    if (result.get(0) instanceof Integer)
      r2 = (Integer)result.get(0);
    else
      r2 = (Integer)result.get(1);
    assertNotNull(r2, "found a String");
    assertEquals(r2, new Integer(42), "correct Integer");
  }

  /**
   * Test method for {@link org.powertac.common.repo.BootstrapDataRepo#add(java.util.List)}.
   */
  @Test
  public void testAddListOfObject ()
  {
    ArrayList<Object> data = new ArrayList<Object>();
    String s1 = "test string";
    data.add(s1);
    Integer i1 = 42;
    data.add(i1);
    uut.add(data);
    
    List<?> result = uut.getData();
    assertEquals(2, result.size(), "two items");
    String r1;
    if (result.get(0) instanceof String)
      r1 = (String)result.get(0);
    else
      r1 = (String)result.get(1);
    assertNotNull(r1, "found a String");
    assertEquals(r1, "test string", "correct String");
    Integer r2;
    if (result.get(0) instanceof Integer)
      r2 = (Integer)result.get(0);
    else
      r2 = (Integer)result.get(1);
    assertNotNull(r2, "found a String");
    assertEquals(r2, new Integer(42), "correct Integer");
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
    assertEquals(5, result.size(), "five items");
    
    TreeSet<String> strings = new TreeSet<>();
    for (Object r: uut.getData(String.class)) {
      strings.add((String)r);
    }
    String[] ar = new String[3];
    ar = strings.toArray(ar);
    assertEquals(ar[0], "test1", "first string");
    assertEquals(ar[1], "test2", "second string");
    assertNull(ar[2], "no third string");
    
    TreeSet<Integer> nums= new TreeSet<>();
    for (Object r: uut.getData(Integer.class)) {
      nums.add((Integer)r);
    }
    Integer[] an = new Integer[4];
    an = nums.toArray(an);
    assertEquals(new Integer(42), an[0], "first int");
    assertEquals(new Integer(43), an[1], "second int");
    assertEquals(new Integer(44), an[2], "third int");
    assertNull(an[3], "no fourth int");
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
    assertEquals(5, result.size(), "five");
    uut.recycle();
    result = uut.getData();
    assertEquals(0, result.size(), "zero");
  }

  /**
   * Test method for {@link org.powertac.common.repo.BootstrapDataRepo#readBootRecord(java.net.URL)}.
   */
  @Test
  public void testReadBootRecord ()
  {
    try {
      // String cwd = System.getProperty("user.dir");
      // System.out.println("file://" + cwd);
      URL url = new URL("file:src/test/resources/boot.xml");
      uut.readBootRecord(url);;
    }
    catch (MalformedURLException e) {
      fail(e.toString());
    }
    List<Object> items;
    Competition bc = uut.getBootstrapCompetition();
    assertNotNull(bc, "found bootstrap competition");
    items = uut.getData(CustomerInfo.class);
    assertEquals(11, items.size(), "11 customers");
    assertEquals(336, uut.getData(WeatherReport.class).size(), "24 weather reports");
  }
}
