/*
 * Copyright 2009-2015 the original author or authors.
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

import java.util.HashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.RandomSeed;
import org.springframework.stereotype.Service;

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
@Service
public class RandomSeedRepo implements DomainRepo
{
  static private Logger log = LogManager.getLogger(RandomSeedRepo.class.getName());
  
  private HashMap<String, RandomSeed> seedMap;
  private HashMap<String, Long> pendingSeedMap;

  public RandomSeedRepo ()
  {
    super();
    seedMap = new HashMap<String, RandomSeed>();
    pendingSeedMap = new HashMap<String, Long>();
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
    if (null == result) {
      // try getting the seed from the pending map
      Long seedValue = pendingSeedMap.get(name);
      if (null == seedValue) {
        // create a new one
        log.debug("New seed created: " + classname + ";" +
                id + ";" + purpose);
        result = new RandomSeed(classname, id, purpose);
      }
      else {
        log.info("Stored seed " + seedValue + " retrieved for " + name);
        result = new RandomSeed(classname, id, purpose, seedValue);
      }
      seedMap.put(name, result);
    }
    else {
      log.info("Seed from map for " + name);
    }
    return result;
  }

  /**
   * Adds the given seed to the map. Intended to be used when loading seeds from a file.
   */
  public void restoreRandomSeed (RandomSeed seed)
  {
    String name = composeName(seed.getRequesterClass(), seed.getRequesterId(), seed.getPurpose());
    seedMap.put(name,  seed);
  }
  
//  /**
//   * Pre-loads seeds from an existing server logfile, or from a stripped-down
//   * logfile containing only the RandomSeed lines.
//   */
//  public void loadSeeds (File inputFile)
//  throws FileNotFoundException
//  {
//    log.info("Loading seeds from file" + inputFile.getPath());
//    loadSeeds(new FileReader(inputFile));
//  }
//  
//  
//  /**
//   * Pre-loads seeds from a stream.
//   */
//  public void loadSeeds (InputStreamReader reader)
//  {
//    BufferedReader input = new BufferedReader(reader);
//    String seedClass = RandomSeed.class.getName();
//    try {
//      String line;
//      while ((line = input.readLine()) != null) {
//        log.debug("original line: " + line);
//        // first, strip off the process time nnnn:
//        int colon = line.indexOf(':');
//        if (colon <= 0 || line.length() <= colon + 2) {
//          log.warn("Malformed line " + line);
//          break;
//        }
//        line = line.substring(colon + 1);
//        String[] fields = line.split("::");
//        if (seedClass.equals(fields[0])) {
//          if (fields.length != 7) {
//            log.error("Bad seed spec: " + line);
//          }
//          else {
//            //System.out.println("fields[3, 4, 5, 6]: " + fields[3]
//            //        + "," + fields[4] + "," + fields[5] + "," + fields[6]);
//            //RandomSeed seed = new RandomSeed(fields[3],
//            //                                 Long.parseLong(fields[4]), 
//            //                                 fields[5],
//            //                                 Long.parseLong(fields[6]));
//            pendingSeedMap.put(composeName(fields[3], 
//                                           Long.parseLong(fields[4]),
//                                           fields[5]),
//                               Long.parseLong(fields[6]));
//          }
//        }
//      }
//    }
//    catch (IOException ioe) {
//      log.error("IOException reading seedfile:" + ioe.toString());
//    }
//  }
  
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
    pendingSeedMap.clear();
  }
  
  // test-support
  int size()
  {
    return seedMap.size();
  }
}
