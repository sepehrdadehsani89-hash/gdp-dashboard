#!/usr/bin/env python3
"""Original deterministic FM/sine sound design. No recordings or movie samples. CC0-1.0."""
import math, struct, wave
from pathlib import Path
root=Path(__file__).resolve().parents[1]/'app/src/main/res/raw'
sounds={
 'wake':(.18,560,1080,2),'reactor':(.42,110,260,3),'ready':(.16,620,930,2),
 'listen':(.15,470,710,2),'recognized':(.10,780,620,1),'processing':(.18,170,240,3),
 'response':(.16,540,810,2),'speak_start':(.12,180,230,1),'speak_stop':(.13,260,150,1),
 'button':(.035,1800,900,1),'tab':(.07,850,1450,1),'panel_open':(.11,360,720,1),
 'panel_close':(.10,720,360,1),'success':(.25,520,1040,3),'permission':(.24,460,400,2),
 'codex':(.28,180,360,2),'identity':(.18,330,510,2),'auth_success':(.28,440,880,3),
 'auth_denied':(.22,240,150,2),'network_lost':(.18,440,260,2),'network_restored':(.16,360,660,2),
 'low_battery':(.22,310,250,2),'charge':(.23,220,620,1),'unplug':(.14,470,290,1),
 'error':(.20,250,190,2),'sleep':(.35,460,110,1)}
for name,(duration,start,end,notes) in sounds.items():
 rate=22050;n=int(duration*rate);values=[];phase=0.
 for i in range(n):
  t=i/rate;x=i/max(1,n-1);part=(x*notes)%1
  envelope=math.sin(math.pi*part)**1.4*min(1,x/.04)*min(1,(1-x)/.08)
  freq=start+(end-start)*x
  phase+=2*math.pi*freq/rate
  signal=math.sin(phase+.18*math.sin(phase*.503))+.14*math.sin(phase*2.01)
  values.append(signal*envelope)
 peak=max(abs(v) for v in values) or 1
 with wave.open(str(root/f'sentinel_{name}.wav'),'wb') as out:
  out.setnchannels(1);out.setsampwidth(2);out.setframerate(rate)
  out.writeframes(b''.join(struct.pack('<h',round(v/peak*12000)) for v in values))
print(f'Generated {len(sounds)} original mono PCM sound effects.')
