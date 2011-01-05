<%@ page import="org.codehaus.groovy.grails.commons.ConfigurationHolder" %>

<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US" lang="en-US">
	<head>
		<meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
		<title>TAC Energy - <g:layoutTitle default="Welcome"/></title>
		<link rel="stylesheet" type="text/css" media="all" href="${resource(dir: 'css', file: 'style.css')}" />
        <g:javascript library="jquery" />
        <jq:plugin name="shuffle" />
        <jq:plugin name="cycle" />
        <g:javascript library="tac" />
        <flot:resources includeJQueryLib="false" plugins="['threshold']"/>
        <g:layoutHead />
	</head>
	<body>
		<div class="container_16">

			<div id="header">

				<div id="logo" class="grid_4">
					<a href="${resource(dir:'')}" title="TAC Energy"></a>
				</div>

				<div id="nav">
					<ul class="grid_7 prefix_5">
						<li><a href="${createLink(uri: '/terms.gsp')}">Terms of Use</a></li>
						<li><a href="${createLink(uri: '/disclaimer.gsp')}">Disclaimer</a></li>
						<li><a href="http://www.tacenergy.org/docs/latest/manual/index.html" target="_blank">Documentation</a></li>
						<li><a href="${createLink(uri: '/contact.gsp')}">Contact</a></li>
					</ul>
				</div>

			</div> <!-- header -->

			<div class="clear"></div>

			<div id="wrapper">

				<div id="navigation" class="grid_4">

					<div class="user-box">
						<ul>
                            %{--<g:isLoggedIn>
                              <li>Logged in as <g:link controller="register" class="user"><g:loggedInUserInfo field="userRealName"></g:loggedInUserInfo></g:link>.</li>
                              <li><g:link controller="logout"><g:message code="auth.logout"/></g:link></li>
                            </g:isLoggedIn>

                            <g:isNotLoggedIn>--}%
                              <li><g:message code="auth.welcome.anonymous"/> <g:link controller="login" action="auth" class="user"><g:message code="auth.login"/></g:link></li>
                            %{--</g:isNotLoggedIn>--}%
						</ul>
					</div>

					<div class="link-box">
						<ul>
							<li>Quick Start</li>
							<li><a href="${createLink(uri: '/')}" class="dashboard" title="Dashboard">Dashboard</a></li>
                            <li><a href="${createLink(uri: '/gettingstarted.gsp')}" class="gettingstarted" title="Getting started">Getting started</a></li>
                            <li><g:link controller="competition" action="create" class="createcompetition" title="Create a competition">Create a competition</g:link></li>
							<li><a href="${createLink(uri: '/downloads.gsp')}" class="downloads" title="Downloads">Downloads</a></li>
                            <li><a href="${createLink(uri: '/getinvolved.gsp')}" class="getinvolved" title="Get involved">Get involved</a></li>
						</ul>
					</div>

                    %{--<g:ifAllGranted role="ROLE_ADMIN">--}%
                    <div class="link-box">
						<ul>
							<li>Admin Area</li>
                            <li><g:link controller="person" class="agents" title="Agents">Agents</g:link></li>
                            <li><g:link controller="role" class="roles" title="Agents">Agent Roles</g:link></li>
                            <li><g:link controller="announcement" class="announcements" title="Agents">Public Announcements</g:link></li>
                            <li><g:link controller="runtimeLogging" class="logging" title="Agents">Adjust Logging</g:link></li>
						</ul>
					</div>
                    %{--</g:ifAllGranted>--}%
                    
					<div class="link-box">
						<ul>
							<li>Historic Data</li>
							<li><g:link controller="competition" action="list" class="competitions" title="Competitions">Competitions</g:link></li>
							<li><g:link controller="transactionLog" action="list" class="quotesntrades" title="Competitions">Quotes and Trades</g:link></li>
							<li><g:link controller="shout" action="list" class="orders" title="Orders">Orders</g:link></li>
							<li><g:link controller="orderbook" action="list" class="orderbooks" title="Orderbooks">Orderbooks</g:link></li>
							<li><g:link controller="product" action="list" class="products" title="Products">Products</g:link></li>
							<li><g:link controller="cashPosition" action="list" class="cashaccounts" title="Cash Accounts">Cash Accounts</g:link></li>
							<li><g:link controller="depotPosition" action="list" class="userdepots" title="User Depots">User Depots</g:link></li>
							<li><g:link controller="forecast" action="list" class="userforecasts" title="User Forecasts">User Forecasts</g:link></li>
						</ul>
					</div>

                    <div id="universities">
                        <a href="http://www.kit.edu" target="_blank" title="Karlsruhe Institute of Technology (KIT)"><img src="${resource(dir: 'images/logos', file: 'kit_logo.png')}" alt="Karlsruhe Institute of Technology"/></a>
                        <a href="http://www.erim.eur.nl/ERIM/Research/Centres/Learning_Agents" target="_blank"><img src="${resource(dir: 'images/logos', file: 'rsm_logo.png')}" alt="Rotterdam School of Management (RSM)" title="Rotterdam School of Management (RSM)"/></a>
                        <a href="http://tac.cs.umn.edu/" target="_blank" title="University of Minnesota"><img src="${resource(dir: 'images/logos', file: 'umn_logo.png')}" alt="University of Minnesota"/></a>
                    </div>

				</div> <!-- navigation -->

				<div id="content" class="competitionview grid_12">

					<g:layoutBody/>

                    <div id="footer">
						<p>Copyright &copy; 2007-2011 Institute of Information Systems and Management (IISM), KIT in cooperation with RSM Erasmus University and
University of Minnesota.</p>
                        <p class="additionalInformation">System Information: URL: <a href="${ConfigurationHolder.config?.grails?.serverURL}">${ConfigurationHolder.config?.grails?.serverURL}</a> - Time: ${new Date()} - Version: <a href="https://launchpad.net/tacenergy" target="_blank"><g:meta name="app.version"/></a></p>
                        <p class="additionalInformation">Server URL inside KIT campus: <a href="${ConfigurationHolder.config?.tacenergy?.connector?.internal?.url}" target="_blank">${ConfigurationHolder.config?.tacenergy?.connector?.internal?.url}</a><br />
                          Server URL outside KIT campus (public): <a href="${ConfigurationHolder.config?.tacenergy?.connector?.external?.url}" target="_blank">${ConfigurationHolder.config?.tacenergy?.connector?.external?.url}</a></p>

				    </div> <!-- footer -->

                </div> <!-- content -->                                         

			</div> <!-- wrapper -->

		</div> <!-- container_16 -->

	</body>

</html>
