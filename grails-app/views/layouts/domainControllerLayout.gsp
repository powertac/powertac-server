<%--
  Created by IntelliJ IDEA.
  User: cblock
  Date: 06.01.11
  Time: 19:09
  To change this template use File | Settings | File Templates.
--%>

<div class="link-box">
  <ul>
    <li>Historic Data</li>
    <li><g:link controller="competition" action="list" class="competitions" title="Competitions">Competitions</g:link></li>
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
