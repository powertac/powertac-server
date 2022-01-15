/*
 * Copyright (c) 2012-2022 by John Collins
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
import org.powertac.logtool.ifc.ObjectReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Given command-line arguments, reads a state log, re-creates and updates objects, calls
 * message handlers in specified Analyzers.
 * 
 * State log can be a .state file produced by a simulation session, with or without the initial
 * metadata block, and classnames can be abbreviated or not. Also, the file can be embedded in an
 * archive (a tar file) and can be compressed. So a typical use is to pass it the URL for a
 * compressed log from a tournament.
 * 
 * A state log can also be a source of weather and/or random-seed data for controlling variability
 * in an experiment design. To serve that purpose, it can be useful to just get the input stream
 * using <code>getLogStream(String source)<code>.
 * 
 * When reading a state log, it can be useful to filter the classes that are processed. To restrict
 * processing to specific classes, call <code>includeClassname(String classname)</code> for each
 * class to be processed.
 * 
 * @author John Collins
 */
@Service
public class LogtoolCore
{
  static private Logger log = LogManager.getLogger(LogtoolCore.class.getName());

  // This value must not be accessed directly other than by the getDOR() method.
  @Autowired
  private DomainObjectReader reader;
  
  @Autowired
  private DomainBuilder domainBuilder;

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

  // lazy initialization for the DOR ensures that the SimStart and SimEnd handlers get installed
  // in all cases, including unit tests.
  private boolean readerInitialized = false;
  public DomainObjectReader getDOR ()
  {
    if (!readerInitialized) {
      initializeReader();
    }
    return reader;
  }

  private void initializeReader ()
  {
    domainBuilder.setup();
    reader.registerNewObjectListener(new SimStartHandler(), SimStart.class);
    reader.registerNewObjectListener(new SimEndHandler(), SimEnd.class);
    readerInitialized = true;    
  }

  /**
   * Sets the per-timeslot pause value, used by Visualizer
   */
  public void setPerTimeslotPause (int msec)
  {
    getDOR().setTimeslotPause(msec);
  }

