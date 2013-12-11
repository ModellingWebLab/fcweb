var entities = {};
var files = {};
var entityType;
var visualizers = {};
var tableParsed = false;
var fileName = null;
var pluginName = null;
var doc;
var gotInfos = false;
var plotDescription;

function getFileContent (file, succ)
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
    
    xmlhttp.open("GET", file.url, true);

    xmlhttp.onreadystatechange = function()
    {
        if(xmlhttp.readyState != 4)
        	return;
    	
        if(xmlhttp.status == 200)
        {
        	file.contents = xmlhttp.responseText;
        	succ.getContentsCallback (true);
        }
        else
        	succ.getContentsCallback (false);
    };
    xmlhttp.send(null);
}
function nextPage (url)
{
	//if (//url)
	window.history.pushState(document.location.href, "", url);
	parseUrl ();
}

function registerFileDisplayer (elem)
{
	elem.addEventListener("click", function (ev) {
		if (ev.which == 1)
		{
			ev.preventDefault();
			nextPage (elem.href);
		}
    	}, true);
}

function setupDownloadFileContents (f)
{
	f.getContents = function (callBack)
	{
		if (f.hasContents)
			callBack.getContentsCallback (true);
		f.hasContents = true;
		
		//console.log ("file appears in " + f.entities.length);
		for (var i = 0; i < f.entities.length; i++)
		{
			//console.log ("getting " + f.entities[i].entityFileLink.sig + " --> " + f.entities[i].entityFileLink.url);
			getFileContent (f.entities[i].entityFileLink, callBack);
		}
	};
}

