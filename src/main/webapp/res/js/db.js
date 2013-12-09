
var pages = [ "matrix", "search" ];
var modelMapper = {};
var protocolMapper = {};
var lock = true;

/*var colorMapper = {
		"RUNNING": "#86b6f1",
		"SUCCESS": "#05db00",
		"FAILED": "#db0000",
		"INAPPRORIATE": "#f1c886"
};*/

function submitNewExperiment (jsonObject, notificationElement)
{
	if (notificationElement)
		notificationElement.innerHTML = "<img src='"+contextPath+"/res/img/loading2-new.gif' alt='loading' />";
	
	var xmlhttp = null;
    // !IE
    if (window.XMLHttpRequest)
    {
        xmlhttp = new XMLHttpRequest();
    }
    // IE -- microsoft, we really hate you. every single day.
    else if (window.ActiveXObject)
    {
        xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
    }
    
    xmlhttp.open("POST", contextPath + '/newexperiment.html', true);
    xmlhttp.setRequestHeader("Content-type", "application/json");

    xmlhttp.onreadystatechange = function()
    {
        if(xmlhttp.readyState != 4)
        	return;
        
        console.log (xmlhttp.responseText);
    	var json = JSON.parse(xmlhttp.responseText);
    	console.log (json);
    	displayNotifications (json);
    	
        if(xmlhttp.status == 200)
        {
        	if (json.newExperiment)
        	{
	        	var msg = json.newExperiment.responseText;
        		if (json.newExperiment.response)
	        	{
        			if (notificationElement)
        			notificationElement.innerHTML = "<img src='"+contextPath+"/res/img/check.png' alt='valid' /> " + msg;
	        	}
	        	else
	        		if (notificationElement)
	        		notificationElement.innerHTML = "<img src='"+contextPath+"/res/img/failed.png' alt='invalid' /> " + msg;
        	}
        }
        else
        {
        	if (notificationElement)
        	notificationElement.innerHTML = "<img src='"+contextPath+"/res/img/failed.png' alt='error' /> sorry, serverside error occurred.";
        }
    };
    xmlhttp.send(JSON.stringify(jsonObject));
}


