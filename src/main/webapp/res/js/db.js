
var pages = [ "matrix" ],//, "search" ],
	comparisonMode = false,
	experimentsToCompare = [],
	linesToCompare = {'row': [], 'col': []};

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
    		td.removeClass ("experiment-QUEUED experiment-RUNNING experiment-INAPPRORIATE experiment-FAILED experiment-PARTIAL experiment-SUCCESS");

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
	    				setTitleAndListeners(td, entry);
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
	//console.log (matrix);
	var models = [],
		protocols = [],
		modelMapper = {},
		protocolMapper = {},
		mat = [];
	
	for (var key in matrix.models)
		if (matrix.models.hasOwnProperty (key))
		{
			var version = matrix.models[key].id;
			modelMapper[version] = matrix.models[key];
//			modelMapper[version].name = matrix.models[key].name;
			models.push(version);
			
		}

	for (var key in matrix.protocols)
		if (matrix.protocols.hasOwnProperty (key))
		{
			var version = matrix.protocols[key].id;
			protocolMapper[version] = matrix.protocols[key];
//			protocolMapper[version].name = matrix.protocols[key].name;
			protocols.push(version);
		}

    // Sort rows & columns alphabetically (case insensitive)
    models.sort(function(a,b) {return (modelMapper[a].name.toLocaleLowerCase() > modelMapper[b].name.toLocaleLowerCase()) ? 1 : ((modelMapper[b].name.toLocaleLowerCase() > modelMapper[a].name.toLocaleLowerCase()) ? -1 : 0);});
    protocols.sort(function(a,b) {return (protocolMapper[a].name.toLocaleLowerCase() > protocolMapper[b].name.toLocaleLowerCase()) ? 1 : ((protocolMapper[b].name.toLocaleLowerCase() > protocolMapper[a].name.toLocaleLowerCase()) ? -1 : 0);});
	
	/*console.log ("models");
	console.log (modelMapper);
	console.log ("protocols");
	console.log (protocolMapper);*/
	
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
			var exp = matrix.experiments[key],
				row = modelMapper[exp.model.id].row,
				col = protocolMapper[exp.protocol.id].col;
			exp.name = exp.model.name + " @ " + exp.model.version + " & " + exp.protocol.name + " @ " + exp.protocol.version;
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
			var td = document.createElement("td"),
				$td = $(td);
			tr.appendChild(td);
			td.setAttribute("id", "matrix-entry-" + row + "-" + col);
			
			//console.log ("row " + row + " col " + col);
			
			if (row == -1 && col == -1)
				continue;
			
			// Top row: protocol names
			if (row == -1)
			{
				var d1 = document.createElement("div");
				var d2 = document.createElement("div");
				var a = document.createElement("a");
				a.href = contextPath + "/protocol/" + convertForURL(mat[0][col].protocol.name) + "/" + mat[0][col].protocol.entityId
					+ "/" + convertForURL(mat[0][col].protocol.version) + "/" + mat[0][col].protocol.id;
				d2.setAttribute("class", "vertical-text");
				d1.setAttribute("class", "vertical-text__inner");
				d2.appendChild(d1);
				a.appendChild(document.createTextNode(mat[0][col].protocol.name));
				d1.appendChild(a);
				td.appendChild(d2);
				$td.addClass("matrixTableCol").data("col", col).click(function (ev) {
					if (comparisonMode) {
						ev.preventDefault();
						addToComparison($(this), 'col');
					}
				});
				continue;
			}
			
			// Left column: model names
			if (col == -1)
			{
				var a = document.createElement("a");
				a.href = contextPath + "/model/" + convertForURL(mat[row][0].model.name) + "/" + mat[row][0].model.entityId
					+ "/" + convertForURL(mat[row][0].model.version) + "/" + mat[row][0].model.id;
				a.appendChild(document.createTextNode(mat[row][0].model.name));
				td.appendChild(a);
				$td.addClass("matrixTableRow").data("row", row).click(function (ev) {
					if (comparisonMode) {
						ev.preventDefault();
						addToComparison($(this), 'row');
					}
				});
				continue;
			}
			
			// Normal case
			var entry = mat[row][col];
			entry.row = row;
			entry.col = col;
			$td.data("entry", entry).addClass("matrix-row-" + row).addClass("matrix-col-" + col);
			if (entry.experiment)
				$td.addClass("experiment experiment-"+entry.experiment.latestResult);
			else
				$td.addClass("experiment experiment-NONE");
			
			setTitleAndListeners($td, entry);
		}
	}
}


