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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.Logger;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.logging.log4j.LogManager;
import org.powertac.common.Competition;
import org.powertac.common.msg.SimEnd;
import org.powertac.common.msg.SimStart;
import org.powertac.common.repo.DomainRepo;
import org.powertac.common.spring.SpringApplicationContext;
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
  private boolean isInterrupted = false;

  static private CompressorStreamFactory compressFactory = new CompressorStreamFactory();
  static private ArchiveStreamFactory archiveFactory = new ArchiveStreamFactory();

  /**
   * Default constructor
   */
  public LogtoolCore ()
  {
    super();
  }

  @PostConstruct
  public void postConstruct() {
    reader.registerNewObjectListener(new SimStartHandler(), SimStart.class);
    reader.registerNewObjectListener(new SimEndHandler(), SimEnd.class);
    builder.setup();
  }

  /**
   * Sets the per-timeslot pause value, used by Visualizer
   */
  public void setPerTimeslotPause (int msec)
  {
    reader.setTimeslotPause(msec);
  }

  /**
   * Processes a command line, providing a state-log file from the local
   * filesystem, or a remote URL.
   */
  public String processCmdLine (String[] args)
  {
    if (args.length < 2) {
      return "Usage: Logtool file analyzer ...";
    }
    String source = args[0];

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

    recycleRepos();

    return readStateLog(source, tools);
  }

  public void recycleRepos ()
  {
    // Only do this if started from the command line?
    // Recycle repos from previous session
    // TODO - make sure time is set first?
    List<DomainRepo> repos =
        SpringApplicationContext.listBeansOfType(DomainRepo.class);
      for (DomainRepo repo : repos) {
        repo.recycle();
      }
  }

  /**
   * Reads the given state-log source using the DomainObjectReader. Specify the
   * state-log as a local filename or a remote URL, or pass "-" or null to read
   * from standard-input.
   */
  public String readStateLog (String source, Analyzer... tools)
  {
    return readStateLog(getLogStream(source), tools);
  }

  public InputStream getLogStream (String source)
  {
    InputStream stream = null;
    
    // First, we figure out what kind of input we are dealing with
    if (null == source || "-".equals(source)) {
      log.info("Reading from standard input");
      stream = System.in;
    }
    else {
      try {
        URL inputURL = new URL(source);
        log.info("Reading url " + source);
        stream =  inputURL.openStream();
      } catch (MalformedURLException x) {
        // Continue, assuming it is a regular file
      } catch (IOException ioe) {
        // Continue
      }
      try {
        if (null == stream) {
          log.info("Reading file " + source);
          File inputFile = new File(source);
          if (!inputFile.canRead()) {
            log.error("Cannot read file {}", source);
            return null;
          }
          stream = new BufferedInputStream (new FileInputStream(inputFile));
        }
      } catch (IOException ioe) {
        // Continue
      }
    }
    
    // Next, we deal with the fact that the stream may be compressed, and may be an archive file
    //try {
    // Stack compression logic if appropriate
    if (stream.markSupported()) {
      // regular file
      stream = new BufferedInputStream(stream);
    }
    else {
      // some sort of compressed file
      try {
        if (!stream.markSupported()) {
          stream = new BufferedInputStream(stream);
        }
        stream = compressFactory.createCompressorInputStream(stream);
      } catch (CompressorException x) {
        // Stream not compressed (or unknown compression scheme)
        stream = null;
      }

      // Stack archive logic if appropriate
      try {
        if (!stream.markSupported()) {
          stream = new BufferedInputStream(stream);
        }
        ArchiveInputStream archiveStream = archiveFactory.createArchiveInputStream(stream);
        ArchiveEntry entry;
        stream = null;
        while ((entry = archiveStream.getNextEntry()) != null) {
          String name = entry.getName();
          if (entry.isDirectory() || !name.startsWith("log/")
                  || !name.endsWith(".state") || name.endsWith("init.state")) {
            continue;
          }
          stream = archiveStream;
          break;
        }
        if (stream == null) {
          log.error("Cannot read archive, no valid state log entry");
        }
      } catch (IOException ioe) {
        log.error(ioe.getMessage());
      } catch (ArchiveException x) {
        // Stream not archived (or unknown archiving scheme)
      }
    }
    return stream;
  }
  
  /**
   * Reads state-log from given input stream using the DomainObjectReader.
   */
  public String readStateLog (InputStream inputStream, Analyzer... tools)
  {
    Reader inputReader;
    String line = null;

    log.info("Reading state log from stream for {}",
             tools[0].getClass().getName());
    simEnd = false;
    isInterrupted = false;

    // Recycle repos from previous session
    // TODO - make sure time is set first? 
    List<DomainRepo> repos =
            SpringApplicationContext.listBeansOfType(DomainRepo.class);
    for (DomainRepo repo : repos) {
      repo.recycle();
    }

    // Now go read the state-log
    try {
      inputReader = new InputStreamReader(inputStream);
      for (Analyzer tool: tools) {
        log.info("Setting up {}", tool.getClass().getName());
        tool.setup();
      }
      BufferedReader in = new BufferedReader(inputReader);
      // extract schema, hand it off to the reader
      reader.setSchema(extractSchema(in));

      int lineNumber = 0;
      while (!simEnd) {
        synchronized(this) {
          if (isInterrupted) {
            in.close();
            break;
          }
        }
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

  public synchronized void interrupt() {
    isInterrupted = true;
  }
  
  private HashMap<String, String[]> extractSchema (BufferedReader input)
  throws IOException
  {
    HashMap<String, String[]> result = new HashMap<>();
    BufferedReader schema = input;
    int offset = 1; // embedded schema has msec field first
    schema.mark(64);
    String line = schema.readLine();
    String[] tokens = line.split(":");
    if (!tokens[offset].startsWith("Domain-schema")) {
      // pull in default schema for older logs
      System.out.println("No schema detected in state log, using default");
      schema.reset();
      InputStream defaultStream =
              Competition.class.getClassLoader().getResourceAsStream("metadata/domain-default.schema");
      schema = new BufferedReader(new InputStreamReader(defaultStream));
      offset = 0;
    }
    while (null != (line = schema.readLine())) {
      tokens = line.split(":");
      if (tokens[offset].startsWith("schema.end"))
        break;
      // first token is class, rest are fields
      result.put(tokens[offset], tokens[offset + 1].split(","));
    }
    return result;
  }

  class SimStartHandler implements NewObjectListener
  {
    @Override
    public void handleNewObject (Object thing) {
    }
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
