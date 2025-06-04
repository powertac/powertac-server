/*
 * Copyright (c) 2012 by the original author
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

/**
 * Simple generic immutable pair structure
 * @author John Collins
 */
public class Pair<L,R>
{
  private final L carv;
  private final R cdrv;

  public Pair(L car, R cdr)
  {
    super();
    this.carv = car;
    this.cdrv = cdr;
  }

  public L car ()
  {
    return carv;
  }

  public R cdr ()
  {
    return cdrv;
  }

  @Override
  public int hashCode ()
  {
    return (carv.hashCode() ^ cdrv.hashCode());
  }

  @Override
  public boolean equals (Object thing)
  {
    if (null == thing || !(thing instanceof Pair)) {
      return false;
    }
    else {
      Pair<?, ?> p = (Pair<?, ?>)thing;
      return (car().equals(p.car()) && cdr().equals(p.cdr()));      
    }
  }
}