/**
 * Set up the title text and click/hover listeners for the given matrix entry
 * @param $td  the table cell
 * @param entry  mat[row][col] for this table cell
 */
function setTitleAndListeners($td, entry)
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
        addMatrixClickListener($td, expUrl, entry.experiment.id, entry.experiment.name, entry.experiment.latestResult);
    }
    else
    {
        titleText += "E: <a id='create-"+entry.row+"-"+entry.col+"'>create experiment</a>";
    }

    $td.attr("title", titleText);
    createClueTip($td, entry);

	// Highlight the relevant row & column labels when the mouse is over this cell
	$td.mouseenter(function (ev) {
		$("#matrix-entry--1-" + entry.col).addClass("matrixHover");
		$("#matrix-entry-" + entry.row + "--1").addClass("matrixHover");
	}).mouseleave(function (ev) {
		$("#matrix-entry--1-" + entry.col).removeClass("matrixHover");
		$("#matrix-entry-" + entry.row + "--1").removeClass("matrixHover");
	});
}

/**
 * Handle a click on a row or column header when in comparison mode.
 * 
 * Toggles the selected state of this row/column.  If only one ends up selected, then all cells in that row/column are
 * included in the comparison.  If at least one row and column are selected, then we only compare experiments that
 * feature both a selected row and column.
 * 
 * Note that users MAY select extra experiments not in a selected row/column.  Such choices should not be affected by
 * this method.  Clicking on individual experiments selected via this mechanism will also toggle their state, but may
 * be overridden by a subsequent selection via this method.
 * 
 * @param $td  the header clicked on
 * @param rowOrCol  either 'row' or 'col'
 */
function addToComparison($td, rowOrCol)
{
	var index = $td.data(rowOrCol),
		lineIndex = linesToCompare[rowOrCol].indexOf(index),
		cells = $(".matrix-" + rowOrCol + "-" + index),
		colOrRow = (rowOrCol == 'row' ? 'col' : 'row'),
		otherTypeSelected = (linesToCompare[colOrRow].length > 0);
	if (lineIndex != -1)
	{
		// Was selected already -> unselect
		linesToCompare[rowOrCol].splice(lineIndex, 1);
		$td.removeClass("patternized");
		// Clear any selected experiments in this line
		cells.filter(".patternized").each(function () {
			var $cell = $(this),
				cellIndex = experimentsToCompare.indexOf($cell.data("entry").experiment.id);
			if (cellIndex != -1)
			{
				experimentsToCompare.splice(cellIndex, 1);
				$cell.removeClass("patternized");
			}
		});
		// If this was the only line of this type selected, select all experiments in any selected colOrRow
		if (linesToCompare[rowOrCol].length == 0)
		{
			$.each(linesToCompare[colOrRow], function (i, otherLineIndex) {
				$(".matrix-" + colOrRow + "-" + otherLineIndex).not(".patternized").each(function (i, elt) {
					var $cell = $(elt),
						exp = $cell.data("entry").experiment;
					if (exp && isSelectableResult(exp.latestResult))
					{
						experimentsToCompare.push(exp.id);
						$cell.addClass("patternized");
					}
				});
			});
		}
	}
	else
	{
		// Select this row/col
		linesToCompare[rowOrCol].push(index);
		$td.addClass("patternized");
		// If this is the first line of this type, clear lines of the other type
		if (linesToCompare[rowOrCol].length == 1)
		{
			$.each(linesToCompare[colOrRow], function (i, otherLineIndex) {
				$(".matrix-" + colOrRow + "-" + otherLineIndex).filter(".patternized").each(function (i, elt) {
					var $cell = $(elt),
						cellIndex = experimentsToCompare.indexOf($cell.data("entry").experiment.id);
					experimentsToCompare.splice(cellIndex, 1);
					$cell.removeClass("patternized");
				});
			});
		}
		// Select experiments in this line
		cells.not(".patternized").each(function () {
			var $cell = $(this),
				entry = $cell.data("entry"),
				exp = entry.experiment;
			if (exp && isSelectableResult(exp.latestResult) &&
					(!otherTypeSelected || linesToCompare[colOrRow].indexOf(entry[colOrRow]) != -1))
			{
				experimentsToCompare.push(exp.id);
				$cell.addClass("patternized");
			}
		});
	}
	computeComparisonLink();
}