function highlightPlots (showDefault)
{
	//console.log (plotDescription);
	for (var i = 1; i < plotDescription.length; i++)
	{
		if (plotDescription[i].length < 2)
			continue;
		//console.log (plotDescription[i][2]);
		var row = document.getElementById ("filerow-" + plotDescription[i][2].hashCode ());
		if (row)
		{
			row.setAttribute("class", "highlight-plot");
			/*if (i == 1 && showDefault)
			{
				var viz = document.getElementById ("filerow-" + plotDescription[i][2] + "-viz-displayPlotFlot");
				if (viz)
					viz.click();
			}*/
		}
		
		if (files[plotDescription[i][2].hashCode ()])
		{
			//console.log (files[plotDescription[i][2].hashCode ()]);
			
			var f = files[plotDescription[i][2].hashCode ()];
			f.xAxes = plotDescription[i][4];
			f.yAxes = plotDescription[i][5];
			f.title = plotDescription[i][0];
			f.linestyle = plotDescription[i][3];
		}
		/*else
		{
			console.log ("no entry for " + files[plotDescription[i][2].hashCode ()]);
			console.log (files);
		}*/

		//console.log ("files: ")
		//console.log (version.files);
		/*for (var f = 0; f < version.files.length; f++)
		{
			if (files[version.files[f]].name == plotDescription[i][2])
			{
				files[version.files[f]].xAxes = plotDescription[i][4];
				files[version.files[f]].yAxes = plotDescription[i][5];
				files[version.files[f]].title = plotDescription[i][0];
				files[version.files[f]].linestyle = plotDescription[i][3];
			}
			//console.log ("file: ")
			//console.log (files[version.files[f]]);
		}*/
	}
}
function parsePlotDescription (file, showDefault)
{
	/*if (file.plotDescription)
		return converter.makeHtml (file.contents);*/
	
	var goForIt = {
			getContentsCallback : function (succ)
			{
				if (succ)
				{
					var str = file.contents.replace(/\s*#.*\n/gm,"");
					var delimiter = ",";
					var patterns = new RegExp(
				    		(
				    			// Delimiters.
				    			"(\\" + delimiter + "|\\r?\\n|\\r|^)" +
				    			// Quoted fields.
				    			"(?:\"([^\"]*(?:\"\"[^\"]*)*)\"|" +
				    			// Standard fields.
				    			"([^\"\\" + delimiter + "\\r\\n]*))"
				    		),
				    		"gi"
				    		);
					var csv = [[]];
					var matches = null;
					while (matches = patterns.exec (str))
					{
						var value;
						var matchDel = matches[1];
						if (matchDel.length && matchDel != delimiter)
				    			csv.push([]);
						if (matches[2])
							value = matches[2].replace (new RegExp ("\"\"", "g"), "\"");
						else
							value = matches[3];
						
						csv[csv.length - 1].push (value);
					}
					
					plotDescription = csv;
					highlightPlots (showDefault);
				}
			}
	};
	getFileContent (file, goForIt);
	
	return null;
}

function parseEntities (entityObj)
{
	//console.log (entityObj);
	for (var i = 0; i < entityObj.length; i++)
	{
		entities[entityObj[i].id] = entityObj[i];
		if (entityObj[i].files)
			for (var j = 0; j < entityObj[i].files.length; j++)
			{
				entityObj[i].files[j].signature = entityObj[i].files[j].name.hashCode ();
				entityObj[i].files[j].url = contextPath + "/download/" + entityType.charAt(0) + "/" + convertForURL (entityObj[i].name) + "/" + entityObj[i].id + "/" + entityObj[i].files[j].id + "/" + convertForURL (entityObj[i].files[j].name);
				var sig = entityObj[i].files[j].signature;
				if (!files[sig])
				{
					
					files[sig] = {};
					files[sig].sig = sig;
					files[sig].name = entityObj[i].files[j].name;
					files[sig].entities = new Array ();
					files[sig].div = {};
					files[sig].viz = {};
					files[sig].hasContents = false;
					setupDownloadFileContents (files[sig]);
					
					if (!plotDescription && entityObj[i].files[j].name.toLowerCase () == "outputs-default-plots.csv")
						parsePlotDescription (entityObj[i].files[j], null);
					/*files[sig]*/
				}
				files[sig].entities.push ({entityLink: entityObj[i], entityFileLink: entityObj[i].files[j]});
			}
	}
	var entitiesToCompare = document.getElementById("entitiesToCompare");
	removeChildren (entitiesToCompare);
	var ul = document.createElement("ul");
	entitiesToCompare.appendChild(ul);
	for (var entity in entities)
	{
		var li = document.createElement("li");
		var a = document.createElement("a");
		a.href = contextPath + "/"+entityType+"/" + convertForURL (entities[entity].name) + "/" + entities[entity].entityId + "/" + convertForURL (entities[entity].created) + "/" + entities[entity].id;
		a.innerHTML = entities[entity].name;
		li.appendChild(a);
		ul.appendChild(li);
	}
	
	buildSite ();
}
function buildSite ()
{
	
	var filestable = document.getElementById("filestable");
	var tr = document.createElement("tr");
	
	var td = document.createElement("th");
	td.appendChild(document.createTextNode("Name"));
	tr.appendChild(td);
	
	td = document.createElement("th");
	td.appendChild(document.createTextNode("Avg size"));
	tr.appendChild(td);
	
	td = document.createElement("th");
	td.appendChild(document.createTextNode("Action"));
	tr.appendChild(td);
	
	filestable.appendChild(tr);
	
	for (var file in files)
	{
		var ents = files[file];
		var curFileName = ents.name;
		tr = document.createElement("tr");
		tr.id = "filerow-" + ents.sig;
		filestable.appendChild(tr);
		td = document.createElement("td");
		td.appendChild(document.createTextNode(curFileName + " ("+ents.entities.length+")"));
		tr.appendChild(td);
		
		td = document.createElement("td");
		var size = 0;
		for (var i = 0; i < ents.entities.length; i++)
		{
			size += ents.entities[i].entityFileLink.size;
		}
		//console.log (size + " --> " + humanReadableBytes (size / ents.length));
		td.appendChild(document.createTextNode(humanReadableBytes (size / ents.entities.length)));
		tr.appendChild(td);
		
		/*td = document.createElement("td");
		td.appendChild(document.createTextNode("action"));*/
		
		td = document.createElement("td");
		for (var vi in visualizers)
		{
			var viz = visualizers[vi];
			if (!viz.canRead (ents.entities[0].entityFileLink))
				continue;
			var a = document.createElement("a");
			a.setAttribute("id", "filerow-" + file + "-viz-" + viz.getName ());
			a.href = basicurl + "show/" + file + "/" + vi;
			var img = document.createElement("img");
			img.src = contextPath + "/res/js/visualizers/" + vi + "/" + viz.getIcon ();
			img.alt = viz.getDescription ();
			img.title = img.alt;
			a.appendChild(img);
			//a.appendChild(document.createTextNode ("view"));
			registerFileDisplayer (a);//, basicurl + convertForURL (v.name) + "/" + v.id + "/");
			td.appendChild(a);
			td.appendChild(document.createTextNode (" "));
		}
		tr.appendChild(td);
		
	}
	handleReq ();
}

function displayFile (id, pluginName)
{
	if (!gotInfos)
		return;
	var f = files[id];
	if (!f)
	{
		addNotification ("no such file", "error");
		return;
	}
    //var df = doc.file;
	//console.log (f);
	doc.fileName.innerHTML = f.name;
	
	if (!f.div[pluginName])
	{
		f.div[pluginName] = document.createElement("div");
		f.viz[pluginName] = visualizers[pluginName].setUpComparision (f, f.div[pluginName]);
	}
	
	f.viz[pluginName].show ();
	
	removeChildren (doc.fileDisplay);
	doc.fileDisplay.appendChild (f.div[pluginName]);
	
	// doc.version.files.style.display = "none";
	doc.fileDetails.style.display = "block";
}

function handleReq ()
{
	if (fileName && pluginName && gotInfos)
	{
		displayFile (fileName, pluginName);
		doc.displayClose.href = basicurl;
		
	}
}

function getInfos (jsonObject)
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
        
        gotInfos = true;
        
        //console.log (xmlhttp.responseText);
    	var json = JSON.parse(xmlhttp.responseText);
    	//console.log (json);
    	displayNotifications (json);
    	
        if(xmlhttp.status == 200)
        {
        	
        	if (json.getEntityInfos)
        	{
        		parseEntities (json.getEntityInfos.entities);
        	}
        }
    };
    xmlhttp.send(JSON.stringify(jsonObject));
}

