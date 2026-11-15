#!/usr/bin/env python3
"""Inspect and operate the installed Android demo through adb accessibility."""
import argparse, json, re, subprocess, time, xml.etree.ElementTree as ET
from pathlib import Path

ADB = '/Users/victor/Library/Android/sdk/platform-tools/adb'
SERIAL = 'emulator-5554'
OUT = Path(__file__).resolve().parent / 'qa'
OUT.mkdir(exist_ok=True)

def adb(*args):
    return subprocess.check_output([ADB, '-s', SERIAL, *args])

def tree(label='current'):
    for attempt in range(4):
        try:
            raw = adb('exec-out', 'uiautomator', 'dump', '/dev/tty').decode()
            xml = raw[raw.index('<?xml'):raw.rindex('</hierarchy>')+12]
            root = ET.fromstring(xml)
            if not any(n.get('clickable') == 'true' and (n.get('text') or n.get('content-desc')) for n in root.iter('node')):
                raise ValueError('WebView accessibility tree is not ready')
            (OUT / f'{label}.xml').write_text(xml)
            return root
        except (ValueError, ET.ParseError, subprocess.CalledProcessError):
            if attempt == 3:
                raise
            time.sleep(.5)

def describe(root):
    for n in root.iter('node'):
        name = n.get('text') or n.get('content-desc') or ''
        if name or n.get('clickable') == 'true':
            print(json.dumps({k:n.get(k) for k in ['text','content-desc','class','clickable','bounds']},ensure_ascii=False))

def tap(label):
    root = tree()
    nodes = list(root.iter('node'))
    exact = [n for n in nodes if label in [n.get('text'),n.get('content-desc')]]
    hits = exact or [n for n in nodes if label in ((n.get('text') or '')+' '+(n.get('content-desc') or ''))]
    hits = [n for n in hits if n.get('enabled') != 'false' and n.get('bounds') != '[0,0][0,0]']
    clickable = [n for n in hits if n.get('clickable') == 'true']
    if clickable:
        hits = clickable
    if len(hits) != 1:
        describe(root)
        raise RuntimeError(f'Expected one visible match for {label!r}, got {len(hits)}')
    n = hits[0]
    x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
    x,y=(x1+x2)//2,(y1+y2)//2
    adb('shell','input','tap',str(x),str(y))
    print(json.dumps({'tapped':label,'x':x,'y':y,'timestamp':time.time()},ensure_ascii=False))

if __name__ == '__main__':
    p=argparse.ArgumentParser();p.add_argument('action',choices=['tree','tap','shot']);p.add_argument('value',nargs='?',default='current');a=p.parse_args()
    if a.action=='tree':describe(tree(a.value))
    elif a.action=='tap':tap(a.value)
    else:
        f=OUT/f'{a.value}.png';f.write_bytes(adb('exec-out','screencap','-p'));print(f)
