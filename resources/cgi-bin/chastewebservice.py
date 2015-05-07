#!/usr/bin/env python
import cgi
import cgitb
import os
import sys

import fcws

temporaryDir = fcws.config['temp_dir']
debugPrefix = fcws.config['debug_log_file_prefix']
cgitb.enable(format='text', context=1, logdir=os.path.join(temporaryDir, debugPrefix+'cgitb'))

def SendError(msg):
    print "Content-Type: text/html\n\n"
    print "<html><head><title>ChastePermissionError</title></head><body>%s</body></html>" % msg
    sys.exit(0)

# Parse sent objects
form = cgi.FieldStorage()

if 'password' not in form or form['password'].value != fcws.config['password']:
    SendError("Missing or incorrect password supplied.")

if 'cancelTask' in form:
    # Special action: cancel or revoke an experiment
    print "Content-Type: text/plain\n\n"
    fcws.CancelExperiment(form['cancelTask'].value)
else:
    for field in ['callBack', 'signature', 'model', 'protocol']:
        if field not in form:
            SendError("Missing required field.")
    
    print "Content-Type: text/plain\n\n"
    signature = form["signature"]
    # Wrap the rest in a try so we alert the caller properly if an exception occurs
    try:
        callBack = form["callBack"]
        modelUrl = form["model"]
        protocolUrl = form["protocol"]
        fcws.ScheduleExperiment(callBack.value, signature.value, modelUrl.value, protocolUrl.value)
    except Exception, e:
        print signature.value, "failed due to unexpected error:", e, "<br/>"
        print "Full internal details follow:<br/>"
        raise
