
/**
 * Create the 'visualiser' portion of the plugin, responsible for displaying content within the div for this file.
 */
function metadataEditor(file, div)
{
    this.file = file;
    this.div = div;
    div.id = 'editmeta_main_div';
    this.loadedModel = false;
    this.loadedOntology = false;
    this.init();
    this.modelDiv = $('<div></div>', {id: 'editmeta_modelvars_div'}).text('loading model...');
    this.ontoDiv = $('<div></div>', {id: 'editmeta_ontoterms_div'}).text('loading available annotations...');
    this.dragDiv = $('<div></div>', {'class': 'editmeta_annotation', 'style': 'position: fixed;'});
    $(div).append(this.modelDiv, this.ontoDiv, this.dragDiv);
};

/**
 * If our required libraries are not yet present, wait until they are then call the callback.
 * Returns true iff a wait is needed.
 */
function waitForLibraries (self, callback, timeout)
{
    timeout = timeout || 150;
    if (self.rdf === undefined || $.ui === undefined)
    {
        console.log("Waiting for libraries to load.");
        window.setTimeout(callback, timeout);
        return true;
    }
    return false;
}

/**
 * Initialisation that depends on rdfQuery being available; waits until it is before proceeding.
 */
metadataEditor.prototype.init = function ()
{
    if ($.rdf === undefined)
    {
        var self = this;
        /// Wait 0.1s for rdfquery to load and try again
        console.log("Waiting for rdfquery to load.");
        window.setTimeout(function(){self.init();}, 100);
        return;
    }
    this.modelBaseUri = $.uri.absolute(window.location.protocol + '//' + window.location.host + this.file.url);
    this.rdf = $.rdf({base: this.modelBaseUri})
                .prefix('bqbiol', 'http://biomodels.net/biology-qualifiers/')
                .prefix('oxmeta', 'https://chaste.comlab.ox.ac.uk/cellml/ns/oxford-metadata#')
                .prefix('rdfs', 'http://www.w3.org/2000/01/rdf-schema#');
}

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
        if (waitForLibraries(self, function(){self.getContentsCallback(true);}))
            return;
        console.log("Model loaded");
        this.loadedModel = true;
        this.model = $.parseXML(this.file.contents);
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
                var li = $('<li></li>'),
                    v = {li: li, elt: this, name: this.getAttribute('name'),
                         metaid: this.getAttributeNS("http://www.cellml.org/metadata/1.0#", "id"),
                         annotations: {}};
                c.vars.push(v);
                v.fullname = c.name + ':' + v.name;
                self.vars_by_name[v.fullname] = v;
                if (v.metaid)
                {
                    v.uri = self.modelBaseUri.toString() + '#' + v.metaid;
                    self.vars_by_uri[v.uri] = v;
                }
                li.html('<span class="editmeta_vname">' + v.name + '</span>');
                clist.appendChild(li.get(0));
                li.droppable({
                    drop: function (event, ui) {
                        console.log("Adding annotation " + ui.helper.data('bindings').ann + " on " + v.fullname);
                        self.addAnnotation(v, ui.helper.data('bindings'));
                    }
                });
            });
        });
        console.log("Found " + keys(this.vars_by_name).length + " variables");
        this.modelDiv.append("<h4>Model variables</h4>", var_list);

        // Find the existing annotations
        var rdf_nodes = this.model.getElementsByTagNameNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "RDF"),
            rdf = this.rdf;
        console.log("Found " + rdf_nodes.length + " RDF nodes");
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
        
        // If ontology is available too, set up linking functionality
        if (this.loadedOntology)
            this.ready();
        
//        td_meta.appendChild(document.createTextNode(rdf_store.dump({format: 'application/rdf+xml', serialize: true})));
    }
};

/**
 * Add a new annotation to a variable.
 * The bindings object should contain at least 'ann', and optionally 'label' and 'comment'.
 * Duplicate annotations will be ignored.
 */