  /**
   * Adds the given classname to the list of IncludesOnly classes in the DomainObjectReader. If this
   * list is non-empty, then only the specified classes will be included in the state log scan.
   * Note that the SimEnd type is always included.
   */
  public void includeClassname (String classname)
  {
    getDOR().addIncludesOnly(classname);
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
        tools[i - 1] = (Analyzer) toolClass.getDeclaredConstructor().newInstance();
      }
      catch (ClassNotFoundException e1) {
        return "Cannot find analyzer class " + args[i];
      }
      catch (Exception ex) {
        return "Exception creating analyzer " + ex.toString();
      }
    }

    recycleRepos();
    getDOR().reset();

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
   * Resets the DomainObjectReader, removing filtering criteria. If scanning a state log twice
   * in a single session, it is almost certainly necessary to call this before each scan. For example,
   * an experiment may specify the same state log for both RandomSeeds and for Weather data, in which
   * case two separate scans will be necessary. If <code>instantiate</code> is <code>false</code>, then
   * the reader will not instantiate objects or set the time in the current running environment. This
   * is needed if the purpose of reading a log is to extract weather or random-seed data in an
   * experiment environment.
   */
  public void resetDOR (boolean instantiate)
  {
    getDOR().reset();
    getDOR().setInstantiate(instantiate);
  }

  /**
   * Reads the given state-log source using the DomainObjectReader. Specify the
   * state-log as a local filename or a remote URL, or pass "-" or null to read
   * from standard-input.
   */
  public String readStateLog (String source, Analyzer... tools)
  {
    BufferedReader reader = getLogStream(source);
    if (null == reader) {
      log.error("Cannot open {}", source);
      return null;
    }
    return readStateLog(reader, tools);
  }

  /**
   * Opens the state log file, uncompressing it and extracting it from an archive as needed.
   * Before returning, the schema (if any) is read and applied to the DomainObjectReader and
   * the stream is positioned just past the schema block
   */
  public BufferedReader getLogStream (String source)
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
        stream = inputURL.openStream();
      } catch (MalformedURLException x) {
        stream = null;
        // Continue, assuming it is a regular file
      } catch (IOException ioe) {
        stream = null;
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
          stream = new FileInputStream(inputFile);
        }
      } catch (IOException ioe) {
        // Continue
      }
    }
    return getLogStream(stream);
  }

  /**
   * Pulls apart an input stream if needed.
   */
  public BufferedReader getLogStream (InputStream stream)
  {
    // Here we deal with the fact that the stream may be compressed, and may be an archive file
    //try {
    // Stack compression logic if appropriate
    //if (stream.markSupported()) {
      // regular file
      //stream = new BufferedInputStream(stream);
    //}
    //else {
      // might be some sort of compressed file
      try {
        if (!stream.markSupported()) {
          stream = new BufferedInputStream(stream);
        }
        stream = compressFactory.createCompressorInputStream(stream);
      } catch (CompressorException x) {
        // Stream not compressed (or unknown compression scheme)
        //stream = null;
      }

      // Stack archive logic if appropriate
      try {
        if (!stream.markSupported()) {
          stream = new BufferedInputStream(stream);
        }
        ArchiveInputStream archiveStream = archiveFactory.createArchiveInputStream(stream);
        ArchiveEntry entry;
        //stream = null;
        while ((entry = archiveStream.getNextEntry()) != null) {
          String name = entry.getName();
          if (entry.isDirectory() || !name.contains("log/")
                  || !name.endsWith(".state") || name.endsWith("init.state")) {
            continue;
          }
          log.debug("Reading state log {}", name);
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
    //}
    Reader inputReader = new InputStreamReader(stream);
    BufferedReader in = new BufferedReader(inputReader);
    // extract schema, hand it off to the reader
    try {
      getDOR().setSchema(extractSchema(in));
    } catch (IOException ioe) {
      log.error("IOException reading schema {}", ioe.getCause());
      return null;
    }
    return in;
  }

  /**
   * Reads a state log that may be compressed and/or archived
   */
  public String readStateLog (InputStream stream, Analyzer...tools)
  {
    return readStateLog(getLogStream(stream), tools);
  }
  
  /**
   * Reads state-log from given input stream using the DomainObjectReader.
   */
  public String readStateLog (BufferedReader in, Analyzer... tools)
  {
    String line = null;

    log.debug("Reading state log from stream for {}",
             tools[0].getClass().getName());
    simEnd = false;
    isInterrupted = false;

    // Now go read the state-log
    for (Analyzer tool: tools) {
      log.info("Setting up {}", tool.getClass().getName());
      try {
        tool.setup();
      } catch (FileNotFoundException fnf) {
        log.error("File not found setting up {}", tool.getClass().getName());
      }
    }

    LogReader logReader = new LogReader(in);
    Object result;
    while (!simEnd) {
      synchronized(this) {
        if (isInterrupted) {
          break;
        }
      }
      result = logReader.getNext();
      if (null == result) {
        // end of file, presumably
        log.error("End of file without finding SimEnd");
        break;
      }
    }
    domainBuilder.report();
    for (Analyzer tool: tools) {
      tool.report();
    }
    try {
      in.close();
    }
    catch (IOException ioe) {
      log.error("Exception closing logfile {}", ioe.getCause());
    }
    return null;
  }

  public synchronized void interrupt() {
    isInterrupted = true;
  }

  /**
   * Returns an incremental ObjectReader instances positioned at the start of content in
   * a state log. Before getting it, reset the DOR by calling resetDOR(), and set the filter
   * criteria by calling includeClassname() or excludeClassname().
   */
  public ObjectReader getObjectReader (String location)
  {
    BufferedReader logInput = getLogStream(location);
    if (null == logInput) {
      log.error("Cannot open log reader at {}", location);
      return null;
    }
    ObjectReader result = new LogReader(logInput);
    return result;
  }
  
  private HashMap<String, String[]> extractSchema (BufferedReader input)
  throws IOException
  {
    HashMap<String, String[]> result = new HashMap<>();
    BufferedReader schema = input;
    int offset = 1; // embedded schema has msec field first
    schema.mark(64);
    String line = schema.readLine();
    log.debug("First line of log: {}", line);
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
      if (tokens[offset].startsWith("schema.end")) {
        log.debug("Schema end: {}", line);
        break;
      }
      // first token is class, rest are fields
      result.put(tokens[offset], tokens[offset + 1].split(","));
    }
    return result;
  }

  class LogReader implements ObjectReader
  {
    int lineNumber = 0;
    BufferedReader in;

    LogReader (BufferedReader input)
    {
      super();
      in = input;
    }
    
    // result is null ONLY on EOF. Otherwise it's either a domain object or a String.
    Object getNext()
    {
      String line = "";
      Object result = null;
      try {
        while (null == result) {
          line = in.readLine();
          if (null == line) {
            log.debug("Last line " + lineNumber);
            in.close();
            return null;
          }
          lineNumber += 1;
          log.debug("getNext reading {}", line);
          result = getDOR().readObject(line);
          log.debug("DOR result {}", result);
          //        if (null != dorResult)
          //          return dorResult;
          //        else
          //          return line;
        }
      }
      catch (IOException e) {
        return "Error reading from stream";
      }
      catch (MissingDomainObject e) {
        return "MDO on " + line;
      }
      return result;
    }

    @Override
    public Object getNextObject ()
    {
      Object result = "start";
      while (result != null) {
        if (result.getClass() != String.class) {
          break;
        }
        result = getNext();
      }
      return result;
    }

    @Override
    public void close ()
    {
      try {
        in.close();
      } catch (IOException ioe) {
        // we'll ignore this, probably the stream is already closed.
      }
    }
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
      log.info("SimEnd");
      simEnd = true;
    }
    
  }
}
