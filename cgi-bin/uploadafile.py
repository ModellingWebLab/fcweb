#!/usr/bin/env python

import requests
import json


url = 'https://userpc58.cs.ox.ac.uk/FunctionalCuration/upload.html'
#url = 'https://userpc58.cs.ox.ac.uk/test.php'
files = {'expriment': open('/tmp/python-file', 'rb')}
payload = {'key1': 'value1', 'key2': 'value2', 'signature': 'abc123'}
r = requests.post(url, files=files, data=payload)

print r.status_code, "\n"
print r.content

