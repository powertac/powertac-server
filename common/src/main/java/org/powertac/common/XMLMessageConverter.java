/*
 * Copyright (c) 2011 by the original author or authors.
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

package org.powertac.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import java.time.Instant;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.powertac.common.msg.CustomerBootstrapData;
import org.powertac.common.msg.SimEnd;
import org.powertac.common.msg.SimPause;
import org.powertac.common.msg.SimResume;
import org.powertac.common.msg.SimStart;
import org.powertac.common.spring.SpringApplicationContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
/**
 * Initializes the XStream message serialization system
 * 
 * @author John Collins
 */
@Service("xmlMessageConverter")
public class XMLMessageConverter 
{
  private static final Logger log = LogManager.getLogger(XMLMessageConverter.class);
  
  @SuppressWarnings("rawtypes")
  private Class[] commandClasses = { CustomerBootstrapData.class, SimEnd.class,
      SimStart.class, SimPause.class, SimResume.class };

  private XStream xstream;

  public static final XStream getXStream() {
      XStream xstream = new XStream();
      //XStream.setupDefaultSecurity(xstream); // TODO Remove with XStream 1.5
      xstream.allowTypesByWildcard(new String[] {"org.powertac.**"});
      
      // Register custom converter for java.time.Instant
      xstream.registerConverter(new CustomInstantConverter());
      
      return xstream;
  }

  private static class CustomInstantConverter
    implements com.thoughtworks.xstream.converters.Converter
  {

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert (Class type)
    {
      return Instant.class.equals(type);
    }

    @Override
    public void marshal (Object source, HierarchicalStreamWriter writer,
                         MarshallingContext context)
    {
      Instant instant = (Instant) source;
      writer.startNode("iMillis");
      writer.setValue(String.valueOf(instant.toEpochMilli()));
      writer.endNode();
    }

    @Override
    public Object unmarshal (HierarchicalStreamReader reader,
                             UnmarshallingContext context)
    {
      reader.moveDown();
      long millis = Long.parseLong(reader.getValue());
      reader.moveUp();
      return Instant.ofEpochMilli(millis);
    }
  }


  // inject context here so that it would be initialized before this class
  // @PostConstruct method get called and use the singleton.
  @SuppressWarnings("unused")
  @Autowired
  private SpringApplicationContext context;

  @SuppressWarnings("rawtypes")
  @PostConstruct
  public void afterPropertiesSet() {
    xstream = XMLMessageConverter.getXStream();
    try {
      List<Class> classes = findMyTypes("org.powertac.common");
      for (Class clazz : classes) {
        log.info("processing class " + clazz.getName());
        xstream.processAnnotations(clazz);
      }
    } catch (IOException | ClassNotFoundException e) {
      log.error("failed to process annotation", e);
    }

    for (Class commandClazz : commandClasses) {
      xstream.processAnnotations(commandClazz);
    }

    xstream.autodetectAnnotations(true);

    xstream.aliasSystemAttribute(null, "class");
  }

  public String toXML(Object message) {
    return xstream.toXML(message);
  }

  public Object fromXML(String xml) {
    return xstream.fromXML(xml);
  }
    
  @SuppressWarnings("rawtypes")
  private List<Class> findMyTypes(String basePackage) throws IOException, ClassNotFoundException
  {
      ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
      MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

      List<Class> candidates = new ArrayList<Class>();
      String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                                 resolveBasePackage(basePackage) + "/" + "**/*.class";
      Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
      for (Resource resource : resources) {
          if (resource.isReadable()) {
              MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
              candidates.add(Class.forName(metadataReader.getClassMetadata().getClassName()));
          }
      }
      return candidates;
  }

  private String resolveBasePackage(String basePackage) {
      return ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage));
  }
}
