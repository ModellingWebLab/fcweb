#!/usr/bin/env python

import sys
import tempfile

my_output_file = tempfile.NamedTemporaryFile(prefix='fc-webservice-output-', delete=False)
sys.stderr = my_output_file
sys.stdout = my_output_file

import os
import glob
import json
import datetime
import shutil
import subprocess
import time
import zipfile

import requests

config = json.load(open(os.path.join(os.path.dirname(__file__), 'config.json')))

# this file is called via batch -> it is executed if there is CPU time available
# arguments:
# sys.argv[1] == callback url
# sys.argv[2] == signature
# sys.argv[3] == path to primary model file
# sys.argv[4] == path to primary protocol file
# sys.argv[5] == path to temporary folder
# so do whatever you want to create the experiment and put it in an combine archive
# send the archive to sys.argv[1], together with the signature (see below)
callback_url, signature, model_path, proto_path, temp_dir = sys.argv[1:6]

# Debug
fout = open(os.path.join(config['temp_dir'], config['debug_log_file_prefix'] + 'backend-debug'), 'a+')
fout.write("======" + datetime.datetime.now().strftime("%Y-%m-%d %H:%M") + "======\n")
fout.write(sys.argv[1] + "\n")
fout.write(sys.argv[2] + "\n")
fout.write(sys.argv[3] + "\n")
fout.write(sys.argv[4] + "\n")
fout.write(sys.argv[5] + "\n")
fout.flush()
os.fsync(fout)
fout.close()

# Tell the website we've started running
r = requests.post(callback_url, data={'signature': signature, 'returntype': 'running'})


# Call FunctionalCuration exe, writing output to the temporary folder containing inputs
# (or rather, a subfolder thereof).
# Also redirect stdout and stderr so we can debug any issues.
for key, value in config['environment'].iteritems():
    os.environ[key] = value
args = [config['exe_path'], model_path, proto_path, os.path.join(temp_dir, 'output')]
child_stdout_name = os.path.join(temp_dir, 'stdout.txt')
output_file = open(child_stdout_name, 'w')
subprocess.call(args, stdout=output_file, stderr=subprocess.STDOUT)
output_file.close()

# Zip up the outputs and post them to the callback
output_path = os.path.join(temp_dir, 'output.zip')
output_files = glob.glob(os.path.join(temp_dir, 'output', '*', '*', '*')) # Yuck!
output_zip = zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED)
output_zip.write(child_stdout_name, 'stdout.txt')
for ofile in output_files:
    if os.path.isfile(ofile):
        output_zip.write(ofile, os.path.basename(ofile))
if 'success' in output_zip.namelist():
    outcome = 'success'
else:
    for filename in output_zip.namelist():
        if filename.endswith('gnuplot_data.csv'):
            outcome = 'partial' # Some output plots created => might be useful
            break
    else:
        outcome = 'failed' # No outputs created => total failure
# Add a manifest if Chaste didn't create one
if 'manifest.xml' not in output_zip.namelist():
    manifest = open(os.path.join(temp_dir, 'manifest.xml'), 'w')
    manifest.write("""<?xml version='1.0' encoding='utf-8'?>
<omexManifest xmlns='http://identifiers.org/combine.specifications/omex-manifest'>
  <content location='manifest.xml' format='http://identifiers.org/combine.specifications/omex-manifest'/>
""")
    for filename in output_zip.namelist():
        try:
            ext = os.path.splitext(filename)[1]
            format = {'.txt': 'text/plain',
                      '.csv': 'text/csv',
                      '.png': 'image/png',
                      '.eps': 'application/postscript',
                      '.xml': 'text/xml',
                      '.cellml': 'http://identifiers.org/combine.specifications/cellml.1.0'
                     }[ext]
        except:
            format = 'application/octet-stream'
        manifest.write("  <content location='%s' format='%s'/>\n" % (filename, format))
    manifest.write("</omexManifest>")
    manifest.close()
    output_zip.write(os.path.join(temp_dir, 'manifest.xml'), 'manifest.xml')
output_zip.close()

files = {'experiment': open(output_path, 'rb')}
payload = {'signature': signature, 'returntype': outcome}
r = requests.post(callback_url, files=files, data=payload)

# Debug
fout = open(os.path.join(config['temp_dir'], config['debug_log_file_prefix'] + 'backend-result'), 'a+')
fout.write("======" + datetime.datetime.now().strftime("%Y-%m-%d %H:%M") + "======\n")
fout.write(str(r.status_code) + "\n")
fout.write(str(r.content) + "\n")
fout.flush()
os.fsync(fout)
fout.close()

# Remove the temporary folder
shutil.rmtree(temp_dir)

