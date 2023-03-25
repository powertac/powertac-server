/*
 * Copyright (c) 2021 by John Collins
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
package org.powertac.logtool;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.MarketPosition;
import org.powertac.common.Order;
import org.powertac.common.RandomSeed;
import org.powertac.common.TariffTransaction;
import org.powertac.common.msg.SimEnd;
import org.powertac.du.DefaultBroker;
import org.powertac.logtool.common.DomainBuilder;
import org.powertac.logtool.common.DomainObjectReader;
import org.powertac.logtool.ifc.Analyzer;
import org.powertac.logtool.ifc.ObjectReader;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * TODO this does nothing?
 * 
 * @author John Collins
 */
public class LogtoolTest
{
  private LogtoolCore uut;
  private DomainObjectReader dor;
  private String relativeArtifactPath = "src/test/resources/artifacts/";

  @BeforeEach
  public void setUp () throws Exception
  {
  }

  private String getAbsoluteArtifactPath ()
  {
    Path currentRelativePath = Paths.get("");
    return currentRelativePath.toAbsolutePath().toString() + "/" + relativeArtifactPath;    
  }

  private void firstInit ()
  {
    uut = new LogtoolCore();
    dor = mock(DomainObjectReader.class);
    ReflectionTestUtils.setField(uut, "reader", dor);    
    DomainBuilder db = mock(DomainBuilder.class);
    ReflectionTestUtils.setField(uut, "domainBuilder", db);
  }

  @Test
  public void findNMDfile ()
  {
    firstInit();
    BufferedReader rdr = uut.getLogStream(relativeArtifactPath + "nmd.state");
    assertNotNull(rdr, "found the file");
    try {
      String line = rdr.readLine();
      assertTrue(line.startsWith("4415:"));
    } catch (IOException ioe) {
      fail("could not read nmd.state -- {}", ioe.getCause());
    }
  }

  @Test
  public void findMDfile ()
  {
    firstInit();
    String path = getAbsoluteArtifactPath() + "md.state";
    //System.out.println(path);
    BufferedReader rdr = uut.getLogStream(path);
    assertNotNull(rdr, "found the file with absolute path");
    try {
      String line = rdr.readLine();
      assertTrue(line.endsWith("finals_2021_8"));
    } catch (IOException ioe) {
      fail("could not read md.state -- {}", ioe.getCause());
    }
  }

  @Test
  public void findMDAfile ()
  {
    firstInit();
    BufferedReader rdr = uut.getLogStream("file://" + getAbsoluteArtifactPath() + "/" + "md-abbr.state");
    assertNotNull(rdr, "found the file with URL");
    try {
      String line = rdr.readLine();
      assertTrue(line.endsWith("i1110-b1"));
    } catch (IOException ioe) {
      fail("could not read md-abbr.state -- {}", ioe.getCause());
    }
  }

  @Test
  public void findCompressedfile ()
  {
    firstInit();
    BufferedReader rdr = uut.getLogStream("file://" + getAbsoluteArtifactPath() + "/" + "md-abbr.state.gz");
    assertNotNull(rdr, "found compressed file with URL");
    try {
      String line = rdr.readLine();
      assertTrue(line.endsWith("i1110-b1"));
    } catch (IOException ioe) {
      fail("could not read md-abbr.state -- {}", ioe.getCause());
    }
  }

  @Test
  public void findFromArchive ()
  {
    firstInit();
    BufferedReader rdr = uut.getLogStream(relativeArtifactPath + "i1110.tar");
    assertNotNull(rdr, "found tar file");
    try {
      String line = rdr.readLine();
      assertTrue(line.endsWith("i1110-b1"));
    } catch (IOException ioe) {
      fail("could not read md-abbr.state -- {}", ioe.getCause());
    }    
  }

  @Test
  public void findFromCompressedArchive ()
  {
    firstInit();
    BufferedReader rdr = uut.getLogStream(relativeArtifactPath + "i1110.tgz");
    assertNotNull(rdr, "found compressed tar file");
    try {
      String line = rdr.readLine();
      assertTrue(line.endsWith("i1110-b1"));
    } catch (IOException ioe) {
      fail("could not read md-abbr.state -- {}", ioe.getCause());
    }    
  }
  
  // ---------------------------------------------------------

  private void secondInit ()
  {
    uut = new LogtoolCore();
    dor = new DomainObjectReader();
    ReflectionTestUtils.setField(uut, "reader", dor);
    DomainBuilder db = mock(DomainBuilder.class);
    ReflectionTestUtils.setField(uut, "domainBuilder", db);
    uut.resetDOR(false);
  }

