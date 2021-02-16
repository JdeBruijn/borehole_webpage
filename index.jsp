<!DOCTYPE html>
<html>

<head>
	<title>Borehole - Dashboard</title>
	<link rel="stylesheet" href="css/Chart.css">
	<link rel="stylesheet" href="css/index.css">

	<script type="text/javascript" src="js/index.js"></script>
	<script type="text/javascript" src="js/moment.min.js"></script>
	<script type="text/javascript" src="js/Chart.js"></script>
</head>

<body>

	<div id="graphs_container" class="column_container" style="width:60%; padding-right:40px;">
		<div id="status_container" class="row_container">
			<div id="borehole_status">Borehole Status: </div>
			<div id="booster_status">Booster Status: </div>
		</div>
		<div class="row_container">
			<canvas id="water_level_graph" class="graph_class"></canvas>
			<canvas id="water_consumption_graph" class="graph_class"></canvas>
		</div>
	</div>
	<div class="column_container" style="width:40%; padding-left: 40px; padding-top:50px;">
		<div id="borehole_logs" class="log_table">
			<div class="logs_row logs_title">Borehole</div>
		</div>
		<div id="booster_logs" class="log_table">
			<div class="logs_row logs_title">Booster</div>
		</div>			
	</div>


</body>

<script>
	document.onload=startUp();
</script>

</html>