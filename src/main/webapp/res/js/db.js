
var pages = [ "matrix", "search" ];
var modelMapper = {};
var protocolMapper = {};
var lock = true;

/**
 * Submit a request to create an experiment.
 * @param jsonObject  the data to send
 * @param linkElement  jQuery wrapper around the link element that was clicked
 * @param td  the table cell to contain this experiment
 * @param entry  the entry for this experiment in the data matrix
 */
function submitNewExperiment (jsonObject, linkElement, td, entry)
{
    linkElement.append("<img src='"+contextPath+"/res/img/loading2-new.gif' alt='loading' />");

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

    	var json = JSON.parse(xmlhttp.responseText);
    	displayNotifications (json);

    	if (td)
    		td.removeClass ("experiment-QUEUED").removeClass ("experiment-RUNNING").removeClass ("experiment-INAPPRORIATE").removeClass ("experiment-FAILED").removeClass ("experiment-PARTIAL").removeClass ("experiment-SUCCESS");

        if(xmlhttp.status == 200)
        {
        	if (json.newExperiment)
        	{
	        	var msg = json.newExperiment.responseText;
        		if (json.newExperiment.response)
	        	{
        		    addNotification(msg, "info");
                    linkElement.unbind("click");
                    linkElement.contents().remove();
                    linkElement.removeAttr("id");
                    entry.experiment = {name: json.newExperiment.expName, id: json.newExperiment.expId};
        		    var expUrl = contextPath + "/experiment/" + convertForURL(entry.experiment.name) + "/" + entry.experiment.id + "/latest";
        		    linkElement.attr("href", expUrl);
        		    linkElement.append("<img src='"+contextPath+"/res/img/check.png' alt='valid' /> ");
        		    linkElement.append(entry.experiment.name);
        			if (td)
        			{
	    				td.addClass ("experiment-QUEUED");
	    				setTitleAndClickListener(td.get(0), entry);
	    				createClueTip(td, entry);
        			}
	        	}
	        	else
	        	{
	        	    addNotification(msg, "error");
                    linkElement.replaceWith("<img src='"+contextPath+"/res/img/failed.png' alt='invalid' /> failed to create experiment");
	    			if (td)
	    			{
                        td.addClass ("experiment-INAPPRORIATE");
	    			}
	        	}
        	}
        }
        else
        {
        	if (notificationElement)
        		notificationElement.replaceWith("<img src='"+contextPath+"/res/img/failed.png' alt='error' /> sorry, serverside error occurred.");
			if (td)
				td.addClass ("experiment-INAPPRORIATE");
        }
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
			var td = row == -1 ? document.createElement("th") : document.createElement("td");
			tr.appendChild (td);
			
			//console.log ("row " + row + " col " + col);
			
			if (row == -1 && col == -1)
				continue;
			
			// Top row: protocol names
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
			
			// Left column: model names
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
			
			// Normal case
			var entry = mat[row][col];
			entry.row = row;
			entry.col = col;
			if (entry.experiment)
				td.setAttribute("class", "experiment experiment-"+entry.experiment.latestResult);
			else
				td.setAttribute("class", "experiment experiment-NONE");
			
			setTitleAndClickListener(td, entry);
		    createClueTip($(td), entry);
		}
	}
}

/**
 * Set up the title text and click listener for the given matrix entry
 * @param td  the table cell
 * @param entry  mat[row][col] for this table cell
 */
function setTitleAndClickListener(td, entry)
{
    var titleText = "";

    if (entry.model)
    {
        titleText +=
            "M: <a href='" + contextPath + "/model/" + convertForURL(entry.model.name) + "/" + entry.model.entityId
            + "/" + convertForURL(entry.model.version) + "/" + entry.model.id + "/'>" + entry.model.name + " @ " + entry.model.version + "</a>|";
    }

    if (entry.protocol)
    {
        titleText +=
            "P: <a href='" + contextPath + "/protocol/" + convertForURL(entry.protocol.name) + "/" + entry.protocol.entityId
            + "/" + convertForURL(entry.protocol.version) + "/" + entry.protocol.id + "/'>" + entry.protocol.name + " @ " + entry.protocol.version + "</a>|";
    }

    if (entry.experiment)
    {
        var expUrl = contextPath + "/experiment/" + convertForURL(entry.experiment.name) + "/" + entry.experiment.id + "/latest";
        titleText += "E: <a href='" + expUrl + "'>" + entry.experiment.name + "</a>";
        addMatrixClickListener(td, expUrl);
    }
    else
    {
        titleText += "E: <a id='create-"+entry.row+"-"+entry.col+"'>create experiment</a>";
    }

    td.setAttribute("title", titleText);
}

function addMatrixClickListener (td, link)
{
	td.addEventListener("click", function () {
		document.location.href = link;
	}, false);
}

function createClueTip (td, entry)
{
	td.cluetip({
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
			var link = $('#create-'+entry.row+'-'+entry.col);
			link.click (function () {
					submitNewExperiment ({
						task: "newExperiment",
						model: entry.model.id,
						protocol: entry.protocol.id
					}, link, td, entry);
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