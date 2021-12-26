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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.logtool.common.DomainObjectReader;
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
    dor = mock(DomainObjectReader.class);
    uut = new LogtoolCore();
    ReflectionTestUtils.setField(uut, "reader", dor);    
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
    dor = new DomainObjectReader();
    uut = new LogtoolCore();
    ReflectionTestUtils.setField(uut, "reader", dor);    
  }
  
  @Test
  public void xyz ()
  {
    secondInit();
  }
}
