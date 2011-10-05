/*
 * Copyright 2009-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.powertac.common.repo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.powertac.common.RandomSeed;
import org.springframework.stereotype.Repository;

/**
 * Repository for RandomSeed instances. RandomSeeds are acquired by calls
 * to <code>getRandomSeed()</code>. These will be newly-constructed only if
 * an existing RandomSeed with the same classname, id, and purpose has not
 * already been created. Seeds may be created by loading a logfile from an
 * existing game, in which case the same random sequences will be repeated
 * in the current game. Otherwise they are created with random starting 
 * points when requested.
 * 
 * @author John Collins
 */
@Repository
public class RandomSeedRepo implements DomainRepo
{
  static private Logger log = Logger.getLogger(RandomSeedRepo.class.getName());
  
  private HashMap<String, RandomSeed> seedMap;
  
  public RandomSeedRepo ()
  {
    super();
    seedMap = new HashMap<String, RandomSeed>();
  }

  /**
   * Returns the RandomSeed instance identified by classname, id, and purpose,
   * creating it if necessary.
   */
  public RandomSeed getRandomSeed (String classname, long id, String purpose)
  {
    log.info("Seed requested: " + classname + ";" +
             id + ";" + purpose);
    String name = composeName(classname, id, purpose);
    RandomSeed result = seedMap.get(name);
    if (result == null) {
      log.info("New seed created: " + classname + ";" +
               id + ";" + purpose);
      result = new RandomSeed(classname, id, purpose);
      seedMap.put(composeName(classname, id, purpose), result);
    }
    return result;
  }
  
  /**
   * Pre-loads seeds from an existing server logfile, or from a stripped-down
   * logfile containing only the RandomSeed lines.
   * @param input
   */
  public void loadSeeds (File inputFile)
  throws FileNotFoundException
  {
    log.info("Loading seeds from " + inputFile.getPath());
    BufferedReader input = new BufferedReader(new FileReader(inputFile));
    String seedClass = RandomSeed.class.getName();
    try {
      String line;
      while ((line = input.readLine()) != null) {
        log.debug("line: " + line);
        String[] fields = line.split("::");
        if (seedClass.equals(fields[1])) {
          if (fields.length != 8) {
            log.error("Bad seed spec: " + line);
          }
          else {
            RandomSeed seed = new RandomSeed(fields[4],
                                             Long.parseLong(fields[5]), 
                                             fields[6],
                                             Long.parseLong(fields[7]));
            seedMap.put(composeName(fields[4], 
                                    Long.parseLong(fields[5]),
                                    fields[6]),
                        seed);
          }
        }
      }
    }
    catch (IOException ioe) {
      log.error("IOException reading seedfile:" + ioe.toString());
    }
  }
  
  private String composeName (String classname, long id, String purpose)
  {
    StringBuffer buf = new StringBuffer();
    buf.append(classname).append(";").append(id).append(";").append(purpose);
    return buf.toString();
  }
  
  @Override
  public void recycle ()
  {
    seedMap.clear();
  }
  
  // test-support
  int size()
  {
    return seedMap.size();
  }
}