function parseUrl (event)
{
	var entityIds = null;
	var t = document.location.href.split ("/");
	//console.log (t);
	for (var i = 0; i < t.length; i++)
	{
		if ("/" + t[i] == contextPath && t[i+2] == "m")
		{
			basicurl = t.slice (0, i + 3).join ("/") + "/";
			entityType = "model";
			entityIds = t.slice (i + 3);
		}
		if ("/" + t[i] == contextPath && t[i+2] == "p")
		{
			basicurl = t.slice (0, i + 3).join ("/") + "/";
			entityType = "protocol";
			entityIds = t.slice (i + 3);
			//entityId = t[i+3];
			//return t.slice (i + 4);
		}
		if ("/" + t[i] == contextPath && t[i+2] == "e")
		{
			basicurl = t.slice (0, i + 3).join ("/") + "/";
			entityType = "experiment";
			entityIds = t.slice (i + 3);
			//entityId = t[i+3];
			//return t.slice (i + 4);
		}
	}
	
	if (!entityIds)
	{
		var entitiesToCompare = document.getElementById("entitiesToCompare");
		removeChildren (entitiesToCompare);
		entitiesToCompare.appendChild(document.createTextNode("ERROR building site"));
		return;
	}
	
	fileName = null;
	pluginName = null;
	var TentityIds = new Array ();
	
	
	for (var i = 0; i < entityIds.length; i++)
	{
		if (entityIds[i] == "show")
		{
			if (i + 2 < entityIds.length)
			{
				fileName = entityIds[i + 1];
				pluginName = entityIds[i + 2];
			}
			entityIds = entityIds.slice (0, i);
			break;
		}
		else if (entityIds[i])
			TentityIds.push (entityIds[i]);
	}
	entityIds = TentityIds;
	basicurl = basicurl + entityIds.join ("/") + "/";
	//console.log("basicurl" + basicurl);
	
	//console.log (entityType);
	//console.log (entityIds);
	
	if (!tableParsed)
	{
		tableParsed = true;
		getInfos ({
			task: "getEntityInfos",
			ids: entityIds
		});
	}
	else
		handleReq ();
		
}

