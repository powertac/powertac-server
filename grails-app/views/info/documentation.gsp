%{--
  - Copyright 2009-2010 the original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -  http://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an
  -
  - "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
  -
  - either express or implied. See the License for the specific language
  - governing permissions and limitations under the License.
  --}%

<html>
<head>
  <title>TAC Energy Registration</title>
  <meta name="layout" content="main"/>
</head>
<body>

<div class="nav">
  <span class="menuButton"><a class="home" href="${createLink(uri: '/')}">Home</a></span>
</div>
<div class="body">

  <div class="prepend-1 span-18 append-1 latest">
    <h1>Reference Documentation</h1>
    At the moment we do not have an online documentation for the TAC Energy demo agent. Simply <a href="http://ibwhudson.iw.uni-karlsruhe.de/job/tacenergy_demoagent/lastStableBuild/" target=_blank">grab the latest version</a>
    from our <a href="http://ibwhudson.iw.uni-karlsruhe.de" target="_blank">CI Server</a>. After unzipping it you may find the reference documentation in the
    subfolder "manual".
 </div>
  <div class="prepend-1 prepend-top span-18 append-1 latest">
    Alternatively you might simply point your command line to the installation directory fo your demo agent and write
    <tt>grails docs</tt>. This will generate the documentation for you on the fly. It is then located in your
    <tt>$INSTALL_DIR/docs</tt> folder.
  </div>

</div>
</body>
</html>