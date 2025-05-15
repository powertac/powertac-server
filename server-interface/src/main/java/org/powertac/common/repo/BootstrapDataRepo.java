/*
 * Copyright (c) 2015-2016 by John Collins
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

import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.powertac.common.Competition;
import org.powertac.common.CustomerInfo;
import org.powertac.common.XMLMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Repository for data contained in a bootstrap record.
 * Presumably this will be unused during a boot session.
 * The bootstrap data is simply an array of various types of objects.
 * The repo allows retrieval of the entire array, or just the items of
 * a particular class. It also reads a boot record, allowing it to be used
 * outside the server without duplicating this functionality.
 * 
 * @author John Collins
 */
@Service
public class BootstrapDataRepo implements DomainRepo
{
  static private Logger log = LogManager.getLogger(BootstrapDataRepo.class);

  @Autowired
  private XMLMessageConverter messageConverter;

  // The data store
  private Competition bootstrapCompetition;
  private ArrayList<Object> data;
  private HashMap<Class<?>, List<Object>> classMap;
  private Properties bootState;

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

  /**
   * Returns the Competition instances from the boot record
   */
  public Competition getBootstrapCompetition ()
  {
    return bootstrapCompetition;
  }

  /**
   * Returns the Properties recorded in the boot record, including various
   * values representing the state of the sim at the end of the boot session.
   */
  public Properties getBootState ()
  {
    return bootState;
  }

  @Override
  public void recycle ()
  {
    data.clear();
    classMap.clear();
  }  

  public void readBootRecord (URL bootUrl)
  {
    Document document = getDocument(bootUrl);
    XPathFactory factory = XPathFactory.newInstance();
    XPath xPath = factory.newXPath();
    try {
      // first grab the Competition
      XPathExpression exp =
          xPath.compile("/powertac-bootstrap-data/config/competition");
      NodeList nodes = (NodeList) exp.evaluate(document,
          XPathConstants.NODESET);
      String xml = nodeToString(nodes.item(0));
      bootstrapCompetition = (Competition) messageConverter.fromXML(xml);
      add(bootstrapCompetition);
      for (CustomerInfo cust: bootstrapCompetition.getCustomers())
        add(cust);

      // next, grab the bootstrap-state and add it to the config
      exp = xPath.compile("/powertac-bootstrap-data/bootstrap-state/properties");
      nodes = (NodeList) exp.evaluate(document, XPathConstants.NODESET);
      if (null != nodes && nodes.getLength() > 0) {
        // handle the case where there is no bootstrap-state clause
        xml = nodeToString(nodes.item(0));
        bootState = (Properties) messageConverter.fromXML(xml);
      }
    }
    catch (XPathExpressionException xee) {
      log.error("Error reading boot record from {}: {}",
                bootUrl, xee.toString());
      System.out.println("Error reading boot dataset: " + xee.toString());
    }
    processBootDataset(document);
  }

  // Extracts a bootstrap dataset from its file
  private void processBootDataset (Document document)
  {
    // Read and convert the bootstrap dataset
    XPathFactory factory = XPathFactory.newInstance();
    XPath xPath = factory.newXPath();
    try {
      // we want all the children of the bootstrap node
      XPathExpression exp =
          xPath.compile("/powertac-bootstrap-data/bootstrap/*");
      NodeList nodes = (NodeList)exp.evaluate(document, XPathConstants.NODESET);
      log.info("Found " + nodes.getLength() + " bootstrap nodes");
      // Each node is a bootstrap data item
      for (int i = 0; i < nodes.getLength(); i++) {
        String xml = nodeToString(nodes.item(i));
        add(messageConverter.fromXML(xml));
      }
    }
    catch (XPathExpressionException xee) {
      log.error("runOnce: Error reading config file: " + xee.toString());
    }
  }

  // Reads an xml doc from a URL
  private Document getDocument (URL bootUrl)
  {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try {
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
      factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    }
    catch (ParserConfigurationException e) {
      log.error("Error setting parser features: " + e.toString());
    }
    factory.setXIncludeAware(false);
    factory.setExpandEntityReferences(false);
    factory.setNamespaceAware(true);
    DocumentBuilder builder;
    Document doc = null;
    try {
      builder = factory.newDocumentBuilder();
      InputStream stream = bootUrl.openStream();
      doc = builder.parse(stream);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return doc;
  }

  // Converts an xml node into a string that can be converted by XStream
  private String nodeToString(Node node) {
    StringWriter sw = new StringWriter();
    try {
      Transformer t = TransformerFactory.newInstance().newTransformer();
      t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      t.setOutputProperty(OutputKeys.INDENT, "no");
      t.transform(new DOMSource(node), new StreamResult(sw));
    }
    catch (TransformerException te) {
      log.error("nodeToString Transformer Exception " + te.toString());
    }
    return sw.toString();
  }
}
