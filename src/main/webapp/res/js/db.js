
var pages = [ "matrix", "search" ];
var modelMapper = {};
var protocolMapper = {};
var lock = true;

var colorMapper = {
		"RUNNING": "#86b6f1",
		"SUCCESS": "#05db00",
		"FAILED": "#db0000",
		"INAPPRORIATE": "#f1c886"
};

function submitNewExperiment (jsonObject, notificationElement)
{
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
        			notificationElement.innerHTML = "<img src='"+contextPath+"/res/img/check.png' alt='valid' /> " + msg;
	        	}
	        	else
	        		notificationElement.innerHTML = "<img src='"+contextPath+"/res/img/failed.png' alt='invalid' /> " + msg;
        	}
        	
        }
        else
        {
        	notificationElement.innerHTML = "<img src='"+contextPath+"/res/img/failed.png' alt='error' /> sorry, serverside error occurred.";
        }
    };
    xmlhttp.send(JSON.stringify(jsonObject));
}


function drawMatrix (matrix)
{
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
		}
	}
	//console.log ("matrix");
	//console.log (mat);
	
	var div = document.getElementById("matrixdiv");
	removeChildren (div);
	
	var canvas = document.createElement("canvas");
	var ctx = canvas.getContext("2d");
	canvas.id = "matrixcanvas";
	div.appendChild(canvas);
	canvas.style.width = "800px";
	canvas.style.height = "600px";
	canvas.width = 800;
	canvas.height = 600;

	var rows = mat.length;
	var cols = mat[0].length;
	
	var width = canvas.width;
	var height = canvas.height;
	var boxWidth = width / cols;
	var boxHeight = height / rows;
	
	/*console.log ("rc: ", rows, cols);
	console.log ("wh: ", width, height);
	console.log ("bwh: ", boxWidth, boxHeight);*/
	
	
	for (var key in matrix.experiments)
		if (matrix.experiments.hasOwnProperty (key))
		{
			var exp = matrix.experiments[key];
			
			exp.name = exp.model.name + " @ " + exp.model.version + " & " + exp.protocol.name + " @ " + exp.protocol.version;

			var row = modelMapper[exp.model.id].row;
			var col = protocolMapper[exp.protocol.id].col;
			mat[row][col].experiment = exp;
		}
	

	for (var row = 0; row < mat.length; row++)
		for (var col = 0; col < mat[row].length; col++)
		{
			if (mat[row][col].experiment)
				ctx.fillStyle = colorMapper[mat[row][col].experiment.latestResult];
			else
				ctx.fillStyle="#fff";
			ctx.fillRect((col) * boxWidth, (row) * boxHeight, boxWidth, boxHeight);
			
			ctx.beginPath ();
			ctx.moveTo ((col) * boxWidth, (row) * boxHeight + boxHeight);
			ctx.lineTo ((col) * boxWidth + boxWidth, (row) * boxHeight + boxHeight);
			ctx.lineTo ((col) * boxWidth + boxWidth, (row) * boxHeight);
			ctx.stroke ();
		}

	var modelLink = document.getElementById("modelLink");
	var protocolLink = document.getElementById("protocolLink");
	var expLink = document.getElementById("experimentLink");
	
	function getMousePos(canvas, evt) {
        var rect = canvas.getBoundingClientRect();
        return {
          x: evt.clientX - rect.left,
          y: evt.clientY - rect.top
        };
      }

	
	canvas.addEventListener("mouseover", function (event) {
		lock = false;
	}, false);
	
//	var rect = canvas.getBoundingClientRect();
	canvas.addEventListener("mousemove", function (event) {
		if (lock)
			return;
		
		var mouse = getMousePos(canvas, event);

		var x = mouse.x;//event.pageX - rect.left;
		var y = mouse.y;//event.pageY - rect.top;
		//console.log ("x: " + x + " y: " + y);
		
		// translate into matrix
		var r = parseInt(y / boxHeight);
		var c = parseInt(x / boxWidth);
		
		//console.log ("r: " + r + " c: " + c);
		if (r >= mat.length || c >= mat[0].length)
			return;
		
		if (mat[r][c].model)
		{
			modelLink.innerHTML = 
				"<a href='" + contextPath + "/model/" + convertForURL (mat[r][c].model.name) + "/" + mat[r][c].model.entityId
				+ "/" + convertForURL (mat[r][c].model.version) + "/" + mat[r][c].model.id + "/'>" + mat[r][c].model.name + " @ " + mat[r][c].model.version + "</a>";
		}
		else
			modelLink.innerHTML = "";
		
		if (mat[r][c].protocol)
			//protocolLink.innerHTML = mat[r][c].protocol.name + " @ " + mat[r][c].protocol.version;
			protocolLink.innerHTML = 
				"<a href='" + contextPath + "/protocol/" + convertForURL (mat[r][c].protocol.name) + "/" + mat[r][c].protocol.entityId
				+ "/" + convertForURL (mat[r][c].protocol.version) + "/" + mat[r][c].protocol.id + "/'>" + mat[r][c].protocol.name + " @ " + mat[r][c].protocol.version + "</a>";
		else
			protocolLink.innerHTML = "";
		
		if (mat[r][c].experiment)
			//expLink.innerHTML = mat[r][c].experiment.name;
			expLink.innerHTML = 
				"<a href='" + contextPath + "/experiment/" + convertForURL (mat[r][c].experiment.name) + "/"
				+  mat[r][c].experiment.id + "/'>" + mat[r][c].experiment.name + "</a>";
		else
		{
			var a = document.createElement ("a");
			a.appendChild(document.createTextNode("create experiment"));
			removeChildren (expLink);
			expLink.appendChild (a);
			
			a.addEventListener("click", function () {
				submitNewExperiment ({
					task: "newExperiment",
					model: mat[r][c].model.id,
					protocol: mat[r][c].protocol.id
				}, expLink);
			}, false);
		}
	}, false);
	
	canvas.addEventListener("click", function (event) {
		lock = true;
		
		
	}, false);
	
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
    	console.log (json);
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
	console.log ("switching to " + page);
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
	console.log ("register switch listener: " + page);
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