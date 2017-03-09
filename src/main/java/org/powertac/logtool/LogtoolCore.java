/*
 * Copyright (c) 2012 by the original author
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
package org.powertac.logtool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.msg.SimEnd;
import org.powertac.logtool.common.DomainObjectReader;
import org.powertac.logtool.common.MissingDomainObject;
import org.powertac.logtool.common.DomainBuilder;
import org.powertac.logtool.common.NewObjectListener;
import org.powertac.logtool.ifc.Analyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Reads a state log file, re-creates and updates objects, calls
 * listeners.
 * @author John Collins
 */
@Service
public class LogtoolCore
{
  static private Logger log = LogManager.getLogger(LogtoolCore.class.getName());

  @Autowired
  private DomainObjectReader reader;
  
  @Autowired
  private DomainBuilder builder;

  private boolean simEnd = false;

  /**
   * Default constructor
   */
  public LogtoolCore ()
  {
    super();
  }

  /**
   * Processes a command line. For now, it's just the name of a
   * state-log file from the simulation server.
   */
  public String processCmdLine (String[] args)
  {
    if (args.length < 2) {
      return "Usage: Logtool file analyzer ...";
    }
    String filename = args[0];

    Analyzer[] tools = new Analyzer[args.length - 1];
    for (int i = 1; i < args.length; i++) {
      try {
        Class<?> toolClass = Class.forName(args[i]);
        tools[i - 1] = (Analyzer) toolClass.newInstance();
      }
      catch (ClassNotFoundException e1) {
        return "Cannot find analyzer class " + args[i];
      }
      catch (Exception ex) {
        return "Exception creating analyzer " + ex.toString();
      }
    }

    return readStateLog(filename, tools);
  }

  /**
   * Reads the given state-log file using the DomainObjectReader.
   * If a filename is given, we assume it names a state log file.
   * If filename is "-" or null, then we assume the state log is
   * on standard-input. This allows for piping the input from
   * a compressed archive.
   */
  public String readStateLog (String filename, Analyzer... tools)
  {
    if (null == filename || "-".equals(filename)) {
      log.info("Reading from standard input");
      return readStateLog(System.in, tools);
    }
    log.info("Reading file " + filename);
    File inputFile = new File(filename);
    if (!inputFile.canRead()) {
      return "Cannot read file " + filename;
    }
    return readStateLog(inputFile, tools);
  }

  /**
   * Reads state-log from given input file using the DomainObjectReader.
   */
  public String readStateLog (File inputFile, Analyzer... tools)
  {
    try{
      return readStateLog(new FileInputStream(inputFile), tools);
    } catch (FileNotFoundException e) {
      return "Cannot open file " + inputFile.getPath();
    }
  }

  /**
   * Reads state-log from given input stream using the DomainObjectReader.
   */
  public String readStateLog (InputStream inputStream, Analyzer... tools)
  {
    reader.registerNewObjectListener(new SimEndHandler(), SimEnd.class);
    Reader inputReader;
    String line = null;
    try {
      inputReader = new InputStreamReader(inputStream);
      builder.setup();
      for (Analyzer tool: tools) {
        tool.setup();
      }
      BufferedReader in = new BufferedReader(inputReader);
      int lineNumber = 0;
      while (!simEnd) {
        line = in.readLine();
        if (null == line) {
          log.info("Last line " + lineNumber);
          break;
        }
        lineNumber += 1;
        reader.readObject(line);
      }
      builder.report();
      for (Analyzer tool: tools) {
        tool.report();
      }
    }
    catch (IOException e) {
      return "Error reading from stream";
    }
    catch (MissingDomainObject e) {
      return "MDO on " + line;
    }
    return null;
  }


  class SimEndHandler implements NewObjectListener
  {

    @Override
    public void handleNewObject (Object thing)
    {
      simEnd = true;
    }
    
  }
}
