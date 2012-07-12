function makeGlobalWeatherChart(){
	
	$(document).ready(function() {

		Highcharts.setOptions({
			global : {
				useUTC : true
			}
		//,
			//colors: ['#356AA0','#D01F3C','#008C00']
		});
		
		// Create the chart
		globalWeather = new Highcharts.StockChart({
			chart : {
				renderTo : globalWeatherString,
				alignTicks: false
							
			},
			plotLines: [{
				value: 0,
				width: 1,
				color: '#808080'
			}],
			yAxis: [{
		        title: {
		            text: 'Temperature(°C)'
		        },
		        height: 140,
		        lineWidth: 2
		    }, {
		        title: {
		            text: 'Wind speed (m/s)'
		        },
		        top: 230,
		        height: 150,
		        offset: 0,
		        lineWidth: 2,
		       // opposite: true
		    },
		    {
		        title: {
		            text: 'Cloud cover'
		        },
		        top: 390,
		        height: 150,
		        offset: 0,
		        lineWidth: 2,
		     //   opposite: true
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
				text : 'Weather information'
			},
			
			exporting: {
				enabled: true
			},
			tooltip: {
				yDecimals : 2
			},
			
			series : [{
		        name: 'Temperature(C°)',
		        data: temperatureData
		    }, {
		        name: 'Wind speed (m/s)',
		        data: windSpeedData,
		        yAxis: 1
		       
		    }, {
		        name: 'Cloud cover',
		        data: cloudCoverData,
		        yAxis: 2
		       
		    }
		    ]
			
		});   
	
	});
	
};