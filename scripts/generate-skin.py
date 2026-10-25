#!/usr/bin/env python3
"""Draw original pixel hardware sprites. stdlib only; .9 borders are Android metadata."""
from pathlib import Path
import struct, zlib
RES = Path(__file__).resolve().parents[1] / 'app/src/main/res'
DENSITIES = {'mdpi':1,'hdpi':1.5,'xhdpi':2,'xxhdpi':3,'xxxhdpi':4}
class Sprite:
    def __init__(self,w,h):
        self.w,self.h=w,h
        self.p=[[(0,0,0,0) for _ in range(w)] for _ in range(h)]
    def rect(self,x,y,w,h,c):
        for j in range(y,y+h):
            for i in range(x,x+w): self.p[j][i]=c
    def save(self,name,patch=False,pad=4):
        for density,scale in DENSITIES.items():
            out=RES/('drawable-'+density);out.mkdir(parents=True,exist_ok=True)
            w,h=round(self.w*scale),round(self.h*scale)
            rows=[[self.p[min(self.h-1,int(y/scale))][min(self.w-1,int(x/scale))] for x in range(w)] for y in range(h)]
            if patch:
                content=rows
                rows=[[(0,0,0,0)]*(w+2) for _ in range(h+2)]
                for y in range(h): rows[y+1][1:w+1]=content[y]
                fixed=round((10 if name=="skin_chassis.9.png" else pad)*scale)
                for x in range(fixed,w-fixed): rows[0][x+1]=(0,0,0,255)
                for y in range(fixed,h-fixed): rows[y+1][0]=(0,0,0,255)
                padding=round(pad*scale)
                for x in range(padding,w-padding): rows[h+1][x+1]=(0,0,0,255)
                for y in range(padding,h-padding): rows[y+1][w+1]=(0,0,0,255)
                w+=2;h+=2
            def chunk(t,b): return struct.pack('>I',len(b))+t+b+struct.pack('>I',zlib.crc32(t+b)&0xffffffff)
            raw=b''.join(b'\0'+bytes(channel for pixel in row for channel in pixel) for row in rows)
            png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',w,h,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
            (out/name).write_bytes(png)
def bevel(s,inset,light,dark):
    w,h=s.w-2*inset,s.h-2*inset
    s.rect(inset,inset,w,1,light);s.rect(inset,inset,1,h,light)
    s.rect(inset,inset+h-1,w,1,dark);s.rect(inset+w-1,inset,1,h,dark)
W=lambda a:(255,255,255,a)
B=lambda a:(0,0,0,a)
for name,recessed,depth in [('skin_chassis',False,5),('skin_panel',False,3),('skin_well',True,3),('skin_key',False,3),('skin_key_down',True,3)]:
    s=Sprite(32,32)
    bevel(s,0,B(220),B(245))
    bevel(s,1,B(175) if recessed else W(155),W(80) if recessed else B(165))
    bevel(s,2,B(105) if recessed else W(42),W(30) if recessed else B(75))
    if name=="skin_key":
        bevel(s,2,W(86),B(95));s.rect(4,3,24,1,W(25))
    if name=="skin_key_down":
        bevel(s,2,B(140),W(30));s.rect(3,3,26,1,B(60))
    if depth==5:
        bevel(s,3,B(110),W(60));bevel(s,4,W(22),B(110))
        for x,y in [(5,5),(23,5),(5,23),(23,23)]:
            s.rect(x,y,4,4,B(180));s.rect(x,y,3,1,W(135));s.rect(x,y,1,3,W(70));s.rect(x+1,y+1,2,2,B(80));s.rect(x+1,y+1,1,2,B(230))
    s.save(name+'.9.png',True,6 if depth==5 else 4)
s=Sprite(32,16)
for y in range(16):
    for x in range(32):
        # Brushed metal: short, subtle horizontal grain, no random build noise.
        n=(x*13+y*7+(x//5)*11)%17
        if y%4==0:s.p[y][x]=W(10 if n<9 else 4)
        elif y%4==2:s.p[y][x]=B(12 if n<10 else 5)
        elif n<3:s.p[y][x]=B(5)
s.save('skin_brushed.png')
s=Sprite(4,4);s.rect(0,1,4,1,B(17));s.save('skin_scanlines.png')
s=Sprite(40,12)
for y,c in [(1,B(210)),(2,W(190)),(3,W(60)),(4,B(175)),(6,B(210)),(7,W(190)),(8,W(60)),(9,B(175))]:s.rect(0,y,40,1,c)
s.save('skin_rail.9.png',True,1)
s=Sprite(16,20);s.rect(0,0,16,20,(205,209,218,255));bevel(s,0,(15,16,22,255),(5,6,12,255));bevel(s,1,(252,253,255,255),(65,68,81,255));bevel(s,2,(229,232,238,255),(123,127,145,255))
for x in [5,8,11]:s.rect(x,6,1,8,(87,92,110,255));s.rect(x+1,6,1,8,(242,243,248,255))
s.save('skin_thumb.png')
print('Generated 9 original sprite designs (6 nine-patch types) at',', '.join(DENSITIES))

# Reactor: original, palette-neutral mechanical frames. Texture is tiled separately
# so rivets and dither never become stretched pixels in a tall playlist.
for name in ['chassis','panel','well','key','key_down']:
    s=Sprite(40,40)
    recessed=name in ('well','key_down')
    bevel(s,0,B(230),B(245))
    bevel(s,1,W(48) if not recessed else B(220),B(170) if not recessed else W(42))
    bevel(s,2,W(20) if not recessed else B(150),B(105) if not recessed else W(20))
    bevel(s,3,B(140),W(28))
    if name=='chassis':
        bevel(s,4,W(30),B(160))
        for x,y in [(5,5),(30,5),(5,30),(30,30)]:
            s.rect(x,y,5,5,B(230));s.rect(x,y,4,1,W(105));s.rect(x,y,1,4,W(52));s.rect(x+1,y+2,3,1,B(250))
        # Keep stretch bands free of isolated detail: otherwise AAPT stretches
        # hatching into long bars. Rivets live entirely in fixed corners.
    if name=='key':bevel(s,2,W(36),B(140))
    if name=='key_down':bevel(s,2,B(175),W(28))
    s.save('reactor_'+name+'.9.png',True,10 if name=='chassis' else 5)
s=Sprite(8,8)
for y in range(8):
    for x in range(8):
        if (x+y)%2==0:s.p[y][x]=B(32)
        elif y%4==0:s.p[y][x]=W(7)
s.save('reactor_dither.png')
s=Sprite(32,12)
for y,c in [(0,B(220)),(1,W(36)),(3,B(200)),(4,B(230)),(5,W(38)),(8,B(220)),(9,W(30)),(11,B(220))]:s.rect(0,y,32,1,c)
s.save('reactor_rail.9.png',True,1)
print('Generated Reactor mechanical frames, dither tile and vent rail')
