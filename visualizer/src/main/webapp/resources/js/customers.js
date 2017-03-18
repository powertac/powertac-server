function makeCashChart(){
	
	$(document).ready(function() {

		Highcharts.setOptions({
			global : {
				useUTC : true
			},
			colors: ['#356AA0','#D01F3C','#008C00']
		});
		
		// Create the chart
		currentCashChart = new Highcharts.StockChart({
			chart : {
				renderTo : currentCashChartString,
							
			},
			yAxis: {
				title: {
					text: 'Cash (EUR)'
				},
				plotLines: [{
					value: 0,
					width: 1,
					color: '#808080'
				}]},
			
			rangeSelector: {
				buttons: [{
					count: 1,
					type: 'hour',
					text: '1H'
				}, {
					count: 1,
					type: 'day',
					text: '1D'
				},{
					count: 1,
					type: 'week',
					text: '1W'
				},
				{
					count: 2,
					type: 'week',
					text: '2W'
				},
				{
					count: 1,
					type: 'month',
					text: '1M'
				},
				{
					type: 'all',
					text: 'All'
				}],
				inputEnabled: true,
				selected: 5
			},
			
			title : {
				text : 'Cash'
			},
			
			exporting: {
				enabled: true
			},
			tooltip: {
//	            formatter: function() {
//	                var s = '<b>'+ Highcharts.dateFormat('%H:00, %A, %b %e, %Y', this.x) +'</b>';
//
//	                $.each(this.points, function(i, point) {
//	                    s += '<br/>'+this.series.name+': '+ Highcharts.numberFormat(point.y, 2) +' EUR';
//	                });
//	            
//	                return s;
				yDecimals : 2,
				ySuffix : " EUR"
				
	  //          }
	            
	        },legend: {
	            enabled: true,
	            align: 'right',
	            backgroundColor: '#FCFFC5',
	            borderColor: 'black',
	            borderWidth: 2,
	            layout: 'vertical',
	            verticalAlign: 'top',
	            y: 100,
	            shadow: true
	        },
			
			
			
			series : [{
				name : 'Diff',
				data : (function() {
					// generate an array of random data
					var data = totalChargeLineChartData;

					return data;
				})()
			}, {
				name : 'Outflow',
				data : (function() {
					// generate an array of random data
					var data = outflowChargeLineChartData;

					return data;
				})()
			}, {
				name : 'Inflow',
				data : (function() {
					// generate an array of random data
					var data = inflowChargeLineChartData;

					return data;
				})()
			}],
			
		});   
	
	});}

function makeEnergyChart(){
	
	$(document).ready(function() {

		Highcharts.setOptions({
			global : {
				useUTC : true
			},
			colors: ['#356AA0','#D01F3C','#008C00']
		}); 
		
		// Create the chart
		currentEnergyChart = new Highcharts.StockChart({
			chart : {
				renderTo : currentEnergyChartString,
							
			},
			yAxis: {
				title: {
					text: 'Energy (kWh)'
				},
				plotLines: [{
					value: 0,
					width: 1,
					color: '#808080'
				}]},
			
			rangeSelector: {
				buttons: [{
					count: 1,
					type: 'hour',
					text: '1H'
				}, {
					count: 1,
					type: 'day',
					text: '1D'
				},{
					count: 1,
					type: 'week',
					text: '1W'
				},
				{
					count: 2,
					type: 'week',
					text: '2W'
				},
				{
					count: 1,
					type: 'month',
					text: '1M'
				},
				{
					type: 'all',
					text: 'All'
				}],
				inputEnabled: true,
				selected: 5
			},
			
			title : {
				text : 'Energy'
			},
			
			exporting: {
				enabled: true
			},
			tooltip: {
	            formatter: function() {
	                var s = '<b>'+ Highcharts.dateFormat('%H:00, %A, %b %e, %Y', this.x) +'</b>';

	                $.each(this.points, function(i, point) {
	                    s += '<br/>'+this.series.name+': '+ Highcharts.numberFormat(point.y, 2) +' kWh';
	                });
	            
	                return s;
	            }
	            
	        },legend: {
	            enabled: true,
	            align: 'right',
	            backgroundColor: '#FCFFC5',
	            borderColor: 'black',
	            borderWidth: 2,
	            layout: 'vertical',
	            verticalAlign: 'top',
	            y: 100,
	            shadow: true
	        },
			
			
			
			series : [{
				name : 'Energy',
				data : (function() {
			
					var data = totalkWhLineChartData;

					return data;
				})()
			}]
			
		});   
	
	});}


function makeBootstrapEnergyChart(){
	
	$(document).ready(function() {

		Highcharts.setOptions({
			global : {
				useUTC : true
			},
			colors: ['#356AA0','#D01F3C','#008C00']
		}); 
		
		// Create the chart
		currentBootstrapEnergyChart = new Highcharts.StockChart({
			chart : {
				renderTo : currentBootstrapEnergyChartString
							
			},
			yAxis: {
				title: {
					text: 'Energy (kWh)'
				},
				plotLines: [{
					value: 0,
					width: 1,
					color: '#808080'
				}]},
			
			rangeSelector: {
				buttons: [{
					count: 1,
					type: 'hour',
					text: '1H'
				}, {
					count: 1,
					type: 'day',
					text: '1D'
				},{
					count: 1,
					type: 'week',
					text: '1W'
				},
				{
					count: 2,
					type: 'week',
					text: '2W'
				},
				{
					count: 1,
					type: 'month',
					text: '1M'
				},
				{
					type: 'all',
					text: 'All'
				}],
				inputEnabled: true,
				selected: 5
			},
			
			title : {
				text : 'Energy'
			},
			
			exporting: {
				enabled: true
			},
			tooltip: {
	            formatter: function() {
	                var s = '<b>'+ Highcharts.dateFormat('%H:00, %A, %b %e, %Y', this.x) +'</b>';

	                $.each(this.points, function(i, point) {
	                    s += '<br/>'+this.series.name+': '+ Highcharts.numberFormat(point.y, 2) +' kWh';
	                });
	            
	                return s;
	            }
	            
	        },legend: {
	            enabled: true,
	            align: 'right',
	            backgroundColor: '#FCFFC5',
	            borderColor: 'black',
	            borderWidth: 2,
	            layout: 'vertical',
	            verticalAlign: 'top',
	            y: 100,
	            shadow: true
	        },
			
			
			
			series : [{
				name : 'Energy',
				data : (function() {
			
					var data = bootstrapEnergyData;

					return data;
				})()
			}]
			
		});   
	
	});}

