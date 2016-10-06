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
	console.log(this);
	// Get info about the chosen template
	$.ajax(contextPath + '/fitting.html',
			{data: JSON.stringify({task: "getFittingProtocol", id: this.templateId}),
			 method: 'post',
			 context: this})
		.done(this.gotTemplateInfo)
		.fail(this.ajaxFailed);
	// Get list of available models
	$.ajax(contextPath + '/fitting.html',
			{data: JSON.stringify({task: "getModelList"}),
			 method: 'post',
			 context: this})
		.done(this.gotModelList)
		.fail(this.ajaxFailed);
	// Add some event handlers
	var self = this;
	$('#model').on('change', function (event) { self.modelChanged(event.target); });
	$('#algName').on('change', function (event) { alert('Algorithm change not yet implemented.'); });
	$('#dateInserter').on('click', function (event) {
		$('#versionName').focus().val(getYMDHMS(new Date())).blur();
	});
	$('#versionName').on('blur', function (event) { self.checkVersionName(this.value); });
	// Handlers relating to file uploads
	var $dropbox = $('#dropbox'),
		$upload = $('#fileupload');
	$dropbox.on('dragenter dragover drop', function (event) {
		event.originalEvent.dataTransfer.dropEffect = 'copy';
		return false; // Stop propagation & default behaviour
	});
	$dropbox.on('drop', function(event) {
		var file = event.originalEvent.dataTransfer.files[0];
		self.uploadDataFile(file, file.name);
	});
	$dropbox.get(0).addEventListener('click', function(event) { $upload.click(); }, false);
	$upload.on('change', function(event) {
		var file = this.files[0],
			fullPath = $upload.val(),
			startIndex = (fullPath.indexOf('\\') >= 0 ? fullPath.lastIndexOf('\\') : fullPath.lastIndexOf('/')),
			name = fullPath.substring(startIndex+1);
		self.uploadDataFile(file, name);
	});
}

/**
 * File upload handler, called when user has chosen a (CSV) data file to upload.
 */
FittingProtocol.prototype.uploadDataFile = function(file, name)
{
	console.log('Upload ' + name);
	console.log(file);
	if (!name.toLowerCase().endsWith('.csv'))
	{
		alert("Only CSV data files are supported, with data in columns.");
		return;
	}
	// Upload the file asynchronously
	$('#uploadaction').text('uploading '+name+'...');
	var self = this,
		fd = new FormData,
		reader = new FileReader();
	fd.append('file', file);
	$.ajax(contextPath + '/upload.html',
			{data: fd,
			 method: 'post',
			 processData: false, // Don't try to transform the file into a query string
			 contentType: false, // Don't set a contentType header
			 context: this})
//		.progress(function(e) {
//			console.log(e)
//			var done = e.position || e.loaded,
//				total = e.totalSize || e.total;
//			$('#uploadaction').text('uploading... ' + (Math.floor(done/total*1000)/10) + "%");
//		})
		.done(function (json) { self.uploadDone(json, file, name); })
		.fail(function () {
			$('#uploadaction').text('file upload failed; try again');
		});
	// Read the file ourselves to discover its columns
	reader.addEventListener('load', function () { self.readDataFile(reader.result); });
	reader.readAsText(file);
}

/**
 * The data file has been uploaded to the server; notify the user and record its temporary name.
 */
FittingProtocol.prototype.uploadDone = function(json, file, name)
{
	console.log(json);
	displayNotifications(json);
	if (json.upload && json.upload.response)
	{
		$('#uploadaction').text('Uploaded ' + name + ' (' + humanReadableBytes(file.size) + ')');
		this.dataTmpName = json.upload.tmpName;
	}
	else
	{
		$('#uploadaction').text('file upload failed; try again');
	}
}

/**
 * We have loaded the CSV data into memory.
 * Parse the header and display the UI to associate columns in the file with simulation outputs.
 */
