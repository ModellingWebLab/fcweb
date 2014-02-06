var versions = new Array ();
var files = new Array ();
var doc;
var basicurl;
var entityType;
var entityId;
var curVersion = null;
var converter = new Showdown.converter();
var filesTable = {};

var visualizers = {};

function parseUrl (href)
{
	var t = href.split ("/");
	for (var i = 0; i < t.length; i++)
	{
		if ("/" + t[i] == contextPath && i + 3 < t.length && t[i+1] == "model")
		{
			basicurl = t.slice (0, i + 4).join ("/") + "/";
			entityType = "model";
			entityId = t[i+3];
			return t.slice (i + 4);
		}
		if ("/" + t[i] == contextPath && i + 3 < t.length && t[i+1] == "protocol")
		{
			basicurl = t.slice (0, i + 4).join ("/") + "/";
			entityType = "protocol";
			entityId = t[i+3];
			return t.slice (i + 4);
		}
		if ("/" + t[i] == contextPath && i + 3 < t.length && t[i+1] == "experiment")
		{
			basicurl = t.slice (0, i + 4).join ("/") + "/";
			entityType = "experiment";
			entityId = t[i+3];
			return t.slice (i + 4);
		}
	}
	return null;
}

function getCurVersionId (url)
{
	if (url.length < 2)
		return null;
	return url[1];
}

function getCurFileId (url)
{
	if (url.length < 4)
		return null;
	return url[3];
}

function getCurPluginName (url)
{
	if (url.length < 5)
		return null;
	return url[4];
}

function updateVisibility (jsonObject, actionIndicator)
{
	actionIndicator.innerHTML = "<img src='"+contextPath+"/res/img/loading2-new.gif' alt='loading' />";
	
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
    
    xmlhttp.open ("POST", document.location.href, true);
    xmlhttp.setRequestHeader ("Content-type", "application/json");

    xmlhttp.onreadystatechange = function()
    {
        if(xmlhttp.readyState != 4)
        	return;
        
    	var json = JSON.parse(xmlhttp.responseText);
    	//console.log (json);
    	displayNotifications (json);
    	
        if(xmlhttp.status == 200)
        {
        	if (json.updateVisibility)
        	{
	        	var msg = json.updateVisibility.responseText;
	        	if (json.updateVisibility.response)
	        	{
	        		actionIndicator.innerHTML = "<img src='"+contextPath+"/res/img/check.png' alt='valid' /> " + msg;
	        	}
	        	else
	        		actionIndicator.innerHTML = "<img src='"+contextPath+"/res/img/failed.png' alt='invalid' /> " + msg;
        	}
        }
        else
        {
        	actionIndicator.innerHTML = "<img src='"+contextPath+"/res/img/failed.png' alt='error' /> sorry, serverside error occurred.";
        }
    };
    xmlhttp.send (JSON.stringify (jsonObject));
}

function deleteEntity (jsonObject)
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
    
    xmlhttp.open ("POST", document.location.href, true);
    xmlhttp.setRequestHeader ("Content-type", "application/json");

    xmlhttp.onreadystatechange = function()
    {
        if(xmlhttp.readyState != 4)
        	return;
        
    	var json = JSON.parse(xmlhttp.responseText);
    	//console.log (json);
    	displayNotifications (json);
    	
        if(xmlhttp.status == 200)
        {
        	if (json.deleteVersion)
        	{
	        	var msg = json.deleteVersion.responseText;
	        	if (json.deleteVersion.response)
	        	{
	        		// go back to version table
	        		document.location.href = basicurl;
	        	}
	        	else
	            	alert(msg);
        	}
        	if (json.deleteEntity)
        	{
	        	var msg = json.deleteEntity.responseText;
	        	if (json.deleteEntity.response)
	        	{
	        		// go back to version table
	        		document.location.href = basicurl;
	        	}
	        	else
	            	alert(msg);
        	}
        }
        else
        {
        	alert("sorry, serverside error occurred.");
        }
    };
    xmlhttp.send (JSON.stringify (jsonObject));
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
    Other files 
    */
	resortPartially (filesTable.plots, "plots");
	resortPartially (filesTable.pngeps, "pngeps");
	resortPartially (filesTable.otherCSV, "otherCSV");
	resortPartially (filesTable.defaults, "defaults");
	resortPartially (filesTable.other, "other");
}


