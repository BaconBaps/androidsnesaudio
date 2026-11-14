#!/usr/bin/env python3
"""Original PSX ADPCM fixtures and independent integer PCM reference (no game audio)."""
from pathlib import Path
import math, struct, sys
root=Path(sys.argv[1]);root.mkdir(parents=True,exist_ok=True)
C=[(0,0),(60,0),(115,-52),(98,-55),(122,-60)]
def clip(v):return max(-32768,min(32767,v))
def decode(v,param,h,xa,bits):
    f,r=param>>4,param&15
    if r>(8 if bits==8 else 12):r=8 if bits==8 else 9
    pred=h[0]*C[f][0]+h[1]*C[f][1]
    val=(v*(256 if bits==8 else 4096))//(1<<r)+( (pred+32)//64 if xa else int(pred/64))
    val=clip(val);h[1]=h[0];h[0]=val;return val
def resample(frames,rate):
    out=[]
    for n in range((len(frames)*44100+rate-1)//rate):
        q,r=divmod(n*rate,44100);a=frames[min(q,len(frames)-1)];b=frames[min(q+1,len(frames)-1)]
        out.extend(int((a[ch]*(44100-r)+b[ch]*r)/44100) for ch in range(2))
    return struct.pack('<'+'h'*len(out),*out)
manifest=[]
def save(name,data,tracks):
    (root/name).write_bytes(data)
    for i,(frames,rate) in enumerate(tracks):
        (root/(name+'.'+str(i)+'.pcm')).write_bytes(resample(frames,rate))
        manifest.append(f'{name}\t{i}\t{(len(frames)*1000+rate-1)//rate}\t{len(tracks)}')
def vag():
    h=[0,0];frames=[];data=bytearray()
    for block in range(1100):
        param=((block%5)<<4)|10;flag=6 if block==256 else 3 if block==1099 else 0
        data.extend([param,flag])
        for i in range(14):
            lo=int(6*math.sin((block*28+i*2)*.09));hi=int(6*math.sin((block*28+i*2+1)*.09));data.append((lo&15)|((hi&15)<<4))
            for v in [lo,hi]:s=decode(v,param,h,False,4);frames.append((s,s))
    header=bytearray(48);header[:4]=b'VAGp';struct.pack_into('>III',header,4,0x20,0,len(data));struct.pack_into('>I',header,16,44100)
    header[32:40]=b'Original';return bytes(header+data),frames
def xa(channels,bits,rate,channel=0,form2=True,sectors=40):
    histories=[[0,0],[0,0]];frames=[];raw=[]
    for sector in range(sectors):
        group_data=bytearray();groups=18 if form2 else 16
        for group in range(groups):
            units=4 if bits==8 else 8;params=[((i%4)<<4)|(6 if bits==8 else 10) for i in range(units)]
            block=bytearray(128)
            if bits==8:block[:16]=bytes(params*4)
            else:block[:16]=bytes(params[:4]+params+params[4:])
            decoded=[[] for _ in range(units)]
            for j in range(28):
                vals=[int((12 if bits==8 else 6)*math.sin((sector*groups*28+group*28+j)*.15+i)) for i in range(units)]
                for i,v in enumerate(vals):
                    if bits==8:block[16+j*4+i]=v&255
                    else:block[16+j*4+i//2]|=(v&15)<<(4*(i%2))
            for i in range(units):
                h=histories[i%channels]
                for j in range(28):
                    b=block[16+j*4+(i if bits==8 else i//2)]
                    v=(b if b<128 else b-256) if bits==8 else ((b>>(4*(i%2)))&15)
                    if bits==4 and v>=8:v-=16
                    decoded[i].append(decode(v,params[i],h,True,bits))
            for unit in range(0,units,channels):
                for j in range(28):frames.append((decoded[unit][j],decoded[unit+1][j] if channels==2 else decoded[unit][j]))
            group_data+=block
        raw_sector=bytearray(2352);raw_sector[:12]=b'\x00'+b'\xff'*10+b'\x00';raw_sector[15]=2
        code=(channels-1)|(4 if rate==18900 else 0)|(16 if bits==8 else 0)
        sub=bytes([1,channel,4|(32 if form2 else 0)|(128 if sector==sectors-1 else 0),code]);raw_sector[16:24]=sub*2;raw_sector[24:24+len(group_data)]=group_data;raw.append(bytes(raw_sector))
    return raw,frames
v,frames=vag();save('tone.vag',v,[(frames,44100)])
for name,ch,bits,rate,form2 in [('stereo4.xa',2,4,37800,True),('mono4.xa',1,4,18900,True),('stereo8.xa',2,8,18900,True),('mono8.xa',1,8,37800,True),('form1.xa',2,4,37800,False)]:
    sectors,frames=xa(ch,bits,rate,form2=form2);raw=b''.join(sectors);save(name,raw,[(frames,rate)])
    if name=='stereo4.xa':
        save('stripped.xa',b''.join(s[16:] for s in sectors),[(frames,rate)])
        chunks=b'fmt '+struct.pack('<I',3)+b'abc\0'+b'data'+struct.pack('<I',len(raw))+raw
        save('riff.xa',b'RIFF'+struct.pack('<I',4+len(chunks))+b'CDXA'+chunks,[(frames,rate)])
a,af=xa(2,4,37800,0);b,bf=xa(1,8,18900,1);c,cf=xa(2,4,37800,0,sectors=3)
save('interleaved.xa',b''.join(x+y for x,y in zip(a,b))+b''.join(c),[(af,37800),(bf,18900),(cf,37800)])
for name,source,at,value in [('bad-vag-predictor.vag',v,48,0x50),('stereo.vag',v,30,2),('bad-mode.xa',a[0],15,1),('bad-subheader.xa',a[0],20,2),('bad-coding.xa',a[0],19,2),('bad-xa-predictor.xa',a[0],28,0x40)]:
    bad=bytearray(source);bad[at]=value
    if name=='bad-coding.xa':bad[23]=value
    (root/name).write_bytes(bad)
(root/'truncated.xa').write_bytes(a[0][:-1]);(root/'truncated.vag').write_bytes(v[:-1]);(root/'not-cdxa.xa').write_bytes(b'RIFF'+b'\0'*40)
(root/'manifest.tsv').write_text('\n'.join(manifest)+'\n')
print('Generated',len(manifest),'reference streams plus malformed PSX audio fixtures')
