# Back-end web service routines for the functional curation website

import json
import os

config = json.load(open(os.path.join(os.path.dirname(__file__), 'config.json')))

def ScheduleExperiment(callbackUrl, signature, modelUrl, protoUrl, user='', isAdmin=False):
    """Schedule a new experiment for execution."""
    from .tasks import CheckExperiment
    # We can set the queue to use based on user information
    if isAdmin:
        queue = 'admin'
    else:
        queue = 'default'
    # Submit the job
    result = CheckExperiment.apply_async((callbackUrl, signature, modelUrl, protoUrl), queue=queue)
    # Tell web interface that the call was successful
    print signature, "succ", result.task_id


def CancelExperiment(taskId):
    """Revoke or terminate an already submitted experiment."""
    import signal
    from .tasks import app
    app.control.revoke(taskId, terminate=True, signal=signal.SIGUSR1)
