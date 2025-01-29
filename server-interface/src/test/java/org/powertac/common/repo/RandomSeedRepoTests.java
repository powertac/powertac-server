package org.powertac.common.repo;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powertac.common.RandomSeed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;

@SpringJUnitConfig(locations = {"classpath:test-config.xml"})
@DirtiesContext
@TestExecutionListeners(listeners = {
  DependencyInjectionTestExecutionListener.class,
  DirtiesContextTestExecutionListener.class
})
public class RandomSeedRepoTests
{
  @Autowired
  private RandomSeedRepo randomSeedRepo;

  @BeforeEach
  public void setUp () throws Exception
  {
    randomSeedRepo.recycle();
  }

  @Test
  public void testRandomSeedRepo ()
  {
    RandomSeedRepo repo = new RandomSeedRepo();
    assertNotNull(repo, "created");
    assertEquals(0, repo.size(), "no entries");
  }
  
  @Test
  public void testAutowiredRepo ()
  {
    assertNotNull(randomSeedRepo, "created");
    assertEquals(0, randomSeedRepo.size(), "no entries");
  }

  @Test
  public void testGetRandomSeed1 ()
  {
    assertEquals(0, randomSeedRepo.size(), "initially empty");
    RandomSeed rs1 = randomSeedRepo.getRandomSeed("Foo", 3, "test");
    assertNotNull(rs1, "created seed");
    assertEquals(1, randomSeedRepo.size(), "one entry");
  }

  @Test
  public void testGetRandomSeed2 ()
  {
    assertEquals(0, randomSeedRepo.size(), "initially empty");
    RandomSeed rs1 = randomSeedRepo.getRandomSeed("Foo", 3, "test");
    RandomSeed rs2 = randomSeedRepo.getRandomSeed("Bar", 42, "more test");
    assertEquals(2, randomSeedRepo.size(), "two entries");
    assertEquals(rs1, randomSeedRepo.getRandomSeed("Foo", 3, "test"), "find rs1");
    assertEquals(rs2, randomSeedRepo.getRandomSeed("Bar", 42, "more test"), "find rs2");
    assertEquals(2, randomSeedRepo.size(), "still two entries");    
  }

  @Test
  public void checkLogfile ()
  {
    try (BufferedReader input = new BufferedReader(new FileReader("log/test.state"))) {
      randomSeedRepo.getRandomSeed("FooTest", 3, "test");
      randomSeedRepo.getRandomSeed("FooTest", 42, "more test");
      randomSeedRepo.getRandomSeed("FooTest", -36, "third test");

      String seedClass = RandomSeed.class.getName();
      ArrayList<String> lines = new ArrayList<String>();
      String line;
      while ((line = input.readLine()) != null) {
        lines.add(line);
      }
      assertTrue(lines.size() >= 3, "at least three lines");
      int rsLines = 0;
      for (String entry : lines) {
        String[] fields = entry.split("::");
        if(seedClass.equals(fields[0].split(":")[1]) && fields[3].equals("FooTest"))
          rsLines += 1;
      }
      assertTrue(rsLines == 3, "exactly three RandomSeed lines");
    }
    catch (IOException ioe) {
      fail("IOException reading seedfile:" + ioe.toString());
    }
  }

  @SuppressWarnings("unused")
  @Test
  public void testRecycle ()
  {
    assertEquals(0, randomSeedRepo.size(), "initially empty");
    RandomSeed rs1 = randomSeedRepo.getRandomSeed("Foo", 3, "test");
    RandomSeed rs2 = randomSeedRepo.getRandomSeed("Bar", 42, "more test");
    assertEquals(2, randomSeedRepo.size(), "two entries");
    randomSeedRepo.recycle();
    assertEquals(0, randomSeedRepo.size(), "empty again");    
  }
//
//  @Test
//  public void testLoadRepo ()
//  {
//    try {
//      randomSeedRepo.loadSeeds(new File("src/test/resources/randomSeedTest.state"));
//    }
//    catch (Exception fnf) {
//      fail(fnf.toString());
//    }
//    assertEquals(0, randomSeedRepo.size(), "two entries");
//    RandomSeed rs1 = randomSeedRepo.getRandomSeed("Foo", 3, "test");
//    assertEquals(-7938709514410200953l, rs1.getValue(), "correct seed value 1");
//    RandomSeed rs2 = randomSeedRepo.getRandomSeed("Bar", 42, "more test");
//    assertEquals(2904941806851623619l, rs2.getValue(), "correct seed value 2");
//    assertEquals(2, randomSeedRepo.size(), "still two entries");
//  }
}
