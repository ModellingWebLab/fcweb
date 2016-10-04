// Global data
var fittingProtocol = null;

// Helper functions for interface go here - callback handlers etc.
// Useful places to look for handy bits of code:
// - upload.js - file uploads, drag & drop
// - visualizers/editMetadata/editMetadata.js - saveNewVersion for creating a new version with a single changed file
//   (note that this does it with a special handler in the Java; we may need to change 2+ files so might want to do better than this!)
// - entity.js & db.js - doing lots of things with models & protocols!

/*
 * Parse a url like .../fitting.html or .../fitting/p/<id> to determine what interface to show, and return the id if present; null if not.
 */
function getTemplateId(location)
{
	var cont = contextPath;
	var contextLen = contextPath.length,
		t = location.pathname.substr(contextLen+1).split('/');
	if (location.pathname.substr(0, contextLen) != contextPath || t.length < 3 || t[0] != 'fitting' || t[1] != 'p')
		return null;
	return t[2];
}

/*
 * Trigger a 'page' change that updates the URL.
 * Note that if the 'replace' argument is not supplied, it in effect defaults to false.
 */
function nextPage(url, replace)
{
	if (replace)
		window.history.replaceState(document.location.href, "", url);
	else
		window.history.pushState(document.location.href, "", url);
	render();
}

/*
 * Reveal/fill in the appropriate UI depending on the URL.
 */
function render()
{
	var templateId = getTemplateId(document.location);
	
	if (templateId)
	{
		// We're specialising a particular fitting protocol
		$('#template_select').hide();
		fittingProtocol = new FittingProtocol(templateId);
		$('#fitting_spec').show();
	}
	else
	{
		// We're showing the template list
		$('#fitting_spec').hide();
		$('#template_select').show();
	}
}

/**
 * An object encapsulating all the information about a parameter fitting protocol,
 * along with methods to simplify guiding a user through creating a new version of it.
 *
 * @param templateId  ID number of the protocol version serving as our template
 */
function FittingProtocol(templateId)
{
	// Member data
	this.templateId = parseInt(templateId);
	// Constructor actions
	$.ajax(contextPath + '/fitting.html',
			{data: JSON.stringify({task: "getFittingProtocol", id: this.templateId}),
			 method: 'post',
			 context: this})
		.done(this.gotTemplateInfo)
		.fail(this.ajaxFailed);
}

FittingProtocol.prototype.gotTemplateInfo = function(json) {
	console.log(json);
	displayNotifications(json);
	var fileUrl = getFileUrl(json.fitProto, json.name, this.templateId);
	console.log(fileUrl);
	$.ajax(fileUrl, {method: 'get', context: this})
		.done(this.gotTemplateProtocol)
		.fail(this.ajaxFailed);
}

FittingProtocol.prototype.ajaxFailed = function() {
	addNotification('server error retrieving template details', 'error');
}

/*
 * Receives fitting protocol files via JSON, parses contents, and populates fitting spec view
 */
FittingProtocol.prototype.gotTemplateProtocol = function(templateFileContents)
{
	templateFileContents = JSON.parse(templateFileContents);
	console.log(templateFileContents);
	// Set algorithm name
	document.getElementById("algName").value = templateFileContents.algorithm;

	// Set algorithm arguments
	var argTable = document.getElementById("algArgs");
	console.log(templateFileContents.arguments);
	for (var arg in templateFileContents.arguments)
	{
		if (templateFileContents.arguments.hasOwnProperty(arg))
		{
			var row = argTable.insertRow(0);
			
			var rowlabel = document.createElement("th");
			rowlabel.innerHTML = arg;
			row.appendChild(rowlabel);

			var rowcontent = document.createElement("td");
			input = document.createElement("input");
			input.value = templateFileContents.arguments[arg];
			rowcontent.appendChild(input);
			row.appendChild(rowcontent);
		}
	}

	// Set model prior information
	// TODO: Must merge with defaults specified in selected model
	var modelTable = document.getElementById("modelParams");
	console.log(templateFileContents.prior);
	for (var arg in templateFileContents.prior)
	{
		if (templateFileContents.prior.hasOwnProperty(arg))
		{
			var row = modelTable.insertRow(0);
			
			var rowlabel = document.createElement("th");
			rowlabel.innerHTML = arg;
			row.appendChild(rowlabel);

			var rowcontent = document.createElement("td");
			input = document.createElement("input");
			input.value = templateFileContents.prior[arg];
			rowcontent.appendChild(input);
			row.appendChild(rowcontent);
		}
	}
}

/*
 * Generate URL for a GET request for a file given the JSON representation of the corresponding
 * ChasteFile object, along with the name and id of the ChasteEntityVersion that contains it.
 */
function getFileUrl(f, vname, vid)
{
	entityType = "protocol";
	fileUrl = contextPath + "/download/" + entityType.charAt(0) + "/" + convertForURL(vname) + "/" + vid + "/" + f.id + "/" + convertForURL(f.name);
	return fileUrl;
}

/*
 * Called when the page content has fully loaded to do JS initialisation.
 */
function initFitting()
{
	// Do initialisation here - fill in initial interface, hide what shouldn't be shown yet,
	// trigger loads of files & set up callbacks, etc.
	
	// This pair of instructions let the JS code mimic switching between different pages (with different URLs shown in the address bar)
	// yet actually still within the one HTML page.  The render() function displays the appropriate bit of the interface depending on
	// the URL, and onpopstate ensures it is called when JS code changes the URL.
	window.onpopstate = render;
	render();
	
	// Set up clicks on entries in the template protocols list to bring us to the specification view
	$("a.template_link").click(function(){
		// TODO: get the protocol id from the li element's id, and use nextPage to change view
		var proto_id = this.id.replace("link_", ""); // I think!
		var url = "fitting/p/".concat(proto_id);
		console.log(url);
		nextPage(url,false);
	});
}

document.addEventListener("DOMContentLoaded", initFitting, false);