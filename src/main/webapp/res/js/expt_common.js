/*
 * Routines related to displaying the results of experiments, common to both
 * entity.js and compare.js.
 *
 */

/**
 * Sort the list of results files from this experiment, so similar files are grouped together,
 * with those defined as plot data appearing first.
 * 
 * Note that when comparing experiments this method gets called multiple times (once for each
 * of the compared experiments which has both metadata files) so the sorting has to be cumulative:
 * any one experiment might not list all the possible plots, if it didn't fully complete.
 */
function sortTable (plots)
{
    // What remains to be sorted?
    var to_be_sorted;
    if (filesTable.beenSorted)
    {
        to_be_sorted = new Array ();
        // Add in filesTable.other and filesTable.otherCSV
        var adder = function(key, idx, arr)
        {
            to_be_sorted.push(this[key]);
        }
        Object.keys(filesTable.otherCSV).forEach(adder, filesTable.otherCSV);
        Object.keys(filesTable.other).forEach(adder, filesTable.other);
        filesTable.otherCSV = {};
        filesTable.other = {};
    }
    else
    {
        to_be_sorted = filesTable.all;
    }

    // Split the file list into categories
    for (var i = 0; i < to_be_sorted.length; i++)
    {
        var f = to_be_sorted[i];
        var found = false;
        for (var j = 0; j < plots.length; j++)
            if (f.name == plots[j])
            {
                filesTable.plots[f.name] = f;
                found = true;
                break;
            }
        if (found)
            continue;
        if (f.name.endsWith ("png") || f.name.endsWith ("eps"))
            filesTable.pngeps[f.name] = f;
        else if (f.name == "outputs-default-plots.csv" || f.name == "outputs-contents.csv")
            filesTable.defaults[f.name] = f;
        else if (f.name.endsWith ("csv"))
            filesTable.otherCSV[f.name] = f;
        else if (f.name.endsWith ("txt"))
            filesTable.text[f.name] = f;
        else 
            filesTable.other[f.name] = f;
    }

    // Figure out how many columns the table has (could be 3 or 4, depending on whether comparing!)
    var colCount = $(filesTable.table).find('tr:first th').length;

    // Function to sort each category individually
    var resortPartially = function (arr, css, title, startHidden)
    {
        var cur = keys(arr).sort();
//        console.log("Resorting " + css + " " + cur.length);
        if (cur.length > 0)
        {
            // Create/find the header row for this section
            var header;
            if (filesTable.beenSorted)
            {
                header = $("#filesTable-header-" + css).get(0);
                filesTable.table.removeChild(header);
            }
            else
            {
                header = document.createElement("tr");
                header.id = "filesTable-header-" + css;
                $(header).addClass("filesTable-" + css).addClass("filesTable-header");
                header.innerHTML = "<th colspan='" + colCount + "' class='filesTable-header-shown'>" + title + "</th>";
                // Make a click on the header toggle visibility of the rest of the section
                $(header).click(function() {
                    $(".filesTable-" + css).not(header).toggle("fast");
                    if ($(header).children().hasClass("filesTable-header-shown"))
                        $(header).children().removeClass("filesTable-header-shown").addClass("filesTable-header-hidden");
                    else
                        $(header).children().removeClass("filesTable-header-hidden").addClass("filesTable-header-shown");
                });
            }
            filesTable.table.appendChild(header);
            // Append the rows for this section, ordered by file name
            for (var i = 0; i < cur.length; i++)
            {
                $(arr[cur[i]].row).addClass ("filesTable-" + css);
                filesTable.table.removeChild (arr[cur[i]].row);
                filesTable.table.appendChild (arr[cur[i]].row);
            }
            if (startHidden)
            {
                $(".filesTable-" + css).not(header).hide();
                $(header).children().removeClass("filesTable-header-shown").addClass("filesTable-header-hidden");
            }
        }
    };

    // Put the categories in order, and sort them
    resortPartially (filesTable.plots, "plots", "Plottable result data", false);
    resortPartially (filesTable.defaults, "defaults", "Result metadata", false);
    resortPartially (filesTable.text, "text", "Experiment information", false);
    resortPartially (filesTable.pngeps, "pngeps", "Pre-generated figures", true);
    resortPartially (filesTable.otherCSV, "otherCSV", "Other result data", true);
    resortPartially (filesTable.other, "other", "Files mainly of use for debugging", true);

    // Remember that we've been called!
    filesTable.beenSorted = true;
}
