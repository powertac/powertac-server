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
  private LogtoolCore uut = new LogtoolCore();
  private DomainObjectReader dor;
  private String relativeArtifactPath = "src/test/resources/artifacts/";

  @BeforeEach
  public void setUp () throws Exception
  {
    dor = mock(DomainObjectReader.class);
    ReflectionTestUtils.setField(uut, "reader", dor);
  }
  
  private String getAbsoluteArtifactPath ()
  {
    Path currentRelativePath = Paths.get("");
    return currentRelativePath.toAbsolutePath().toString() + relativeArtifactPath;    
  }

  @Test
  public void findNMDfile ()
  {
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
    BufferedReader rdr = uut.getLogStream(relativeArtifactPath + "md.state");
    assertNotNull(rdr, "found the file");
    try {
      String line = rdr.readLine();
      assertTrue(line.endsWith("finals_2021_8"));
    } catch (IOException ioe) {
      fail("could not read md.state -- {}", ioe.getCause());
    }
  }
}
