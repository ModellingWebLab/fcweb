var choicesDivId = 'choices';
var resetButtonDivId = 'reset-button-div'
var colouredSpanIdPrefix = 'span';
var legendDivId = 'legend';
var plottedGraph = {};
var resetButtonId = 'resetButton';
var selectTogglerId = 'selectToggler';

/* create and append the div for showing the plot choices */
function createAppendChoicesDiv(parentDiv) {
    var choicesDiv = document.createElement("div");
    choicesDiv.id = choicesDivId;
    parentDiv.appendChild (choicesDiv);
}

/* create and append the flot plotting div element with specified id attr */
function createAppendFlotPlotDiv(parentDiv, flotPlotDivId) {
    var flotPlotDiv = document.createElement("div");
    flotPlotDiv.id = flotPlotDivId;
    flotPlotDiv.title = "Zoom available by selecting an area of the plot";
    flotPlotDiv.style.width = "780px";
    flotPlotDiv.style.height = "450px";
    parentDiv.appendChild(flotPlotDiv);
}

/* create and append the div for showing the legend */
function createAppendLegendDiv(parentDiv) {
    var legendContainer =  document.createElement("div");
    legendContainer.id = legendDivId;
    parentDiv.appendChild (legendContainer);
}

/* create and append a reset button to the div element */
function createAppendResetButton(parentDiv) {
    var resetButtonDiv = document.createElement("div");
    resetButtonDiv.id = resetButtonDivId;
    parentDiv.appendChild(resetButtonDiv);
	
    var resetButton = document.createElement('input');
    resetButton.id = resetButtonId;
    resetButton.title = 'Reset graph zoom based on currently selected datasets';
    resetButton.type = 'button';
    resetButton.value = 'reset zoom';
    resetButtonDiv.appendChild (resetButton);
}

/* create and append a select toggler to the div element */
function createAppendSelectToggler(parentDiv) {
    var selectTogglerEl = document.createElement('input');
    selectTogglerEl.id = selectTogglerId;
    selectTogglerEl.type = 'checkbox';
    parentDiv.appendChild (selectTogglerEl);
    
    var label = document.createElement('label')
    label.setAttribute('for', selectTogglerId);
    label.innerHTML = 'select all';
    parentDiv.appendChild(label);

    var selectToggler = $('#' + selectTogglerId);
    selectToggler.attr({ 'checked': 'checked' });
    setTogglerTitle(selectToggler);
}

/* indicator of linestyle type */
function isStyleLinespointsOrPoints(lineStyle) {
  return (lineStyle == "linespoints" || lineStyle == "points");
}

/* Plot the graph */
function plotAccordingToChoices(plotProperties, selectedCoords) {
    var choicesContainer = plotProperties.choicesContainer;
    var datasets = plotProperties.datasets;
    var styleLinespointsOrPoints = plotProperties.styleLinespointsOrPoints;
    var flotPlotDivId = plotProperties.flotPlotDivId;
    var x_label = plotProperties.x_label;
    var y_label = plotProperties.y_label;

    var data = [];

    choicesContainer.find("input:checked").each(function () {
        var key = $(this).attr("name");
        if (key && datasets[key]) {
            data.push(datasets[key]);
        }
    });

    var genericSettings = retrieveGenericSettings($('#' + legendDivId));
    var settings;
    if (selectedCoords != undefined)
    {
        settings = $.extend(true, {}, genericSettings, {
          xaxis: { min: selectedCoords.x[0], max: selectedCoords.x[1] },
          yaxis: { min: selectedCoords.y[0], max: selectedCoords.y[1] }
        });
    }
    else
    {
        settings = genericSettings;
    }
    settings.xaxis.axisLabel = x_label;
    settings.yaxis.axisLabel = y_label;

    if (styleLinespointsOrPoints)
        settings.points = { show: true, radius: 2 };

    plottedGraph = $.plot("#" + flotPlotDivId, data, settings);
};

/* retrieve min and max x and y axes values of current plot */
function retrieveCurrentPlotCoords(plottedGraph) {
  var xAxis = plottedGraph.getAxes().xaxis;
  var yAxis = plottedGraph.getAxes().yaxis;
  var coords = { 'x': [xAxis.min, xAxis.max],
                 'y': [yAxis.min, yAxis.max] };
  return coords;
}