  // repository for log data
  List<RandomSeed> randomSeedList;
  List<MarketPosition> mpList;
  List<Order> orderList;

  @Test
  public void testReadLog ()
  {
    secondInit();
    TestAnalyzer ta = new TestAnalyzer(getAbsoluteArtifactPath() + "md.state");
    ta.loadData();
    assertEquals(7, randomSeedList.size());
    assertEquals(1874, randomSeedList.get(1).getId());
    assertEquals(1, mpList.size());
    assertEquals(1, orderList.size());
  }

  @Test
  public void testReadFromCompressedArchive ()
  {
    secondInit();
    TestAnalyzer ta = new TestAnalyzer(relativeArtifactPath + "i1110.tgz");
    ta.loadData();
    assertEquals(4, randomSeedList.size());
    assertEquals(1874, randomSeedList.get(1).getId());
  }

  @Test
  public void testIncrementalRead ()
  {
    secondInit();
    uut.includeClassname("org.powertac.du.DefaultBroker");
    uut.includeClassname("org.powertac.common.RandomSeed");
    uut.includeClassname("org.powertac.common.MarketPosition");
    uut.includeClassname("org.powertac.common.Order");
    ObjectReader or = uut.getObjectReader(getAbsoluteArtifactPath() + "md.state");
    Object next = or.getNextObject();
    assertNotNull(next);
    assertEquals(RandomSeed.class, next.getClass());
    RandomSeed rs = (RandomSeed)next;
    assertEquals(1, rs.getId());
    next = or.getNextObject();
    assertNotNull(next);
    assertEquals(RandomSeed.class, next.getClass());
    rs = (RandomSeed)next;
    assertEquals(1874, rs.getId());
    next = or.getNextObject();
    assertNotNull(next);
    rs = (RandomSeed)next;
    assertEquals(1876, rs.getId());
    next = or.getNextObject();
    assertNotNull(next);
    rs = (RandomSeed)next;
    assertEquals(1878, rs.getId());
    next = or.getNextObject();
    assertNotNull(next);
    rs = (RandomSeed)next;
    assertEquals(1880, rs.getId());
    next = or.getNextObject();
    assertNotNull(next);
    rs = (RandomSeed)next;
    assertEquals(1881, rs.getId());
    next = or.getNextObject();
    assertNotNull(next);
    assertEquals(DefaultBroker.class, next.getClass());
    DefaultBroker db = (DefaultBroker) next;
    assertEquals("default broker", db.getUsername());
    next = or.getNextObject();
    assertNotNull(next);
    rs = (RandomSeed)next;
    assertEquals(1883, rs.getId());
    next = or.getNextObject();
    assertNotNull(next);
    assertEquals(MarketPosition.class, next.getClass());
    assertEquals(db, ((MarketPosition)next).getBroker());
    next = or.getNextObject();
    assertNotNull(next);
    assertEquals(Order.class, next.getClass());
    next = or.getNextObject();
    assertNotNull(next);
    assertEquals(SimEnd.class, next.getClass());
    or.close();
  }

  class TestAnalyzer extends LogtoolContext implements Analyzer
  {
    String filename;

    TestAnalyzer (String log)
    {
      filename = log;
      this.core = uut;
      this.dor = uut.getDOR();
    }

    @Override
    public void setup () throws FileNotFoundException
    {
      randomSeedList = new ArrayList<>();
      //ttList = new ArrayList<>();
      mpList = new ArrayList<>();
      orderList = new ArrayList<>();
    }

    void loadData ()
    {
      uut.includeClassname("org.powertac.common.Broker");
      uut.includeClassname("org.powertac.du.DefaultBroker");
      uut.includeClassname("org.powertac.common.RandomSeed");
      uut.includeClassname("org.powertac.common.MarketPosition");
      uut.includeClassname("org.powertac.common.Order");
      registerMessageHandlers();
      Analyzer[] tools = new Analyzer[1];
      tools[0] = this;
      uut.readStateLog(filename, tools);
      System.out.println("Finished reading log");
    }
    
    // message handlers
    public void handleMessage (RandomSeed thing)
    {
      randomSeedList.add(thing);
    }

    public void handleMessage (MarketPosition thing)
    {
      mpList.add(thing);
    }

    public void handleMessage (Order thing)
    {
      orderList.add(thing);
    }

    @Override
    public void report ()
    {
      // Nothing to do here      
    }
  }
}
