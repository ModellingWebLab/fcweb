"""
Utility module for Functional Curation web services.

It handles unpacking COMBINE archives containing CellML models or Functional Curation protocols,
and determining which file within is the primary model/protocol.

It also contains a method for determining whether a model and protocol are compatible.
"""

import json
import os
import re
import sys
import urllib2
import xml.etree.ElementTree as ET
import zipfile

from . import config

CHASTE_ROOT = config['chaste_root']
FC_ROOT = os.path.join(CHASTE_ROOT, 'projects', 'FunctionalCuration')

EXPECTED_EXTENSIONS = {'model': ['.cellml'],
                       'proto': ['.txt', '.xml']}

MANIFEST = 'manifest.xml'

sys.path[0:0] = [os.path.join(FC_ROOT, 'src/proto/parsing'),
                 os.path.join(FC_ROOT, 'src/python'),
                 os.path.join(CHASTE_ROOT, 'python/pycml')]
import CompactSyntaxParser as CSP
import cellml_metadata
import pycml


def Wget(url, localPath):
    """Retrieve a binary file from the given URL and save it to disk."""
    local_file = open(localPath, 'wb')
    source = urllib2.urlopen(url)
    while True:
        chunk = source.read(10240)
        if not chunk: break
        local_file.write(chunk)
    local_file.flush()
    os.fsync(local_file)
    local_file.close()


def UnpackArchive(archivePath, tempPath, contentType):
    """Unpack a COMBINE archive, and return the path to the primary unpacked file.
    
    :param archivePath:  path to the archive
    :param tempPath:  path to a temporary folder under which to unpack
    :param contentType:  whether the archive contains a model ('model') or protocol ('proto')
    
    Files will be unpacked into the path tempPath/contentType.
    """
    assert contentType in ['model', 'proto']
    archive = zipfile.ZipFile(archivePath)
    output_path = os.path.join(tempPath, contentType)
    archive.extractall(output_path)
    # Check if the archive manifest specifies the primary file
    primary_file = None
    manifest_path = os.path.join(output_path, MANIFEST)
    if os.path.exists(manifest_path):
        manifest = ET.parse(manifest_path)
        for item in manifest.iter('{http://identifiers.org/combine.specifications/omex-manifest}content'):
            if item.get('master', 'false') == 'true':
                primary_file = item.get('location')
                if primary_file[0] == '/':
                    # There's some debate over the preferred form of location URIs...
                    primary_file = primary_file[1:]
                break
    if not primary_file:
        # No manifest or no master listed, so try to figure it out ourselves: find the first item with expected extension
        for item in archive.infolist():
            if item.filename != MANIFEST and os.path.splitext(item.filename)[1] in EXPECTED_EXTENSIONS[contentType]:
                primary_file = item.filename
                break
    if not primary_file:
        raise ValueError('No suitable primary file detected in COMBINE archive')
    primary_path = os.path.join(output_path, primary_file)
    if not os.path.exists(primary_path):
        raise ValueError('Declared primary file not present in archive')
    return primary_path


def GetProtoInterface(protoPath):
    """Get the set of ontology terms used by the given protocol, recursively processing imports."""
    parser = CSP.CompactSyntaxParser
    nested_proto = CSP.MakeKw('nests') + CSP.MakeKw('protocol') - parser.quotedUri
    import_stmt = CSP.p.Group(CSP.MakeKw('import') - CSP.Optional(parser.ncIdent + parser.eq, default='') + parser.quotedUri)
    var_ref = parser.cIdent.re
    ns_maps = {}
    terms = set()
    optional_terms = set()
    def AddTerm(qname, termSet=terms):
        prefix, name = qname.split(':')
        nsuri = ns_maps[prefix]
        termSet.add(nsuri + name)
    def ProcessInput(res):
        qname = res[0].tokens['name']
        if not 'units' in res[0].tokens or not 'initial_value' in res[0].tokens:
            # Input is not optional, so record as part of the interface
            AddTerm(qname)
        else:
            AddTerm(qname, optional_terms)
    def ProcessOutput(res):
        AddTerm(res[0].tokens['name'])
    def ProcessOptional(res):
        AddTerm(res[0].tokens['name'], optional_terms)
    def ProcessNsDecl(res):
        ns_maps[res[0]['prefix']] = res[0]['uri']
    def ProcessImport(source_uri):
        # Relative URIs must be resolved relative to this protocol file, or the library folder if not found
        base = os.path.dirname(protoPath)
        source = source_uri
        if not os.path.isabs(source):
            source = os.path.join(base, source)
        if not os.path.exists(source):
            # Try resolving relative to the library folder
            library = os.path.join(FC_ROOT, 'src', 'proto', 'library')
            source = os.path.join(library, source_uri)
        import_terms, import_optional_terms = GetProtoInterface(source)
        terms.update(import_terms)
        optional_terms.update(import_optional_terms)
    grammars = {parser.nsDecl: ProcessNsDecl,
                import_stmt: (lambda res: ProcessImport(res[0][1])),
                parser.inputVariable: ProcessInput,
                parser.outputVariable: ProcessOutput,
                parser.optionalVariable: ProcessOptional,
                nested_proto: (lambda res: ProcessImport(res[0]))}
    in_conversion_rule = False
    for line in open(protoPath, 'rU'):
        stripped = line.strip()
        if stripped.startswith('convert'):
            in_conversion_rule = True
            continue
        for grammar, processor in grammars.items():
            try:
                match = grammar.parseString(line)
                in_conversion_rule = False
            except CSP.p.ParseBaseException:
                continue
            processor(match)
            break
        else:
            if stripped.startswith('define ') or stripped.startswith('clamp ') or stripped.startswith('var ') or stripped == '}':
                in_conversion_rule = False
            if not in_conversion_rule:
                # Default check: Scan for any variable references, and see if they're in a known namespace
                for match in var_ref.finditer(line):
                    prefix, name = match.group(0).split(':')
                    nsuri = ns_maps.get(prefix, None)
                    if nsuri:
                        terms.add(nsuri + name)
    return terms - optional_terms, optional_terms


def DetermineCompatibility(protoPath, modelPath):
    """Determine whether the given protocol and model are compatible.
    
    This checks whether the ontology terms accessed by the protocol are present in the model.
    It returns a list of terms required but not present, so the pair are compatible if this list is empty.
    
    NB: Only works with textual syntax protocols at present; assumes OK otherwise.
    """
    if not protoPath.endswith('.txt'):
        return [], []
    proto_terms, optional_terms = GetProtoInterface(protoPath)
    # Get the terms defined by the model
    model_doc = pycml.amara_parse_cellml(modelPath)
    named_uris = cellml_metadata.get_targets(model_doc.model, None, cellml_metadata.create_rdf_node(('bqbiol:is', pycml.NSS['bqbiol'])))
    category_uris = cellml_metadata.get_targets(model_doc.model, None, cellml_metadata.create_rdf_node(('bqbiol:isVersionOf', pycml.NSS['bqbiol'])))
    model_terms = set(str(uri) for uri in named_uris + category_uris)
    model_terms.add('https://chaste.comlab.ox.ac.uk/cellml/ns/oxford-metadata#state_variable') # Present implicitly
    # Return the mismatch, if any, as a sorted list
    needed_terms = list(proto_terms - model_terms)
    needed_terms.sort()
    missing_optional_terms = list(optional_terms - model_terms)
    missing_optional_terms.sort()
    return needed_terms, missing_optional_terms
