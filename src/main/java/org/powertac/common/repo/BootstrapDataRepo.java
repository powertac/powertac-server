/*
 * Copyright (c) 2015 by John Collins
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.stereotype.Repository;

/**
 * Repository for data contained in a bootstrap record.
 * Presumably this will be unused during a boot session.
 * The bootstrap data is simply an array of various types of objects.
 * The repo allows retrieval of the entire array, or just the items of
 * a particular class.
 * 
 * @author John Collins
 */
@Repository
public class BootstrapDataRepo implements DomainRepo
{
  // The data store
  private ArrayList<Object> data;
  private HashMap<Class<?>, List<Object>> classMap;

  public BootstrapDataRepo ()
  {
    super();
    data = new ArrayList<Object>();
    classMap = new HashMap<Class<?>, List<Object>>();
  }

  /** Adds a single item to the repo. */
  public void add (Object item)
  {
    data.add(item);
    List<Object> things = classMap.get(item.getClass());
    if (null == things) {
      things = new ArrayList<Object>();
      classMap.put(item.getClass(), things);
    }
    things.add(item);
  }

  /** Adds a list of objects to the repo. */
  public void add (List<Object> items)
  {
    for (Object item: items) {
      add(item);
    }
  }

  /** Returns the entire list of objects */
  public List<Object> getData ()
  {
    return data;
  }

  /** Returns the list of items of a particular class */
  public List<Object> getData (Class<?> type)
  {
    return classMap.get(type);
  }

  @Override
  public void recycle ()
  {
    data.clear();
    classMap.clear();
  }
}
