		$(document).ready(function() {
			setTimeout("animation()",300);
		});
		
		function animation(){
			
//			enableBrokers();
			
			//duration: 10000 ms, starts at 1000 ms
//			setTimeout("moveTarrifs()",1000);
			//duration: 15000 ms, starts at 11000 ms
//			setTimeout("moveSignupCustomers()",11000);
			//duration: 15000 ms, starts at 26000 ms
//			setTimeout("moveWithdrawCustomers()",26000);
			//duration: 15000 ms, starts at 41000 ms
//			setTimeout("moveBalancingTransactions()",41000);
			//duration: 15000 ms, starts at 56000 ms
//			setTimeout("moveCashBalances()",56000);
						
		}
			
		
		function enableBrokers(){
			enableBroker("#broker1");
			enableBroker("#broker2");
			enableBroker("#broker3");
			enableBroker("#broker4");
			enableBroker("#broker5");
			enableBroker("#broker6");
			enableBroker("#broker7");
			enableBroker("#broker8");
			enableBroker("#broker9");
		}
		
		function moveTarrifs(){
			moveNewTarrif("#broker1","#tariffMarket","#newTariff1");
			moveNewTarrif("#broker2","#tariffMarket","#newTariff2");
			moveNewTarrif("#broker3","#tariffMarket","#newTariff3");
		}
		
		function moveSignupCustomers(){
			moveSignupCustomer("#customers","#tariffMarket","#broker5","#signupCustomers5");
			moveSignupCustomer("#customers","#tariffMarket","#broker6","#signupCustomers6");
			moveSignupCustomer("#customers","#tariffMarket","#broker7","#signupCustomers7");
		}
		
		function moveWithdrawCustomers(){
			moveWithdrawCustomer("#broker5","#tariffMarket","#customers","#withdrawCustomers5");
			moveWithdrawCustomer("#broker6","#tariffMarket","#customers","#withdrawCustomers6");
			moveWithdrawCustomer("#broker7","#tariffMarket","#customers","#withdrawCustomers7");
		}
		
		function moveCashBalances(){
			moveCashBalance("#accountingService","#broker1","#cashBalance1");
			moveCashBalance("#accountingService","#broker2","#cashBalance2");
			moveCashBalance("#accountingService","#broker3","#cashBalance3");
			moveCashBalance("#accountingService","#broker4","#cashBalance4");
			moveCashBalance("#accountingService","#broker5","#cashBalance5");
			moveCashBalance("#accountingService","#broker6","#cashBalance6");
			moveCashBalance("#accountingService","#broker7","#cashBalance7");
			moveCashBalance("#accountingService","#broker8","#cashBalance8");
			moveCashBalance("#accountingService","#broker9","#cashBalance9");
		}
		
		function moveBalancingTransactions(){
			moveBalancingTransaction("#distributionUtility","#broker1","#balancingTransaction1");
			moveBalancingTransaction("#distributionUtility","#broker2","#balancingTransaction2");
			moveBalancingTransaction("#distributionUtility","#broker3","#balancingTransaction3");
			moveBalancingTransaction("#distributionUtility","#broker4","#balancingTransaction4");
			moveBalancingTransaction("#distributionUtility","#broker5","#balancingTransaction5");
			moveBalancingTransaction("#distributionUtility","#broker6","#balancingTransaction6");
			moveBalancingTransaction("#distributionUtility","#broker7","#balancingTransaction7");
			moveBalancingTransaction("#distributionUtility","#broker8","#balancingTransaction8");
			moveBalancingTransaction("#distributionUtility","#broker9","#balancingTransaction9");
		}
		/*sets broker's visibility to true, calculates and sets broker's left position */
		function enableBroker(broker,iteration){
			$(broker).css("visibility", "visible");
			var position=420;
			var offset=105;
			//if brokerIteration is even use - , else use +
			var offsetTimes;
			if(iteration%2==0){
				offsetTimes = Math.floor(iteration/2);
				position-=offset*offsetTimes;
			} else {
				var result = (iteration-1)/2;
				offsetTimes = Math.floor(result);
				position+=offset*offsetTimes;
			}
			$(broker).animate({left:position+"px"},0)
		}
		
		/*from broker to tariffMarket*/
		function moveNewTarrif(broker,tariffMarket,tariff){
			//become visible:
			$(tariff).css("visibility", "visible");
			//motion:			
			changePosition(tariff,broker); //from
			var to = $(tariffMarket).position(); //to			
			$(tariff).animate({top:to.top},6000).animate(to,2000).animate(to,2000,
			//callback: reset moved object's position and make it invisible:
			function(){
			changePosition(tariff,broker);
			$(tariff).css("visibility", "hidden");
			});
		}
				
		/*from customers to tariffMarket to broker*/
		function moveSignupCustomer(customers,tariffMarket,broker,signupCustomers){
			//become visible:
			$(signupCustomers).css("visibility", "visible");
			//motion:			
			var to = $(broker).position();
			var proxy = $(tariffMarket).position();
			changePosition(signupCustomers,customers);
			$(signupCustomers).animate(proxy,3000).animate({left:to.left},3000).animate(to,7000).animate(to,2000,
			//callback: reset moved object's position and make it invisible:
			function(){
			changePosition(signupCustomers,customers);
			$(signupCustomers).css("visibility", "hidden");
			});
					
		}
		
		/*from broker to tariffMarket to customers*/
		function moveWithdrawCustomer(broker,tariffMarket,customers,withdrawCustomers){
			//become visible:
			$(withdrawCustomers).css("visibility", "visible");
			//motion:			
			var to = $(customers).position();
			var proxy = $(tariffMarket).position();
			changePosition(withdrawCustomers,broker);
			$(withdrawCustomers).animate({top:proxy.top},6000).animate(proxy,2000).animate(to,5000).animate(to,2000,
			//callback: reset moved object's position and make it invisible:
			function(){
			changePosition(withdrawCustomers,broker);
			$(withdrawCustomers).css("visibility", "hidden");
			});
					
		}
		
		/*from accountingService to broker*/
		function moveCashBalance(accountingService,broker,cashBalance){
			//become visible:
			$(cashBalance).css("visibility", "visible");
			//motion:			
			var to = $(broker).position();	
			changePosition(cashBalance,accountingService);
			$(cashBalance).animate({top:"+=115px"},1500).animate({left:to.left},1500).animate(to,10000).animate(to,2000,
			//callback: reset moved object's position and make it invisible:
			function(){
			changePosition(cashBalance,accountingService);
			$(cashBalance).css("visibility", "hidden");
			});
		}
		
		/*from distributionUtility to broker*/
		function moveBalancingTransaction(distributionUtility,broker,balancingTransaction){
			//become visible:
			$(balancingTransaction).css("visibility", "visible");
			//motion:			
			var to = $(broker).position();	
			changePosition(balancingTransaction,distributionUtility);
			$(balancingTransaction).animate({top:"+=115px"},1500).animate({left:to.left},1500).animate(to,10000).animate(to,2000,
			//callback: reset moved object's position and make it invisible:
			function(){
			changePosition(balancingTransaction,distributionUtility);
			$(balancingTransaction).css("visibility", "hidden");
			});
		}
		
		function changePosition(target,positionElement){
			var position = $(positionElement).position();
			$(target).css(position);
		}
		
	
