#!/usr/bin/env python3
"""Original flat grey pixel cog. MIT."""
import math,pathlib
size=32
mask=set()
for y in range(size):
 for x in range(size):
  dx=x-15.5;dy=y-15.5;r=math.hypot(dx,dy);angle=math.atan2(dy,dx)
  tooth=abs((angle+math.pi/8)%(math.pi/4)-math.pi/8)<.19
  if r<=10.5 or (r<=13.5 and tooth):mask.add((x,y))
colors={};pixels={}
for y in range(size):
 for x in range(size):
  r=math.hypot(x-15.5,y-15.5)
  inside=(x,y) in mask
  edge=any((x+dx,y+dy) in mask for dx in range(-2,3) for dy in range(-2,3))
  if r<4:continue
  if not inside and not edge:continue
  color='#000000' if not inside or r<5.5 else '#B8B8B8' if r>10 else '#8C8C8C'
  pixels[x,y]=color
# Merge adjacent pixels, then keep each path short for Android vector caching.
for y in range(size):
 x=0
 while x<size:
  color=pixels.get((x,y));start=x;x+=1
  while x<size and pixels.get((x,y))==color:x+=1
  if color:colors.setdefault(color,[]).append(f'M{start},{y}h{x-start}v1h-{x-start}z')
xml='<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="32dp" android:height="32dp" android:viewportWidth="32" android:viewportHeight="32">\n'
for color,paths in colors.items():
 group=[];length=0
 for path in paths:
  if length+len(path)>600:
   xml+=f'    <path android:fillColor="{color}" android:pathData="'+ ' '.join(group)+'"/>\n';group=[];length=0
  group.append(path);length+=len(path)+1
 if group:xml+=f'    <path android:fillColor="{color}" android:pathData="'+ ' '.join(group)+'"/>\n'
xml+='</vector>\n';pathlib.Path('app/src/main/res/drawable/ic_menu.xml').write_text(xml)
