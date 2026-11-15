#!/usr/bin/env python3
"""Record actual Android screen and app audio while replaying verified taps.

Plan format: {"duration":120,"actions":[{"at":4,"xml":"qa/home.xml",
"label":"오늘 기록하기"}, {"at":60,"swipe":[1600,900,1600,300,800]}]}
All tap coordinates are derived from saved accessibility trees from the QA pass.
"""
import json, os, re, subprocess, sys, time, xml.etree.ElementTree as ET
from pathlib import Path
from qa import ADB, SERIAL, adb

base=Path(__file__).resolve().parent
plan=json.loads(Path(sys.argv[1]).read_text())
out=Path(sys.argv[2]).resolve()
out.parent.mkdir(parents=True,exist_ok=True)
env=dict(os.environ,ADB=ADB)
log=out.with_suffix('.capture.log').open('w')
p=subprocess.Popen(['scrcpy','--serial',SERIAL,'--no-playback','--require-audio',
    '--audio-codec=aac','--audio-bit-rate=192K','--video-bit-rate=16M','--max-fps=30',
    '--record',str(out),'--time-limit',str(plan['duration']+2)],env=env,stdout=log,stderr=log)
time.sleep(2)
t0=time.monotonic()
events=[]
try:
    for a in plan['actions']:
        delay=a['at']-(time.monotonic()-t0)
        if delay>0:time.sleep(delay)
        if 'label' in a:
            root=ET.parse(base/a['xml']).getroot()
            hits=[n for n in root.iter('node') if a['label'] in [n.get('text'),n.get('content-desc')]]
            hits=[n for n in hits if n.get('enabled') != 'false' and n.get('bounds') != '[0,0][0,0]']
            clickable=[n for n in hits if n.get('clickable') == 'true']
            if clickable:hits=clickable
            if len(hits)!=1:raise RuntimeError(f"Ambiguous label {a['label']!r}: {len(hits)}")
            x1,y1,x2,y2=map(int,re.findall(r'\d+',hits[0].get('bounds')))
            pos=[(x1+x2)//2,(y1+y2)//2]
            adb('shell','input','tap',*map(str,pos))
            event={'at':time.monotonic()-t0,'tap':a['label'],'point':pos}
        elif 'swipe' in a:
            adb('shell','input','swipe',*map(str,a['swipe']))
            event={'at':time.monotonic()-t0,'swipe':a['swipe']}
        else:raise ValueError(a)
        events.append(event);print(json.dumps(event,ensure_ascii=False),flush=True)
    p.wait(timeout=max(10,plan['duration']+10-(time.monotonic()-t0)))
finally:
    if p.poll() is None:p.terminate();p.wait(timeout=10)
    log.close()
    out.with_suffix('.events.json').write_text(json.dumps(events,ensure_ascii=False,indent=2))
print('Capture exit',p.returncode,flush=True)
sys.exit(p.returncode)
