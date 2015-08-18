# Back-end web service routines for the functional curation website

import json
import os

config = json.load(open(os.path.join(os.path.dirname(__file__), 'config.json')))


def GetQueue(user, isAdmin):
    """Determine which Celery queue to use for a task, based on the user submitting it."""
    if isAdmin:
        queue = 'admin'
    else:
        queue = 'default'


def ScheduleExperiment(callbackUrl, signature, modelUrl, protoUrl, user='', isAdmin=False):
    """Schedule a new experiment for execution."""
    from .tasks import CheckExperiment
    # Submit the job
    result = CheckExperiment.apply_async((callbackUrl, signature, modelUrl, protoUrl), queue=GetQueue(user, isAdmin))
    # Tell web interface that the call was successful
    print signature, "succ", result.task_id


def CancelExperiment(taskId):
    """Revoke or terminate an already submitted experiment."""
    import signal
    from .tasks import app
    app.control.revoke(taskId, terminate=True, signal=signal.SIGUSR1)


def GetProtocolInterface(callbackUrl, signature, protoUrl):
    """Get the ontology terms forming the interface for a protocol."""
    from .tasks import GetProtocolInterface
    GetProtocolInterface.apply_async((callbackUrl, signature, protoUrl), queue=GetQueue('', True))
