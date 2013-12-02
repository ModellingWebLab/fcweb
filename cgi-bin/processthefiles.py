#!/usr/bin/env python
import os
import sys
import time
import datetime
import requests
import json


# this file is called via batch -> it is executed if there is CPU time available
# arguments:
# sys.argv[1] == callback url
# sys.argv[2] == signature
# sys.argv[3] == combine archive containing the model
# sys.argv[4] == combine archive containing the protocol
# so do whatever you want to create the experiment and put it in an combine archive
# send the archive to sys.argv[1], together with the signature (see below)

####################################
####### DEBUG START ################
####################################

time.sleep (10)

fout = open ("/tmp/python-webservice.debug", 'a+')
fout.write ("======")
fout.write (datetime.datetime.now().strftime("%Y-%m-%d %H:%M"))
fout.write ("======\n")
fout.write (sys.argv[1] + "\n")
fout.write (sys.argv[2] + "\n")
fout.write (sys.argv[3] + "\n")
fout.write (sys.argv[4] + "\n")
fout.flush()
os.fsync(fout)
fout.close()


####################################
####### DEBUG END ##################
####################################


# since this is just a dummy, we delete the input files and send a default experiment.
# here you should call chaste and send the correct result...

os.remove (sys.argv[3])
os.remove (sys.argv[4])

# send some default experiment
url = sys.argv[1]
files = {'expriment': open('/var/www/cgi-bin/samplemodel.zip', 'rb')}
payload = {'signature': 'abc123'}
r = requests.post(url, files=files, data=payload)

# if that was successful we'll receive 200 and some JSON stream
# but i guess we shouldn't care (what should we do if it fails?)
# so, just write some log to be able to reconstruct what happened

fout = open ("/tmp/python-webservice-result.debug", 'a+')
fout.write ("======")
fout.write (datetime.datetime.now().strftime("%Y-%m-%d %H:%M"))
fout.write ("======\n")
fout.write (r.status_code + "\n")
fout.write (r.content + "\n")
fout.flush()
os.fsync(fout)
fout.close()


# that's it so far. thanks for your attention...


