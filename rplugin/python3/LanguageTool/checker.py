#!/usr/bin/env python3

import requests
import subprocess

class Checker:

    def __init__(self, server_path, port):
        self.port = port
        self.process = subprocess.Popen('java -cp {} org.languagetool.server.HTTPServer --port {}'.format(server_path, port),
                shell=True)

    def __del__(self):
        self.process.kill()

    def send(self, data={}, endpoint='v2/check'):
        r = requests.post('http://localhost:{}/{}'.format(self.port, endpoint), params=data)

        if not r.ok:
            return {'err' : 'An error occured : {}'.format(r.reason)}
        return r.json()