function drawMatrix (matrix)
{
	/*var minBoxWidth = 10px;
	var minBoxHeight = 10px;*/
	
	
	
	var models = [];
	//console.log (matrix);
	for (var key in matrix.models)
		if (matrix.models.hasOwnProperty (key))
			for (var version in matrix.models[key].versions)
				if (matrix.models[key].versions.hasOwnProperty (version))
				{
					modelMapper[version] = matrix.models[key].versions[version];
					modelMapper[version].row = models.length;
					modelMapper[version].name = matrix.models[key].name;
					modelMapper[version].entityId = matrix.models[key].id;
					models.push(version);
				}

	var protocols = [];
	for (var key in matrix.protocols)
		if (matrix.protocols.hasOwnProperty (key))
			for (var version in matrix.protocols[key].versions)
				if (matrix.protocols[key].versions.hasOwnProperty (version))
				{
					protocolMapper[version] = matrix.protocols[key].versions[version];
					protocolMapper[version].col = protocols.length;
					protocolMapper[version].name = matrix.protocols[key].name;
					protocolMapper[version].entityId = matrix.protocols[key].id;
					protocols.push(version);
				}
	
	/*console.log ("models");
	console.log (modelMapper);*/
	console.log ("protocols");
	console.log (protocolMapper);
	
	var mat = [];
	for (var i = 0; i < models.length; i++)
	{
		mat[i] = [];
		for (var j = 0; j < protocols.length; j++)
		{
			mat[i][j] = {
					model: modelMapper[models[i]],
					protocol: protocolMapper[protocols[j]]
			};
		}
	}
	//console.log ("matrix");
	//console.log (mat);
	
	var div = document.getElementById("matrixdiv");
	removeChildren (div);
	
	

	
	for (var key in matrix.experiments)
		if (matrix.experiments.hasOwnProperty (key))
		{
			var exp = matrix.experiments[key];
			
			exp.name = exp.model.name + " @ " + exp.model.version + " & " + exp.protocol.name + " @ " + exp.protocol.version;

			var row = modelMapper[exp.model.id].row;
			var col = protocolMapper[exp.protocol.id].col;
			mat[row][col].experiment = exp;
		}
	
	
	
	
	var tableDiv = document.createElement("div");
	tableDiv.id = "matrixContainer";

	var table = document.createElement("table");
	table.setAttribute("class", "matrixTable");
	
	div.appendChild (tableDiv);
	tableDiv.appendChild (table);


	for (var row = 0; row < mat.length; row++)
	{
		var tr = document.createElement("tr");
		table.appendChild (tr);
		for (var col = 0; col < mat[row].length; col++)
		{
			var td = document.createElement("td");
			tr.appendChild (td);
			/*if (mat[row][col].experiment)
				ctx.fillStyle = colorMapper[mat[row][col].experiment.latestResult];
			else
				ctx.fillStyle="#fff";*/
			
			if (row == 0 && col == 0)
				continue;
			
			if (row == 0)
			{
				var d1 = document.createElement("div");
				var d2 = document.createElement("div");
				var a = document.createElement("a");
				a.href = contextPath + "/protocol/" + convertForURL (mat[row][col].protocol.name) + "/" + mat[row][col].protocol.entityId
				+ "/" + convertForURL (mat[row][col].protocol.version) + "/" + mat[row][col].protocol.id;
				d2.setAttribute("class", "vertical-text");
				d1.setAttribute("class", "vertical-text__inner");
				d2.appendChild (d1);
				a.appendChild (document.createTextNode (mat[row][col].protocol.name));
				d1.appendChild(a);
				td.appendChild (d2);//document.createTextNode ("<div class='vertical-text'><div class='vertical-text__inner'>" + mat[row][col].protocol.name + "</div></div>"));
				td.setAttribute("class", "matrixTableCol");
				continue;
			}
			
			if (col == 0)
			{
				var a = document.createElement("a");
				a.href = contextPath + "/model/" + convertForURL (mat[row][col].model.name) + "/" + mat[row][col].model.entityId
				+ "/" + convertForURL (mat[row][col].model.version) + "/" + mat[row][col].model.id;
				a.appendChild (document.createTextNode (mat[row][col].model.name));
				td.appendChild (a);
				td.setAttribute("class", "matrixTableRow");
				continue;
			}
			
			
			if (mat[row][col].experiment)
				td.setAttribute("class", "experiment experiment-"+mat[row][col].experiment.latestResult);
			else
				td.setAttribute("class", "experiment experiment-NONE");
			

			//td.setAttribute("rel", "test123|test");
			//td.setAttribute("rel", "test123");
			//td.setAttribute("title", "test123");
			

			var titleText = "";

			//var r = row, c = col;
			
			if (mat[row][col].model)
			{
				titleText += 
					"M: <a href='" + contextPath + "/model/" + convertForURL (mat[row][col].model.name) + "/" + mat[row][col].model.entityId
					+ "/" + convertForURL (mat[row][col].model.version) + "/" + mat[row][col].model.id + "/'>" + mat[row][col].model.name + " @ " + mat[row][col].model.version + "</a>|";
			}
			
			if (mat[row][col].protocol)
				//protocolLink.innerHTML = mat[row][col].protocol.name + " @ " + mat[row][col].protocol.version;
				titleText += 
					"P: <a href='" + contextPath + "/protocol/" + convertForURL (mat[row][col].protocol.name) + "/" + mat[row][col].protocol.entityId
					+ "/" + convertForURL (mat[row][col].protocol.version) + "/" + mat[row][col].protocol.id + "/'>" + mat[row][col].protocol.name + " @ " + mat[row][col].protocol.version + "</a>|";
						
			if (mat[row][col].experiment)
			{
				//expLink.innerHTML = mat[row][col].experiment.name;
				titleText +=  
					"E: <a href='" + contextPath + "/experiment/" + convertForURL (mat[row][col].experiment.name) + "/"
					+  mat[row][col].experiment.id + "/latest/'>" + mat[row][col].experiment.name + "</a>";
				//if (mat[row][col].experiment.latestResult == "")
				addMatrixClickListener (td, contextPath + "/experiment/" + convertForURL (mat[row][col].experiment.name) + "/"
				+  mat[row][col].experiment.id + "/latest");
			}
			else
			{
				titleText +=  
					"E: <a id='create-"+row+"-"+col+"'>create experiment</a>";
				
			}
			
			td.setAttribute("title", titleText);
			createClueTip (td, mat, row, col);
		}
	}
	
}

