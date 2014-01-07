
function submitBatch (jsonObject, actionIndicator)
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
    
    xmlhttp.open("POST", document.location.href, true);
    xmlhttp.setRequestHeader("Content-type", "application/json");

    xmlhttp.onreadystatechange = function()
    {
        if(xmlhttp.readyState != 4)
        	return;
        
    	var json = JSON.parse(xmlhttp.responseText);
    	console.log (json);
    	displayNotifications (json);
    	
        if(xmlhttp.status == 200)
        {
        	if (json.batchSubmit)
        	{
	        	var msg = json.batchSubmit.responseText;
	        	if (json.batchSubmit.response)
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


function initBatch ()
{
	var btn = document.getElementById("batchcreator");
	var actionIndicator = document.getElementById("batchcreatoraction");

	if (btn)
		btn.addEventListener("click", 
        function (event)
        {
			var toCreate = [];
			var toForce = false;
			var boxes = document.getElementsByTagName("input");
			for (var i = 0; i < boxes.length; i++)
			{
				if (boxes[i].type != "checkbox")
					continue;
				if (boxes[i].checked)
				{
					if (boxes[i].id == "forceoverwrite")
						toForce = true;
					else
						toCreate.push(boxes[i].name);
				}
			}
			
			console.log(toCreate);
			console.log(toForce);
			submitBatch ({
		    	task: "batchSubmit",
		    	entities: toCreate,
		    	force: toForce
		    }, actionIndicator);
        }, 
        false);
	

	var checker = document.getElementById("checkAll");
	var checkerLatest = document.getElementById("checkLatest");
	var unchecker = document.getElementById("uncheckAll");
	checker.addEventListener("click", function ()
	{
		var boxes = document.getElementsByTagName("input");
		for (var i = 0; i < boxes.length; i++)
		{
			if (boxes[i].type != "checkbox" || boxes[i].id == "forceoverwrite")
				continue;
			boxes[i].checked = true;
		}
	}, false);
	checkerLatest.addEventListener("click", function ()
	{
		$( ".latestVersion" ).prop('checked', true);
	}, false);
	unchecker.addEventListener("click", function ()
	{
		var boxes = document.getElementsByTagName("input");
		for (var i = 0; i < boxes.length; i++)
		{
			if (boxes[i].type != "checkbox" || boxes[i].id == "forceoverwrite")
				continue;
			boxes[i].checked = false;
		}
	}, false);
}

document.addEventListener("DOMContentLoaded", initBatch, false);