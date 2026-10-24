#include "usf_decoder.h"
#include <iostream>
#include <stdexcept>
int main(int argc,char** argv){
 if(argc!=2)return 2;std::string root=std::string(argv[1])+"/";
 for(auto name:{"tone.usf","mini.miniusf","unicode.usf","aux.miniusf"}){
  UsfDecoder d(root+name);d.start();int16_t pcm[4096];
  for(int i=0;i<60;i++)d.render(pcm,4096);d.seek(100);d.seek(2500);d.render(pcm,4096);
  std::cout<<"PASS ASan render/reset/seek "<<name<<std::endl;
 }
 for(auto name:{"bad.usf","truncated.usf","bad-ram.usf","missing.miniusf","cycle.miniusf","escape.miniusf","bad-crc.usf","bad-dma.usf","stuck.usf"}){
  bool rejected=false;
  try {UsfDecoder d(root+name);d.start();int16_t pcm[4096];d.render(pcm,4096);}
  catch(const std::exception& e){rejected=true;std::cout<<"PASS ASan reject "<<name<<": "<<e.what()<<std::endl;}
  if(!rejected)return 1;
 }
}