function highlightPlots (version, showDefault)
{
	//console.log (plotDescription);
	// Plot description has fields: Plot title,File name,Data file name,Line style,First variable id,Optional second variable id,Optional key variable id
	// Output contents has fields: Variable id,Variable name,Units,Number of dimensions,File name,Type,Dimensions
	var plotDescription = version.plotDescription;
	var outputContents = version.outputContents;
	var plots = new Array ();
	for (var i = 1; i < plotDescription.length; i++)
	{
		if (plotDescription[i].length < 2)
			continue;
		//console.log (plotDescription[i][2]);
		var row = document.getElementById ("filerow-" + plotDescription[i][2].hashCode ());
		if (row)
		{
			row.setAttribute("class", "highlight-plot");
			if (i == 1 && showDefault)
			{
				var viz = document.getElementById ("filerow-" + plotDescription[i][2] + "-viz-displayPlotFlot");
				if (viz)
					viz.click();
			}
		}

		//console.log ("files: ")
		//console.log (version.files);
		for (var f = 0; f < version.files.length; f++)
		{
			if (files[version.files[f]].name == plotDescription[i][2])
			{
				// Find the plot x and y object names and units from the output contents file.
				for (var output_idx = 0; output_idx < outputContents.length; output_idx++)
				{
					if (plotDescription[i][4] == outputContents[output_idx][0])
					{
						files[version.files[f]].xAxes = outputContents[output_idx][1] + ' (' + outputContents[output_idx][2] + ')';
					}
					if (plotDescription[i][5] == outputContents[output_idx][0])
					{
						files[version.files[f]].yAxes = outputContents[output_idx][1] + ' (' + outputContents[output_idx][2] + ')';
					}
					if (plotDescription[i].length > 6 && plotDescription[i][6] == outputContents[output_idx][0])
					{
						files[version.files[f]].keyId = outputContents[output_idx][0];
						files[version.files[f]].keyName = outputContents[output_idx][1];
						files[version.files[f]].keyUnits = outputContents[output_idx][2];
						for (var fkey=0; fkey < version.files.length; fkey++)
						{
							if (files[version.files[fkey]].name == outputContents[output_idx][4])
							{
								files[version.files[f]].keyFile = files[version.files[fkey]];
							}
						}
					}
				}
				files[version.files[f]].title = plotDescription[i][0];
				files[version.files[f]].linestyle = plotDescription[i][3];
			}
			//console.log ("file: ")
			//console.log (files[version.files[f]]);
		}
		
		plots.push (plotDescription[i][2]);
	}
	sortTable (plots);
}

function parseOutputContents (file, version)
{
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
				
				version.outputContents = csv;			
			}
		}
	};
	getFileContent (file, goForIt);
	
	return null;
}

function parsePlotDescription (file, version, showDefault)
{
	if (file.plotDescription)
		return converter.makeHtml (file.contents);
	
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
				
				version.plotDescription = csv;
				highlightPlots (version, showDefault);
				
			}
		}
	};
	getFileContent (file, goForIt);
	
	return null;
}

function parseReadme (file, version)
{
	if (file.contents)
		return converter.makeHtml (file.contents);
	
	var goForIt = {
			getContentsCallback : function (succ)
			{
				if (succ)
				{
					version.readme = converter.makeHtml (file.contents);
					doc.version.readme.innerHTML = version.readme;
				}
			}
	};
	getFileContent (file, goForIt);
	
	return null;
}

