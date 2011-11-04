package org.powertac.common.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.powertac.common.PluginConfig;

public class PluginConfigRepoTests
{
  PluginConfigRepo repo;
  
  @Before
  public void setUp () throws Exception
  {
    // start each test with a fresh repo
    repo = new PluginConfigRepo();
  }

  @Test
  public void testInit ()
  {
    assertNotNull("repo not null", repo);
  }

  @Test
  public void testMake ()
  {
    PluginConfig pic = repo.makePluginConfig("role", "name");
    assertNotNull("created pic", pic);
    assertEquals("correct name", "name", pic.getName());
    assertEquals("correct role", "role", pic.getRoleName());
  }

  @Test
  public void testList ()
  {
    assertEquals("no entries", 0, repo.list().size());
    PluginConfig pic = repo.makePluginConfig("role", "name");
    assertNotNull("created pic", pic);
    List<PluginConfig> result = repo.list();
    assertEquals("one item", 1, result.size());
    assertEquals("correct item", pic, result.get(0));
    pic = repo.makePluginConfig("another-role", "another-name");
    result = repo.list();
    assertEquals("two items", 2, result.size());
  }
  
  @Test
  public void testFindByRoleName ()
  {
    PluginConfig pic1 = repo.makePluginConfig("role1", "name");
    PluginConfig pic2 = repo.makePluginConfig("role2", "name");
    assertEquals("find first", pic1, repo.findByRoleName("role1"));
    assertEquals("find second", pic2, repo.findByRoleName("role2"));
  }
  
  @Test
  public void testFindAllByRoleName ()
  {
    PluginConfig pic1 = repo.makePluginConfig("role1", "name");
    PluginConfig pic21 = repo.makePluginConfig("role2", "name1");
    PluginConfig pic22 = repo.makePluginConfig("role2", "name2");
    List<PluginConfig> result = repo.findAllByRoleName("role1");
    assertEquals("one found", 1, result.size());
    assertEquals("found first", pic1, result.get(0));
    result = repo.findAllByRoleName("bogus");
    assertEquals("none found", 0, result.size());
    result = repo.findAllByRoleName("role2");
    assertEquals("two found", 2, result.size());
    assertTrue("includes 21", result.contains(pic21));
    assertTrue("includes 22", result.contains(pic22));
  }
  
  @Test
  public void testFindMatching ()
  {
    // stick some in the repo
    PluginConfig pic1 = repo.makePluginConfig("role1", "name");
    PluginConfig pic21 = repo.makePluginConfig("role2", "name1");
    PluginConfig pic22 = repo.makePluginConfig("role2", "name2");
    // make some standalone and match them
    PluginConfig pic1x = new PluginConfig("role1", "name");
    assertEquals("match pic1", pic1, repo.findMatching(pic1x));
    assertEquals("match pic21", pic21,
                 repo.findMatching(new PluginConfig("role2", "name1")));
    assertEquals("match pic22", pic22,
                 repo.findMatching(new PluginConfig("role2", "name2")));
    assertNull("no match",
               repo.findMatching(new PluginConfig("role2", "Name2")));
  }
  
  @Test
  public void testIsPrivileged ()
  {
    PluginConfig pic1 = repo.makePluginConfig("role1", "name");
    PluginConfig pic21 = repo.makePluginConfig("role2", "name1").asPrivileged();
    PluginConfig pic22 = repo.makePluginConfig("role2", "name2");
    assertFalse("pic1 not privileged", pic1.isPrivileged());
    assertTrue("pic21 is privileged", pic21.isPrivileged());
    assertFalse("pic22 not privileged", pic22.isPrivileged());
    assertEquals("3 pics", 3, repo.list().size());
    assertEquals("2 public pics", 2, repo.findAllPublic().size());
  }

  @Test
  public void testRecycle ()
  {
    PluginConfig pic = repo.makePluginConfig("role", "name");
    assertNotNull("created pic", pic);
    List<PluginConfig> result = repo.list();
    pic = repo.makePluginConfig("another-role", "another-name");
    result = repo.list();
    assertEquals("two items", 2, result.size());
    repo.recycle();
    result = repo.list();
    assertEquals("empty repo", 0, result.size());
    pic = repo.makePluginConfig("third-role", "third-name");
    result = repo.list();
    assertEquals("two items", 1, result.size());
    assertEquals("correct item", "third-role", result.get(0).getRoleName());
  }
}
