
var pages = [ "matrix", "search" ];
var modelMapper = {};
var protocolMapper = {};
var lock = true;

function submitNewExperiment (jsonObject, notificationElement, td)
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

    	console.log ("td prev");
    	console.log (td);
    	
    	if (td)
    		td.removeClass ("experiment-QUEUED").removeClass ("experiment-RUNNING").removeClass ("experiment-INAPPRORIATE").removeClass ("experiment-FAILED").removeClass ("experiment-PARTIAL").removeClass ("experiment-SUCCESS");
    	
        if(xmlhttp.status == 200)
        {
        	if (json.newExperiment)
        	{
	        	var msg = json.newExperiment.responseText;
        		if (json.newExperiment.response)
	        	{
        			if (notificationElement)
        				notificationElement.innerHTML = "<img src='"+contextPath+"/res/img/check.png' alt='valid' /> " + msg;
        			if (td)
	    				td.addClass ("experiment-QUEUED");
        				//td.attr ("class", td.attr ("class").replace (/experiment-[A-Z]+/, "") + "experiment-RUNNING");
	        	}
	        	else
	        	{
	        		if (notificationElement)
	        			notificationElement.innerHTML = "<img src='"+contextPath+"/res/img/failed.png' alt='invalid' /> " + msg;
	    			if (td)
	    				td.addClass ("experiment-INAPPRORIATE");
	    				//td.attr ("class", td.attr ("class").replace (/experiment-[A-Z]+/, "") + "experiment-INAPPRORIATE");
	        	}
        	}
        }
        else
        {
        	if (notificationElement)
        		notificationElement.innerHTML = "<img src='"+contextPath+"/res/img/failed.png' alt='error' /> sorry, serverside error occurred.";
			if (td)
				td.addClass ("experiment-INAPPRORIATE");
				//td.attr ("class", td.attr ("class").replace (/experiment-[A-Z]+/, "") + "experiment-INAPPRORIATE");
        }
    	console.log ("td post");
    	console.log (td);
    };
    xmlhttp.send(JSON.stringify(jsonObject));
}


