var versions = new Array ();
var files = new Array ();
var doc;
var basicurl;
var entityType;
var entityId;

var visualizers = {};

function parseUrl (href)
{
	var t = href.split ("/");
	for (var i = 0; i < t.length; i++)
	{
		if ("/" + t[i] == contextPath && i + 4 < t.length && t[i+1] == "model")
		{
			basicurl = t.slice (0, i + 4).join ("/") + "/";
			entityType = "model";
			entityId = t[i+3];
			return t.slice (i + 4);
		}
		if ("/" + t[i] == contextPath && i + 4 < t.length && t[i+1] == "protocol")
		{
			basicurl = t.slice (0, i + 4).join ("/") + "/";
			entityType = "protocol";
			entityId = t[i+3];
			return t.slice (i + 4);
		}
		if ("/" + t[i] == contextPath && i + 4 < t.length && t[i+1] == "experiment")
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

function displayVersion (id)
{
	var v = versions[id];
	if (!v)
	{
		addNotification ("no such version", "error");
		return;
	}
    var dv = doc.version;
	dv.name.innerHTML = v.name + " ";
	
	if (entityType != "experiment" && ROLE.isAllowedToCreateNewExperiment)
	{
		var createExpLink = document.createElement("a");
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
		
		dv.name.appendChild (document.createTextNode (" "));
		
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
		var new_element = dv.visibility.cloneNode(true);
		dv.visibility.parentNode.replaceChild (new_element, dv.visibility);
		dv.visibility = new_element;
		
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
		var new_element = dv.deleteBtn.cloneNode(true);
		dv.deleteBtn.parentNode.replaceChild (new_element, dv.deleteBtn);
		dv.deleteBtn = new_element;
		
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
		
		
		for (var vi in visualizers)
		{
			var viz = visualizers[vi];
			if (!viz.canRead (file))
				continue;
			var a = document.createElement("a");
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
		removeChildren (dv.experimentlist);
		var ul = document.createElement ("ul");
		for (var i = 0; i < v.experiments.length; i++)
		{
			
			var li = document.createElement ("li");
			var a = document.createElement ("a");
			if (entityType == "protocol")
			{
				//console.log ("protoc");
				console.log (v.experiments[i].model);
				a.appendChild(document.createTextNode(v.experiments[i].model.name + " @ " + v.experiments[i].model.version));
			}
			else
			{
				a.appendChild(document.createTextNode(v.experiments[i].protocol.name + " @ " + v.experiments[i].protocol.version));
			}
			a.href = contextPath + "/experiment/" + v.experiments[i].model.id + v.experiments[i].protocol.id + "/" + v.experiments[i].id;
			li.appendChild (a);
			ul.appendChild (li);
		}
		dv.experimentlist.appendChild (ul);
		//dv.experimentlist.style.display = "block";
		dv.switcher.style.display = "block";
		//dv.details.style.display = "none";
	}
	else
	{
		dv.switcher.style.display = "none";
		dv.experimentlist.style.display = "none";
		dv.details.style.display = "block";
	}
	
	
	doc.entity.details.style.display = "none";
	doc.entity.version.style.display = "block";

	doc.version.files.style.display = "block";
	doc.version.filedetails.style.display = "none";
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
	file.downsampled = [];
	for (var i = 1; i < file.columns.length; i++)
	{
		file.downsampled[i] = [];
		//file.downsampled[i][0] = file.columns[i][0];
		file.downsampled[i][0] = {x : file.columns[0][0], y : file.columns[i][0]};
		for (var j = 1; j < file.columns[i].length - 1; j++)
		{
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
	
	file.csv = csv;
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
	
	doc.version.files.style.display = "none";
	doc.version.filedetails.style.display = "block";
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
    
    xmlhttp.open("POST", '', true);
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
	//console.log (basicurl);
	//console.log (url);
	//console.log ("entityId: " + entityId);
	var curVersionId = getCurVersionId (url);
	
	if (curVersionId)
	{
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
		else
			displayVersion (curVersionId);
		
		
		var curFileId = getCurFileId (url);
		var pluginName = getCurPluginName (url);
		
		if (curFileId && pluginName)
		{
			displayFile (curFileId, pluginName);
			doc.file.close.href = basicurl + convertForURL (v.name) + "/" + v.id + "/";
		}
	}
	else
	{
		doc.entity.version.style.display = "none";
		doc.entity.details.style.display = "block";
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
				archivelink : document.getElementById("downloadArchive"),
				filedetails : document.getElementById("entityversionfiledetails"),
				experimentlist: document.getElementById("entityexperimentlist"),
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
	
	render ();
	window.onpopstate = render;
	
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
    
}



document.addEventListener("DOMContentLoaded", initModel, false);