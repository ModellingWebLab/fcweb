


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
		var csvData = getCSVColumnsDownsampled (this.file);
		
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
                //plot.polyline("line " + i, { x: csvData[0], y: csvData[i], stroke:  colorPalette.getRgba (col), thickness: 1 });
                datasets.push ({name : "line " + i, data: curData});
        }
        
        
		
		
		//$(id).plot (data, {});
		$("#"+id).highcharts({
	        title: {
	            text: 'some title'
	        },
	        plotOptions: {
	            series: {
	                allowPointSelect: true
	            }
	        },
	/*,plotOptions: {
	            series: {
	                cursor: 'pointer',
	                point: {
	                    events: {
	                        click: function() {
	                            hs.htmlExpand(null, {
	                                pageOrigin: {
	                                    x: this.pageX,
	                                    y: this.pageY
	                                },
	                                headingText: this.series.name,
	                                maincontentText: Highcharts.dateFormat('%A, %b %e, %Y', this.x) +':<br/> '+
	                                    this.y +' visits',
	                                width: 200
	                            });
	                        }
	                    }
	                },
	                marker: {
	                    lineWidth: 1
	                }
	            }
	        },*/
	
	        series: datasets/*[
		        {
		        	name: 'All visits',
		            lineWidth: 4,
		            marker:
		            {
		                radius: 4
		            }
		        },
		        {
		            name: 'New visitors'
		        }
		    ]*/
		});
	}
		
};

HCPlotter.prototype.show = function ()
{
	console.log ("show");
	console.log (this.div);
	if (!this.setUp)
		this.file.getContents (this);
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
	return file.type.match (/csv/gi) || file.name.split('.').pop() == "csv";
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




function initHCPlotContent ()
{
	visualizers["displayPlotHC"] = new HCPlot ();
}



document.addEventListener("DOMContentLoaded", initHCPlotContent, false);