/* Retrieve generic plot settings */
function retrieveGenericSettings(legendContainer) {
  var genericSettings = {
      xaxis: { tickDecimals: 0, 
               position: 'bottom', 
               axisLabelPadding: 10, 
               axisLabelUseCanvas: true  },
      yaxis: { position: 'left', 
               axisLabelPadding: 10, 
               axisLabelUseCanvas: true},
      lines: { show: true},
      selection: { mode: 'xy' },
      legend: { backgroundOpacity: 0,
                container: legendContainer } 
  };
  return genericSettings;
}

/* attach the click, select listeners to the plot */
function setListeners(plotProperties, moreThanOneDataset) {
    var choicesContainer = plotProperties.choicesContainer;
    var flotPlotDivId = plotProperties.flotPlotDivId;

    /* someone's clicked on a dataset checkbox */
    choicesContainer.find("input").click(function() {
        var checkedCount = choicesContainer.find('input:checkbox:checked').length;
        if (checkedCount == 0) {
          /* should not happen! */
        }
        else
        {
          if (checkedCount == 1) {
            /* disable the input checkbox on whichever checked dataset remains */
            $('#' + choicesDivId + ' input:checkbox:checked').prop('disabled', true);
          }
          else
          {
            /* more than one dataset selected, remove any disabled */
            $('#' + choicesDivId + ' input:checkbox:disabled').prop('disabled', false);
          }
          /* re-plot using existing coordinates */
          plotAccordingToChoices(plotProperties, retrieveCurrentPlotCoords(plottedGraph));
        }
    });

    /* listen to user selecting an area of the plot to zoom into */
    $('#' + flotPlotDivId).bind('plotselected', function (event, ranges) {
        /* clamp the zooming to prevent eternal zoom */
        if (ranges.xaxis.to - ranges.xaxis.from < 0.00001)
          ranges.xaxis.to = ranges.xaxis.from + 0.00001;
        if (ranges.yaxis.to - ranges.yaxis.from < 0.00001)
          ranges.yaxis.to = ranges.yaxis.from + 0.00001;

        var coords = { 'x': [ranges.xaxis.from, ranges.xaxis.to],
                       'y': [ranges.yaxis.from, ranges.yaxis.to] };

        plottedGraph.setSelection(ranges, true);
        plotAccordingToChoices(plotProperties, coords);
    });

    /* reset graphical display according to ranges defined by current dataset selection */
    $('#' + resetButtonId).click(function() {
        plotAccordingToChoices(plotProperties);
    });

    if (moreThanOneDataset)
    {
        /* action when all datasets toggler is clicked */
        $('#' + selectTogglerId).click(function() {
          var toggler = $(this);
          setTogglerTitle(toggler);
          /* remove any disabled inputs */
          $('#' + choicesDivId + ' input:checkbox:disabled').prop('disabled', false);
          if (toggler.is(':checked'))
          {
            /* check all currently unchecked datasets */
            choicesContainer.find('input:checkbox:not(:checked)').each(function() {
              $(this).prop('checked', true);
            });
          }
          else
          {
            /* remove all the checked properties except on the first (and disabled that!) */
            choicesContainer.find('input:checkbox:not(:eq(0))').each(function() {
              $(this).prop('checked', false);
            });
            /* switch on the first (just in case it was already de-selected) and disable it */
            $('#' + choicesDivId + ' input:checkbox:eq(0)').prop({ 'disabled': true, 'checked': true });
          }
          plotAccordingToChoices(plotProperties, retrieveCurrentPlotCoords(plottedGraph));
      });
    }
}

/* provide dataset toggler title */
function setTogglerTitle(toggler) {
  toggler.attr('title', toggler.is(':checked') ? 'Select one (cannot select none!)' : 'Select all');
}

