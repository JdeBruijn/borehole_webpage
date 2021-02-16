
var data_interval=null;

var refresh_wait_time = 1000*60*5;//5 min.

var shown_warning=false;

function getStatusData()
{
	fetch("main-servlet",
		{
			method:"GET",
			headers:
			{
				oper:"get_status_data"
			}
		}
	)
	.then(function(response)
	{
		if(response.redirected)
		{window.location=response.url; return;}

		response.json()
		.then(function(data)
		{
			var borehole_status_div = document.getElementById("borehole_status");
			if(data.borehole_ok)
			{borehole_status_div.style.color="green";}
			else
			{borehole_status_div.style.color="red";}
			borehole_status_div.innerHTML="Borehole Status: "+data.borehole_status;

			var booster_status_div = document.getElementById("booster_status");
			if(data.booster_ok)
			{booster_status_div.style.color="green";}
			else
			{booster_status_div.style.color="red";}
			booster_status_div.innerHTML="Booster Status: "+data.booster_status;
		})//then.
	})//then.
	.catch(function(error)
	{alert("Error: "+error);});//catch.
}//getStatsData().

function getLogs()
{
	fetch("main-servlet",
		{
			method:"GET",
			headers:
			{
				oper:"getLogs"
			}
		}
	)
	.then(function(response)
	{
		if(response.redirected)
		{window.location=response.url; return;}

		response.json()
		.then(function(data)
		{
			displayLogs("borehole_logs", data.borehole_headers, data.borehole_values);
			displayLogs("booster_logs", data.booster_headers, data.booster_values);
		//	setTimeout(equalizeRowCellHeights,100);
		})//then.
	})//then.
	.catch(function(error)
	{alert("Error: "+error);});//catch.
}//getLogs().

function displayLogs(log_id, headers, values)
{
	console.log("displayLogs()...");//debug**
	var log_table = document.getElementById(log_id);

	var log_rows = log_table.getElementsByClassName("logs_row");
	var index=0;
	while(log_rows[index]!=null)
	{
		if(log_rows[index].classList.contains("logs_title"))
		{index++; continue;}
		log_table.removeChild(log_rows[index]);
	}//while.

	var headers_list = headers.split(",");
	var cell_width = 100/(headers_list.length);
	var header_row = document.createElement("DIV");
	header_row.classList.add("logs_row", "logs_header");
	for(h=0; h<headers_list.length; h++)
	{
		var cell = document.createElement("DIV");
		cell.classList.add("logs_cell");
		cell.innerHTML=headers_list[h];
		cell.style.width=cell_width+"%";
		header_row.appendChild(cell);
	}//for(h).
	log_table.appendChild(header_row);

	//console.log("displayLogs(): log_id="+log_id+" values="+values);//debug**

	for(vr=0; vr<values.length; vr++)
	{
		var value_row = document.createElement("DIV");
		value_row.classList.add("logs_row");


		var values_list = values[vr].split(",");
	//	console.log("values_list="+values_list);//debug**
		for(v=0; v<headers_list.length; v++)//headers_list.length is correct (not values_list.length)
		{
			var cell = document.createElement("DIV");
			cell.classList.add("logs_cell");
			cell.innerHTML=values_list[v];
			cell.style.width=cell_width+"%";
			value_row.appendChild(cell);
		}//for(h).
		if(Number(values_list[values_list.length])>=20)
		{value_row.style.color="red";}
		log_table.appendChild(value_row);
	}//for(v).

	equalizeRowCellHeights();
}//displayLogs().

function equalizeRowCellHeights()
{
	var log_rows = document.getElementsByClassName("logs_row");
	for(r=0; r<log_rows.length; r++)
	{
		cellHeightEqualizer(log_rows[r]);
	}//for(r).
}//equalizeRowCellHeights().

