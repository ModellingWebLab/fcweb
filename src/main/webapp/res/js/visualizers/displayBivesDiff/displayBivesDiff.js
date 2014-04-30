
function bivesDiffer (file, div)
{
	this.file = file;
	this.div = div;
	this.setUp = false;
	
	this.formerFile = null;
	this.laterFile = null;
	this.diffs = new Array ();
	
	var jqDiv = $(div);

	var table = $("<table></table>").append ("<thead>" +
			"<tr><th>Available Versions</th><th>select as predecessor</th><th>select as successor</th>" +
			"</thead>");
	var tableBody = $("<tbody></tbody>");
	table.append (tableBody);
	
	this.displayer = $("<div></div>").addClass ("bivesDiffDisplayer");
	jqDiv.append (table).append (this.displayer);

	for (var i = 0; i < this.file.entities.length; i++)
	{
		var tr = $("<tr></tr>").addClass ("bivesDiffFileVersionTableRow");
		
		var name = $("<td></td>").text (this.file.entities[i].entityLink.name + " - " + this.file.entities[i].entityLink.version);
		var prev = $("<input type='radio' name='former'/>");
		var succ = $("<input type='radio' name='later'/>");
		
		tr.append (name).append ($("<td></td>").append (prev)).append ($("<td></td>").append (succ));
		tableBody.append (tr);
		
		this.formerClickListener (prev, this.file.entities[i], tr);
		this.laterClickListener (succ, this.file.entities[i], tr);
	}
};

bivesDiffer.prototype.formerClickListener = function (former, file, tr)
{
	var outer = this;
	former.click (function () 
	{
		$(".bivesDiffFileVersionTableRow").each (function () {$(this).removeClass ("bivesDiffDel");});
		outer.formerFile = file;
		tr.addClass ("bivesDiffDel");
		outer.showDiff ();
	});
};

bivesDiffer.prototype.laterClickListener = function (later, file, tr)
{
	var outer = this;
	later.click (function () 
	{
		$(".bivesDiffFileVersionTableRow").each (function () {$(this).removeClass ("bivesDiffIns");});
		outer.laterFile = file;
		tr.addClass ("bivesDiffIns");
		outer.showDiff ();
	});
};

bivesDiffer.prototype.computeDifferences = function (former, later, matrixKey)
{
	/*
	 * this is to be implemented
	 * var request = {
			task: "getUnixDiff",
			entity1: former.entityLink.id,
			file1: former.entityFileLink.id,
			entity2: later.entityLink.id,
			file2: later.entityFileLink.id
	};
	
	var diffs = this.diffs;
	
	$.post (document.location.href, JSON.stringify(request)).done (function (data)
	{
		console.log (data);
		if (data && data.getUnixDiff && data.getUnixDiff.response)
		{
			var diff = data.getUnixDiff.unixDiff;
			
			diff = diff
			// stop thinking these are tags
			.replace (/</g, "&lt;")
			.replace (/>/g, "&gt;")
			// highlight line numbers
			.replace (/^(\d.*)$/gm, "<strong>$1</strong>")
			// highlight inserted/deleted stuff
			.replace (/^(&lt;.*)$/gm, "<span class='unixDiffDel'>$1</span>")
			.replace (/^(&gt;.*)$/gm, "<span class='unixDiffIns'>$1</span>")
			;

			diffs[matrixKey].empty ().append (
					"<strong>Differences</strong> between <strong>" + former.entityLink.name + "</strong> - <strong>" + former.entityLink.version + "</strong> and <strong>" + later.entityLink.name + "</strong> - <strong>" + later.entityLink.version + "</strong>")
					.append ("<pre>"+diff+"</pre>");
		}
		else
			diffs[matrixKey].empty ().append ("failed to compute the differences");
	}).fail (function () 
	{
		diffs[matrixKey].empty ().append ("failed to compute the differences");
	});*/
};

bivesDiffer.prototype.showDiff = function ()
{
	if (this.laterFile && this.formerFile)
	{
		var matrixKey = this.formerFile.entityLink.name + "--" + this.formerFile.entityLink.version + "--" + this.laterFile.entityLink.name + "--" + this.laterFile.entityLink.version;
		if (!this.diffs[matrixKey])
		{
			// compute the diff and show it afterwards
			this.diffs[matrixKey] = $("<div></div>").text ("calling BiVeS from WHICH BIVES URL?");
			this.computeDifferences (this.formerFile, this.laterFile, matrixKey);
		}

		// show diff
		this.displayer.empty ().append (this.diffs[matrixKey]);
	}
};

bivesDiffer.prototype.getContentsCallback = function (succ)
{
	
};

bivesDiffer.prototype.show = function ()
{
	
};


function bivesDiffContent ()
{
    this.name = "displayBivesDiff";
    this.icon = "displayBivesDiff.png";
    this.description = "use BiVeS to compare versions";
};

bivesDiffContent.prototype.canRead = function (file)
{
	var allowedExt = [
	                  "xmlprotocol",
	                  "xml",
	                  "cellml",
	                  "cpp",
	                  "hpp",
	                  "txt",
	                  "gp"
	                  // to be extended?
	                  ];
	
	for (var i = 0; i < allowedExt.length; i++)
		if (file.name.endsWith(allowedExt[i]))
			return true;
	
	return false;
};

bivesDiffContent.prototype.getName = function ()
{
    return this.name;
};

bivesDiffContent.prototype.getIcon = function ()
{
    return this.icon;
};

bivesDiffContent.prototype.getDescription = function ()
{
    return this.description;
};

bivesDiffContent.prototype.setUpComparision = function (files, div)
{
    return new bivesDiffer (files, div);
};


function initbivesDiffContent ()
{
    visualizers["displayBivesDiff"] = new bivesDiffContent ();
}

document.addEventListener("DOMContentLoaded", initbivesDiffContent, false);