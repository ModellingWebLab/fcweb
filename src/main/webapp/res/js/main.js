function addNotification (err, list)
{
	console.log ("adding" + list);
	if (!list)
		list = "error";
	var errsList = document.getElementById(list + "list");
	var errors = document.getElementById(list);
	var item = document.createElement("li");
	item.innerHTML = err;
	errsList.appendChild(item);

	if (errsList.firstChild)
		errors.setAttribute("class", "");
}

function displayNotifications (json)
{
	if (json && json.notifications)
	{
		if (json.notifications.errors)
		{
			var errs = json.notifications.errors;
			for(var i = 0; i < errs.length; i++)
				addNotification (errs[i], "error");
		}
		if (json.notifications.notes)
		{
			var errs = json.notifications.notes;
			for(var i = 0; i < errs.length; i++)
				addNotification (errs[i], "info");
		}
	}
}

/**
 * Raw parsing function for CSV files into columns of numerical/textual data
 * @param file  the loaded file to parse
 */
function parseCsvRaw(file)
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
    file.csv = csv;
}

function removeListeners (element)
{
	var new_element = element.cloneNode(true);
	element.parentNode.replaceChild (new_element, element);
	return new_element;
}
function keys (obj)
{
    var keys = [];

    for(var key in obj)
        if(obj.hasOwnProperty(key))
            keys.push(key);

    return keys;
}
function getPos (ele)
{
    var x = 0;
    var y = 0;
    while (true)
    {
    	if (!ele)
    		break;
        x += ele.offsetLeft;
        y += ele.offsetTop;
        if (ele.offsetParent === null)
            break;
        ele = ele.offsetParent;
    }
    return {xPos:x, yPos:y};
}

function batchProcessing (jsonObject, actionIndicator)
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
    
    xmlhttp.open("POST", contextPath + '/batch/batch', true);
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
        	if (json.batchTasks)
        	{
	        	var msg = json.batchTasks.responseText;
	        	if (json.batchTasks.response)
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
    xmlhttp.send(JSON.stringify(jsonObject));
}
function newExperiment (jsonObject, modelname, protocolname)
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
        	if (json.newExpModel)
        	{
        		var status = json.newExpModel;
        		if (status.response)
        		{
        			document.getElementById ("newexpmodel").innerHTML = modelname;
        			document.getElementById ("newexpcontainer").setAttribute("class", "");
        		}
        		else
        			addNotification (status.responseText, "error");
        	}
        	if (json.newExpProtocol)
        	{
        		var status = json.newExpProtocol;
        		if (status.response)
        		{
        			document.getElementById ("newexpprotocol").innerHTML = protocolname;
        			document.getElementById ("newexpcontainer").setAttribute("class", "");
        		}
        		else
        			addNotification (status.responseText, "error");
        	}
        	if (json.runExperiment)
        	{
        		var status = json.runExperiment;
        		if (status.response)
        		{
        			document.getElementById ("newexpcontainer").setAttribute("class", "invisible");
        			document.getElementById ("newexpmodel").innerHTML = "";
        			document.getElementById ("newexpprotocol").innerHTML = "";
        			addNotification (status.responseText, "info");
        		}
        		else
        			addNotification (status.responseText, "error");
        	}
        	
        	if (json.scheduledModel)
        	{
        		document.getElementById ("newexpmodel").innerHTML = json.scheduledModel;
        	}
        	
        	if (json.scheduledProtocol)
        	{
        		document.getElementById ("newexpprotocol").innerHTML = json.scheduledProtocol;
        	}
        }
    };
    xmlhttp.send(JSON.stringify(jsonObject));
}

function newExperimentModel (modelid, modelname)
{
	newExperiment ({
	    	task: "newExpModel",
	    	model: modelid
	}, modelname, null);
}

function newExperimentProtocol (protocolid, protocolname)
{
	newExperiment ({
	    	task: "newExpProtocol",
	    	protocol: protocolid
	}, null, protocolname);
}

function runExperiment (force)
{
	// TODO:  loading indicator
	newExperiment ({
    	task: "runExperiment",
    	forceNewVersion: force
    }, null, null);
	
}

function removeChildren (elem)
{
    if (elem)
        while (elem.firstChild)
            elem.removeChild(elem.firstChild);
}

function clearNotifications (type)
{
	var list = document.getElementById(type+"list");
	removeChildren (list);
	list = document.getElementById(type);
	list.setAttribute("class", "invisible");
}

