"""Original MIPS/SPU test tone; generated files are test fixtures, not bundled music."""
import struct,zlib,pathlib,sys
p=pathlib.Path(sys.argv[1]);p.mkdir(parents=True,exist_ok=True)
code=[]
def add(v):code.append(v)
def li(r,v):add(0x34000000|(r<<16)|v)
def sh(r,offset):add(0xa4000000|(8<<21)|(r<<16)|offset)
add(0x3c081f80);add(0x35081c00)
for off,v in [(0x180,0x3fff),(0x182,0x3fff),(0x1aa,0xc000),(0x1a6,0x200)]:li(9,v);sh(9,off)
for v in [0x0400,0x7777,0x7777,0x9999,0x9999,0x7777,0x9999,0x7777,0x0300,0x7777,0x7777,0x9999,0x9999,0x7777,0x9999,0x7777]:li(9,v);sh(9,0x1a8)
for off,v in [(0,0x3fff),(2,0x3fff),(4,0x1000),(6,0x200),(8,0x000f),(10,0),(0x188,1)]:li(9,v);sh(9,off)
pc=0x80010000+len(code)*4;add(0x08000000|((pc>>2)&0x3ffffff));add(0)
exe=bytearray(2048);exe[:8]=b'PS-X EXE';struct.pack_into('<II',exe,0x10,0x80010000,0);struct.pack_into('<II',exe,0x18,0x80010000,len(code)*4);struct.pack_into('<I',exe,0x30,0x801fff00);exe+=b''.join(struct.pack('<I',x) for x in code)
def psf(exe,tags):
 c=zlib.compress(exe);return b'PSF\x01'+struct.pack('<III',0,len(c),zlib.crc32(c))+c+b'[TAG]'+tags.encode()
(p/'tone.psf').write_bytes(psf(exe,'title=PSF test tone\ngame=PSF Fixture\nartist=ChipAmp9x test\nlength=0:02\nfade=0:01\n'))
(p/'base.psflib').write_bytes(psf(exe,'game=PSF Fixture\n'))
patch=bytearray(2048);patch[:8]=b'PS-X EXE';struct.pack_into('<I',patch,0x18,0x80010000)
(p/'mini.minipsf').write_bytes(psf(patch,'_lib=base.psflib\ntitle=Mini test tone\nlength=0:02\nfade=0:01\n'))

(p/'missing.minipsf').write_bytes(psf(patch,'_lib=absent.psflib\ntitle=Missing library\n'))
(p/'cycle.minipsf').write_bytes(psf(patch,'_lib=cycle.minipsf\n'))
(p/'escape.minipsf').write_bytes(psf(patch,'_lib=../../outside.psflib\n'))
(p/'unicode.psf').write_bytes(psf(exe,'utf8=1\ntitle=Forêt 🎵\nlength=0:02\nfade=0:01\n'))
(p/'bad.psf').write_bytes(b'PSF\x01'+bytes(12))
