# Back-end web service routines for the functional curation website

import json
import os

config = json.load(open(os.path.join(os.path.dirname(__file__), 'config.json')))

def ScheduleExperiment(callbackUrl, signature, modelUrl, protoUrl):
    """Schedule a new experiment for execution."""
    from .tasks import CheckExperiment
    result = CheckExperiment.delay(callbackUrl, signature, modelUrl, protoUrl)
    # print success to calling script -> tell web interface that the call was successful
    print signature, "succ", result.task_id


def CancelExperiment(taskId):
    """Revoke or terminate an already submitted experiment."""
    import signal
    from .tasks import app
    app.control.revoke(taskId, terminate=True, signal=signal.SIGUSR1)