FittingProtocol.prototype.readDataFile = function(csvData)
{
	console.log('Read CSV file');
	var data = { contents: csvData },
		$table = $('#dataColumns'),
		outputMap = this.fittingProtocol.output,
		numOutputs = Object.keys(outputMap).length;
	parseCsvRaw(data);
	console.log(data.csv);
	if (data.csv[0].length < numOutputs)
		alert("Insufficient columns in data file: should be at least " + numOutputs);
	for (var outputName in outputMap)
	{
		if (outputMap.hasOwnProperty(outputName))
		{
			$table.append('<tr><th>'+outputName+'</th><td><select class="outputMap" id="'+outputName+'-col-select"/></td></tr>');
		}
	}
	for (var i=0; i<data.csv[0].length; i++)
	{
		$table.find("select").append('<option value="'+i+'">'+i+'</option>');
	}
	$.each(outputMap, function (name, value) {
		$table.find("#"+name+"-col-select option[value="+value+"]").prop('selected', true);
	});
}

/**
 * Event handler for a change in the model selected to fit.
 * Will update the displayed prior selection interface to grey out inapplicable parameters.
 */
FittingProtocol.prototype.modelChanged = function(modelElt)
{
	console.log('Model changed to ' + $(modelElt).val());
	this.checkModelParameters($(modelElt).val());
}

/**
 * Callback when the basic info for this fitting protocol template has been retrieved from the server.
 */
FittingProtocol.prototype.gotTemplateInfo = function(json)
{
	console.log('Template info');
	console.log(json);
	displayNotifications(json);
	this.templateName = json.name;
	this.templateVisibility = json.visibility;
	this.templateFileInfo = { fitProto: json.fitProto, simProto: json.simProto, dataFile: json.dataFile };
	var fileUrl = getFileUrl(json.fitProto, 'protocol', json.name, this.templateId);
	$.ajax(fileUrl, {method: 'get', context: this})
		.done(this.gotTemplateProtocol)
		.fail(this.ajaxFailed);
	$('#uploadaction').text('Existing data file: ' + json.dataFile.name + ' (' +humanReadableBytes(json.dataFile.size) + ')');
	$('#visibility-' + this.templateVisibility).prop('selected', true);
}

/**
 * Generic error callback for AJAX requests.
 */
FittingProtocol.prototype.ajaxFailed = function()
{
	addNotification('server error retrieving template details', 'error');
}

/**
 * Callback when list of available models has been retrieved from the server.
 * Stores the list, populates a drop-down select for model to fit,
 * and triggers loading the model files so we can parse the parameters and compare to our prior settings.
 */
FittingProtocol.prototype.gotModelList = function(json)
{
	console.log('Model list');
	console.log(json);
	displayNotifications(json);
	this.models = json.latestVersions;
	var $select = $('#model');
	for (var versionId in this.models)
		if (this.models.hasOwnProperty(versionId))
		{
			var model = this.models[versionId],
				file = model.files[0],
				fileUrl = getFileUrl(file, 'model', model.name, model.id);
			console.log('Retrieving model ' + model.name + ' from ' + fileUrl);
			$.ajax(fileUrl, {method: 'get', context: this})
				.done(function(contents) { this.gotModelFile(versionId, contents); })
				.fail(this.ajaxFailed);
			$select.append('<option value=""></option>')
				.children(':last-child').attr('value', model.id).text(model.name);
		}
	$select.trigger('change');
}

/**
 * Called when the selected model has changed (or that model's details have been loaded) to grey out
 * any parameter inputs that aren't relevant.
 *
 * Also adds text to each td that gives the default values for the parameters.
 */
FittingProtocol.prototype.checkModelParameters = function(versionId)
{
	console.log('Check params ' + versionId);
	var model = this.models[versionId],
		params = model.params;
	if (params)
	{
		$('#modelParams input').prop('disabled', true);
		$('#modelParams td span').remove();
		for (var name in params)
		{
			if (params.hasOwnProperty(name))
			{
				$('#'+name+'-td')
					.append('<span>(default: '+params[name]+')</span>')
					.children('input').prop('disabled', false);
			}
		}
	}
}

