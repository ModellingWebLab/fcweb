BROKER_URL = 'amqp://guest@localhost'

CELERY_TASK_SERIALIZER = 'json'
CELERY_ACCEPT_CONTENT=['json']
CELERY_TIMEZONE = 'Europe/London'
CELERY_ENABLE_UTC = True

# We expect to have few tasks, but long running, so don't reserve more than you're working on
CELERYD_PREFETCH_MULTIPLIER = 1
# Since tasks are long-running, we want to know if they are actually running
CELERY_TRACK_STARTED = True

CELERYD_TASK_SOFT_TIME_LIMIT = 60 * 60 * 15  # 15 hours

# TODO: Check default time limits; should probably differ for admin & normal users
# can set with decorator: @app.task(soft_time_limit=) or config CELERYD_TASK_SOFT_TIME_LIMIT
# Use ignore_result? How about being able to kill tasks when expt deleted?  'revoked' http://docs.celeryproject.org/en/latest/userguide/workers.html#id8
#   Do we need terminate? Is there any other way of stopping a running task?
# We don't need to store results, since the task will callback to the front-end
# But may need backend anyway for tracking state?

CELERY_RESULT_BACKEND = 'amqp'
#CELERY_TASK_RESULT_EXPIRES = 18000  # 5 hours
CELERY_IGNORE_RESULT = True
CELERY_DISABLE_RATE_LIMITS = True
