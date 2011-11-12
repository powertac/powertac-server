package org.powertac.common.repo;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.RandomSeed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"file:src/test/resources/test-config.xml"})
@DirtiesContext
public class RandomSeedRepoTests
{
  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @BeforeClass
  public static void setUpBeforeClass () throws Exception
  {
    PropertyConfigurator.configure("src/test/resources/log.config");
  }

  @Before
  public void setUp () throws Exception
  {
    randomSeedRepo.recycle();
  }

  @Test
  public void testRandomSeedRepo ()
  {
    RandomSeedRepo repo = new RandomSeedRepo();
    assertNotNull("created", repo);
    assertEquals("no entries", 0, repo.size());
  }
  
  @Test
  public void testAutowiredRepo ()
  {
    assertNotNull("created", randomSeedRepo);
    assertEquals("no entries", 0, randomSeedRepo.size());
  }

  @Test
  public void testGetRandomSeed1 ()
  {
    assertEquals("initially empty", 0, randomSeedRepo.size());
    RandomSeed rs1 = randomSeedRepo.getRandomSeed("Foo", 3, "test");
    assertNotNull("created seed", rs1);
    assertEquals("one entry", 1, randomSeedRepo.size());
  }

  @Test
  public void testGetRandomSeed2 ()
  {
    assertEquals("initially empty", 0, randomSeedRepo.size());
    RandomSeed rs1 = randomSeedRepo.getRandomSeed("Foo", 3, "test");
    RandomSeed rs2 = randomSeedRepo.getRandomSeed("Bar", 42, "more test");
    assertEquals("two entries", 2, randomSeedRepo.size());
    assertEquals("find rs1", rs1,
                 randomSeedRepo.getRandomSeed("Foo", 3, "test"));
    assertEquals("find rs1", rs2,
                 randomSeedRepo.getRandomSeed("Bar", 42, "more test"));
    assertEquals("still two entries", 2, randomSeedRepo.size());    
  }
  
  @Test
  public void checkLogfile ()
  {
    // at this point, the file log/test.state should contain two lines from
    // the two preceding tests, assuming the tests are run in order.
    try {
      BufferedReader input = new BufferedReader(new FileReader("log/test.state"));
      String seedClass = RandomSeed.class.getName();
      ArrayList<String> lines = new ArrayList<String>();
      String line;
      while ((line = input.readLine()) != null) {
        lines.add(line);
      }
      assertEquals("three lines", 3, lines.size());
      for (String entry : lines) {
        String[] fields = entry.split("::");
        assertEquals("correct classname", seedClass, fields[1]);
      }
    }
    catch (IOException ioe) {
      fail("IOException reading seedfile:" + ioe.toString());
    }
  }

  @SuppressWarnings("unused")
  @Test
  public void testRecycle ()
  {
    assertEquals("initially empty", 0, randomSeedRepo.size());
    RandomSeed rs1 = randomSeedRepo.getRandomSeed("Foo", 3, "test");
    RandomSeed rs2 = randomSeedRepo.getRandomSeed("Bar", 42, "more test");
    assertEquals("two entries", 2, randomSeedRepo.size());
    randomSeedRepo.recycle();
    assertEquals("empty again", 0, randomSeedRepo.size());    
  }

  @SuppressWarnings("unused")
  @Test
  public void testLoadRepo ()
  {
    try {
      randomSeedRepo.loadSeeds(new File("src/test/resources/randomSeedTest.state"));
    }
    catch (Exception fnf) {
      fail(fnf.toString());
    }
    assertEquals("two entries", 2, randomSeedRepo.size());
    RandomSeed rs1 = randomSeedRepo.getRandomSeed("Foo", 3, "test");
    RandomSeed rs2 = randomSeedRepo.getRandomSeed("Bar", 42, "more test");
    assertEquals("still two entries", 2, randomSeedRepo.size());
  }
}