function maxDist (val1, val2, val3)
{
	var a = val1 > val2 ?
			(val1 > val3 ? val1 : val3) :
			(val2 > val3 ? val2 : val3); 
	var b = val1 < val2 ?
			(val1 < val3 ? val1 : val3) :
			(val2 < val3 ? val2 : val3);
	return a - b;
}


function parseCSVContent (file)
{
	//console.log ("parsing csv: " + file.contents);
	
	var str = file.contents.replace(/\s*#.*\n/gm,"");
	var delimiter = ",";
	var patterns = new RegExp(
    		(
    			// Delimiters.
    			"(\\" + delimiter + "|\\r?\\n|\\r|^)" +
    			// Quoted fields.
    			"(?:\"([^\"]*(?:\"\"[^\"]*)*)\"|" +
    			// Standard fields.
    			"([^\"\\" + delimiter + "\\r\\n]*))"
    		),
    		"gi"
    		);
	var csv = [[]];
	var matches = null;
	while (matches = patterns.exec (str))
	{
		var value;
		var matchDel = matches[1];
		if (matchDel.length && matchDel != delimiter)
    			csv.push([]);
		if (matches[2])
			value = matches[2].replace (new RegExp ("\"\"", "g"), "\"");
		else 
			value = matches[3];
		
		csv[csv.length - 1].push (value);
	}
	file.csv = csv;

	var min = Math.pow(2, 32);
	var max = -min;
	file.columns = [];
	for (var i = 0; i < csv[0].length; i++)
	{
		file.columns[i] = [];
		for (var j = 0; j < csv.length; j++)
			if (csv[j][i])
			{
				file.columns[i][j] = Number (csv[j][i]);
				if (i > 0)
				{
					if (max < file.columns[i][j])
						max = file.columns[i][j];
					if (min > file.columns[i][j])
						min = file.columns[i][j];
				}
			}
	}
	var dropDist = (max-min) / 5000.;//100.;
	file.nonDownsampled = [];
	file.downsampled = [];
	for (var i = 1; i < file.columns.length; i++)
	{
		file.downsampled[i] = [];
		file.nonDownsampled[i] = [];
		//file.downsampled[i][0] = file.columns[i][0];
		file.downsampled[i][0] = {x : file.columns[0][0], y : file.columns[i][0]};
		file.nonDownsampled[i][0] = {x : file.columns[0][0], y : file.columns[i][0]};
		for (var j = 1; j < file.columns[i].length - 1; j++)
		{
			file.nonDownsampled[i].push ({x : file.columns[0][j], y : file.columns[i][j]});
			var last = file.downsampled[i][file.downsampled.length - 1];
			var cur = file.columns[i][j];
			var next = file.columns[i][j + 1];
			if (maxDist (last, cur, next) > dropDist || (cur < last && cur < next) || (cur > last && cur > next))
				file.downsampled[i].push ({x : file.columns[0][j], y : file.columns[i][j]});
		}
		var last = file.columns[0].length - 1;
		//file.downsampled[i].push (file.columns[i][j]);
		file.downsampled[i].push ({x : file.columns[0][last], y : file.columns[i][last]});
		//console.log ("column " + i + " prev: " + file.columns[i].length + " now: " + file.downsampled[i].length);
	}
	
}

function getCSVColumnsNonDownsampled (file)
{
	if (!file.nonDownsampled)
	{
		parseCSVContent (file);
	}
	return file.nonDownsampled;
}

function getCSVColumnsDownsampled (file)
{
	if (!file.downsampled)
	{
		parseCSVContent (file);
	}
	return file.downsampled;
}

function getCSVColumns (file)
{
	if (!file.columns)
	{
		parseCSVContent (file);
	}
	return file.columns;
}

function getCSV (file)
{
	if (!file.csv)
	{
		parseCSVContent (file);
	}
	return file.csv;
}

function initCompare ()
{
	doc = {
		displayClose: document.getElementById("fileclose"),
		fileName: document.getElementById("filename"),
		fileDisplay: document.getElementById("filedisplay"),
		fileDetails: document.getElementById("filedetails")
	};
	doc.fileDetails.style.display = "none";
	window.onpopstate = parseUrl;
	parseUrl ();
}


document.addEventListener("DOMContentLoaded", initCompare, false);