function addMatrixClickListener (td, link)
{
				td.addEventListener("click", function () {
					document.location.href = link;
				}, false);
}

function createClueTip (td, mat, r, c)
{

	$(td).cluetip({
		  hoverIntent: {
			    sensitivity:  1,
			    interval:     350,
			    timeout:      350
			  },
		cluetipClass: 'jtip',
		  dropShadow: false,
		  mouseOutClose: true,
		  sticky: true,
		  positionBy: 'bottomTop', topOffset: 0,
		  showTitle: false,
		  splitTitle: '|',
		  onShow:           function(ct, ci){
			  console.log ("shown: " + '#create-'+r+'-'+c);
			  $('#create-'+r+'-'+c).click (function () {
				  console.log ("clicked");
					submitNewExperiment ({
						task: "newExperiment",
						model: mat[r][c].model.id,
						protocol: mat[r][c].protocol.id
					}, document.getElementById ("actionIndicator"));
					  console.log ("clicked2");
				});
			  console.log ("shown2");
		  },
		});
}


function getMatrix (jsonObject, actionIndicator)
{
    
	var xmlhttp = null;
    // !IE
    if (window.XMLHttpRequest)
    {
        xmlhttp = new XMLHttpRequest();
    }
    // IE -- microsoft, we really hate you. every single day.
    else if (window.ActiveXObject)
    {
        xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
    }
    
    xmlhttp.open("POST", '', true);
    xmlhttp.setRequestHeader("Content-type", "application/json");

    xmlhttp.onreadystatechange = function()
    {
        if(xmlhttp.readyState != 4)
        	return;
        
    	var json = JSON.parse(xmlhttp.responseText);
    	//console.log (json);
    	displayNotifications (json);
    	
        if(xmlhttp.status == 200)
        {
        	if (json.getMatrix)
        	{
        		drawMatrix (json.getMatrix);
        	}
        }
        else
        {
        	actionIndicator.innerHTML = "<img src='"+contextPath+"/res/img/failed.png' alt='error' /> sorry, serverside error occurred.";
        }
    };
    xmlhttp.send(JSON.stringify(jsonObject));
}

function prepareMatrix ()
{
	var div = document.getElementById("matrixdiv");
	
	div.appendChild(document.createTextNode("preparing matrix. please be patient"));

	getMatrix ({
    	task: "getMatrix"
    }, div);
	
}

function switchPage (page)
{
	//console.log ("switching to " + page);
	for (var i = 0; i < pages.length; i++)
	{
		if (pages[i] == page)
			document.getElementById(pages[i] + "Tab").style.display = "block";
		else
			document.getElementById(pages[i] + "Tab").style.display = "none";
	}
}
function registerSwitchPagesListener (btn, page)
{
	//console.log ("register switch listener: " + page);
	btn.addEventListener("click", function () {
		console.log ("switch listener triggered " + page);
		switchPage (page);
	}, true);
}

function initDb ()
{
	for (var i = 0; i < pages.length; i++)
	{
		var btn = document.getElementById(pages[i] + "chooser");
		registerSwitchPagesListener (btn, pages[i]);
	}
	switchPage (pages[0]);
	
	prepareMatrix ();
}
document.addEventListener("DOMContentLoaded", initDb, false);