
function HCPlotter (file, div)
{
	this.file = file;
	this.div = div;
	this.setUp = false;
	div.appendChild (document.createTextNode ("loading"));
};

HCPlotter.prototype.getContentsCallback = function (succ)
{
	//console.log ("insert content");
	//console.log (this.div);
    if ($(this.div).highcharts === undefined)
    {
        // Wait for the library to finish loading!
        var t = this;
        window.setTimeout(function(){t.getContentsCallback(succ)}, 100);
        return;
    }
    removeChildren (this.div);
	if (!succ)
		this.div.appendChild (document.createTextNode ("failed to load the contents"));
	else
	{
	    var thisFile = this.file;
		//var plotPoints = true;
		//var csvData = getCSVColumnsDownsampled (this.file);
		var csvData = (thisFile.linestyle == "linespoints" || thisFile.linestyle == "points") ? getCSVColumnsNonDownsampled (thisFile) : getCSVColumnsDownsampled (thisFile);
		
		var div = document.createElement("div");
		var id = "hcplot-" + thisFile.id;
		div.id = id;
		div.style.width = "780px";
		div.style.height = "450px";
		this.div.appendChild (div);
		

        var datasets = [];
        for (var i = 1; i < csvData.length; i++)
        {
                var curData = [];
                for (var j = 0; j < csvData[i].length; j++)
                        curData.push ([csvData[i][j].x, csvData[i][j].y]);
                //if (curData.length > 100)
                //	plotPoints = false;
                //plot.polyline("line " + i, { x: csvData[0], y: csvData[i], stroke:  colorPalette.getRgba (col), thickness: 1 });
                datasets.push ({name : "line " + i, data: curData});
        }
        
		var options = {
	        title: {
	            text: ''
	        },
	        plotOptions: {
	            series: {
	                allowPointSelect: true
	            },
	            line: {
	            	marker: {
	            		enabled: thisFile.linestyle == "linespoints"
	            	}
	            }
	        },
	
	        series: datasets
		};
		
		if (thisFile.xAxes)
			options.xAxis = {title : { text : thisFile.xAxes}};
		if (thisFile.yAxes)
			options.yAxis = {title : { text : thisFile.yAxes}};
		if (thisFile.title)
			options.title = {text : thisFile.title};
		
		
		$("#"+id).highcharts(options);
	}
		
};

HCPlotter.prototype.show = function ()
{
	if (!this.setUp)
		this.file.getContents (this);
};

function HCPlotterComparer (file, div)
{
	this.file = file;
	this.div = div;
	this.setUp = false;
	div.appendChild (document.createTextNode ("loading"));
	div.setAttribute ("class", "HighChartDiv");
	this.gotFileContents = 0;
	this.ok = true;
};

HCPlotterComparer.prototype.getContentsCallback = function (succ)
{
	//console.log ("getContentsCallback : " + succ + " -> so far: " + this.gotFileContents + " of " + this.file.entities.length);
	if (!succ)
		this.ok = false;
	
	this.gotFileContents++;
	
	if (this.gotFileContents >= this.file.entities.length)
		this.showContents ();
};

