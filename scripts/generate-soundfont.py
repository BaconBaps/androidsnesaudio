#!/usr/bin/env python3
"""Original synthetic GM-compatible retro bank. No sampled game instruments. MIT."""
import math,random,struct,pathlib
P=lambda fmt,*v:struct.pack('<'+fmt,*v)
def name(s):return s.encode()[:19].ljust(20,b'\0')
def chunk(tag,data):return tag.encode()+P('I',len(data))+data+(b'\0' if len(data)%2 else b'')
def listing(tag,data):return chunk('LIST',tag.encode()+data)
def gen(op,v):return P('HH',op,v&65535)
samples=[];headers=[];gens=[];bags=[];inst=[];rng=random.Random(199)
for family in range(17):
 start=len(samples)
 if family<16:
  # Periodic harmonic waveforms, tuned to C4 through a sample rate of 33,488 Hz.
  n=128
  for j in range(n):
   a=2*math.pi*j/n
   if family in (0,1,8,9,10,11):v=math.sin(a)+.25*math.sin(2*a)+.12*math.sin(3*a)
   elif family in (2,3,4,5):v=sum(math.sin(k*a)/k for k in range(1,10,2))
   elif family in (6,7,12,13):v=sum((-1)**(k+1)*math.sin(k*a)/k for k in range(1,8))
   else:v=math.sin(a+1.5*math.sin(2*a))
   samples.append(int(v*10000))
  end=len(samples);rate=33488;root=60
 else:
  n=6615
  for j in range(n):
   t=j/22050;v=(.65*rng.uniform(-1,1)+.35*math.sin(2*math.pi*(90*t-70*t*t)))*math.exp(-t*18)
   samples.append(int(v*16000))
  end=len(samples);rate=22050;root=60
 headers.append(name('Pixel %02d'%family)+P('IIIIIBbHH',start,end,start,end,rate,root,0,0,1))
 samples.extend([0]*46)
 inst.append(name('Voice %02d'%family)+P('H',len(bags)))
 bags.append(P('HH',len(gens),0))
 if family==16:gens.extend([gen(54,0),gen(56,0)])
 else:
  attack=-9000 if family<8 else -5500
  gens.extend([gen(34,attack),gen(36,-2400 if family<2 else 0),gen(37,180 if family<2 else 0),gen(38,-3000),gen(54,1)])
 gens.append(gen(53,family))
inst.append(name('EOI')+P('H',len(bags)));bags.append(P('HH',len(gens),0));gens.append(gen(0,0))
headers.append(name('EOS')+bytes(26))
phdr=[];pbag=[];pgen=[]
for program in range(129):
 bank=0 if program<128 else 128;patch=program if program<128 else 0
 phdr.append(name('Pixel GM %03d'%patch if bank==0 else 'Pixel drums')+P('HHHIII',patch,bank,len(pbag),0,0,0))
 pbag.append(P('HH',len(pgen),0));pgen.append(gen(41,program//8 if program<128 else 16))
phdr.append(name('EOP')+P('HHHIII',0,0,len(pbag),0,0,0));pbag.append(P('HH',len(pgen),0));pgen.append(gen(0,0))
info=listing('INFO',chunk('ifil',P('HH',2,1))+chunk('isng',b'EMU8000\0')+chunk('INAM',b'ChipAmp9x Pixel GM\0')+chunk('ICOP',b'Original synthetic waveforms; MIT license\0'))
sdta=listing('sdta',chunk('smpl',b''.join(P('h',x) for x in samples)))
pdta=listing('pdta',b''.join(chunk(tag,data) for tag,data in [('phdr',b''.join(phdr)),('pbag',b''.join(pbag)),('pmod',bytes(10)),('pgen',b''.join(pgen)),('inst',b''.join(inst)),('ibag',b''.join(bags)),('imod',bytes(10)),('igen',b''.join(gens)),('shdr',b''.join(headers))]))
out=pathlib.Path('app/src/main/assets/soundfonts/Pixel-GM.sf2');out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(chunk('RIFF',b'sfbk'+info+sdta+pdta));print(out,out.stat().st_size)