/* Transfer the colours placed into the legend div by flot's plotting, to the spans corresponding
to the dataset plot label. Once transfered the legend div serves no purpose. */
function transferLegendColours(datasets) {
 /* we don't want to see the legend data so hide immediately, only use it to pinch the colour! */
 $('#' + legendDivId).hide();
 $.each(datasets, function(key, val) {
     /* class legendColorBox defined in jquery.flot.js in function insertLegend() */
     var thisDatasetNumber = val.color;
     var legendColorBox = $('td.legendColorBox:eq(' + thisDatasetNumber + ') div div');
     /* moz wasn't happy using border-color, which IE didn't mind */
     var colour = legendColorBox.css('border-left-color');
     $('#' + colouredSpanIdPrefix + thisDatasetNumber).css('background-color', colour);
 });
 /* legend element is no longer required */
 $('#' + legendDivId).remove();
}

/* Extract key data for a file if available. */
function getKeyValues(file)
{
    var keyVals = [];
    if (file.keyId)
    {
        var keyData = getCSVColumns(file.keyFile);
        if (keyData.length > 0)
            for (var i=0; i<keyData[0].length; i++)
                keyVals.push(keyData[0][i]);
    }
    return keyVals;
}

function contentFlotPlot (file, div)
{
    this.file = file;
    this.div = div;
    this.setUp = false;
    div.appendChild (document.createTextNode ("loading"));
    div.setAttribute ("class", "flotDiv");
};

contentFlotPlot.prototype.getContentsCallback = function (succ)
{
    var thisFile = this.file;
    var thisFileId = thisFile.id;
    var thisDiv = this.div;

    removeChildren (thisDiv);
    if (!succ)
        thisDiv.appendChild (document.createTextNode ("failed to load the contents"));
    else
    {
        if (thisFile.keyId && !thisFile.keyFile.contents)
        {
            // Load the key data and try again
            thisFile.keyFile.getContents (this);
            return;
        }

        var lineStyle = thisFile.linestyle;
        var styleLinespointsOrPoints = isStyleLinespointsOrPoints(lineStyle);
        var csvData = styleLinespointsOrPoints ? getCSVColumnsNonDownsampled (thisFile) :
                                                 getCSVColumnsDownsampled (thisFile);
        var keyVals = getKeyValues(thisFile);

        var datasets = {};
        for (var i = 1; i < csvData.length; i++)
        {
            var curData = [];
            for (var j = 0; j < csvData[i].length; j++)
                curData.push ([csvData[i][j].x, csvData[i][j].y]);

            if (keyVals.length == csvData.length)
                datasets["line" + i] = {label: thisFile.keyName + " = " + keyVals[i] + " " + thisFile.keyUnits, data: curData};
            else
                datasets["line" + i] = {label : "line " + i, data: curData};
        }

        // Some of the plots won't come from specified plots, so these are missing.
        var x_label = thisFile.xAxes || "";
        var y_label = thisFile.yAxes || "";

        /* hard-coded colour indexes to prevent from shifting when turned off */
        var datasetNumber = 0;
        $.each(datasets, function(key, val)
        {
            val.color = datasetNumber++;
        });
        var lastDatasetNumber = datasetNumber - 1;

        var flotPlotDivId = 'flotplot-' + thisFileId;
        createAppendFlotPlotDiv(thisDiv, flotPlotDivId);
        (datasetNumber > 1) && createAppendSelectToggler(thisDiv);
        createAppendResetButton(thisDiv);
        createAppendChoicesDiv(thisDiv);
        createAppendLegendDiv(thisDiv);

        var choicesContainer = $('#' + choicesDivId);
        var onlyOneDataset = (datasetNumber == 1);

        /* insert checkboxes - note that colours will be applied to spans after plotting */ 
        $.each(datasets, function(key, val) {
            var thisDatasetNumber = val.color;
            var colouredSpan = $('<span />').attr('id', colouredSpanIdPrefix + thisDatasetNumber)
                                            .addClass('flotColour')
                                            .html('&nbsp;&nbsp;');
            var inputId = 'id' + key;
            var newLabel = $('<label />').attr('for', inputId).html(val.label);
            var newInput = $('<input />').attr({ 'type': 'checkbox',
                                                 'name': key,
                                                 'checked': 'checked',
                                                 'id': inputId });
            if (onlyOneDataset)
            {
                newInput.attr('disabled', 'disabled');
            }
            choicesContainer.append(newInput).append(colouredSpan).append('&nbsp;').append(newLabel);
            /* if more than one dataset and it's not the last one to be processed.. */
            if (!onlyOneDataset && (thisDatasetNumber != lastDatasetNumber))
            {
                choicesContainer.append('<br />');
            }
        });

        var plotProperties = {
          'choicesContainer': choicesContainer,
          'datasets': datasets,
          'styleLinespointsOrPoints': styleLinespointsOrPoints,
          'flotPlotDivId': flotPlotDivId,
          'x_label': x_label,
          'y_label': y_label
        };

        plotAccordingToChoices(plotProperties, undefined);
        /* legend generated when graph plotted, so this must follow the plot creation! */
        transferLegendColours(datasets);
        setListeners(plotProperties, (datasetNumber > 1));
    }
};