HCPlotterComparer.prototype.showContents = function ()
{
	//console.log ("insert content");
	//console.log (this.div);
    if ($(this.div).highcharts === undefined)
    {
        // Wait for the library to finish loading!
        var t = this;
        window.setTimeout(function(){t.showContents()}, 100);
        return;
    }
	removeChildren (this.div);
	if (!this.ok)
		this.div.appendChild (document.createTextNode ("failed to load the contents"));
	else
	{
		this.setUp = true;
		var thisFile = this.file;
		//var plotPoints = true;
		//var csvData = getCSVColumnsDownsampled (this.file);
		
		var lineStyle = thisFile.linestyle;
		
		var csvDatas = new Array ();
		
		for (var i = 0; i < thisFile.entities.length; i++)
		{
			csvDatas.push ({
					data: (lineStyle == "linespoints" || lineStyle == "points") ?
							getCSVColumnsNonDownsampled (thisFile.entities[i].entityFileLink) : getCSVColumnsDownsampled (thisFile.entities[i].entityFileLink),
					entity: thisFile.entities[i].entityLink,
					file: thisFile.entities[i].entityFileLink
			});
		}
		
		var div = document.createElement("div");
		var id = "hcplot-" + thisFile.id;
		div.id = id;
		div.style.width = "780px";
		div.style.height = "450px";
		this.div.appendChild (div);
		
		
        var datasets = [];

        for (var j = 0; j < csvDatas.length; j++)
        {
        	//console.log (csvDatas[j]);
        	//var tmp = "<p><strong>" + csvDatas[j].entity.name + ":</strong> ";
        	var csvData = csvDatas[j].data;
        	for (var i = 1; i < csvData.length; i++)
        	{
        		var curData = [];
	            for (var k = 0; k < csvData[i].length; k++)
	                curData.push ([csvData[i][k].x, csvData[i][k].y]);
	            
	            var key = csvDatas[j].entity.id + "-" + csvDatas[j].file.sig + "-" + i;
	            var label = csvDatas[j].entity.name + " line " + i;
	            //datasets[key] = {label : label, data: curData, color: curColor};
	            //curColor++;
                datasets.push ({name : label, data: curData});
	            
	            /*tmp += "<input type='checkbox' name='" + key +
                //"' id='id" + key + "'></input>" +
                "' checked='checked' id='id" + key + "'></input>" +
                "<label for='id" + key + "'>"
                + " line " + i + "</label>";*/
        	}
        	//choiceContainer.append(tmp + "</p>");
        	
        }
        
        
        
/*
        for (var i = 1; i < csvData.length; i++)
        {
                var curData = [];
                for (var j = 0; j < csvData[i].length; j++)
                        curData.push ([csvData[i][j].x, csvData[i][j].y]);
                //if (curData.length > 100)
                //	plotPoints = false;
                //plot.polyline("line " + i, { x: csvData[0], y: csvData[i], stroke:  colorPalette.getRgba (col), thickness: 1 });
        }*/
        
		var options = {
	        title: {
	            text: ''
	        },
	        plotOptions: {
	            series: {
	                allowPointSelect: true
	            },
	            line: {
	            	marker: {
	            		enabled: thisFile.linestyle == "linespoints"
	            	}
	            }
	        },
	
	        series: datasets
		};
		
		if (thisFile.xAxes)
			options.xAxis = {title : { text : thisFile.xAxes}};
		if (thisFile.yAxes)
			options.yAxis = {title : { text : thisFile.yAxes}};
		if (thisFile.title)
			options.title = {text : thisFile.title};
		
		
		$("#"+id).highcharts(options);
	}
		
};

HCPlotterComparer.prototype.show = function ()
{
	if (!this.setUp)
	{
		this.file.getContents (this);
	}
	else
		this.showContents ();
};

function HCPlot ()
{
	this.name = "displayPlotHC";
	this.icon = "displayPlotHC.png";
	this.description = "display graphs using HighChart library";
	
	var el = document.createElement('script');
	el.async = false;
	el.src = contextPath + "/res/js/visualizers/displayPlotHC/js/highcharts.js";//excanvas.min.js";
	el.type = 'text/javascript';

	(document.getElementsByTagName('head')[0]||document.body).appendChild(el);
};

HCPlot.prototype.canRead = function (file)
{
    return file.name.endsWith("gnuplot_data.csv");
//	if (file.name && file.name == "outputs-default-plots.csv")
//		return false;
//	if (file.name && file.name == "outputs-contents.csv")
//		return false;
//	
//	return (file.type && file.type.match (/csv/gi)) || file.name.split('.').pop() == "csv";
};

HCPlot.prototype.getName = function ()
{
	return this.name;
};

HCPlot.prototype.getIcon = function ()
{
	return this.icon;
};

HCPlot.prototype.getDescription = function ()
{
	return this.description;
};

HCPlot.prototype.setUp = function (file, div)
{
	return new HCPlotter (file, div);
};

HCPlot.prototype.setUpComparision = function (files, div)
{
	return new HCPlotterComparer (files, div);
};

function initHCPlotContent ()
{
	visualizers["displayPlotHC"] = new HCPlot ();
}

document.addEventListener("DOMContentLoaded", initHCPlotContent, false);