metadataEditor.prototype.addAnnotation = function (v, bindings)
{
    if (v.annotations[bindings.ann.value.toString()] !== undefined)
    {
        console.log("Ignoring duplicate annotation " + bindings.ann + " on " + v.fullname);
        return;
    }
    var self = this,
        s = $('<span></span>', {'class': 'editmeta_annotation editmeta_spaced'}),
        del = $('<img>', {src: contextPath + '/res/img/delete.png',
                          alt: 'remove this annotation',
                          title: 'remove this annotation',
                          'class': 'editmeta_spaced pointer'});
    s.text(bindings.label === undefined ? bindings.ann.value.fragment : bindings.label.value);
    s.append(del);
    if (bindings.comment !== undefined)
        s.attr('title', bindings.comment.value);
    v.annotations[bindings.ann.value.toString()] = {ann: bindings.ann, span: s};
    v.li.append(s);
    // Add the handler for deleting this annotation
    del.click(function (ev) {
        console.log("Removing annotation: <" + v.uri + '> bqbiol:is ' + bindings.ann);
        delete v.annotations[bindings.ann.value.toString()];
        self.rdf.remove('<' + v.uri + '> bqbiol:is ' + bindings.ann);
        s.remove();
    });
}

/**
 * Called when both the model and Oxford metadata ontology have been loaded and parsed.
 * Adds details of the existing variable annotations to the UI.
 */
metadataEditor.prototype.ready = function ()
{
    var self = this;
    console.log("Ready!");
    this.rdf.where('?v bqbiol:is ?ann')
            .optional('?ann rdfs:label ?label')
            .optional('?ann rdfs:comment ?comment')
            .each(function(i, bindings, triples) {
                 var v = self.vars_by_uri[bindings.v.value.toString()];
                 if (v === undefined)
                     console.log("Annotation of non-existent id! " + bindings.v + " is " + bindings.ann);
                 else
                     self.addAnnotation(v, bindings);
            });
}

/**
 * Callback function for when the Oxford metadata ontology has been fetched from the server.
 */
metadataEditor.prototype.ontologyLoaded = function (data, status, jqXHR)
{
    var self = this;
    if (waitForLibraries(self, function(){self.ontologyLoaded(data, status, jqXHR);}))
        return;
    console.log("Ontology loaded");
    this.loadedOntology = true;
    this.ontoDiv.empty();
    
    // Parse XML
    this.rdf.load(data, {});
    
    // Show available terms
    this.terms = [];
    var ul = $('<ul></ul');
    this.ontoDiv.append("<h4>Available annotations</h4>", ul);
    this.rdf.where('?ann a oxmeta:Annotation')
            .optional('?ann rdfs:label ?label')
            .optional('?ann rdfs:comment ?comment')
            .each(function(i, bindings, triples) {
                var li = $('<li></li>', {'class': 'editmeta_annotation'});
                li.text(bindings.label === undefined ? bindings.ann.value.fragment : bindings.label.value);
                if (bindings.comment !== undefined)
                    li.attr('title', bindings.comment.value);
                self.terms.push({uri: bindings.ann.value, li: li});
                ul.append(li);
                li.draggable({
                    containment: self.div,
                    cursor: 'move',
                    helper: function (event) {
                        self.dragDiv.text(li.text())
                                    .data('bindings', bindings);
                        return self.dragDiv;
                    },
                    scroll: false,
//                    start: function (event, ui) {
//                        console.log("Start drag of " + bindings.ann);
//                        console.log(event);
//                        console.log(ui);
//                    }
                });
            });
    // Sort the list!
    var items = $('li', ul).get();
    items.sort(function(a,b) { return $(a).text().localeCompare($(b).text()); });
    $.each(items, function(i, li) { ul.append(li); });

    // If model is available too, set up linking functionality
    if (this.loadedModel)
        this.ready();
}

/**
 * Called to generate the content for this visualiser plugin.
 * Actually just triggers a fetch of the file contents, with our getContentsCallback method
 * doing the work when this completes.
 */
metadataEditor.prototype.show = function ()
{
    var self = this;
    if (!this.loadedModel)
        this.file.getContents(this);
    if (!this.loadedOntology)
        $.ajax(contextPath + '/res/js/visualizers/editMetadata/oxford-metadata.rdf',
               {dataType: 'xml',
                success: function(d,s,j) {self.ontologyLoaded(d,s,j);}
               });

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
