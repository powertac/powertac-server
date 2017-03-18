package org.powertac.visualizer.services;

import java.io.IOException;

import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Service
public class VisualizerResourceLoaderService implements ResourceLoaderAware
{
  private ResourceLoader resourceLoader;

  public void setResourceLoader (ResourceLoader resourceLoader)
  {
    this.resourceLoader = resourceLoader;
  }

  public Resource getResource (String location)
  {
    return resourceLoader.getResource(location);
  }

  public String getOutputCanonicalPath () throws IOException
  {
    return resourceLoader.getResource("WEB-INF/output").getFile()
            .getCanonicalPath();
  }

  public String getConfigCanonicalPath () throws IOException
  {
    return resourceLoader.getResource("WEB-INF/config").getFile()
            .getCanonicalPath();
  }
}
