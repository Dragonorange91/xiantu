"""Pinned MCP-HELPER HTTP adapter for the authorized Xiantu QA world."""
import json
import urllib.request
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
QA=ROOT/'work/qa/beast-photon-20261017'
BASE='http://127.0.0.1:9874'
WORLD='Xiantu-QA-BeastFlat-20261017'

def status():
    with urllib.request.urlopen(BASE+'/api/status',timeout=5) as response:
        return json.load(response)

def call(cmd, **fields):
    request=urllib.request.Request(BASE+'/api/cmd', json.dumps({'cmd':cmd,**fields}).encode(), {'Content-Type':'application/json'})
    with urllib.request.urlopen(request,timeout=25) as response: result=json.load(response)
    if result.get('error') or result.get('ok') is False: raise RuntimeError((cmd,result))
    return result

def guard():
    s=status()
    assert s['type']=='minecraft-mod' and s['port']==9874 and s['control_mode'],s
    assert call('get_world_info')['world_name']==WORLD
    return s

if __name__=='__main__':
    import sys
    guard()
    print(json.dumps(call(sys.argv[1],**(json.loads(sys.argv[2]) if len(sys.argv)>2 else {})),ensure_ascii=False,indent=2))
