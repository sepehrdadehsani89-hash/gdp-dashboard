"""Generate original short UI cues (CC0-1.0). No samples, loops or third-party audio."""
from pathlib import Path
import math, struct, wave
root=Path(__file__).resolve().parents[1]/'app/src/main/res/raw'
cues={'weather':(680,1010),'location':(540,810),'battery':(420,840),'wifi':(740,1110),'bluetooth':(610,915),'network':(820,1230),'music':(440,660),'camera':(950,650),'clock':(880,1320),'system':(370,740)}
for name,(start,end) in cues.items():
    rate=22050;duration=.18;n=int(rate*duration);samples=[];phase=0.0
    for i in range(n):
        t=i/n;freq=start if t<.5 else end;phase+=2*math.pi*freq/rate
        envelope=math.sin(math.pi*t)**2
        samples.append(int(4200*envelope*(math.sin(phase)+.12*math.sin(2*phase))))
    with wave.open(str(root/f'module_{name}.wav'),'wb') as out:
        out.setparams((1,2,rate,n,'NONE','not compressed'));out.writeframes(struct.pack('<'+'h'*n,*samples))
