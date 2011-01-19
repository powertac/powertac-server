<%--
  Sidebar navigation rendering
  User: cblock
  Date: 06.01.11
  Time: 19:09
--%>

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
    <li><g:link controller="info" action="gettingstarted">Getting Started</g:link></li>
    <li><g:link controller="info" action="downloads">Downloads</g:link></li>
    <li><g:link controller="info" action="getinvolved">Get involved</g:link></li>
  </ul>
</div>

%{--<g:ifAllGranted role="ROLE_ADMIN">--}%
<div class="link-box">
  <ul>
    <li>Admin Area</li>

    %{--<li><g:link controller="person" class="agents" title="Agents">Agents</g:link></li>
<li><g:link controller="role" class="roles" title="Agents">Agent Roles</g:link></li>
<li><g:link controller="announcement" class="announcements" title="Agents">Public Announcements</g:link></li>--}%
    <li><g:link controller="runtimeLogging" class="logging" title="Agents">Adjust Logging</g:link></li>
  </ul>
</div>
%{--</g:ifAllGranted>--}%

<div class="link-box">
  <ul>
    <li>Historic Data</li>
    <li><g:link controller="competition" action="list" class="competitions" title="Competitions">Competitions</g:link></li>
    <li><g:link controller="broker" action="list" class="brokers" title="Brokers">Brokers</g:link></li>
    <li><g:link controller="transactionLog" action="list" class="quotesntrades" title="Transaction Log">Transaction Log</g:link></li>
    <li><g:link controller="shout" action="list" class="orders" title="Shouts">Shouts</g:link></li>
    <li><g:link controller="orderbook" action="list" class="orderbooks" title="Orderbooks">Orderbooks</g:link></li>
    <li><g:link controller="product" action="list" class="products" title="Products">Products</g:link></li>
    <li><g:link controller="timeslot" action="list" class="products" title="Products">Timeslots</g:link></li>
    <li><g:link controller="cashUpdate" action="list" class="cashaccounts" title="Cash Updates ">Cash Updated</g:link></li>
    <li><g:link controller="positionUpdate" action="list" class="userdepots" title="Position Updates">Position Updates</g:link></li>
    <li><g:link controller="weather" action="list" class="userforecasts" title="Weather data">Weather data</g:link></li>
  </ul>
</div>
