
/**
 * Create the 'visualiser' portion of the plugin, responsible for displaying content within the div for this file.
 */
function metadataEditor(file, div)
{
    this.file = file;
    this.div = div;
    this.loadedModel = false;
    this.loadedOntology = false;
    this.modelDiv = $('<div></div>', {id: 'editmeta_modelvars_div'}).text('loading model...');
    this.ontoDiv = $('<div></div>', {id: 'editmeta_ontoterms_div'}).text('loading available annotations...');
    $(div).append(this.modelDiv).append(this.ontoDiv);
};

//$( "#result" ).load(contextPath + '/res/visualizers/editMetadata/pageOutline.html',
//        function(response, status, xhr){if (status == "error") ... });
//$.ajax(contextPath + '/res/visualizers/editMetadata/pageOutline.html',
//       {dataType: 'html',
//        success: function(data, status, jqXHR) {}
//       });

/**
 * This is called when the file to be edited has been fetched from the server,
 * and parses it and sets up the editing UI.
 */
metadataEditor.prototype.getContentsCallback = function (succ)
{
    var self = this;
    if (!succ)
        this.modelDiv.text("failed to load the contents");
    else
    {
        if ($.rdf === undefined)
        {
            /// Wait 0.1s for rdfquery to load and try again
            console.log("Waiting for rdfquery to load.");
            window.setTimeout(function(){self.getContentsCallback(true);}, 100);
            return;
        }
        this.loadedModel = true;
        this.model = $.parseXML(this.file.contents);
        this.modelBaseUri = $.uri.absolute(window.location.protocol + '//' + window.location.host + this.file.url);
        console.log(this.modelBaseUri);
        var $model = $(this.model);
        this.modelDiv.empty();

        // Find the variables that can be annotated, and display as a nested list in their components
        var components = $model.find("component"),
            var_list = document.createElement("ul");
        this.components = {};
        this.vars_by_name = {};
        this.vars_by_uri = {};
        components.each(function() {
            // Store component info and create display items
            var li = document.createElement("li"),
                clist = document.createElement("ul"),
                c = {li: li, ul: clist, elt: this, name: this.getAttribute('name'), vars: []};
            self.components[c.name] = c;
            li.innerHTML = '<span class="editmeta_cname">' + c.name + '</span>';
            li.appendChild(clist);
            var_list.appendChild(li);
            // Toggle display of this component's variables on click of the component name
            $('span', li).click(function (ev) {
                $(clist).toggle();
            });
            $(clist).hide();
            // Find variables in this component
            $(this).children('variable[public_interface != "in"][private_interface != "in"]').each(function() {
                var li = document.createElement("li"),
                    v = {li: li, elt: this, name: this.getAttribute('name'),
                         metaid: this.getAttributeNS("http://www.cellml.org/metadata/1.0#", "id"),
                         annotations: []};
                c.vars.push(v);
                v.fullname = c.name + ':' + v.name;
                self.vars_by_name[v.fullname] = v;
                if (v.metaid)
                    self.vars_by_uri[self.modelBaseUri.toString() + '#' + v.metaid] = v;
                li.innerHTML = '<span class="editmeta_vname">' + v.name + '</span>';
                clist.appendChild(li);
            });
        });
        console.log("Found " + keys(this.vars_by_name).length + " variables");
        this.modelDiv.append("<h4>Model variables:</h4>", var_list);

        // Find the existing annotations
        var rdf_nodes = this.model.getElementsByTagNameNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "RDF");
        console.log("Found " + rdf_nodes.length + " RDF nodes");
        var rdf = $.rdf({base: this.modelBaseUri})
                   .prefix('bqbiol', 'http://biomodels.net/biology-qualifiers/')
                   .prefix('rdfs', 'http://www.w3.org/2000/01/rdf-schema#');
        $(rdf_nodes).each(function () {
            var doc_type = document.implementation.createDocumentType("RDF", "", ""),
                new_doc = document.implementation.createDocument("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "RDF", doc_type);
            $(this).children().each(function(){
                var new_elt = new_doc.adoptNode(this);
                new_doc.documentElement.appendChild(new_elt);
                new_doc.documentElement.setAttributeNS('http://www.w3.org/XML/1998/namespace', 'base', rdf.base());
            });
            rdf.load(new_doc, {});
        });
        console.log("Found " + rdf.databank.size() + " triples");
        console.log(rdf);
        rdf.where('?s bqbiol:is ?o')
           .optional('?o rdfs:label ?label')
           .optional('?o rdfs:comment ?comment')
           .each(function(i, bindings, triples){
            var v = self.vars_by_uri[this.s.value.toString()];
            if (v === undefined)
                console.log("Annotation of non-existent id! " + this.s + " is " + this.o);
            else
            {
                var s = $('<span></span>', {'class': 'editmeta_annotation'});
                s.text(this.o.value.fragment);
                v.annotations.push({ann: this.o, span: s});
                v.li.appendChild(s.get(0));
            }
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
    if (!this.loadedModel)
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
