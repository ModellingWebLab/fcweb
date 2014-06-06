/**
 * Create the 'visualiser' portion of the plugin, responsible for displaying content within the div for this file.
 */
function metadataEditor(file, div)
{
    this.file = file;
    this.div = div;
    this.setUp = false;
    div.appendChild(document.createTextNode("loading..."));
};

/**
 * This is called when the file to be edited has been fetched from the server,
 * and parses it and sets up the editing UI.
 */
metadataEditor.prototype.getContentsCallback = function (succ)
{
    removeChildren (this.div); // Remove loading indicator
    if (!succ)
        this.div.appendChild (document.createTextNode ("failed to load the contents"));
    else
    {
        if ($.rdf === undefined)
        {
            /// Wait 0.1s for rdfquery to load and try again
            console.log("Waiting for rdfquery to load.");
            var editor = this;
            window.setTimeout(function(){editor.getContentsCallback(true);}, 100);
            return;
        }
        this.model = $.parseXML(this.file.contents);
        var rdf_nodes = this.model.getElementsByTagNameNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "RDF");
        var source_variables = $(this.model).find("component > variable").not("[public_interface='in']").not("[private_interface='in']");
        this.div.innerHTML = "<p>Found " + rdf_nodes.length + " RDF nodes and " + source_variables.length + " variables.</p>";
        var table = this.div.appendChild(document.createElement("table")),
            tr = table.appendChild(document.createElement("tr")),
            td_vars = tr.appendChild(document.createElement("td")),
            td_meta = tr.appendChild(document.createElement("td")),
            vars_ul = td_vars.appendChild(document.createElement("ul"));
        source_variables.each(function (index) {
            var li = document.createElement("li");
            li.innerHTML = "Var " + index + ": " + this.parentNode.getAttribute("name") + ", " + this.getAttribute("name")
                + " [" + this.getAttributeNS("http://www.cellml.org/metadata/1.0#", "id") + "]";
            vars_ul.appendChild(li);
        });
        console.log("Our base: " + window.location.protocol + '//' + window.location.host + this.file.url);
        var rdf_store = $.rdf.databank([]),
            rdf = $.rdf({//databank: rdf_store,
                         base: $.uri.absolute(window.location.protocol + '//' + window.location.host + this.file.url)
                         //base: 'test'
                        }).prefix('bqbiol' ,'http://biomodels.net/biology-qualifiers/');
        console.log("Store base: " + rdf.base());
        console.log("Databank base: " + rdf.databank.base());
        $(rdf_nodes).each(function () {
            var doc_type = document.implementation.createDocumentType("RDF", "", ""),
                new_doc = document.implementation.createDocument("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "RDF", doc_type);
            console.log("Frag URI: " + new_doc.documentURI);
            console.log("New doc node base: " + new_doc.documentElement.baseURI);
            $(this).children().each(function(){
                var new_elt = new_doc.adoptNode(this);
                console.log("Will load node with base: " + new_elt.baseURI);
                new_doc.documentElement.appendChild(new_elt);
                new_doc.documentElement.setAttributeNS('http://www.w3.org/XML/1998/namespace', 'base', rdf.base());
                console.log(new XMLSerializer().serializeToString(new_doc));
            });
            rdf.load(new_doc, {});//, {base: rdf.base()});
            console.log("Size now " + rdf.databank.size() + " " + rdf.size());
        });
        console.log(rdf);
        var meta_ul = td_meta.appendChild(document.createElement("ul"));
        rdf.where('?s bqbiol:is ?o').each(function(i, bindings, triples){
            var li = meta_ul.appendChild(document.createElement("li"));
            li.appendChild(document.createTextNode(triples[0].toString()));
        });
//        td_meta.appendChild(document.createTextNode(rdf_store.dump({format: 'application/rdf+xml', serialize: true})));
    }
};

/**
 * Called to generate the content for this visualiser plugin.
 * Actually just triggers a fetch of the file contents, with our getContentsCallback method
 * doing the work when this completes.
 */
metadataEditor.prototype.show = function ()
{
    if (!this.setUp)
        this.file.getContents(this);
};


/**
 * A 'visualiser' plugin for editing the metadata in a CellML model.
 */
function editMetadata()
{
    this.name = "editMetadata";
    this.icon = "editMetadata.png";
    this.description = "edit the metadata annotations in this model";

    addScript(contextPath + "/res/js/3rd/jquery.rdfquery.core-1.0.js");
};

/**
 * Determine whether this plugin can be applied to the given file.
 * Checks whether the file is marked as CellML, or has a .cellml extension.
 */
editMetadata.prototype.canRead = function (file)
{
    if (file.type == 'CellML')
        return true;
    var ext = file.name.split('.').pop();
    return (ext == 'cellml');
};

/** Get the name of this plugin. */
editMetadata.prototype.getName = function ()
{
    return this.name;
};

/** Get the icon filename for displaying this plugin. */
editMetadata.prototype.getIcon = function ()
{
    return this.icon;
};

/** Get the brief description of this plugin. */
editMetadata.prototype.getDescription = function ()
{
    return this.description;
};

/**
 * Create the visualiser UI for a specific file, to display within the given div.
 * This must provide a show() method which will be called to generate the content.
 */
editMetadata.prototype.setUp = function (file, div)
{
    return new metadataEditor(file, div);
};

/**
 * Add ourselves to the available plugins for 'visualising' entities.
 */
function initEditMetadata ()
{
    visualizers["editMetadata"] = new editMetadata();
}

document.addEventListener("DOMContentLoaded", initEditMetadata, false);
