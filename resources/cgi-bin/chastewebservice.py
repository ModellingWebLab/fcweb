#!/usr/bin/env python
import cgi
import cgitb
import os

import fcws

temporaryDir = fcws.config['temp_dir']
debugPrefix = fcws.config['debug_log_file_prefix']
cgitb.enable(format='text', context=1, logdir=os.path.join(temporaryDir, debugPrefix+'cgitb'))

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
