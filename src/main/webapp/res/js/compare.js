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
var plotFiles = new Array ();
var filesTable = {};
var shownDefault = false;

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

function nextPage (url, replace)
{
    if (replace)
        window.history.replaceState(document.location.href, "", url);
    else
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

function sortTable (plots)
{
	for (var i = 0; i < filesTable.all.length; i++)
	{
		var f = filesTable.all[i];
		var found = false;
		for (var j = 0; j < plots.length; j++)
			if (f.name == plots[j])
			{
				filesTable.plots[f.name] = f;
				found = true;
				break;
			}
		if (found)
			continue;
		if (f.name.endsWith ("png") || f.name.endsWith ("eps"))
			filesTable.pngeps[f.name] = f;
		else if (f.name == "outputs-default-plots.csv" || f.name == "outputs-contents.csv")
			filesTable.defaults[f.name] = f;
		else if (f.name.endsWith ("csv"))
			filesTable.otherCSV[f.name] = f;
        else if (f.name.endsWith ("txt"))
            filesTable.text[f.name] = f;
		else 
			filesTable.other[f.name] = f;
	}
		
	var resortPartially = function (arr, css)
	{
		var cur = keys(arr).sort();
		for (var i = 0; i < cur.length; i++)
		{
			$(arr[cur[i]].row).addClass ("filesTable-" + css);
			filesTable.table.removeChild (arr[cur[i]].row);
			filesTable.table.appendChild (arr[cur[i]].row);
		}
	};
	
	/*
	according to keytask :
	Those CSV files corresponding to plots, in the order given in default-plots.csv
    png & eps files
    Other CSV files corresponding to outputs
    The contents & default-plots files (although don't give buttons to plot these!)
    Text files
    Other files
    */
	resortPartially (filesTable.plots, "plots");
	resortPartially (filesTable.pngeps, "pngeps");
	resortPartially (filesTable.otherCSV, "otherCSV");
	resortPartially (filesTable.defaults, "defaults");
    resortPartially (filesTable.text, "text");
    resortPartially (filesTable.other, "other");
}

function highlightPlots (showDefault)
{
//	console.log (plotDescription);
//	console.log(outputContents);
    // Plot description has fields: Plot title,File name,Data file name,Line style,First variable id,Optional second variable id,Optional key variable id
    // Output contents has fields: Variable id,Variable name,Units,Number of dimensions,File name,Type,Dimensions
	for (var i = 1; i < plotDescription.length; i++)
	{
		if (plotDescription[i].length < 2)
			continue;
		//console.log (plotDescription[i][2]);
		var row = document.getElementById ("filerow-" + plotDescription[i][2].hashCode ());
		if (row)
		{
			row.setAttribute("class", "highlight-plot");
			if (i == 1 && showDefault && !shownDefault)
			{
				var viz = document.getElementById ("filerow-" + plotDescription[i][2].hashCode () + "-viz-displayPlotFlot");
				if (viz)
				{
					shownDefault = true;
					nextPage(viz.href, true); // 'Invisible' redirect
				}
			}
		}
		
		
		if (files[plotDescription[i][2].hashCode ()])
		{
			//console.log (files[plotDescription[i][2].hashCode ()]);
			
			var f = files[plotDescription[i][2].hashCode ()];
			
			// Find the plot x and y object names and units from the output contents file.
			for (var output_idx = 0; output_idx < outputContents.length; output_idx++)
			{
				if (plotDescription[i][4] == outputContents[output_idx][0])
				{
					f.xAxes = outputContents[output_idx][1] + ' (' + outputContents[output_idx][2] + ')';
				}
				if (plotDescription[i][5] == outputContents[output_idx][0])
				{
					f.yAxes = outputContents[output_idx][1] + ' (' + outputContents[output_idx][2] + ')';
				}
                if (plotDescription[i].length > 6 && plotDescription[i][6] == outputContents[output_idx][0])
                {
//                    console.log("Key exists : " + outputContents[output_idx][0]);
//                    console.log(f);
                    // TODO: This may not handle keys differing between experiments. Does this matter?
                    for (var ent_idx=0; ent_idx<f.entities.length; ent_idx++)
                    {
                        var ent_f = f.entities[ent_idx].entityFileLink;
                        ent_f.keyId = outputContents[output_idx][0];
                        ent_f.keyName = outputContents[output_idx][1];
                        ent_f.keyUnits = outputContents[output_idx][2];
                        ent_f.keyFile = files[outputContents[output_idx][4].hashCode()].entities[ent_idx].entityFileLink;
                    }
                }
			}
			f.title = plotDescription[i][0];
			f.linestyle = plotDescription[i][3];
			
			plotFiles.push (plotDescription[i][2]);
		}
	}
	sortTable (plotFiles);
}

function parseOutputContents (file, showDefault)
{
    outputContents = null; // Note that there is one to parse
    
	var goForIt = {
		getContentsCallback : function (succ)
		{
			if (succ)
			{
				parseCsvRaw(file);
				outputContents = file.csv;
				if (plotDescription)
                    highlightPlots (showDefault);
			}
		}
	};
	getFileContent (file, goForIt);
	
	return null;
}

function parsePlotDescription (file, showDefault)
{
    plotDescription = null; // Note that there is one to parse
	
	var goForIt = {
		getContentsCallback : function (succ)
		{
			if (succ)
			{
			    parseCsvRaw(file);
				plotDescription = file.csv;
				if (outputContents)
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
					
					/*files[sig]*/
				}
				if (entityObj[i].files[j].name.toLowerCase () == "outputs-default-plots.csv")
					parsePlotDescription (entityObj[i].files[j], !(fileName && pluginName));
				if (entityObj[i].files[j].name.toLowerCase () == "outputs-contents.csv")
					parseOutputContents (entityObj[i].files[j], !(fileName && pluginName));
				
				files[sig].entities.push ({entityLink: entityObj[i], entityFileLink: entityObj[i].files[j]});
			}
	}
	
	// Create a drop-down box that allows display of/navigate to experiments being compared
	var entitiesToCompare = document.getElementById("entitiesToCompare");
	removeChildren (entitiesToCompare);
	var form = document.createElement("form");
	entitiesToCompare.appendChild(form);
	var select_box = document.createElement("select");
	select_box.name = "experiment_box";
	select_box.id = "exptSelect";
	var default_option = document.createElement("option");
	default_option.selected = true;
	default_option.value = document.location.href;
	default_option.innerHTML = "Click to view, select to show a single experiment";
	select_box.onchange = function(){sel=document.getElementById("exptSelect"); console.log(sel); document.location.href = sel.options[sel.selectedIndex].value;};
	select_box.appendChild(default_option);
	for (var entity in entities)
	{
		var option = document.createElement("option");
		option.value = contextPath + "/"+entityType+"/" + convertForURL (entities[entity].name) + "/" + entities[entity].entityId + "/" + convertForURL (entities[entity].created) + "/" + entities[entity].id;
		option.innerHTML = entities[entity].name;
		select_box.appendChild(option);
	}	
	form.innerHTML = "Experiments selected for comparison: ";
	form.appendChild(select_box);
	
	buildSite ();
}


function buildSite ()
{
	var filestable = document.getElementById("filestable");
	filesTable = {};
	filesTable.table = filestable;
	filesTable.plots = {};
	filesTable.pngeps = {};
	filesTable.otherCSV = {};
	filesTable.defaults = {};
	filesTable.text = {};
	filesTable.other = {};
	filesTable.all = new Array ();
	
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
		
		filesTable.all.push ({
			name: curFileName,
			row: tr
		});
		
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
	if (outputContents === null || plotDescription === null)
    {
	    // Try again in 0.1s, by which time hopefully they have been parsed
	    console.log("Waiting for metadata to be parsed.");
        window.setTimeout(function(){displayFile(id, pluginName)}, 100);
        return;
    }
	var f = files[id];
	if (!f)
	{
		addNotification ("no such file", "error");
		return;
	}
	doc.fileName.innerHTML = f.name;
	
	if (!f.div[pluginName])
	{
//	    console.log("Creating visualizer");
		f.div[pluginName] = document.createElement("div");
		f.viz[pluginName] = visualizers[pluginName].setUpComparision (f, f.div[pluginName]);
//		console.log(f);
	}
//	else
//	{
//	    console.log("Reusing vis");
//	    console.log(f);
//	}
    removeChildren (doc.fileDisplay);
	doc.fileDisplay.appendChild (f.div[pluginName]);
    f.viz[pluginName].show ();

    // Show parent div of the file display, and scroll there
	doc.fileDetails.style.display = "block";
	var pos = getPos (doc.fileDetails);
	window.scrollTo(pos.xPos, pos.yPos);
}

function handleReq ()
{
	if (fileName && pluginName && gotInfos)
	{
		displayFile (fileName, pluginName);
		doc.displayClose.href = basicurl;
	}
	else
	{
		doc.fileDetails.style.display = "none";
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
    
    xmlhttp.open("POST", document.location.href, true);
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
	parseCsvRaw(file);
	var csv = file.csv;

	file.columns = [];
	var dropDist = [];
	for (var i = 0; i < csv[0].length; i++)
	{
	        var min = Math.pow(2, 32);
        	var max = -min;
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
                dropDist.push ( (max - min) / 500.0 );
                //console.log( "scale for line " + i + ": " + min + ":" + dropDist[dropDist.length-1] + ":" + max);
	}
	file.nonDownsampled = [];
	file.downsampled = [];
	for (var i = 1; i < file.columns.length; i++)
	{
		file.downsampled[i] = [];
		file.nonDownsampled[i] = [];
		file.downsampled[i][0] = {x : file.columns[0][0], y : file.columns[i][0]};
		file.nonDownsampled[i][0] = {x : file.columns[0][0], y : file.columns[i][0]};
        var last_j = file.columns[i].length - 1;
        for (var j = 1; j <= last_j; j++)
        {
            file.nonDownsampled[i].push ({x : file.columns[0][j], y : file.columns[i][j]});
            var last = file.downsampled[i][file.downsampled[i].length - 1]['y'];
            var cur = file.columns[i][j];
            var next = file.columns[i][j + 1];
            if (j == last_j || maxDist (last, cur, next) > dropDist[i] || (cur < last && cur < next) || (cur > last && cur > next))
                file.downsampled[i].push ({x : file.columns[0][j], y : file.columns[i][j]});
        }
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
	
	// Prevent redirection to the default plot when we close it
	doc.displayClose.addEventListener("click", function (ev) {
		if (ev.which == 1)
		{
			ev.preventDefault();
			doc.fileDetails.style.display = "none";
			shownDefault = true;
			nextPage (doc.displayClose.href);
		}
    }, true);
	
	window.onpopstate = parseUrl;
	parseUrl ();
}


document.addEventListener("DOMContentLoaded", initCompare, false);