function displayVersion (id, showDefault)
{
	var v = versions[id];
	if (!v)
	{
		addNotification ("no such version", "error");
		return;
	}
	//console.log(v);
	var dv = doc.version;
	dv.name.innerHTML = v.name + " ";
	
	if (entityType != "experiment" && ROLE.isAllowedToCreateNewExperiment)
	{
		/*var createExpLink = document.createElement("a");
		var createExpImg = document.createElement("img");
		createExpImg.src = contextPath + "/res/img/create-experiment-small.png";
		createExpImg.alt = "create experiment from this " + entityType;
		createExpLink.addEventListener("click", function (ev) {
			if (entityType == "model")
				newExperimentModel (v.id, v.name);
			else
				newExperimentProtocol (v.id, v.name);
	    	}, true);
		createExpLink.appendChild (createExpImg);
		createExpLink.setAttribute ("class", "pointer");
		dv.name.appendChild (createExpLink);
		
		dv.name.appendChild (document.createTextNode (" "));*/
		
		var createBatchLink = document.createElement("a");
		var createBatchImg = document.createElement("img");
		createBatchImg.src = contextPath + "/res/img/batch.png";
		createBatchImg.alt = "create batch jobs from this " + entityType;
		createBatchLink.appendChild (createBatchImg);
		createBatchLink.href = contextPath + "/batch/" + entityType + "/" + convertForURL (v.name) + "/" + v.id;
		dv.name.appendChild (createBatchLink);
	}
	
	if (dv.visibility)
	{
		//var new_element = dv.visibility.cloneNode(true);
		//dv.visibility.parentNode.replaceChild (new_element, dv.visibility);
		dv.visibility = removeListeners (dv.visibility);//new_element;
		
		document.getElementById("visibility-" + v.visibility).selected=true;
		
		dv.visibility.addEventListener("change", function () {
			/*console.log (v.id);
			console.log (dv.visibility.options[dv.visibility.selectedIndex].value);*/
			updateVisibility ({
		    	task: "updateVisibility",
		    	version: v.id,
		    	visibility: dv.visibility.options[dv.visibility.selectedIndex].value
		    }, dv.visibilityAction);
	    }, true);
/*
		if (dv.visibility)
		{
			dv.visibility.addEventListener("change", function () {
				console.log (curVersionId);
				console.log (dv.visibility.options[dv.visibility.selectedIndex].value);
		    }, true);
		}*/
	}
	
	if (dv.deleteBtn)
	{
		//var new_element = dv.deleteBtn.cloneNode(true);
		//dv.deleteBtn.parentNode.replaceChild (new_element, dv.deleteBtn);
		dv.deleteBtn = removeListeners (dv.deleteBtn); //new_element;
		
		dv.deleteBtn.addEventListener("click", function () {
			if (confirm("Are you sure to delete this version? (including all files and experiments associated to it)"))
			{
				deleteEntity ({
					task: "deleteVersion",
			    	version: v.id
				});
			}
				//console.log ("deleting " + v.id);
			/*else
				console.log ("not deleting " + v.id);*/
		});
	}
	
	dv.author.innerHTML = v.author;
	dv.time.setAttribute ("datetime", v.created);
	dv.time.innerHTML = beautifyTimeStamp (v.created);
	
	removeChildren (dv.filestable);
	filesTable = {};
	filesTable.table = dv.filestable;
	filesTable.plots = {};
	filesTable.pngeps = {};
	filesTable.otherCSV = {};
	filesTable.defaults = {};
	filesTable.other = {};
	filesTable.all = new Array ();

	var tr = document.createElement("tr");
	var td = document.createElement("th");
	td.appendChild(document.createTextNode ("Name"));
	tr.appendChild(td);
	td = document.createElement("th");
	td.appendChild(document.createTextNode ("Type"));
	tr.appendChild(td);
	td = document.createElement("th");
	td.colSpan = 2;
	td.appendChild(document.createTextNode ("Size"));
	tr.appendChild(td);
	td = document.createElement("th");
	td.appendChild(document.createTextNode ("Actions"));
	tr.appendChild(td);
	dv.filestable.appendChild(tr);
	
	for (var i = 0; i < v.files.length; i++)
	{
		var file = files[v.files[i]];
		tr = document.createElement("tr");
		tr.setAttribute("id", "filerow-" + file.name.hashCode ());
		if (file.masterFile)
			tr.setAttribute("class", "masterFile");
		td = document.createElement("td");
		td.appendChild(document.createTextNode (file.name));
		tr.appendChild(td);
		td = document.createElement("td");
		td.appendChild(document.createTextNode (file.type.replace (/^.*identifiers.org\/combine.specifications\//,"")));
		tr.appendChild(td);
		
		var fsize = humanReadableBytes (file.size).split (" ");
		td = document.createElement("td");
		td.appendChild(document.createTextNode (fsize[0]));
		td.setAttribute("class", "right");
		tr.appendChild(td);
		td = document.createElement("td");
		td.appendChild(document.createTextNode (fsize[1]));
		tr.appendChild(td);
		td = document.createElement("td");
		
		if (!v.readme && file.name.toLowerCase () == "readme.md")
			v.readme = parseReadme (file, v);
		
		if (!v.plotDescription && file.name.toLowerCase () == "outputs-default-plots.csv")
			parsePlotDescription (file, v, showDefault);
		
		if (!v.outputContents && file.name.toLowerCase () == "outputs-contents.csv")
			parseOutputContents (file, v);
		

		filesTable.all.push ({
			name: file.name,
			row: tr
		});
		
		
		
		for (var vi in visualizers)
		{
			var viz = visualizers[vi];
			if (!viz.canRead (file))
				continue;
			var a = document.createElement("a");
			a.setAttribute("id", "filerow-" + file.name + "-viz-" + viz.getName ());
			a.href = basicurl + convertForURL (v.name) + "/" + v.id + "/" + convertForURL (file.name) + "/" + file.id + "/" + vi;
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
		
		
		
		var a = document.createElement("a");
		a.href = file.url;
		img = document.createElement("img");
		img.src = contextPath + "/res/img/document-save-5.png";
		img.alt = "download document";
		img.title = "download document";
		a.appendChild(img);//document.createTextNode ("download"));
		td.appendChild(a);
		tr.appendChild(td);
		dv.filestable.appendChild(tr);
		dv.archivelink.href = contextPath + "/download/" + entityType.charAt(0) + "/" + convertForURL (v.name) + "/" + v.id + "/a/archive";
		
	}
		
		

	if (v.experiments.length > 0)
	{
		removeChildren (dv.experimentpartners);
		
		var compares = new Array();
		var compareType = "";
		
		var ul = document.createElement ("ul");
		for (var i = 0; i < v.experiments.length; i++)
		{
			
			var li = document.createElement ("li");
			var chk = document.createElement ("input");
			chk.type = "checkbox";
			chk.value = v.experiments[i].id;
			compares.push (chk);
			var a = document.createElement ("a");
			if (entityType == "protocol")
			{
			    compareType = "model";
				//console.log ("protoc");
				//console.log (v.experiments[i].model);
				a.appendChild(document.createTextNode(v.experiments[i].model.name + " @ " + v.experiments[i].model.version));
			}
			else
			{
			    compareType = "protocol";
				a.appendChild(document.createTextNode(v.experiments[i].protocol.name + " @ " + v.experiments[i].protocol.version));
			}
			a.href = contextPath + "/experiment/" + v.experiments[i].model.id + v.experiments[i].protocol.id + "/" + v.experiments[i].id + "/latest";
			li.appendChild (chk);
			li.appendChild (a);
			ul.appendChild (li);
		}

		dv.experimentSelAll = removeListeners (dv.experimentSelAll);
		dv.experimentSelNone = removeListeners (dv.experimentSelNone);
		dv.experimentcompare = removeListeners (dv.experimentcompare);
		
		dv.experimentSelAll.addEventListener("click", function () {
			for (var i = 0; i < compares.length; i++)
				compares[i].checked = true;
		});
		dv.experimentSelNone.addEventListener("click", function () {
			for (var i = 0; i < compares.length; i++)
				compares[i].checked = false;
		});
		dv.experimentcompare.addEventListener("click", function () {
			var url = "";
			for (var i = 0; i < compares.length; i++)
				if (compares[i].checked)
					url += compares[i].value + "/";
			if (url)
			    document.location = contextPath + "/compare/e/" + url;
			else
			    window.alert("You need to select some " + compareType + "s to compare.");
		});
		
		if (dv.compareAll)
		{
			dv.compareAll = removeListeners(dv.compareAll);
			if (compares.length > 0)
			{
				dv.compareAll.addEventListener("click", function () {
					var url = "";
					for (var i = 0; i < compares.length; i++)
						url += compares[i].value + "/";
				    document.location = contextPath + "/compare/e/" + url;
				});
				dv.compareAll.style.display = "block";
			}
			else
			{
				dv.compareAll.style.display = "none";
			}
		}
		
		dv.experimentpartners.appendChild (ul);
		//dv.experimentlist.style.display = "block";
		dv.switcher.style.display = "block";
		//dv.details.style.display = "none";
	}
	else
	{
		dv.switcher.style.display = "none";
		dv.experimentpartners.style.display = "none";
		dv.details.style.display = "block";
	}
	
	removeChildren (dv.readme);
	if (v.readme)
		dv.readme.innerHTML = v.readme;
	if (v.plotDescription)
		highlightPlots (v, showDefault);
	
	
	doc.entity.details.style.display = "none";
	doc.entity.version.style.display = "block";

	doc.version.files.style.display = "block";
	//doc.version.filedetails.style.display = "none";
	// update address bar
	
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
	
	file.csv = csv;
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


function registerFileDisplayer (elem)
{
	elem.addEventListener("click", function (ev) {
		if (ev.which == 1)
		{
			ev.preventDefault();
			//doc.file.close.href = closeurl;
			nextPage (elem.href);
			//displayFile (fileid);
		}
    	}, true);
}

function registerVersionDisplayer (elem)
{
	elem.addEventListener("click", function (ev) {
		if (ev.which == 1)
		{
			ev.preventDefault();
			//doc.file.close.href = closeurl;
			nextPage (elem.href);
			//displayFile (fileid);
		}
    	}, true);
}

function updateVersion (rv)
{
	var v = versions[rv.id];
	if (!v)
	{
		v = new Array ();
		versions[rv.id] = v;
	}
	
	v.name = rv.version;
	v.author = rv.author;
	v.created = rv.created;
	v.visibility = rv.visibility;
	v.id = rv.id;
	v.readme = null;
	v.files = new Array ();
	for (var i = 0; i < rv.files.length; i++)
	{
		updateFile (rv.files[i], v);
		v.files.push (rv.files[i].id);
	}
	v.experiments = new Array ();
	if (rv.experiments)
		for (var i = 0; i < rv.experiments.length; i++)
		{
			v.experiments.push ({
				model: rv.experiments[i].model,
				protocol: rv.experiments[i].protocol,
				id: rv.experiments[i].id
			});
		}
	versions[v.id] = v;
}

function getFileContent (file, succ)
{
	// TODO: loading indicator.. so the user knows that we are doing something
    
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

function updateFile (rf, v)
{
	var f = files[rf.id];
	if (!f)
	{
		f = new Array ();
		files[rf.id] = f;
	}
	
	f.id = rf.id;
	f.created = rf.created;
	f.type = rf.filetype;
	f.author = rf.author;
	f.name = rf.name;
	f.masterFile = rf.masterFile;
	f.size = rf.size;
	f.url = contextPath + "/download/" + entityType.charAt(0) + "/" + convertForURL (v.name) + "/" + v.id + "/" + f.id + "/" + convertForURL (f.name);
	f.div = {};
	f.viz = {};
	f.contents = null;
	f.getContents = function (callBack)
	{
		if (!f.contents)
		{
			//console.log ("missing file contents. calling for: " + f.id);
			getFileContent (f, callBack);
		}
		else
			getFileContent (f, callBack);
	};
}

function displayFile (id, pluginName)
{
	var f = files[id];
	if (!f)
	{
		//console.log (id);
		//console.log (files);
		addNotification ("no such file", "error");
		return;
	}
    var df = doc.file;
	df.name.innerHTML = f.name;
	df.time.setAttribute ("datetime", f.created);
	df.time.innerHTML = beautifyTimeStamp (f.created);
	df.author.innerHTML = f.author;
	
	if (!f.div[pluginName])
	{
		f.div[pluginName] = document.createElement("div");
		f.viz[pluginName] = visualizers[pluginName].setUp (f, f.div[pluginName]);
	}
	
	f.viz[pluginName].show ();
	
	removeChildren (df.display);
	df.display.appendChild (f.div[pluginName]);
	
	// doc.version.files.style.display = "none";
	doc.version.filedetails.style.display = "block";
	
	var pos = getPos (doc.version.filedetails);
	window.scrollTo(pos.xPos, pos.yPos);
}

function requestInformation (jsonObject, onSuccess)
{
	// TODO: loading indicator.. so the user knows that we are doing something
    
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
        
        //console.log (xmlhttp.responseText);
    	var json = JSON.parse(xmlhttp.responseText);
    	//console.log (json);
    	displayNotifications (json);
    	
        if(xmlhttp.status == 200)
        {
        	
        	if (json.version)
        	{
        		var rv = json.version;
        		
        		updateVersion (rv);
        		onSuccess ();
        	}
        }
    };
    xmlhttp.send(JSON.stringify(jsonObject));
}

function nextPage (url)
{
	//if (//url)
	window.history.pushState(document.location.href, "", url);
	
	render ();
}

function render ()
{
	var url = parseUrl (document.location.href);
	var curVersionId = getCurVersionId (url);
	
	//console.log ("curVersionId " + curVersionId);
	if (curVersionId)
	{
		var curFileId = getCurFileId (url);
		var pluginName = getCurPluginName (url);

		//console.log ("curFileId  " + curFileId);
		//console.log ("pluginName " + pluginName);
		
		var v = versions[curVersionId];
		if (!v)
		{
			//console.log ("missing version. calling for: " + curVersionId);
			// request info about version only
			requestInformation ({
		    	task: "getInfo",
		    	version: curVersionId
			}, render);
			return;
		}
		else if (v != curVersion)
		{
			displayVersion (curVersionId, !(curFileId && pluginName));
			curVersion = v;
		}
		
		
		if (curFileId && pluginName)
		{
			displayFile (curFileId, pluginName);
			doc.file.close.href = basicurl + convertForURL (v.name) + "/" + v.id + "/";
		}
		else
			doc.version.filedetails.style.display = "none";
			
	}
	else
	{
		if (url.length > 0 && url[0] == "latest")
		{
			//document.getElementById("entityversionlist")
			$(".entityversionlink").each(function (){nextPage ($(this).attr('href')); return false;});
		}
		doc.entity.version.style.display = "none";
		doc.entity.details.style.display = "block";
		curVersion = null;
	}
}

function initModel ()
{
	
	doc = {
			entity : {
				details : document.getElementById("entitydetails"),
				version : document.getElementById("entityversion"),
				deleteBtn : document.getElementById("deleteEntity")
			},
			version : {
				close : document.getElementById("entityversionclose"),
				name : document.getElementById("entityversionname"),
				time : document.getElementById("entityversiontime"),
				author : document.getElementById("entityversionauthor"),
				details : document.getElementById("entityversiondetails"),
				files : document.getElementById("entityversionfiles"),
				filestable : document.getElementById("entityversionfilestable"),
				readme : document.getElementById("entityversionfilesreadme"),
				archivelink : document.getElementById("downloadArchive"),
				filedetails : document.getElementById("entityversionfiledetails"),
				experimentlist: document.getElementById("entityexperimentlist"),
				experimentpartners: document.getElementById("entityexperimentlistpartners"),
				experimentSelAll: document.getElementById("entityexperimentlistpartnersactall"),
				experimentSelNone: document.getElementById("entityexperimentlistpartnersactnone"),
				experimentcompare: document.getElementById("entityexperimentlistpartnersactcompare"),
				compareAll: document.getElementById("compare-all-models"),
				switcher: document.getElementById("experiment-files-switcher"),
				visibility: document.getElementById("versionVisibility"),
				visibilityAction : document.getElementById("versionVisibilityAction"),
				deleteBtn : document.getElementById("deleteVersion")
			},
			file: {
				close : document.getElementById("entityversionfileclose"),
				name : document.getElementById("entityversionfilename"),
				time : document.getElementById("entityversionfiletime"),
				author : document.getElementById("entityversionfileauthor"),
				display : document.getElementById("entityversionfiledisplay")
			}
	};
	
	window.onpopstate = render;
	render ();
	
	document.getElementById("experiment-files-switcher-exp").addEventListener("click", function (ev) {
		doc.version.details.style.display = "none";
		doc.version.experimentlist.style.display = "block";
	}, false);
	
	document.getElementById("experiment-files-switcher-files").addEventListener("click", function (ev) {
		doc.version.experimentlist.style.display = "none";
		doc.version.details.style.display = "block";
	}, false);
	
	
	doc.version.close.href = basicurl;
	doc.version.close.addEventListener("click", function (ev) {
		if (ev.which == 1)
		{
			ev.preventDefault();
			curVersion = null;
			doc.entity.version.style.display = "none";
			doc.entity.details.style.display = "block";
			nextPage (doc.version.close.href);
		}
    }, true);
		
	doc.file.close.addEventListener("click", function (ev) {
		if (ev.which == 1)
		{
			ev.preventDefault();
			doc.version.filedetails.style.display = "none";
			doc.version.files.style.display = "block";
			nextPage (doc.file.close.href);
		}
    }, true);

	var list = document.getElementById("entityversionlist");
	if (list)
		sortChildrenByAttribute (list, true, "title");
	
	
	var resubmit = document.getElementById("rerunExperiment");
	var resubmitAction = document.getElementById("rerunExperimentAction");
	if (resubmit && resubmitAction)
	{
		resubmit.addEventListener("click", function (ev) {
			batchProcessing ({
				batchTasks : [{
					experiment : entityId
				}],
				force: true
			},resubmitAction);
		});
	}
	
	// search for special links
	var elems = document.getElementsByTagName('a');
    for (var i = 0; i < elems.length; i++)
    {
    	var classes = ' ' + elems[i].className + ' ';
        if(classes.indexOf(' entityversionlink ') > -1)
        {
        	// links to see the model details
        	//var link = elems[i].href;
        	registerVersionDisplayer (elems[i]);
        	/*elems[i].addEventListener("click", function (ev) {
        		if (ev.which == 1)
        		{
        			ev.preventDefault();
        			// set new url
        			// call action
        			//nextPage (elems[i].href);
        		}
        	}, true);
        	*/
            //elems[i].href = "";
            //console.log ("test");
        }

        if(classes.indexOf(' entityversionfilelink ') > -1)
        {
        	// links to see the file details
        }
        
    }
    

	if (doc.entity.deleteBtn)
	{
		doc.entity.deleteBtn.addEventListener("click", function () {
			if (confirm("Are you sure to delete this entity? (including all versions, files, and experiments associated to it)"))
			{
				deleteEntity ({
					task: "deleteEntity",
			    	entity: entityId
				});
			}
				//console.log ("deleting " + v.id);
			/*else
				console.log ("not deleting " + v.id);*/
		});
	}
	

	$(".deleteVersionLink").click (function () {
		if (confirm("Are you sure to delete this version? (including all files and experiments associated to it)"))
		{
			deleteEntity ({
				task: "deleteVersion",
		    	version: $(this).attr("id").replace("deleteVersion-", "")
			});
		}
	});
    
}

document.addEventListener("DOMContentLoaded", initModel, false);
