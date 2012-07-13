function makeGlobalWholesaleLastChart(){
	
	$(document).ready(function() {

		Highcharts.setOptions({
			global : {
				useUTC : true
			}
		//,
			//colors: ['#356AA0','#D01F3C','#008C00']
		});
		
		// Create the chart
		globalWholesaleLastChart = new Highcharts.StockChart({
			chart : {
				renderTo : globalWholesaleLastChartString,
				alignTicks: false
							
			},
			plotLines: [{
				value: 0,
				width: 1,
				color: '#808080'
			}],
			yAxis: [{
		        title: {
		            text: 'Clearing price (EUR)'
		        },
		        height: 200,
		        lineWidth: 2
		    }, {
		        title: {
		            text: 'Cleared trade (MWh)'
		        },
		        top: 300,
		        height: 100,
		        offset: 0,
		        lineWidth: 2
		    }],
			
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
				text : 'Most recent clearings for each timeslot'
			},
			
			exporting: {
				enabled: true
			},
			tooltip: {
				yDecimals : 2
			},
//			tooltip: {
//	            formatter: function() {
//	                var s = '<b>'+ Highcharts.dateFormat('%H:00, %A, %b %e, %Y', this.x) +'</b>';
//
//	                $.each(this.points, function(i, point) {
//	                    s += '<br/>'+this.series.name+': '+ Highcharts.numberFormat(point.y, 2) +' EUR';
//	                });
//	            
//	                return s;
//	            },
//	            
//	        },
//			legend: {
//	            enabled: true,
//	            align: 'right',
//	            backgroundColor: '#FCFFC5',
//	            borderColor: 'black',
//	            borderWidth: 2,
//	            layout: 'vertical',
//	            verticalAlign: 'top',
//	            y: 100,
//	            shadow: true
//	        },
			
			
			
			series : [{
		        name: 'Clearing price (EUR)',
		        data: globalLastClearingPrices
		    }, {
		        type: 'column',
		        name: 'Cleared trade (MWh)',
		        data: globalLastClearingVolumes,
		        yAxis: 1
		       
		    }]
			
		});   
	
	});
	
};

function makeSpecificWholesaleChart(){
	
	$(document).ready(function() {

		Highcharts.setOptions({
			global : {
				useUTC : true
			}
		//,
			//colors: ['#356AA0','#D01F3C','#008C00']
		});
		
		// Create the chart
		specificWholesaleChart = new Highcharts.StockChart({
			chart : {
				renderTo : specificWholesaleChartString,
				alignTicks: false
							
			},
			plotLines: [{
				value: 0,
				width: 1,
				color: '#808080'
			}],
			yAxis: [{
		        title: {
		            text: 'Clearing price (EUR)'
		        },
		        height: 200,
		        lineWidth: 2
		    }, {
		        title: {
		            text: 'Cleared trade (MWh)'
		        },
		        top: 300,
		        height: 100,
		        offset: 0,
		        lineWidth: 2
		    }],
			
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
				text : 'Clearings for a timeslot'
			},
			
			exporting: {
				enabled: true
			},
			tooltip: {
				yDecimals : 2
			},
//			tooltip: {
//	            formatter: function() {
//	                var s = '<b>'+ Highcharts.dateFormat('%H:00, %A, %b %e, %Y', this.x) +'</b>';
//
//	                $.each(this.points, function(i, point) {
//	                    s += '<br/>'+this.series.name+': '+ Highcharts.numberFormat(point.y, 2) +' EUR';
//	                });
//	            
//	                return s;
//	            },
//	            
//	        },
//			legend: {
//	            enabled: true,
//	            align: 'right',
//	            backgroundColor: '#FCFFC5',
//	            borderColor: 'black',
//	            borderWidth: 2,
//	            layout: 'vertical',
//	            verticalAlign: 'top',
//	            y: 100,
//	            shadow: true
//	        },
			
			
			
			series : [{
		        name: 'Clearing price (EUR)',
		        data: specificWholesaleChartPrices
		    }, {
		        type: 'column',
		        name: 'Traded quantity (MWh)',
		        data: specificWholesaleChartVolumes,
		        yAxis: 1
		       
		    }]
			
		});   
	
	});
	
}