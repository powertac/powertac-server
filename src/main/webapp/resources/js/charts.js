function getOneDynYAxisData(firstTitle) {
	return [ {
		title : {
			text : firstTitle
		},
		lineWidth : 2
	}];
}
function checkRange(x, n, m) {
    if (x >= n && x <= m) { return true; }
    else { return false; }
};
function getBaseDynYAxisData(firstTitle, secondTitle) {
	return [ {
		title : {
			text : firstTitle
		},
		height : 140,
		lineWidth : 2
	}, {
		title : {
			text : secondTitle
		},
		top : 230,
		height : 150,
		offset : 0,
		lineWidth : 2,
	// opposite: true
	} ];
}
function getTariffDynYAxisData(title, firstBaseTitle,secondBaseTitle) {
	var baseDynYAxisData = getBaseDynYAxisData(firstBaseTitle, secondBaseTitle);
	var newPart = {
		title : {
			text : title
		},
		top : 390,
		height : 150,
		offset : 0,
		lineWidth : 2,
	// opposite: true
	};
	baseDynYAxisData.push(newPart);
	return baseDynYAxisData;
}

function dynDataGraph(renderDiv, seriesData, titleData, yAxisData) {
	return new Highcharts.StockChart({
		chart : {
			renderTo : renderDiv,
			alignTicks : false,
			backgroundColor: null,
			marginRight: 130,
			marginBottom: 70 

		},
		plotLines : [ {
			value : 0,
			width : 1,
			color : '#808080'
		} ],
		yAxis : yAxisData,

		rangeSelector : {
			buttons : [ {
				count : 1,
				type : 'hour',
				text : '1H'
			}, {
				count : 1,
				type : 'day',
				text : '1D'
			}, {
				count : 1,
				type : 'week',
				text : '1W'
			}, {
				count : 2,
				type : 'week',
				text : '2W'
			}, {
				count : 1,
				type : 'month',
				text : '1M'
			}, {
				type : 'all',
				text : 'All'
			} ],
			inputEnabled : true,
			selected : 5
		},

		title : {
			text : titleData
		},

		exporting : {
			enabled : true
		},
		tooltip : {
			yDecimals : 2
			
		},
		legend : {
			align : "right",
			layout : "vertical",
			enabled : true,
			verticalAlign : "middle"
			
		/*
		 * labelFormatter: function() { return this.name + ' (T)'; }
		 */

		},

		series : seriesData

	});

}


function wholesaleClearingEnergy(renderDiv, receivedData, titleData, yAxisData) {
	
	var data = receivedData[0]["data"];
	// split the data set into revenue and amount of energy
	var revenue = [];
	var	energy = [];
	var	dataLength = data.length;
		
	for (var i = 0; i < dataLength; i++) {
		
		revenue.push([
		    data[i][0], //timeslot
			data[i][1]  //revenue

		]);
		
		energy.push([
			data[i][0], //timeslot
			data[i][2]  //energy
		]);
		
	}

	return new Highcharts.StockChart({
		
		chart : {
			renderTo : renderDiv,
			alignTicks : false,
			backgroundColor: null,
			marginRight: 130,
			marginBottom: 70 
		},
		
		plotLines : [ {
			value : 0,
			width : 1,
			color : '#808080'
		} ],
		
		yAxis : yAxisData,

		rangeSelector : {
			buttons : [ {
				count : 1,
				type : 'hour',
				text : '1H'
			}, {
				count : 1,
				type : 'day',
				text : '1D'
			}, {
				count : 1,
				type : 'week',
				text : '1W'
			}, {
				count : 2,
				type : 'week',
				text : '2W'
			}, {
				count : 1,
				type : 'month',
				text : '1M'
			}, {
				type : 'all',
				text : 'All'
			} ],
			inputEnabled : true,
			selected : 5
		},

	    title: {
	        text: ''
	    },

	    yAxis: yAxisData,
	    
	    series: [
	    {
	        name: 'Average clearing price',
			//marker : {enabled : true,radius : 3	},
			dataGrouping: {enabled: false},
			data: revenue,
			tooltip: {
				valueDecimals: 2
			}
	    }, 
	    {
	    	name: 'Total energy',
	        type: 'column',
	        data: energy,
	        yAxis: 1,
	        dataGrouping: {enabled: false},
	        color: '#8BBC21',
	        tooltip: {
				valueDecimals: 2
			}
	    },
	    ]
	});
}




function scatterMarketTxs(targetDiv, title, subtitle, xAxisTitle,yAxisTitle,xMeasureUnit,yMeasureUnit,seriesData) {
	return new Highcharts.Chart({
		chart : {
			renderTo : targetDiv,
			type : 'scatter',
			zoomType : 'xy',
			backgroundColor: null
		},
		title : {
			text : title
		},
		subtitle : {
			text : subtitle
		},
		xAxis : {
			title : {
				enabled : true,
				text : xAxisTitle
			},
			startOnTick : true,
			endOnTick : true,
			showLastLabel : true
		},
		yAxis : {
			title : {
				text : yAxisTitle
			}
		},
		tooltip : {
			formatter : function() {
				var x=Math.round(this.x*100)/100;
				var y=Math.round(this.y*100)/100;
				return '' + x + " "+xMeasureUnit + " "+ y +" "+ yMeasureUnit;
			}
		},
		legend : {
			layout : 'vertical',
			align : 'left',
			verticalAlign : 'top',
			x : 100,
			y : 70,
			floating : true,
			backgroundColor : '#FFFFFF',
			borderWidth : 1
		},
		plotOptions : {
			scatter : {
				marker : {
					radius : 5,
					states : {
						hover : {
							enabled : true,
							lineColor : 'rgb(100,100,100)'
						}
					}
				},
				states : {
					hover : {
						marker : {
							enabled : false
						}
					}
				}
			}
		},
		series : seriesData
							
	});
}

function weatherReportGraph(targetDiv,title, temperatureData, windSpeedData, windDirectionData, cloudCoverData){
	
	return new Highcharts.StockChart({
			chart : {
				renderTo : targetDiv,
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
		    },{
		        title: {
		            text: 'Wind direction (°)'
		        },
		        top: 390,
		        height: 150,
		        offset: 0,
		        lineWidth: 2,
		       // opposite: true
		    },
		    {
		        title: {
		            text: 'Cloud cover'
		        },
		        top: 550,
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
				text : title
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
		       
		    },{
		        name: 'Wind direction (°)',
		        data: windDirectionData,
		        yAxis: 2,
		        step: true
		       
		    },
		    {
		        name: 'Cloud cover',
		        data: cloudCoverData,
		        yAxis: 3,
		        step: true
		       
		    }
		    ]
			
		});   
	
};
