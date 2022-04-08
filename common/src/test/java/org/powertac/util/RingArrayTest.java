/**
 * Copyright (c) 2022 by John Collins
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
package org.powertac.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author John Collins
 *
 */
class RingArrayTest
{

  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  void setUp () throws Exception
  {
    
  }

  @Test
  void testLoad ()
  {
    RingArray<Integer> uut = new RingArray<>(9);
    for (int i = 4; i < 12; i++) {
      uut.set(i, Integer.valueOf(i));
    }
    assertNull(uut.get(3));
    assertEquals(4, uut.get(4));
    assertEquals(10, uut.get(10));
    assertEquals(11, uut.get(11));

    for (int i = 15; i < 30; i++) {
      uut.set(i, Integer.valueOf(i));
    }
    assertNull(uut.get(20));
    assertEquals(21, uut.get(21));
    assertEquals(29, uut.get(29));
    assertNull(uut.get(30));
  }

  @Test
  void testClean ()
  {
    RingArray<Integer> uut = new RingArray<>(9);
    for (int i = 4; i < 12; i++) {
      uut.set(i, Integer.valueOf(i));
    }
    assertNull(uut.get(3));
    assertEquals(4, uut.get(4));
    uut.clean(4);
    assertNull(uut.get(3));
    assertNotNull(uut.get(4));
    assertNotNull(uut.get(11));
    assertNull(uut.get(12));
  }
}