function convertForURL (str)
{
	var url = str.replace(/\W/g, '');
	if (url.length >= 5)
		return url;
	while (url.length < 7)
		url += Math.random().toString(36).substring(7);
	return url.substring (0, 5);
}

function beautifyTimeStamp (datestring)
{
	var date = new XDate(datestring, true);
	if (date && date.valid ())
	{
		return date.toString ("MMM d'<sup>'S'</sup>', yyyy 'at' h:mm tt");
	}
	return datestring;
}

function getYMDHMS (datestring)
{
	var date = new XDate(datestring, true);
	if (date && date.valid ())
	{
		return date.toString ("yyyy-MM-dd_HH-mm-ss");
	}
	return datestring;
}
function humanReadableBytes (bytes)
{
    var sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    if (bytes == 0)
    	return '0 Bytes';
    var i = parseInt (Math.floor(Math.log(bytes) / Math.log(1024)));
    return Math.round (bytes / Math.pow(1024, i), 2) + ' ' + sizes[i];
};
function sortChildrenByAttribute (elem, reverse, attr)
{
	//console.log (elem);
	var children = elem.childNodes;
	//console.log (children);
	var items = [];
	
	var ret = reverse ? -1 : 1;
	
	for (var i = 0; i < children.length; i++)
	{
		//console.log (children[i]);
		//console.log (children[i].nodeType);
		//console.log (children[i].attr);
		//console.log (children[i][attr]);
		if (children[i].nodeType == 1 && children[i][attr])
			items.push(children[i]);
	}
	//console.log ("sorting");
	//console.log (items);
	
	items.sort(function (a, b)
			{
				return a[attr] == b[attr] ? 0 : (a[attr] > b[attr] ? ret : -1 * ret);
			});

	//console.log (items);
	//console.log ("sorted");
	
	for (var i = 0; i < items.length; i++)
		elem.appendChild (items[i]);
	
}

function addScript (link)
{
	var el = document.createElement('script');
	el.async = false;
	el.src = link;
	el.type = 'text/javascript';
	(document.getElementsByTagName('head')[0]||document.body).appendChild(el);
}

function addLink (link)
{
	var el = document.createElement('link');
	el.rel = "stylesheet";
	el.href = link;
	el.type = 'text/css';
	(document.getElementsByTagName('head')[0]||document.body).appendChild(el);
}

function initPage ()
{
	// java's implementation of string's hashcode
	String.prototype.hashCode = function()
	{
	    var hash = 0, i, char, l;
	    if (this.length == 0)
	    	return hash;
	    for (i = 0, l = this.length; i < l; i++)
	    {
	        char  = this.charCodeAt(i);
	        hash  = ((hash<<5)-hash)+char;
	        hash |= 0; // Convert to 32bit integer
	    }
	    return hash;
	};
	
	
	String.prototype.endsWith = function(suffix) {
	    return this.indexOf(suffix, this.length - suffix.length) !== -1;
	};
	

	var dismissErrs = document.getElementById("dismisserrors");
	dismissErrs.addEventListener("click", 
	        function (event)
	        {
				clearNotifications ("error");
	        }, 
	        false);
	var dismissNotes = document.getElementById("dismissnotes");
	dismissNotes.addEventListener("click", 
	        function (event)
	        {
				clearNotifications ("info");
	        }, 
	        false);
	
	var times = document.getElementsByTagName("time");
	for (var i = 0; i < times.length; i++)
	{
		//console.log (times[i].innerHTML);
		/*var date = new XDate(times[i].innerHTML, true);
		if (date && date.valid ())
		{
			times[i].setAttribute ("datetime", times[i].innerHTML);
			times[i].innerHTML = date.toString ("MMM dS, yyyy 'at' h:mm tt");
		}*/
		var tm = times[i].innerHTML;
		if (tm)
		{
			times[i].setAttribute ("datetime", tm);
			times[i].innerHTML = beautifyTimeStamp (tm);
		}
	}
	
	
	/*var as = document.getElementsByTagName("a");
	for (var i = 0; i < times.length; i++)
	{
		var a = as[i].innerHTML;
		if (!a.href)
		{
			a.className = (a.className ? a.className + " " : "") + "pointer";
		}
	}*/
}

document.addEventListener("DOMContentLoaded", initPage, false);