/**
 * Callback for when a model definition file has been retrieved from the server.
 * Stores & parses the file contents, which consist of lines "<param name>\t<param value>".
 * Will also trigger greying out inapplicable model parameters if this is the currently selected model.
 */
FittingProtocol.prototype.gotModelFile = function(versionId, contents)
{
	var model = this.models[versionId];
	console.log('Got contents for model ' + model.name);
	model.contents = contents;
	model.params = {};
	contents.split('\n').forEach(function (value, index, array) {
		var items = value.split('\t');
		model.params[items[0]] = items[1];
	});
	if ($('#model').val() == versionId)
		this.checkModelParameters(versionId);
}

/**
 * Receives fitting protocol files via JSON, parses contents, and populates fitting spec view
 */
FittingProtocol.prototype.gotTemplateProtocol = function(templateFileContents)
{
	console.log('got proto');
	templateFileContents = JSON.parse(templateFileContents);
	console.log(templateFileContents);
	this.fittingProtocol = templateFileContents;
	// Set algorithm name
	$('#algName > option').attr('value', templateFileContents.algorithm).text(templateFileContents.algorithm);

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
	var modelTable = $("#modelParams"),
		objTable = $("#objParams"),
		priors = templateFileContents.prior;
	console.log(priors);
	for (var arg in priors)
	{
		if (priors.hasOwnProperty(arg))
		{
			var isObj = (arg.substr(0,4) == 'obj:'),
				table = (isObj ? objTable : modelTable),
				row = $('<tr/>').appendTo(table),
				th = $('<th/>').text(arg),
				td = $('<td id="'+arg+'-td"/>'),
				ids = [arg+'-low', arg+'-high'],
				labels = ['From', 'to'];
			row.append(th, td);
			if (priors[arg] instanceof Array)
			{
				for (var i=0; i<2; i++)
				{
					td.append('<label for="'+ids[i]+'">'+labels[i]+'</label> <input id="'+ids[i]+'" size="5" value="'+priors[arg][i]+'"/> ');
				}
			}
			else
			{
				td.append('<input id="'+arg+'-val" size="5" value="'+priors[arg]+'"/>');
			}
		}
	}
}

/**
 * Check that there doesn't already exist a version of this protocol with the chosen name.
 */
FittingProtocol.prototype.checkVersionName = function (versionName)
{
	var $actionElt = $('#versionAction');
	$actionElt.html("<img src='"+contextPath+"/res/img/loading2-new.gif' alt='loading' />");
	$.ajax(contextPath+'/protocol/createnew',
		   {data: JSON.stringify({task: 'verifyNewEntity', entityName: this.templateName, versionName: versionName}),
			method: 'post',
			context: this})
		.fail(function () {
			$actionElt.html("<img src='"+contextPath+"/res/img/failed.png' alt='error' /> sorry, serverside error occurred.");
		})
		.done(function (json) {
			console.log(json);
			displayNotifications(json);
			var msg = json.versionName.responseText;
			if (json.versionName.response)
				$actionElt.html("<img src='"+contextPath+"/res/img/check.png' alt='valid' /> " + msg);
			else
				$actionElt.html("<img src='"+contextPath+"/res/img/failed.png' alt='invalid' /> " + msg);
		});
}

/**
 * Generate URL for a GET request for a file given the JSON representation of the corresponding
 * ChasteFile object, along with the name and id of the ChasteEntityVersion that contains it.
 */
function getFileUrl(f, entityType, vname, vid)
{
	fileUrl = contextPath + "/download/" + entityType.charAt(0) + "/" + convertForURL(vname) + "/" + vid + "/" + f.id + "/" + convertForURL(f.name);
	return fileUrl;
}

/**
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