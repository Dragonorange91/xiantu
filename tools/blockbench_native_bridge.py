"""Client for the installed BlockbenchMCP renderer bridge (protocol 1, port 8787)."""
import requests

class NativeBridge:
    def __init__(self):
        self.session = requests.Session()
    def call(self, action, **params):
        r = self.session.post('http://127.0.0.1:8787/command',
                              json={'action': action, 'params': params}, timeout=120)
        r.raise_for_status()
        v = r.json()
        if not v.get('ok'):
            raise RuntimeError(f'{action}: {v.get("error")}')
        return v['result']
