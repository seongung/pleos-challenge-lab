#!/usr/bin/env python3
"""Export actual Android footage with quiet instrumental music and speech ducking."""
import json,re,subprocess,sys
from pathlib import Path
raw,music,out=map(Path,sys.argv[1:4])
probe=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_format','-show_streams','-of','json',str(raw)]))
duration=float(probe['format']['duration'])
r=subprocess.run(['ffmpeg','-hide_banner','-i',str(raw),'-vn','-af','loudnorm=I=-18:TP=-2:LRA=9:print_format=json','-f','null','-'],capture_output=True,text=True,check=True)
stats=json.loads(re.findall(r'\{[^{}]+"input_i"[^{}]+\}',r.stderr,re.S)[-1])
gain=min(-18-float(stats['input_i']),-2-float(stats['input_tp']))
if not (-60<gain<60):raise RuntimeError('Invalid audio gain; check captured sound')
filters=(f'[0:v]fps=30,tpad=stop_mode=clone:stop_duration={duration},trim=duration={duration}[v];[0:a]volume={gain:.3f}dB,aresample=48000,asplit=2[voice][side];'
 f'[1:a]atrim=0:{duration},asetpts=PTS-STARTPTS,aresample=48000,afade=t=in:d=1.2,afade=t=out:st={duration-3}:d=3[bed];'
 '[bed][side]sidechaincompress=threshold=0.015:ratio=6:attack=120:release=650:makeup=1[ducked];'
 '[voice][ducked]amix=inputs=2:duration=first:normalize=0,alimiter=limit=0.891:level=false[mix]')
subprocess.run(['ffmpeg','-y','-hide_banner','-loglevel','warning','-i',str(raw),'-i',str(music),'-filter_complex',filters,'-map','[v]','-map','[mix]','-c:v','libx264','-preset','medium','-crf','19','-pix_fmt','yuv420p','-c:a','aac','-b:a','192k','-movflags','+faststart',str(out)],check=True)
out.with_suffix('.audio-mix.json').write_text(json.dumps({'source':str(raw),'music':str(music),'duration':duration,'captured_speech_loudness':stats,'speech_gain_db':gain,'music':'normalized instrumental bed, sidechain ducked by captured speech','video':'actual Android capture, CFR30 with final screen held to audio end'},indent=2))
print('Exported',out,'duration',duration,'speech gain',gain,flush=True)
