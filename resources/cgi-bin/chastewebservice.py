#!/usr/bin/env python
import cgi
import cgitb
import os
import tempfile
import time
import random
import subprocess

import fcws
import fcws.utils

temporaryDir = fcws.config['temp_dir']
debugPrefix = fcws.config['debug_log_file_prefix']

cgitb.enable(format='text', context=1, logdir=os.path.join(temporaryDir, debugPrefix+'cgitb'))


def WriteFile(source, destination):
    """Save a file submitted by HTTP POST to disk at the given path."""
    fout = open(destination, 'wb')
    while 1:
        chunk = source.read(10240)
        if not chunk: break
        fout.write(chunk)
    fout.flush()
    os.fsync(fout)
    fout.close()


# parse sent objects
form = cgi.FieldStorage()
if (not form.has_key("password")) or (form["password"].value != fcws.config['password']) or (not form.has_key("callBack")) or (not form.has_key("signature")) or (not form.has_key("model")) or (not form.has_key("protocol")):
    print "Content-Type: text/html\n\n";
    print '''
        <html><head><title>ChastePermissionError</title></head><body>
        looks like you're not allowed to do that.
        </body></html>
        '''
else:
    print "Content-Type: text/plain\n\n"
    callBack = form["callBack"]
    signature = form["signature"]
    model = form["model"]
    protocol = form["protocol"]

    # Wrap the rest in a try so we alert the caller properly if an exception occurs
    try:
        # Save the submitted COMBINE archives to disk in a temporary folder
        temp_dir = tempfile.mkdtemp(dir=temporaryDir)
        model_path = os.path.join(temp_dir, 'model.zip')
        proto_path = os.path.join(temp_dir, 'protocol.zip')
        WriteFile(model.file, model_path)
        WriteFile(protocol.file, proto_path)
        
        # Unpack the model & protocol
        main_model_path = fcws.utils.UnpackArchive(model_path, temp_dir, 'model')
        main_proto_path = fcws.utils.UnpackArchive(proto_path, temp_dir, 'proto')
        
        # Check whether their interfaces are compatible
        missing_terms, missing_optional_terms = fcws.utils.DetermineCompatibility(main_proto_path, main_model_path)
        if missing_terms:
            print signature.value, "inappropriate - required ontology terms are not present in the model."
            print "Missing terms are:<br/>"
            for term in missing_terms:
                print "&nbsp;" * 4, term, "<br/>"
            if missing_optional_terms:
                print "Missing optional terms are:<br/>"
                for term in missing_optional_terms:
                    print "&nbsp;" * 4, term, "<br/>"
        else:
            # Make all the files we've created group-writable, so the experiment can clean them up when it's done
            os.chmod(temp_dir, 0o770)
            for root, dirs, files in os.walk(temp_dir):
                for leaf in dirs + files:
                    os.chmod(os.path.join(root, leaf), 0o770)
            fcws.ScheduleExperiment(callBack.value, signature.value, main_model_path, main_proto_path, temp_dir)
    except Exception, e:
        print signature.value, "failed due to unexpected error:", e, "<br/>"
        print "Full internal details follow:<br/>"
        raise