function drawMatrix (matrix)
{
	/*var minBoxWidth = 10px;
	var minBoxHeight = 10px;*/
	
	//console.log (matrix);
	
	var models = [];
	for (var key in matrix.models)
		if (matrix.models.hasOwnProperty (key))
		{
			var version = matrix.models[key].id;
			modelMapper[version] = matrix.models[key];
			modelMapper[version].name = matrix.models[key].name;
			models.push(version);
			
		}

	var protocols = [];
	for (var key in matrix.protocols)
		if (matrix.protocols.hasOwnProperty (key))
		{
			var version = matrix.protocols[key].id;
			protocolMapper[version] = matrix.protocols[key];
			protocolMapper[version].name = matrix.protocols[key].name;
			protocols.push(version);
		}

    // Sort rows & columns alphabetically (case insensitive)
    models.sort(function(a,b) {return (modelMapper[a].name.toLocaleLowerCase() > modelMapper[b].name.toLocaleLowerCase()) ? 1 : ((modelMapper[b].name.toLocaleLowerCase() > modelMapper[a].name.toLocaleLowerCase()) ? -1 : 0);});
    protocols.sort(function(a,b) {return (protocolMapper[a].name.toLocaleLowerCase() > protocolMapper[b].name.toLocaleLowerCase()) ? 1 : ((protocolMapper[b].name.toLocaleLowerCase() > protocolMapper[a].name.toLocaleLowerCase()) ? -1 : 0);});
	
	/*console.log ("models");
	console.log (modelMapper);
	console.log ("protocols");
	console.log (protocolMapper);*/
	
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
			modelMapper[models[i]].row = i;
			protocolMapper[protocols[j]].col = j;
			//console.log (mat[i][j]);
		}
	}
	//console.log ("matrix");
	//console.log (mat);
	
	var div = document.getElementById("matrixdiv");
	removeChildren (div);
	
	for (var key in matrix.experiments)
	{
		if (matrix.experiments.hasOwnProperty (key))
		{
			var exp = matrix.experiments[key];
			
			exp.name = exp.model.name + " @ " + exp.model.version + " & " + exp.protocol.name + " @ " + exp.protocol.version;
			
			//if (!modelMapper[exp.model.id] || !protocolMapper[exp.protocol.id])
			//	continue;
			
			var row = modelMapper[exp.model.id].row;

			/*console.log(exp);
			console.log(exp.protocol.id);
			console.log(exp.protocol);*/
			var col = protocolMapper[exp.protocol.id].col;
			mat[row][col].experiment = exp;
		}
	}
	
	
	
	var tableDiv = document.createElement("div");
	tableDiv.id = "matrixContainer";

	var table = document.createElement("table");
	table.setAttribute("class", "matrixTable");
	
	div.appendChild (tableDiv);
	tableDiv.appendChild (table);


	for (var row = -1; row < mat.length; row++)
	{
		var tr = document.createElement("tr");
		table.appendChild (tr);
		for (var col = -1; col < mat[0].length; col++)
		{
			var td = document.createElement("td");
			tr.appendChild (td);
			
			//console.log ("row " + row + " col " + col);
			
			if (row == -1 && col == -1)
				continue;
			
			if (row == -1)
			{
				var d1 = document.createElement("div");
				var d2 = document.createElement("div");
				var a = document.createElement("a");
				a.href = contextPath + "/protocol/" + convertForURL (mat[0][col].protocol.name) + "/" + mat[0][col].protocol.entityId
				+ "/" + convertForURL (mat[0][col].protocol.version) + "/" + mat[0][col].protocol.id;
				d2.setAttribute("class", "vertical-text");
				d1.setAttribute("class", "vertical-text__inner");
				d2.appendChild (d1);
				a.appendChild (document.createTextNode (mat[0][col].protocol.name));
				d1.appendChild(a);
				td.appendChild (d2);//document.createTextNode ("<div class='vertical-text'><div class='vertical-text__inner'>" + mat[row][col].protocol.name + "</div></div>"));
				td.setAttribute("class", "matrixTableCol");
				continue;
			}
			
			if (col == -1)
			{
				var a = document.createElement("a");
				a.href = contextPath + "/model/" + convertForURL (mat[row][0].model.name) + "/" + mat[row][0].model.entityId
				+ "/" + convertForURL (mat[row][0].model.version) + "/" + mat[row][0].model.id;
				a.appendChild (document.createTextNode (mat[row][0].model.name));
				td.appendChild (a);
				td.setAttribute("class", "matrixTableRow");
				continue;
			}
			
			
			if (mat[row][col].experiment)
				td.setAttribute("class", "experiment experiment-"+mat[row][col].experiment.latestResult);
			else
				td.setAttribute("class", "experiment experiment-NONE");

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
			interval:     800, // Delay before showing, in ms
			timeout:      350 // Delay after leaving element before removing cluetip, in ms
			},
		cluetipClass: 'jtip',
		dropShadow: false,
		mouseOutClose: true,
		sticky: true,
		positionBy: 'bottomTop', topOffset: 0,
		showTitle: false,
		splitTitle: '|',
		onShow: function(ct, ci){
			console.log ("shown: " + '#create-'+r+'-'+c);
			$('#create-'+r+'-'+c).click (function () {
					submitNewExperiment ({
						task: "newExperiment",
						model: mat[r][c].model.id,
						protocol: mat[r][c].protocol.id
					}, document.getElementById ("actionIndicator"), $(td));
				});
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
    
    xmlhttp.open("POST", document.location.href, true);
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
        	actionIndicator.innerHTML = "<img src='"+contextPath+"/res/img/failed.png' alt='error' />Sorry, serverside error occurred.";
        }
    };
    xmlhttp.send(JSON.stringify(jsonObject));
}

function prepareMatrix ()
{
	var div = document.getElementById("matrixdiv");
	
	div.appendChild(document.createTextNode("Preparing experiment matrix; please be patient."));

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
		{
			document.getElementById(pages[i] + "Tab").style.display = "block";
			$("#" + pages[i] + "chooser").addClass("selected");
		}
		else
		{
			document.getElementById(pages[i] + "Tab").style.display = "none";
			$("#" + pages[i] + "chooser").removeClass("selected");
		}
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