contentFlotPlot.prototype.show = function ()
{
    //console.log ("show");
    //console.log (this.div);
    if (!this.setUp)
        this.file.getContents (this);
};

function contentFlotPlotComparer (file, div)
{
    this.file = file;
    this.div = div;
    this.setUp = false;
    div.appendChild (document.createTextNode ("loading"));
    div.setAttribute ("class", "flotDiv");
    this.gotFileContents = 0;
    this.gotKeyContents = 0;
    this.expectedKeyContents = -1;
    this.ok = true;
};

contentFlotPlotComparer.prototype.getContentsCallback = function (succ)
{
//    console.log ("getContentsCallback : " + succ + " -> so far: " + this.gotFileContents + " of " + this.file.entities.length);
    if (!succ)
        this.ok = false;

    if (this.expectedKeyContents > 0)
    {
//        console.log("getKey: " + succ + " so far: " + this.gotKeyContents + " of " + this.expectedKeyContents);
        // We've loaded the main data, and are now loading plot key data
        this.gotKeyContents++;
        if (this.gotKeyContents >= this.expectedKeyContents)
            this.showContents();
    }
    else
    {
        // We're loading the main plot data
        this.gotFileContents++;
        if (this.gotFileContents >= this.file.entities.length)
            this.showContents ();
    }
};

