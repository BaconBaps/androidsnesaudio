#include "usf_decoder.h"
#include <chrono>
#include <cstdint>
#include <iostream>
int main(int argc,char** argv){
    if(argc!=2)return 2;
    UsfDecoder decoder(argv[1]);decoder.start();decoder.loop(true);
    int16_t pcm[4096];uint64_t hash=1469598103934665603ull;
    auto start=std::chrono::steady_clock::now();
    for(int i=0;i<108;i++){
        decoder.render(pcm,4096);
        for(auto sample:pcm){hash^=uint16_t(sample);hash*=1099511628211ull;}
    }
    double seconds=std::chrono::duration<double>(std::chrono::steady_clock::now()-start).count();
    std::cout<<"PCM hash="<<hash<<" audio_seconds="<<108*2048./44100<<" decode_seconds="<<seconds<<std::endl;
}
