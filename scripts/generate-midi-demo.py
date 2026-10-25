#!/usr/bin/env python3
"""Original short MIDI phrase for trying SoundFonts. MIT; no game music."""
import pathlib,struct
P=lambda fmt,*v:struct.pack('>'+fmt,*v)
def var(n):
 a=[n&127];n>>=7
 while n:a.insert(0,128|(n&127));n>>=7
 return bytes(a)
def make(events,end=15360):
 data=b'';last=0
 for tick,message in sorted(events,key=lambda e:e[0]):data+=var(tick-last)+message;last=tick
 data+=var(end-last)+b'\xff\x2f\x00'
 return b'MTrk'+P('I',len(data))+data
tracks=[]
title=b'ChipAmp9x - Pixel Steps';tracks.append(make([(0,b'\xff\x03'+bytes([len(title)])+title),(0,b'\xff\x51\x03\x07\xa1\x20')]))
melody=[72,76,79,76,74,77,81,77,71,74,79,74,72,76,79,84,79,76,72,76,77,81,84,81,79,74,71,74,76,72,67,72]*2
ev=[(0,b'\xc0\x50'),(0,b'\xb0\x07\x64')]
for i,note in enumerate(melody):ev.extend([(i*240,bytes([0x90,note,80])),(i*240+200,bytes([0x80,note,0]))])
tracks.append(make(ev))
ev=[(0,b'\xc1\x26'),(0,b'\xb1\x07\x50')]
for i in range(32):
 note=[48,53,43,48][(i//4)%4];ev.extend([(i*480,bytes([0x91,note,80])),(i*480+350,bytes([0x81,note,0]))])
tracks.append(make(ev));ev=[]
for i in range(64):
 note=36 if i%4==0 else 38 if i%4==2 else 42
 ev.extend([(i*240,bytes([0x99,note,55])),(i*240+60,bytes([0x89,note,0]))])
tracks.append(make(ev));out=pathlib.Path('downloads/ChipAmp9x-MIDI-demo.mid');out.write_bytes(b'MThd'+P('IHHH',6,1,len(tracks),480)+b''.join(tracks));print(out)
