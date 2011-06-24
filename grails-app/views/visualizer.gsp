%{--
- Copyright 2010-2011 [PowerTAC / Adis].
    -
- Licensed under the Apache License, Version 2.0 (the "License");
- you may not use this file except in compliance with the License.
    - You may obtain a copy of the License at
-
-  http://www.apache.org/licenses/LICENSE-2.0
-
- Unless required by applicable law or agreed to in writing, software
- distributed under the License is distributed on an
-
- "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
-
- either express or implied. See the License for the specific language
- governing permissions and limitations under the License.
    --}%

<!DOCTYPE html>

    <head>
	<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
		<title>Visualizer - work version</title>
		<link type="text/css" href="${resource(dir:'css',file:'style.css',plugin:'powertac-visualizer')}" rel="stylesheet" />
                <link type="text/css" href="${resource(dir:'css/south-street/',file:'jquery-ui-1.8.13.custom.css',plugin:'powertac-visualizer')}" rel="stylesheet" />
		<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jquery/1.5.1/jquery.min.js"></script>
		<script type="text/javascript" src="https://ajax.googleapis.com/ajax/libs/jqueryui/1.8.13/jquery-ui.min.js"></script>
                <script type="text/javascript" src="${resource(dir:'js',file:'jquery.flot.min.js',plugin:'powertac-visualizer')}"></script>
		<!--[if IE]><script language="javascript" type="${resource(dir:'js',file:'excanvas.min.js',plugin:'powertac-visualizer')}"></script><![endif]-->
	
		<script type="text/javascript">
		$(document).ready(function($) {
			// Accordions
			$(".accordion").accordion({
				collapsible:'true'
			});

			// Tabs
			$("#tabs").tabs();
			
			// Progressbar
			$("#progressbar").progressbar({
				value: 68 
			});
			
			// Flot code
			$(function () {
				var data1 = []
				for (var i = 0; i < 15; i += 0.5) {
						data1.push([i, i]);
					}
					
					var data2 = []
					for (var i = 0; i < 15; i += 0.5) {
						data2.push([i, Math.sqrt(i * 10)]);
					}
					
					var data3 = []
					for (var i = 0; i < 15; i += 0.5) {
						data3.push([i, Math.cos(i)]);
					}
					
					$.plot($("#graph1"), [
						{
							data: data1,
							lines: { show: true }
						}
					]);
					
					$.plot($("#graph2"), [
						{
							data: data2,
							lines: { show: true }
						}
					]);
					
					$.plot($("#graph3"), [
						{
							data: data3,
							lines: { show: true }
						}
					]);
					
					$(".tickLabels").css("position","static");
					
			});
			//return false;
		});
		</script>	
	</head>

<g:render template="/layouts/main-red" plugin="powertac-visualizer"/>