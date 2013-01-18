function getBaseDynYAxisData(){
	return [ {
		title : {
			text : 'Profit (euros)'
		},
		height : 140,
		lineWidth : 2
	}, {
		title : {
			text : 'Net energy (kWh)'
		},
		top : 230,
		height : 150,
		offset : 0,
		lineWidth : 2,
	// opposite: true
	}];
}
function getTariffDynYAxisData(){
	var baseDynYAxisData = getBaseDynYAxisData();
	var newPart= {
			title : {
				text : 'Customer count'
			},
			top : 390,
			height : 150,
			offset : 0,
			lineWidth : 2,
		// opposite: true
		};
	baseDynYAxisData.push(newPart);
	return  baseDynYAxisData;
}

function dynDataGraph(renderDiv,seriesData, titleData, yAxisData){
	return new Highcharts.StockChart({
		chart : {
			renderTo : renderDiv,
			alignTicks : false

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
		legend: {
            align: "right",
            layout: "vertical",
            enabled: true,
            verticalAlign: "middle"
            /*
			 * labelFormatter: function() { return this.name + ' (T)'; }
			 */

        },

		series : seriesData

	});
}
