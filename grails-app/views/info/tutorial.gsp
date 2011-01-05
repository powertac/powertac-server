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
    <title>Quick Start Tutorial</title>
    <meta name="layout" content="main"/>
</head>
<body>

<h1>Quick Start Tutorial</h1>

<div class="clear"></div>

<div id="content-box">

    <div class="section">

        <div class="section-header">
            1. Download the demo client
        </div>

        <div class="section-content tutorial">

            <div class="grid_6"><img src="${resource(dir: 'images/tutorial', file: 'download.jpg')}" alt="TAC Energy"/></div>

            <div class="grid_5">
                <p>Go the the <a href="${createLink(uri: '/downloads.gsp')}" title="Downloads">downloads page</a> and get the source code of the latest TAC Energy Demo Agent.</p>
                <p><strong>Note:</strong> You also need to have a current version of the <a href="http://java.sun.com/javase/downloads/widget/jdk6.jsp"
                        target="_blank">Sun Java Development Kit</a> (> 1.6) and the <a href="http://grails.org/Download"
                        target="_blank">Grails Development Framework</a> (v1.3.2) installed on your machine.</p>
            </div>

        </div>

    </div> <!-- section -->

    <div class="clear"></div>

    <div class="section">

        <div class="section-header">
            2. Configure your agent settings
        </div>

        <div class="section-content tutorial">

            <div class="grid_6"><img src="${resource(dir: 'images/tutorial', file: 'Config.jpg')}" alt="TAC Energy"/></div>

            <div class="grid_5">
                <p>Open <tt>$INSTALL_DIR/grails-app/Config.groovy</tt> and scroll down to the bottom. Here you need enter your personal
                login credentials for the TAC Energy server. Depending on which server you use you might also have to adjust the
                TAC Energy server url. If you're unsure about this, just <g:link url="user-list.gsp">drop us a note</g:link>.</p>
            </div>

        </div>

    </div> <!-- section -->

    <div class="clear"></div>

    <div class="section">

        <div class="section-header">
            3. Implement your trading strategy
        </div>

        <div class="section-content tutorial">

            <div class="grid_6"><img src="${resource(dir: 'images/tutorial', file: 'AgentStrategyService.jpg')}" alt="TAC Energy"/></div>

            <div class="grid_5"><p>Now you're set and the fun part can begin. Have the super cool algo trading strategy in your mind? Give it a try
            and pitch it against others! Head over to <tt>$INSTALL_DIR/grails-app/services/AgentStrategyService</tt> and start
            coding. For your convenience we provide you with lots of event listeners that you might want to use to jump in
            on trading energy. Just give it a try.<br/>
                Any Questions? <g:link url="user-list.gsp">Discuss them with others</g:link> or try to find answers in the
                <a href="http://www.tacenergy.org/docs/latest/manual/index.html" target="_blank">reference documentation</a>.</p>
            </div>

        </div>

    </div> <!-- section -->

    <div class="clear"></div>

    <div class="section">

        <div class="section-header">
            4. Run your agent
        </div>

        <div class="section-content tutorial">

            <div class="grid_6"><img src="${resource(dir: 'images/tutorial', file: 'Run.jpg')}" alt="TAC Energy"/></div>

            <div class="grid_5"><p>Once you're finished simply type <tt>grails run-app</tt> in the command line of your install directory.
            The agent now starts up and waits for you at <a href="http://localhost:8080/tacenergydemo">http://localhost:8080/tacenergydemo</a>.<br/>
                Once you see the web interface, make sure that you send a ready notification to the TAC Energy Server by simply
                clicking the corresponding link on the start page. After the competition starts your agent will start trading
                automatically. Still, if you want you can submit orders manually too. Just click the corresponding link.<br/>
                <strong>And now have fun...!</strong></p>
            </div>

        </div>

    </div> <!-- section -->

    <div class="clear"></div>

</div> <!-- content-box -->

</body>
</html>
