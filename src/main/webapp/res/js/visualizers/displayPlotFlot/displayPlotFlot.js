


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
		//console.log ("nondown -vs- down");
		//console.log (getCSVColumns (this.file));
		//console.log (getCSVColumnsDownsampled (this.file));
		
		var csvData = (THISfile.linestyle == "linespoints") ? getCSVColumnsNonDownsampled (this.file) : getCSVColumnsDownsampled (this.file);

		//var plotPoints = true;

        var div = document.createElement("div");
        div.id = "choices";
        this.div.appendChild (div);

        div = document.createElement("div");
        var id = "flotplot-" + this.file.id;
        div.id = id;
        div.style.width = "780px";
        div.style.height = "450px";


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
legendContainer.id = "legend";this.div.appendChild (legendContainer);


                function plotAccordingToChoices() {

                    var data = [];

                    choiceContainer.find("input:checked").each(function () {
                        var key = $(this).attr("name");
                        if (key && datasets[key]) {
                            data.push(datasets[key]);
                        }
                    });

                    if (data.length > 0) {
                        //$.plot("#flotplot-262", data, {
                        var settings = {
                            /*yaxis: {
                                min: 0
                            },*/
                            xaxis: {
                                tickDecimals: 0
                            }
,
lines: { show: true},



zoom: {
	interactive: true
	},
	pan: {
	interactive: true
	},
legend: {backgroundOpacity: 0,container: $("#legend")}
                        };
                        
                        if (THISfile.linestyle == "linespoints")
                        	settings.points = { show: true, radius:2};
                        
                        $.plot("#" + id, data, settings);
                    }
                };
                
                
                choiceContainer.find("input").click(plotAccordingToChoices);

                plotAccordingToChoices();
        }

		
};

contentFlotPlot.prototype.show = function ()
{
	console.log ("show");
	console.log (this.div);
	if (!this.setUp)
		this.file.getContents (this);
};






function flotContent ()
{
	this.name = "displayPlotFlot";
	this.icon = "displayPlotFlot.png";
	this.description = "display graphs using flot library";
	
	addScript (contextPath + "/res/js/visualizers/displayPlotFlot/flot/jquery.flot.js");
	addScript (contextPath + "/res/js/visualizers/displayPlotFlot/flot/jquery.flot.navigate.min.js");
	//addScript (contextPath + "/res/js/visualizers/displayPlotFlot/flot/jquery.flot.navigationControl.js");
};

flotContent.prototype.canRead = function (file)
{
	var ext = file.name.split('.').pop();
	
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




function initFlotContent ()
{
	visualizers["displayPlotFlot"] = new flotContent ();
}



document.addEventListener("DOMContentLoaded", initFlotContent, false);