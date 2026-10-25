"""Original N64 MIPS/AI tone program. No Nintendo code, ROM or music samples."""
import math,pathlib,struct,sys,zlib
p=pathlib.Path(sys.argv[1]);p.mkdir(parents=True,exist_ok=True)
def u32(v):return struct.pack('<I',v)
def block(words):return b''.join(u32(v) for v in words)
# Project64 state layout consumed by LazyUSF2's loader.
s=bytearray(b'\xc8\xa6\xd8\x23'+u32(0x400000))
header=bytearray(64);header[:4]=b'\x80\x37\x12\x40';header[0x3e]=ord('E');s+=header
s+=block([0x10000000,0x80010000]) # VI timer and PC
s+=bytes(32*8*2) # GPR/FPR
cp0=[0]*32;cp0[11]=0x7fffffff;s+=block(cp0)
s+=bytes(32*4+16) # FCR and HI/LO
s+=bytes(10*4) # RDRAM registers
s+=block([0,0,0,0,1,0,0,0,0,0]) # SP (halted)
s+=bytes(10*4+4*4) # DPC/MI
vi=[0]*14;vi[6]=524;s+=block(vi)
s+=block([0,0,1,0,1103,15]) # AI
s+=bytes((13+8+4)*4) # PI/RI/SI
s+=bytes(32*5*4+64) # TLB/PIF
ram_offset=len(s)
ram=bytearray(0x400000)
# Feed a 4KiB original sine-wave PCM block repeatedly to the N64 audio interface.
code=[0x3c08a450,0x3c090002,0x340a1000,0xad090000,0xad0a0004,0x08004003,0]
ram[0x10000:0x10000+len(code)*4]=block(code)
for frame in range(1024):
 v=round(10000*math.sin(2*math.pi*frame/64));struct.pack_into('<hh',ram,0x20000+frame*4,v,v)
s+=ram+bytes(0x2000)
# The published state allocation can be larger than the consumed fields.
assert len(s)<=0x40275c,(hex(ram_offset),hex(len(s)))
s+=bytes(0x40275c-len(s))
def sparse(state=b'',rom=b''):
 def part(data):return u32(0x34365253)+u32(len(data))+u32(0)+data+u32(0) if data else u32(0)
 return part(rom)+part(state)
def usf(reserved,tags=''):
 return b'PSF\x21'+block([len(reserved),0,0])+reserved+(b'[TAG]'+tags.encode() if tags else b'')
tags='title=N64 test tone\ngame=N64 Fixture\nartist=ChipAmp9x test\nlength=0:02\nfade=0:01\n'
(p/'tone.usf').write_bytes(usf(sparse(s),tags))
(p/'base.usflib').write_bytes(usf(sparse(s),'game=N64 Fixture\n'))
(p/'mini.miniusf').write_bytes(usf(b'','_lib=base.usflib\n'+tags.replace('N64 test tone','Mini test tone')))
(p/'unicode.usf').write_bytes(usf(sparse(s),'utf8=1\n'+tags.replace('N64 test tone','Forêt 🎵')))
# Alternate state patch via a secondary library, for dependency order testing.
patch=u32(0)+u32(0x34365253)+u32(4)+u32(ram_offset+0x10000)+u32(code[0])+u32(0)
(p/'patch.usflib').write_bytes(usf(patch,'title=Ignored auxiliary title\n'))
(p/'aux.miniusf').write_bytes(usf(b'','_lib=base.usflib\n_lib2=patch.usflib\n'+tags))
for name,ref in [('missing','absent.usflib'),('cycle','cycle.miniusf'),('escape','../../outside.usflib')]:
 (p/(name+'.miniusf')).write_bytes(usf(b'','_lib='+ref+'\n'))
(p/'bad.usf').write_bytes(usf(u32(0)+u32(0x34365253)+u32(16)+u32(0xffffffff)+bytes(16)+u32(0)))
(p/'truncated.usf').write_bytes(usf(u32(0x34365253)+u32(65536)+u32(0)+b'x'))
(p/'empty.usf').write_bytes(usf(b''))
invalid=bytearray(s);struct.pack_into('<I',invalid,4,0xffffffff)
(p/'bad-ram.usf').write_bytes(usf(sparse(invalid)))
# Valid state containing a CPU loop with no audio. The watchdog must stop it.
stuck=bytearray(s);stuck[ram_offset+0x10000:ram_offset+0x10008]=block([0x08004000,0])
(p/'stuck.usf').write_bytes(usf(sparse(stuck),tags))
print('Generated original USF fixtures, RDRAM offset',hex(ram_offset))

# CP1252 metadata and an explicitly zlib-encoded empty program section.
(p/'cp1252.usf').write_bytes(usf(sparse(s),'title=Forêt\nlength=0:02\nfade=0:01\n').replace('Forêt'.encode(),b'For\xeat'))
c=zlib.compress(b'');reserved=sparse(s)
(p/'zlib-empty.usf').write_bytes(b'PSF\x21'+block([len(reserved),len(c),zlib.crc32(c)])+reserved+c+b'[TAG]'+tags.encode())
(p/'bad-crc.usf').write_bytes(b'PSF\x21'+block([len(reserved),len(c),0x12345678])+reserved+c)
(p/'bad-time.usf').write_bytes(usf(sparse(s),'length=invalid\n'))
# Valid patch data but an invalid N64 AI DMA destination, rejected at playback.
dma=bytearray(s);dma[ram_offset+0x10000:ram_offset+0x10008]=block([code[0],0x3c0900ff])
(p/'bad-dma.usf').write_bytes(usf(sparse(dma),tags))

# CPU-heavy original workload for decoder optimisation benchmarking.
# Delay each DMA with 262144 counted MIPS iterations (no game driver code).
busy=bytearray(s)
work=[0x3c08a450,0x3c090002,0x340a1000,0x3c0b0004,0x256bffff,0x1560fffe,0,0xad090000,0xad0a0004,0x08004003,0]
busy[ram_offset+0x10000:ram_offset+0x10000+len(work)*4]=block(work)
(p/'busy.usf').write_bytes(usf(sparse(busy),tags))
