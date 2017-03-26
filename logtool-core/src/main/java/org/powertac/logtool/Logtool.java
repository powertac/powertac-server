/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.powertac.logtool;

/**
 * This is the top level of the top-down version of the
 * Power TAC logtool analyzer. To use it, give a filename and the
 * classnames of a set of Analyzers.
 * @author John Collins
 */
public class Logtool extends LogtoolContext
{
  /**
   * Sets up the logtool, delegates everything to a LogtoolCore instance.
   */
  public static void main (String[] args)
  {
    Logtool lt = new Logtool();
    LogtoolCore lc = lt.getCore();

    int exitCode = 0;
    String error = lc.processCmdLine(args);

    if (error != null) {
      System.out.println(error);
      exitCode = -1;
    }

    // if we get here, it's time to exit
    System.exit(exitCode);
  }
}
