# Back-end web service routines for the functional curation website

import json
import os

config = json.load(open(os.path.join(os.path.dirname(__file__), 'config.json')))

def ScheduleExperiment(callbackUrl, signature, modelPath, protoPath, tempDir):
    from .tasks import RunExperiment
    RunExperiment.delay(callbackUrl, signature, modelPath, protoPath, tempDir)
    # print success to calling script -> tell web interface that the call was successful
    print signature, "succ"