/**
 * Toggle whether the given experiment is selected in comparison mode.
 * @param $td  the table cell
 * @param expId  the experiment id
 * @returns whether the experiment is now selected
 */
function toggleSelected($td, expId)
{
	var index = experimentsToCompare.indexOf(expId);
	if (index != -1)
	{
		// was selected -> unselect
		experimentsToCompare.splice(index, 1);
		$td.removeClass("patternized");
	}
	else
	{
		// add a new element to the list
		experimentsToCompare.push(expId);
		$td.addClass("patternized");
	}
	return (index == -1);
}

/**
 * Compute the 'compare experiments' link in comparison mode,
 * and show the button iff there are experiments to compare.
 */
function computeComparisonLink()
{
	if (experimentsToCompare.length > 0)
	{
		var newHref = contextPath + "/compare/e/";
		for (var i = 0; i < experimentsToCompare.length; i++)
			newHref += experimentsToCompare[i] + "/";
		$("#comparisonLink").data("href", newHref).show();
	}
	else
		$("#comparisonLink").hide();
}

/**
 * Determine whether an experiment with the given result status can be selected for comparison.
 * @param result
 * @returns {Boolean}
 */
function isSelectableResult(result)
{
	return (result == "SUCCESS" || result == "PARTIAL");
}

function addMatrixClickListener($td, link, expId, expName, result)
{
	$td.click(function (ev) {
		if (comparisonMode)
		{
			if (!isSelectableResult(result))
				return;
			toggleSelected($td, expId);
			computeComparisonLink();
		}
		else
		{
			document.location.href = link;
		}
	});
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
			link.click(function () {
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
	
	var loadingImg = document.createElement("img");
	loadingImg.src = contextPath+"/res/img/loading2-new.gif";
	div.appendChild(loadingImg);
	div.appendChild(document.createTextNode("Preparing experiment matrix; please be patient."));

	getMatrix ({
    	task: "getMatrix"
    }, div);
	
	$("#comparisonModeButton").text(comparisonMode ? "Disable" : "Enable")
	                          .click(function () {
		comparisonMode = !comparisonMode;
		$("#comparisonModeButton").text(comparisonMode ? "Disable" : "Enable");
		if (!comparisonMode)
		{
			// Clear all selections
			experimentsToCompare.splice(0, experimentsToCompare.length);
			linesToCompare.row.splice(0, linesToCompare.row.length);
			linesToCompare.col.splice(0, linesToCompare.col.length);
			$(".patternized").removeClass("patternized");
			$("#comparisonLink").hide();
		}
	});
	$("#comparisonLink").hide().click(function () {
	    document.location = $(this).data("href");
	});
	
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
		switchPage (page);
	}, true);
}

function initDb ()
{
//	for (var i = 0; i < pages.length; i++)
//	{
//		var btn = document.getElementById(pages[i] + "chooser");
//		registerSwitchPagesListener (btn, pages[i]);
//	}
	switchPage (pages[0]);
	
	prepareMatrix ();
}
document.addEventListener("DOMContentLoaded", initDb, false);