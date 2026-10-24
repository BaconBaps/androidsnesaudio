"""Original MIDI regression fixtures, no game music. MIT."""
import struct,pathlib,sys
p=pathlib.Path(sys.argv[1]);p.mkdir(parents=True,exist_ok=True)
def var(v):
 a=[v&127];v>>=7
 while v:a.insert(0,128|(v&127));v>>=7
 return bytes(a)
def event(dt,data):return var(dt)+data
def track(data):return b'MTrk'+struct.pack('>I',len(data))+data
def midi(fmt,tracks):return b'MThd'+struct.pack('>IHHH',6,fmt,len(tracks),480)+b''.join(track(t) for t in tracks)
eot=b'\xff\x2f\x00'
tempo=event(0,b'\xff\x03\x0cMIDI Fixture')+event(0,b'\xff\x51\x03\x07\xa1\x20')+event(480,b'\xff\x51\x03\x03\xd0\x90')+event(960,eot)
notes=event(0,b'\xc0\x00')+event(0,b'\xb0\x07\x64')+event(0,b'\x90\x3c\x64')+event(240,b'\x3c\x00')+event(240,b'\xc0\x50')+event(0,b'\x90\x40\x64')+event(480,b'\x80\x40\x00')+event(480,eot)
(p/'tempo.mid').write_bytes(midi(1,[tempo,notes]))
(p/'plain.midi').write_bytes(midi(0,[event(0,b'\xc0\x05')+event(0,b'\x90\x3c\x64')+event(960,b'\x80\x3c\x00')+event(960,eot)]))
(p/'drums.mid').write_bytes(midi(0,[event(0,b'\x99\x26\x64')+event(240,b'\x89\x26\x00')+event(240,eot)]))
(p/'sustain.mid').write_bytes(midi(0,[event(0,b'\xb0\x40\x7f')+event(0,b'\x90\x3c\x64')+event(120,b'\x80\x3c\x00')+event(840,b'\xb0\x40\x00')+event(480,eot)]))
(p/'bad.mid').write_bytes(midi(0,[b'\x00\x3c\x64'+event(480,eot)]))
(p/'truncated.mid').write_bytes((p/'tempo.mid').read_bytes()[:-4])
(p/'smpte.mid').write_bytes(b'MThd'+struct.pack('>IHHH',6,0,1,0xe728)+track(event(0,eot)))
b=bytearray(pathlib.Path('app/src/main/assets/soundfonts/Pixel-GM.sf2').read_bytes());at=b.index(b'smpl');n=struct.unpack_from('<I',b,at+4)[0]
for i in range(at+8,at+8+n,2):struct.pack_into('<h',b,i,-struct.unpack_from('<h',b,i)[0])
(p/'inverted.sf2').write_bytes(b)
b[8:12]=b'xxxx';(p/'invalid.sf2').write_bytes(b)
b=bytearray(pathlib.Path('app/src/main/assets/soundfonts/Pixel-GM.sf2').read_bytes());at=b.index(b'phdr');struct.pack_into('<H',b,at+8+24,65535);(p/'bad-index.sf2').write_bytes(b)

b=bytearray(pathlib.Path("app/src/main/assets/soundfonts/Pixel-GM.sf2").read_bytes());at=b.index(b"igen");struct.pack_into("<HH",b,at+8,0,32767);(p/"bad-offset.sf2").write_bytes(b)

# Reproduce a one-shot sample with equal source loop markers at a nonzero
# offset: TSF converts the exclusive loop end to start - 1. These are unused.
b=bytearray(pathlib.Path("app/src/main/assets/soundfonts/Pixel-GM.sf2").read_bytes())
at=b.index(b"shdr")+8+46;start=struct.unpack_from("<I",b,at+20)[0]
struct.pack_into("<II",b,at+28,start,start)
at=b.index(b"igen")+8+10*4;struct.pack_into("<HH",b,at,54,0)
(p/"oneshot-unused-loops.sf2").write_bytes(b)
# The same malformed markers in a genuinely active loop still fail.
b=bytearray(pathlib.Path("app/src/main/assets/soundfonts/Pixel-GM.sf2").read_bytes())
at=b.index(b"igen")+8;struct.pack_into("<HH",b,at,3,32767)
(p/"bad-active-loop.sf2").write_bytes(b)

(p/"oneshot.mid").write_bytes(midi(0,[event(0,b"\xc0\x08")+event(0,b"\x90\x3c\x64")+event(480,b"\x80\x3c\x00")+event(480,eot)]))
