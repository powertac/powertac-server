package org.powertac.common.repo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
