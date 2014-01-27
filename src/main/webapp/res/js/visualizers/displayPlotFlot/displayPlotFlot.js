


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
	//console.log ("insert content");
	//console.log (this.div);
	var THISfile = this.file;
	removeChildren (this.div);
	if (!succ)
		this.div.appendChild (document.createTextNode ("failed to load the contents"));
	else
	{
		//console.log (getCSVColumns (this.file));
		//console.log (getCSVColumnsDownsampled (this.file));
		
		var csvData = (THISfile.linestyle == "linespoints" || THISfile.linestyle == "points") ? getCSVColumnsNonDownsampled (this.file) : getCSVColumnsDownsampled (this.file);

		//var plotPoints = true;

        var div = document.createElement("div");
        div.id = "choices";
        this.div.appendChild (div);

        div = document.createElement("div");
        var id = "flotplot-" + this.file.id;
        div.id = id;
        div.style.width = "780px";
        div.style.height = "450px";
        
        // Some of the plots won't come from specified plots, so these are missing.
    	var x_label = "";
    	var y_label = "";    	
    	if (THISfile.xAxes) {
    		x_label = THISfile.xAxes;
    	}
    	if (THISfile.yAxes) {
    		y_label = THISfile.yAxes;
    	}


        var datasets = {};
        for (var i = 1; i < csvData.length; i++)
        {
            var curData = [];
            for (var j = 0; j < csvData[i].length; j++)
                    curData.push ([csvData[i][j].x, csvData[i][j].y]);

            //if (curData.length > 100)
            	//plotPoints = false;
            //plot.polyline("line " + i, { x: csvData[0], y: csvData[i], stroke:  colorPalette.getRgba (col), thickness: 1 });
            datasets["line" + i] = {label : "line " + i, data: curData};
        }

        this.div.appendChild (div);

        // hard-code color indices to prevent them from shifting as
        // countries are turned on/off

        var i = 0;
        $.each(datasets, function(key, val) {
            val.color = i;
            ++i;
        });


        // insert checkboxes 
        var choiceContainer = $("#choices");
        $.each(datasets, function(key, val) {
            choiceContainer.append("<input type='checkbox' name='" + key +
                //"' id='id" + key + "'></input>" +
                "' checked='checked' id='id" + key + "'></input>" +
                "<label for='id" + key + "'>"
                + val.label + "</label>");
        });

        var legendContainer =  document.createElement("div");
        legendContainer.id = "legend";
        this.div.appendChild (legendContainer);


        function plotAccordingToChoices() {

            var data = [];

            choiceContainer.find("input:checked").each(function () {
                var key = $(this).attr("name");
                if (key && datasets[key]) {
                    data.push(datasets[key]);
                }
            });

            //if (data.length > 0) {
                //$plot("#flotplot-262", data, {
                       
                var settings = {
                    xaxis: { tickDecimals: 0, 
                             position: 'bottom', 
                             axisLabel: x_label, 
                             axisLabelPadding: 10, 
                             axisLabelUseCanvas: true  },
                    yaxis: { position: 'left', 
                             axisLabel: y_label, 
                             axisLabelPadding: 10, 
                             axisLabelUseCanvas: true},
                    lines: { show: true},
                    zoom: {	interactive: true },
                    pan: { interactive: true },
                    legend: {backgroundOpacity: 0,container: $("#legend")} 
                };
                            
                if (THISfile.linestyle == "linespoints" || THISfile.linestyle == "points")
                    settings.points = { show: true, radius:2};
                    $.plot("#" + id, data, settings);
            //}
        };
                
        choiceContainer.find("input").click(plotAccordingToChoices);

        plotAccordingToChoices();
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
	this.ok = true;
};

contentFlotPlotComparer.prototype.getContentsCallback = function (succ)
{
	//console.log ("getContentsCallback : " + succ + " -> so far: " + this.gotFileContents + " of " + this.file.entities.length);
	if (!succ)
		this.ok = false;
	
	this.gotFileContents++;
	
	if (this.gotFileContents >= this.file.entities.length)
		this.showContents ();
};

