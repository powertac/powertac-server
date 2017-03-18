function getOneDynYAxisData(firstTitle) {
	return [ {
		title : {
			text : firstTitle
		},
		lineWidth : 2
	} ];
}

function shown(dataGraph) {

	var chart = new Highcharts.Chart({

		chart : {
			renderTo : 'chart'
		},

		yAxis : {
			title : {
				text : 'Price (€cent/kWh)'
			}
		},

		legend : {
			enabled : false
		},

		tooltip : {
			enabled : false
		},

		title : {
			text : 'Rates'
		},

		xAxis : {
			type : 'datetime',
			minTickInterval : 3600 * 1000,
			labels : {
				formatter : function() {
					var d = new Date(this.value);
					if (d.getFullYear() == 2007)
						return Highcharts.dateFormat('%a, %Hh', this.value);
					else
						return Highcharts.dateFormat('%Hh', this.value);
				}
			}

		},
		series : dataGraph

	});
}

function checkRange(x, n, m) {
	if (x >= n && x <= m) {
		return true;
	} else {
		return false;
	}
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
function getTariffDynYAxisData(title, firstBaseTitle, secondBaseTitle) {
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

function dynDataGraph(renderDiv, seriesData, titleData, yAxisData, appending) {

	return new Highcharts.StockChart(
			{
				chart : {
					renderTo : renderDiv,
					alignTicks : false,
					backgroundColor : null,
					marginRight : 130,
					marginBottom : 70

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
				// tooltip : {
				// yDecimals : 2
				//
				// },
				tooltip : {
					formatter : function() {
						// var appending = new Array("PRICE", "ENER", "CUST");
						var broker = this.points[0].series.name;
						var increment = 1;
						// console.log(this);
						if (this.points[increment] !== undefined) {

							while (this.points[increment] !== undefined
									&& this.points[increment].series.name == broker)

								increment++;
						}
						
						var builder = "";

						for ( var i = 0; i < this.points.length; i++) {
							builder += '<span style="color:'
									+ this.points[i].series.color
									+ '">'
									+ this.points[i].series.name
									+ ' '
									+ appending[i % increment]
									+ ': '
									+ '</span><b>'
									+ Highcharts.numberFormat(this.points[i].y,
											2) + '</b><br />';
						}
						builder += Highcharts.dateFormat('%A, %b %e, %Hh',
								this.x);
						return builder;
					}
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

			},
			function(chart) {
				var broker = chart.series[0].name;
				var increment = 1;
				if (chart.series[increment] !== undefined)
					while (chart.series[increment].name == broker)
						increment++;
				
				for ( var index = 0; index < chart.series.length - 2; index += increment)
					$(chart.series[index]).each(function(i, e) {
						e.legendItem.on('click', function(event) {
							var legendItem = e.name;
							event.stopPropagation();
							$(chart.series).each(function(j, f) {
								$(this).each(function(k, z) {
									if (z.name == legendItem) {
										if (z.visible) {
											z.setVisible(false);
										} else {
											z.setVisible(true);
										}
									}
								});
							});

						});
					});
			});

}

function wholesaleClearingEnergy(renderDiv, receivedData, titleData, yAxisData) {

	var data = receivedData[0]["data"];
	// split the data set into revenue and amount of energy
	var revenue = [];
	var energy = [];
	var dataLength = data.length;
	
	for ( var i = 0; i < dataLength; i++) {

		revenue.push([ data[i][0], // timeslot
		data[i][1] // revenue

		]);

		energy.push([ data[i][0], // timeslot
		data[i][2] // energy
		]);

	}

	return new Highcharts.StockChart({

		chart : {
			renderTo : renderDiv,
			alignTicks : false,
			backgroundColor : null,
			marginRight : 130,
			marginBottom : 70
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
			text : ''
		},

		yAxis : yAxisData,

		series : [ {
			name : 'Average clearing price(€)',
			// marker : {enabled : true,radius : 3 },
			dataGrouping : {
				enabled : false
			},
			data : revenue,
			tooltip : {
				valueDecimals : 2
			}
		}, {
			name : 'Total energy(MWh)',
			type : 'column',
			data : energy,
			yAxis : 1,
			dataGrouping : {
				enabled : false
			},
			color : '#8BBC21',
			tooltip : {
				valueDecimals : 2
			}
		}, ]
	});
}

function scatterMarketTxs(targetDiv, title, subtitle, xAxisTitle, yAxisTitle,
		xMeasureUnit, yMeasureUnit, seriesData) {
	return new Highcharts.Chart({
		chart : {
			renderTo : targetDiv,
			type : 'scatter',
			zoomType : 'xy',
			backgroundColor : null
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
				var x = Math.round(this.x * 100) / 100;
				var y = Math.round(this.y * 100) / 100;
				return '' + x + " " + xMeasureUnit + " " + y + " "
						+ yMeasureUnit;
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

function weatherReportGraph(targetDiv, title, temperatureData, windSpeedData,
		windDirectionData, cloudCoverData) {

	return new Highcharts.StockChart({
		chart : {
			renderTo : targetDiv,
			alignTicks : false

		},
		plotLines : [ {
			value : 0,
			width : 1,
			color : '#808080'
		} ],
		yAxis : [ {
			title : {
				text : 'Temperature(°C)'
			},
			height : 140,
			lineWidth : 2
		}, {
			title : {
				text : 'Wind speed (m/s)'
			},
			top : 230,
			height : 150,
			offset : 0,
			lineWidth : 2,
		// opposite: true
		}, {
			title : {
				text : 'Wind direction (°)'
			},
			top : 390,
			height : 150,
			offset : 0,
			lineWidth : 2,
		// opposite: true
		}, {
			title : {
				text : 'Cloud cover'
			},
			top : 550,
			height : 150,
			offset : 0,
			lineWidth : 2,
		// opposite: true
		} ],

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
			text : title
		},

		exporting : {
			enabled : true
		},
		tooltip : {
			yDecimals : 2
		},

		series : [ {
			name : 'Temperature(C°)',
			data : temperatureData
		}, {
			name : 'Wind speed (m/s)',
			data : windSpeedData,
			yAxis : 1

		}, {
			name : 'Wind direction (°)',
			data : windDirectionData,
			yAxis : 2,
			step : true

		}, {
			name : 'Cloud cover',
			data : cloudCoverData,
			yAxis : 3,
			step : true

		} ]

	});

};

function customerStatisticsPieChart(graphData) {

	var colors = Highcharts.getOptions().colors, categories = [], data = graphData;

	function setChart(name, categories, data, color) {
		customerStatistics.xAxis[0].setCategories(categories);
		customerStatistics.series[0].remove();
		customerStatistics.addSeries({
			name : name,
			data : data,
			color : color || 'white'
		});
	}

	return new Highcharts.Chart(
			{
				chart : {
					renderTo : 'customerStatistics',
					type : 'pie'
				},
				title : {
					text : ''
				},
				xAxis : {
					categories : categories
				},
				yAxis : {
					title : {
						text : ''
					}
				},
				plotOptions : {
					pie : {
						cursor : 'pointer',
						point : {
							events : {
								click : function() {
									var drilldown = this.drilldown;
									if (drilldown) { // drill down
										setChart(drilldown.name,
												drilldown.categories,
												drilldown.data, drilldown.color);
									} else { // restore
										setChart(name, categories, data);
									}
								}
							}
						},
						dataLabels : {
							enabled : true,
							color : colors[0],
							style : {
								fontWeight : 'bold'
							},
							formatter : function() {
								return this.point.name + '<br>' + this.y
										+ ' customers';
							}
						}
					}
				},
				tooltip : {
					formatter : function() {
						var point = this.point, s = '<b>' + this.point.name
								+ '</b><br>';
						if (point.drilldown) {
							s += 'Click for detail view';
						} else {
							s += 'Click to return';
						}
						return s;
					}
				},
				series : [ {
					name : name,
					data : data,
					color : 'white'
				} ],
				exporting : {
					enabled : false
				}
			});
}

function transactionsSummary(seriesData) {
	return new Highcharts.Chart(
			{

				chart : {
					renderTo : 'transactionsSummary',
					polar : true,
					type : 'line'
				},

				title : {
					text : '',
					x : -80
				},

				pane : {
					size : '80%'
				},

				xAxis : {
					categories : [ 'Tariff', 'Wholesale', 'Balancing',
							'Distribution' ],
					tickmarkPlacement : 'on',
					lineWidth : 0
				},

				yAxis : {
					gridLineInterpolation : 'polygon',
					lineWidth : 0
				},

				tooltip : {
					shared : true,
					formatter : function() {
						builder = "";
						builder += this.x + ' grade includes following KPIs:<br />';
						if (this.x == 'Wholesale') {
							builder += "<li>*Broker's price compared to overall average price for buying energy<br/></li>";
							builder += "<li>*Broker's price compared to overall average price for selling energy<br/></li>";
						}
						if (this.x == 'Tariff') {
							builder += "<li>*Broker's share in overall money flow<br/></li>";
							builder += "<li>*Broker's share in overall sold energy<br/></li>";
							builder += "<li>*Broker's share in overall bought energy<br/></li>";
						}
						if (this.x == 'Balancing') {
							builder += "<li>*Ratio of imbalance to total energy delivered<br/></li>";
							builder += "<li>*Imbalance fee<br/></li>";
						}
						if (this.x == 'Distribution') {
							builder += "<li>*Broker's share in overall energy distribution<br/></li>";
						}
						
						
						for ( var i = 0; i < this.points.length; i++) {
							builder += '<span style="color:'
									+ this.points[i].series.color
									+ '">'
									+ this.points[i].series.name
									+ ': '
									+ '</span><b>'
									+ Highcharts.numberFormat(this.points[i].y,
											0) + '</b><br />';
						}
						return builder
					}

				},

				legend : {
					align : 'right',
					verticalAlign : 'top',
					y : 40,
					layout : 'vertical',
					floating : true
				},

				series : seriesData
			/*
			 * [{ name: 'Broker 1', data: [1000000,900000, 111000, 12000,
			 * -3000], pointPlacement: 'on' }, { name: 'Broker2', data:
			 * [1400000, 1430000, 137000, 386000, 239000], pointPlacement: 'on' }]
			 */
			});

}

function customerModelsChart(renderDiv, seriesData, titleData, yAxisData,
		appending) {

	return new Highcharts.StockChart({
		chart : {
			renderTo : renderDiv,
			alignTicks : false,
			backgroundColor : null,
			marginRight : 130,
			marginBottom : 70

		},

		plotLines : [ {
			value : 0,
			width : 1

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
		 formatter : function() {

		 var builder = "";
		 				
		 for ( var i = 0; i < this.points.length; i++) {
		 builder += '<span style="color:'
		 + this.points[i].series.color
		 + '">'
		 + this.points[i].series.name
		 + ': '
		 + '</span><b>'
		 + Highcharts.numberFormat(this.points[i].y,
		 2) + '</b><br />';
		 }
		 builder += Highcharts.dateFormat('%A, %b %e, %Hh', this.x);
		 return builder;
		 }
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