contentFlotPlotComparer.prototype.showContents = function ()
{
    var thisFile = this.file;
    var thisFileSig = thisFile.sig;
    var thisDiv = this.div;

    removeChildren (thisDiv);
    if (!this.ok)
        thisDiv.appendChild (document.createTextNode ("failed to load the contents"));
    else
    {
        // Check whether we need to load plot key data
        if (this.expectedKeyContents == -1)
        {
            this.expectedKeyContents = 0;
            // First figure out how many keys we expect
            for (var i = 0; i < thisFile.entities.length; i++)
            {
                var f = thisFile.entities[i].entityFileLink;
                if (f.keyId && !f.keyFile.contents)
                {
                    this.expectedKeyContents++;
//                    console.log("Found key " + f.keyId);
                }
            }
//            console.log("Expecting " + this.expectedKeyContents + " keys");
            // Then set them loading and wait!
            for (var i = 0; i < thisFile.entities.length; i++)
            {
                var f = thisFile.entities[i].entityFileLink;
                if (f.keyId && !f.keyFile.contents)
                {
                    getFileContent(f.keyFile, this);
                }
            }
            if (this.expectedKeyContents > 0)
                return;
        }

        this.setUp = true;

        var lineStyle = thisFile.linestyle;
        var styleLinespointsOrPoints = isStyleLinespointsOrPoints(lineStyle);

        var csvDatas = new Array ();
        for (var i = 0; i < thisFile.entities.length; i++)
        {
            csvDatas.push ({
                data: (styleLinespointsOrPoints) ? getCSVColumnsNonDownsampled (thisFile.entities[i].entityFileLink) :
                                                   getCSVColumnsDownsampled (thisFile.entities[i].entityFileLink),
                entity: thisFile.entities[i].entityLink,
                file: thisFile.entities[i].entityFileLink
            });
        }

        // Some of the plots won't come from specified plots, so these are missing.
        var x_label = thisFile.xAxes || "";
        var y_label = thisFile.yAxes || "";

        var flotPlotDivId = 'flotplot-' + thisFileSig;
        createAppendFlotPlotDiv(thisDiv, flotPlotDivId);
        createAppendSelectToggler(thisDiv);
        createAppendResetButton(thisDiv);
        createAppendChoicesDiv(thisDiv);
        createAppendLegendDiv(thisDiv);

        // insert checkboxes 
        var choicesContainer = $('#' + choicesDivId);

        var datasets = {};
        var curColor = 0;

        for (var j = 0; j < csvDatas.length; j++)
        {
            var eachCSVData = csvDatas[j];
            var entityName = eachCSVData.entity.name;
            var entityId = eachCSVData.entity.id;
            var fileSig = eachCSVData.file.sig;
            var csvData = eachCSVData.data;
            var keyVals = getKeyValues(eachCSVData.file);

            //var paragraph = $('<p />').html(eachCSVData.entity.name).css('font-weight', 'bold');
            //choicesContainer.append(paragraph);

            for (var i = 1; i < csvData.length; i++)
            {
                var curData = [];
                for (var k = 0; k < csvData[i].length; k++)
                    curData.push ([csvData[i][k].x, csvData[i][k].y]);

                var key = entityId + "-" + fileSig + "-" + i;
                var label = entityName;
                if (keyVals.length == csvData.length)
                    label += ", " + eachCSVData.file.keyName + " = " + keyVals[i] + " " + eachCSVData.file.keyUnits
                else if (csvData.length > 2)
                    label += " line " + i;
                datasets[key] = {label: label, data: curData, color: curColor};

                var colouredSpan = $('<span />').attr('id', colouredSpanIdPrefix + curColor)
                                                .addClass('flotColour')
                                                .html('&nbsp;&nbsp;');
                var inputId = 'id' + key;
                var newLabel = $('<label />').attr('for', inputId).html(label);
                var newInput = $('<input />').attr({ 'type': 'checkbox',
                                                     'name': key,
                                                     'checked': 'checked',
                                                     'id': inputId });
                choicesContainer.append(newInput).append(colouredSpan).append('&nbsp;').append(newLabel);
                curColor++;
                if (i!=csvData.length)
                {
                    choicesContainer.append('<br/>');
                }
            }
        }

        var plotProperties = {
            'choicesContainer': choicesContainer,
            'datasets': datasets,
            'styleLinespointsOrPoints': styleLinespointsOrPoints,
            'flotPlotDivId': flotPlotDivId,
            'x_label': x_label,
            'y_label': y_label
        };

        plotAccordingToChoices(plotProperties, undefined);
        /* legend generated when graph plotted, so this must follow the plot creation! */
        transferLegendColours(datasets);
        setListeners(plotProperties, true);
    }
};

contentFlotPlotComparer.prototype.show = function ()
{
    //console.log ("show");
    //console.log (this.div);
    if (!this.setUp)
    {
        this.file.getContents (this);
    }
    else
    {
        this.showContents ();
    }
};


function flotContent ()
{
    this.name = "displayPlotFlot";
    this.icon = "displayPlotFlot.png";
    this.description = "display graphs using flot library";
    
    addScript (contextPath + "/res/js/visualizers/displayPlotFlot/flot/jquery.flot.js");
    addScript (contextPath + "/res/js/visualizers/displayPlotFlot/flot/jquery.flot.navigate.min.js");
    addScript (contextPath + "/res/js/visualizers/displayPlotFlot/flot/jquery.flot.axislabels.js");
    addScript (contextPath + "/res/js/visualizers/displayPlotFlot/flot/jquery.flot.selection.js");
};

flotContent.prototype.canRead = function (file)
{
    var ext = file.name.split('.').pop();
    
    if (file.name && file.name == "outputs-default-plots.csv")
        return false;
    if (file.name && file.name == "outputs-contents.csv")
        return false;
    
    return ext == "csv";
};

flotContent.prototype.getName = function ()
{
    return this.name;
};

flotContent.prototype.getIcon = function ()
{
    return this.icon;
};

flotContent.prototype.getDescription = function ()
{
    return this.description;
};

flotContent.prototype.setUp = function (file, div)
{
    return new contentFlotPlot (file, div);
};

flotContent.prototype.setUpComparision = function (files, div)
{
    return new contentFlotPlotComparer (files, div);
};


function initFlotContent ()
{
    visualizers["displayPlotFlot"] = new flotContent ();
}

document.addEventListener("DOMContentLoaded", initFlotContent, false);

document.addEventListener("DOMContentLoaded", initFlotContent, false);

