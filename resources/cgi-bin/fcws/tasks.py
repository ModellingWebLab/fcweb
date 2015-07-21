# Task queue for Functional Curation web service

import glob
import os
import shutil
import subprocess
import tempfile
import time
import zipfile

import celery
from celery.exceptions import SoftTimeLimitExceeded
import requests


from . import config
from . import celeryconfig
from . import utils

app = celery.Celery('fcws.tasks')
app.config_from_object(celeryconfig)


def Callback(callbackUrl, signature, data, **kwargs):
    """Make a callback to the front-end server."""
    data['signature'] = signature
    return requests.post(callbackUrl, data=data, verify=False, **kwargs)


def ReportError(callbackUrl, signature):
    """Report an unexpected error, with details, to the front-end, then re-raise."""
    import sys, traceback
    message = "failed due to unexpected error: " + str(sys.exc_info()[1]) + "<br/>Full internal details follow:<br/>"
    message += traceback.format_exc().replace('\n', '<br/>')
    Callback(callbackUrl, signature, {'returntype': 'failed', 'returnmsg': message})
    raise


@app.task(name="fcws.tasks.CheckExperiment")
def CheckExperiment(callbackUrl, signature, modelUrl, protocolUrl):
    """Check a model/protocol combination for compatibility.
    
    If the interfaces match up, then the experiment can be run.
    Otherwise, we alert the front-end to update the experiment status.
    As a side effect this downloads & unpacks the model & protocol definitions, ready for the RunExperiment task.
    
    @param callbackUrl: URL to post status updates to
    @param signature: unique identifier for this experiment run
    @param modelUrl: where to download the model archive from
    @param protoUrl: where to download the protocol archive from
    """
    try:
        # Download the submitted COMBINE archives to disk in a temporary folder
        try:
            os.makedirs(config['temp_dir'], 0775)
        except os.error:
            pass
        temp_dir = tempfile.mkdtemp(dir=config['temp_dir'])
        model_path = os.path.join(temp_dir, 'model.zip')
        proto_path = os.path.join(temp_dir, 'protocol.zip')
        utils.Wget(modelUrl, model_path)
        utils.Wget(protocolUrl, proto_path)
    
        # Unpack the model & protocol
        main_model_path = utils.UnpackArchive(model_path, temp_dir, 'model')
        main_proto_path = utils.UnpackArchive(proto_path, temp_dir, 'proto')
    
        # Check whether their interfaces are compatible
        missing_terms, missing_optional_terms = utils.DetermineCompatibility(main_proto_path, main_model_path)
        if missing_terms:
            message = "inapplicable - required ontology terms are not present in the model. Missing terms are:<br/>"
            for term in missing_terms:
                message += "&nbsp;" * 4 + term + "<br/>"
            if missing_optional_terms:
                message += "Missing optional terms are:<br/>"
                for term in missing_optional_terms:
                    message +="&nbsp;" * 4 + term + "<br/>"
            # Report & clean up temporary files
            Callback(callbackUrl, signature, {'returntype': 'inapplicable', 'returnmsg': message})
            shutil.rmtree(temp_dir)
        else:
            # Run the experiment directly in this task, to ensure it has access to the unpacked model & protocol
            RunExperiment(callbackUrl, signature, main_model_path, main_proto_path, temp_dir)
    except:
        ReportError(callbackUrl, signature)


@app.task(name="fcws.tasks.RunExperiment")
def RunExperiment(callbackUrl, signature, modelPath, protoPath, tempDir):
    """Run a functional curation experiment.
    
    @param callbackUrl: URL to post status updates and results to
    @param signature: unique identifier for this experiment run
    @param modelPath: path to the main model file
    @param protoPath: path to the main protocol file
    @param tempDir: folder in which to store any temporary files
    """
    try:
        # Tell the website we've started running
        Callback(callbackUrl, signature, {'returntype': 'running'})
    
        # Call FunctionalCuration exe, writing output to the temporary folder containing inputs
        # (or rather, a subfolder thereof).
        # Also redirect stdout and stderr so we can debug any issues.
        for key, value in config['environment'].iteritems():
            os.environ[key] = value
        args = [config['exe_path'], modelPath, protoPath, os.path.join(tempDir, 'output')]
        child_stdout_name = os.path.join(tempDir, 'stdout.txt')
        output_file = open(child_stdout_name, 'w')
        timeout = False
        try:
            child = subprocess.Popen(args, stdout=output_file, stderr=subprocess.STDOUT)
            child.wait()
        except SoftTimeLimitExceeded:
            # If we're timed out, kill off the child process, but send back any partial output - don't re-raise
            child.terminate()
            time.sleep(5)
            child.kill()
            timeout = True
        except:
            # If any other error happens, just make sure the child is dead then report it
            child.terminate()
            time.sleep(5)
            child.kill()
            raise
        output_file.close()
    
        # Zip up the outputs and post them to the callback
        output_path = os.path.join(tempDir, 'output.zip')
        output_files = glob.glob(os.path.join(tempDir, 'output', '*', '*', '*')) # Yuck!
        output_zip = zipfile.ZipFile(output_path, 'w', zipfile.ZIP_DEFLATED)
        output_zip.write(child_stdout_name, 'stdout.txt')
        if timeout:
            # Add a message about the timeout to the errors.txt file (which is created if not present)
            for ofile in output_files:
                if os.path.isfile(ofile) and os.path.basename(ofile) == "errors.txt":
                    error_file_path = ofile
                    break
            else:
                error_file_path = os.path.join(tempDir, 'errors.txt')
                output_files.append(error_file_path)
                # Remove any manifest file since we'll need to create a new one with errors.txt in
                for ofile in output_files:
                    if os.path.basename(ofile) == 'manifest.xml':
                        output_files.remove(ofile)
                        break
            error_file = open(error_file_path, 'a+')
            error_file.write("\nExperiment terminated due to exceeding time limit\n")
            error_file.close()
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
            manifest = open(os.path.join(tempDir, 'manifest.xml'), 'w')
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
            output_zip.write(os.path.join(tempDir, 'manifest.xml'), 'manifest.xml')
        output_zip.close()
    
        files = {'experiment': open(output_path, 'rb')}
        r = Callback(callbackUrl, signature, {'returntype': outcome}, files=files)
    
        # Remove the temporary folder
        shutil.rmtree(tempDir)
    
        return r.status_code
    except:
        ReportError(callbackUrl, signature)
