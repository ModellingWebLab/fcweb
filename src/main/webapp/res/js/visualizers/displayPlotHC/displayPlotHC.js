


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
	removeChildren (this.div);
	if (!succ)
		this.div.appendChild (document.createTextNode ("failed to load the contents"));
	else
	{
		//var plotPoints = true;
		//var csvData = getCSVColumnsDownsampled (this.file);
		var csvData = (this.file.linestyle == "linespoints" || THISfile.linestyle == "points") ? getCSVColumnsNonDownsampled (this.file) : getCSVColumnsDownsampled (this.file);
		
		var div = document.createElement("div");
		var id = "hcplot-" + this.file.id;
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
	            		enabled: this.file.linestyle == "linespoints"
	            	}
	            }
	        },
	
	        series: datasets
		};
		
		if (this.file.xAxes)
			options.xAxis = {title : { text : this.file.xAxes}};
		if (this.file.yAxes)
			options.yAxis = {title : { text : this.file.yAxes}};
		if (this.file.title)
			options.title = {text : this.file.title};
		
		
		$("#"+id).highcharts(options);
	}
		
};

HCPlotter.prototype.show = function ()
{
	console.log ("show");
	console.log (this.div);
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
	removeChildren (this.div);
	if (!this.ok)
		this.div.appendChild (document.createTextNode ("failed to load the contents"));
	else
	{
		this.setUp = true;
		//var plotPoints = true;
		//var csvData = getCSVColumnsDownsampled (this.file);
		
		var lineStyle = this.file.linestyle;
		
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
		
		
		
		
		
		
		
		//var csvData = (this.file.linestyle == "linespoints" || THISfile.linestyle == "points") ? getCSVColumnsNonDownsampled (this.file) : getCSVColumnsDownsampled (this.file);
		
		var div = document.createElement("div");
		var id = "hcplot-" + this.file.id;
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
	            		enabled: this.file.linestyle == "linespoints"
	            	}
	            }
	        },
	
	        series: datasets
		};
		
		if (this.file.xAxes)
			options.xAxis = {title : { text : this.file.xAxes}};
		if (this.file.yAxes)
			options.yAxis = {title : { text : this.file.yAxes}};
		if (this.file.title)
			options.title = {text : this.file.title};
		
		
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
	return (file.type && file.type.match (/csv/gi)) || file.name.split('.').pop() == "csv";
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