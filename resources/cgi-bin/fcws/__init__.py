# Back-end web service routines for the functional curation website

import json
import os

config = json.load(open(os.path.join(os.path.dirname(__file__), 'config.json')))

def ScheduleExperiment(callbackUrl, signature, modelUrl, protoUrl):
    from .tasks import CheckExperiment
    CheckExperiment.delay(callbackUrl, signature, modelUrl, protoUrl)
    # print success to calling script -> tell web interface that the call was successful
    print signature, "succ"
