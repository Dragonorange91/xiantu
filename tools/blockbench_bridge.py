"""Small streamable-HTTP client for the user's installed local Blockbench MCP."""
import json
import requests

class Bridge:
    def __init__(self):
        self.session = requests.Session()
        self.session.headers.update({'Accept': 'application/json, text/event-stream'})
        self.counter = 0
        self.request('initialize', {'protocolVersion': '2024-11-05', 'capabilities': {},
                     'clientInfo': {'name': 'xiantu-building-assets', 'version': '1'}})

    def request(self, method, params):
        self.counter += 1
        response = self.session.post('http://127.0.0.1:3000/bb-mcp',
            json={'jsonrpc': '2.0', 'id': self.counter, 'method': method, 'params': params}, timeout=60)
        response.raise_for_status()
        if response.headers.get('Mcp-Session-Id'):
            self.session.headers['Mcp-Session-Id'] = response.headers['Mcp-Session-Id']
        payload = response.text
        if payload.startswith(('event:', 'data:')):
            payload = next(line[6:] for line in payload.splitlines() if line.startswith('data: '))
        result = json.loads(payload)
        if 'error' in result:
            raise RuntimeError(result['error'])
        return result.get('result', {})

    def call(self, tool_name, **arguments):
        result = self.request('tools/call', {'name': tool_name, 'arguments': arguments})
        if result.get('isError'):
            raise RuntimeError(result)
        return result

    @staticmethod
    def value(result):
        for item in result.get('content', []):
            if item.get('type') == 'text':
                try:
                    return json.loads(item['text'])
                except ValueError:
                    return item['text']
        return result

if __name__ == '__main__':
    bridge = Bridge()
    print(json.dumps(bridge.value(bridge.call('get_project_info')), ensure_ascii=False, indent=2))
    print(json.dumps(bridge.value(bridge.call('list_export_formats')), ensure_ascii=False, indent=2))
