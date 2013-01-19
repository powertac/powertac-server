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
			backgroundColor: null

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