contentFlotPlotComparer.prototype.showContents = function ()
{
	//console.log ("insert content");
	//console.log (this.div);
	//var THISfile = this.file;
	removeChildren (this.div);
	if (!this.ok)
		this.div.appendChild (document.createTextNode ("failed to load the contents"));
	else
	{
		this.setUp = true;
		//this.div.appendChild (document.createTextNode ("loaded"));
		//console.log ("nondown -vs- down");
		//console.log (getCSVColumns (this.file));
		//console.log (getCSVColumnsDownsampled (this.file));
		console.log (this.file);
		
		var lineStyle = this.file.linestyle;
		
		// Some of the plots won't come from specified plots, so these are missing.
    	var x_label = "";
    	var y_label = "";    	
    	if (this.file.xAxes) {
    		x_label = this.file.xAxes;
    	}
    	if (this.file.yAxes) {
    		y_label = this.file.yAxes;
    	}
		
		var csvDatas = new Array ();
		
		for (var i = 0; i < this.file.entities.length; i++)
		{
			csvDatas.push ({
					data: (lineStyle == "linespoints" || lineStyle == "points") ?
							getCSVColumnsNonDownsampled (this.file.entities[i].entityFileLink) : getCSVColumnsDownsampled (this.file.entities[i].entityFileLink),
					entity: this.file.entities[i].entityLink,
					file: this.file.entities[i].entityFileLink
			});
		}

		//var plotPoints = true;

        var div = document.createElement("div");
        div.id = "choices";
        this.div.appendChild (div);

        div = document.createElement("div");
        var id = "flotplot-" + this.file.sig;
        div.id = id;
        div.style.width = "780px";
        div.style.height = "450px";

        // insert checkboxes 
        var choiceContainer = $("#choices");
                
        var datasets = {};
        var curColor = 0;

        for (var j = 0; j < csvDatas.length; j++)
        {
        	//console.log (csvDatas[j]);
        	var tmp = "<p><strong>" + csvDatas[j].entity.name + ":</strong> ";
        	var csvData = csvDatas[j].data;
        	for (var i = 1; i < csvData.length; i++)
        	{
        		var curData = [];
	            for (var k = 0; k < csvData[i].length; k++)
	                curData.push ([csvData[i][k].x, csvData[i][k].y]);
	            
	            var key = csvDatas[j].entity.id + "-" + csvDatas[j].file.sig + "-" + i;
	            var label = csvDatas[j].entity.name + " line " + i;
	            datasets[key] = {label : label, data: curData, color: curColor};
	            curColor++;
	            
	            tmp += "<input type='checkbox' name='" + key +
                //"' id='id" + key + "'></input>" +
                "' checked='checked' id='id" + key + "'></input>" +
                "<label for='id" + key + "'>"
                + " line " + i + "</label>";
        	}
        	choiceContainer.append(tmp + "</p>");
        	
        }
    	//console.log (datasets);

        this.div.appendChild (div);
                
        var legendContainer =  document.createElement("div");
        legendContainer.id = "legend";
        this.div.appendChild (legendContainer);


        function plotAccordingToChoices() {

            var data = [];

            choiceContainer.find("input:checked").each(function () {
                var key = $(this).attr("name");
                if (key && datasets[key]) {
                    data.push(datasets[key]);
                }
            });      
            
            //if (data.length > 0) {
                var settings = {
                    xaxis: { tickDecimals: 0,
                             position: 'bottom', 
                             axisLabel: x_label, 
                             axisLabelPadding: 10, 
                             axisLabelUseCanvas: true },
                    yaxis: { position: 'left', 
                             axisLabel: y_label, 
                             axisLabelPadding: 10, 
                             axisLabelUseCanvas: true },
                    lines: { show: true},
                    zoom: {	interactive: true },
                    pan: { interactive: true },
                    legend: {backgroundOpacity: 0,container: $("#legend")}
                };
                        
                if (lineStyle == "linespoints" || lineStyle == "points")
                	settings.points = { show: true, radius:2};
                
                $.plot("#" + id, data, settings);
            //}
            
        };
                
        choiceContainer.find("input").click(plotAccordingToChoices);

        plotAccordingToChoices ();
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
		this.showContents ();
};


function flotContent ()
{
	this.name = "displayPlotFlot";
	this.icon = "displayPlotFlot.png";
	this.description = "display graphs using flot library";
	
	addScript (contextPath + "/res/js/visualizers/displayPlotFlot/flot/jquery.flot.js");
	addScript (contextPath + "/res/js/visualizers/displayPlotFlot/flot/jquery.flot.navigate.min.js");
	addScript (contextPath + "/res/js/visualizers/displayPlotFlot/flot/jquery.flot.axislabels.js");
	//addScript (contextPath + "/res/js/visualizers/displayPlotFlot/flot/jquery.flot.navigationControl.js");
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