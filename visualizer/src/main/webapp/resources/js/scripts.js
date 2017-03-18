function lineChart(IDString, JSONdata, chartTitle, xlabel, ylabel,
		seriesOptions) {

	return  $.jqplot(IDString, [ JSONdata ], {
		title : chartTitle,
		axesDefaults : {
			labelRenderer : $.jqplot.CanvasAxisLabelRenderer
		},
		axes : {
			xaxis : {
				label : xlabel,
				pad : 0
			},
			yaxis : {
				label : ylabel
			}
		},

		series : [ seriesOptions ],

		seriesDefaults : {
			showMarker : false
		}

	});
}

function lineChartMinMax(IDString, JSONdata, chartTitle, xlabel, ylabel,
		seriesOptions, minValue, maxValue) {

	return $.jqplot(IDString, [ JSONdata ], {
		title : chartTitle,
		axesDefaults : {
			labelRenderer : $.jqplot.CanvasAxisLabelRenderer
		},
		axes : {
			xaxis : {
				label : xlabel,
				pad : 0
			},
			yaxis : {
				label : ylabel,
				min: minValue,
				max: maxValue				
			}
		},

		series : [ seriesOptions ],

		seriesDefaults : {
			showMarker : false
		}

	});
}

function lineChartGlobal(IDString, JSONdata, chartTitle, xlabel, ylabel,
		seriesColorData) {
	
	return $.jqplot(IDString,  JSONdata, 
	{
	title: chartTitle, 
	seriesColors: seriesColorData,
	axesDefaults: {
	        labelRenderer: $.jqplot.CanvasAxisLabelRenderer},
	axes: {
		xaxis: {
	            label: xlabel,
	            pad: 0},
	    yaxis: {
	            label: ylabel}},
	   
									
			
	seriesDefaults: {
		showMarker: false
	}
}); 	}

function pieChart(IDString, JSONdata, chartTitle, seriesColorData){

return jQuery.jqplot (IDString, [JSONdata],
	    {
		title: chartTitle,
		seriesColors: seriesColorData,
        highlighter: { show: false },
	   	seriesDefaults: {
	        // Make this a pie chart.
	        renderer: jQuery.jqplot.PieRenderer,
	        rendererOptions: {
	        	fill: false,
	          // Put data labels on the pie slices.
	          // By default, labels show the percentage of the slice.
	          showDataLabels: true,
	          
	          // stroke the slices with a little thicker line.
	          lineWidth: 4
	        }
	      },
	     
	     legend: { show:false, location: 's' }
	    }
	  );
}