function cellHeightEqualizer(cells_parent)
{
	var max_height = cells_parent.offsetHeight-1;

	console.log("cellHeightEqualizer(): max_height="+max_height);//debug**

	var cells = cells_parent.children;
	for(c=0; c<cells.length; c++)
	{
		cells[c].style.height=max_height+"px";
	}//for(c).

}//cellHeightEqualizer().

function getWaterLevelGraphData()
{

	fetch("main-servlet",
	{
		method:"GET",
		headers:
		{
			"oper":"getWaterLevelGraphData"
		}
	})
	.then(function(response)
	{
		if(response.redirected)
		{window.location=response.url;}

		response.json()
		.then(function(data)
		{
			if(!data.success)
			{
				alert("Error: "+data.message);
				return;
			}//if.
			if(data.message!=null && data.message!="" && !shown_warning)
			{
				alert(data.message);
				shown_warning=true;
			}//if.

			setupGraph("water_level_graph", "Water Level", data.last_week, data.this_week, data.labels);
		});//then.
	})
	.catch(function(error)
	{
		alert("Error: "+error);
	});//catch.

}//getWaterLevelGraphData().

function getWaterConsumptionGraphData()
{
	fetch("main-servlet",
	{
		method:"GET",
		headers:
		{
			"oper":"getWaterConsumptionGraphData"
		}
	})
	.then(function(response)
	{
		if(response.redirected)
		{window.location=response.url;}

		response.json()
		.then(function(data)
		{
			if(!data.success)
			{
				alert("Error: "+data.message);
				return;
			}//if.
			if(data.message!=null && data.message!="")
			{
				alert(data.message);
			}//if.

			setupGraph("water_consumption_graph", "Water Consumption", data.last_week, data.this_week, data.labels);
		});//then.
	})
	.catch(function(error)
	{
		alert("Error: "+error);
	});//catch.
}//getWaterConsumptionGraphData().


function setupGraph(graph_name, title, last_week_points, this_week_points, labels_str)
{
	var labels = labels_str.split(",");

	var graph_dataset = 
	[
		{
			label:"this week",
			backgroundColor:"red",
			borderColor:"red",
			data:this_week_points,
			fill:false
		},
		{
			label:"last week",
			backgroundColor:"black",
			borderColor:"black",
			data:last_week_points,
			fill:false
		}
	];

	drawGraph(graph_name, title, graph_dataset, labels);
}//setupGraph().

function drawGraph(name, title, graph_dataset, graph_labels)
{
	var graph_context = document.getElementById(name);
	var graph = new Chart(graph_context,
	{
		type:"line",
		data:
		{
			labels:graph_labels,
			datasets: graph_dataset
		},//data.
		options:
		{
			responsive: true,
			title:
			{
				display: true,
				text: title
			},
			tooltips:
			{
				mode: 'index',
				intersect: false,
			},
			hover:
			{
				mode: 'nearest',
				intersect: true
			},
			scales:
			{
				xAxes:
				[{
					display: true,
					scaleLabel:
					{
						display: true,
						labelString: 'Day'
					}
				}],
				yAxes:
				[{
					display: true,
					scaleLabel:
					{
						display: true,
						labelString: 'Volume (L)'
					},
					ticks:{min:0}
				}]
			}//scales.
		}//options.
	});//Chart.
}//drawGraph().




function startUp()
{
	var graphs_container = document.getElementById("graphs_container");
	var graphs = graphs_container.getElementsByClassName("graphs_class");

	var container_width = graphs_container.offsetWidth;
	var graph_width = container_width-graphs_container.style.paddingRight;
	for(g=0; g<graphs.length; g++)
	{
		if(graphs[g]==null)
		{continue;}

		graphs[g].style.width=graph_width+"px";
	}//for(g).

	refreshData();
}//startUp().

function refreshData()
{
	getStatusData();
	getWaterLevelGraphData();
	getWaterConsumptionGraphData();
	getLogs();

	if(data_interval==null)
	{data_interval = setInterval(refreshData, refresh_wait_time);}
}//refreshData().