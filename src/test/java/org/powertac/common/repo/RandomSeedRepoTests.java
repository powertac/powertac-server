package org.powertac.common.repo;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powertac.common.RandomSeed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class RandomSeedRepoTests
{
  @Autowired
  private RandomSeedRepo randomSeedRepo;

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
    assertEquals("find rs2", rs2,
                 randomSeedRepo.getRandomSeed("Bar", 42, "more test"));
    assertEquals("still two entries", 2, randomSeedRepo.size());    
  }
  
  @Test
  public void checkLogfile ()
  {
    // Need to do this without depending on test sequence:
    File state = new File("log/test.state");
    try {
      //FileOutputStream stateFile = new FileOutputStream(state);
      //stateFile.getChannel().truncate(0);
      //stateFile.close();
      randomSeedRepo.getRandomSeed("FooTest", 3, "test");
      randomSeedRepo.getRandomSeed("FooTest", 42, "more test");
      randomSeedRepo.getRandomSeed("FooTest", -36, "third test");

      BufferedReader input = new BufferedReader(new FileReader("log/test.state"));
      String seedClass = RandomSeed.class.getName();
      ArrayList<String> lines = new ArrayList<String>();
      String line;
      while ((line = input.readLine()) != null) {
        lines.add(line);
      }
      assertTrue("at least three lines", lines.size() >= 3);
      int rsLines = 0;
      for (String entry : lines) {
        String[] fields = entry.split("::");
        if(seedClass.equals(fields[0].split(":")[1]) && fields[3].equals("FooTest"))
          rsLines += 1;
      }
      assertTrue("exactly three RandomSeed lines", rsLines == 3);
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
    assertEquals("two entries", 0, randomSeedRepo.size());
    RandomSeed rs1 = randomSeedRepo.getRandomSeed("Foo", 3, "test");
    assertEquals("correct seed value 1", -7938709514410200953l, rs1.getValue());
    RandomSeed rs2 = randomSeedRepo.getRandomSeed("Bar", 42, "more test");
    assertEquals("correct seed value 2", 2904941806851623619l, rs2.getValue());
    assertEquals("still two entries", 2, randomSeedRepo.size());
  }
}
