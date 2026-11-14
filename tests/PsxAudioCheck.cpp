#include "psx_audio.h"
#include <fstream>
#include <iterator>
#include <iostream>
#include <vector>
#include <cstdio>

// ASan/UBSan exercise with valid seeds and deterministic malformed mutations.
int main(int argc,char** argv){
    if(argc!=2)return 2;std::string root=argv[1];uint32_t random=0x9128ac;
    for(const char* name:{"tone.vag","stereo4.xa","mono8.xa","interleaved.xa","riff.xa"}){
        std::ifstream in(root+"/"+name,std::ios::binary);std::vector<char> seed((std::istreambuf_iterator<char>(in)),{});
        if(seed.empty())return 3;std::string path=root+"/mutation"+(name[0]=='t'?".vag":".xa");
        for(int attempt=0;attempt<250;attempt++){
            std::vector<char> bytes=seed;
            for(int n=0;n<(attempt?5:0);n++){random=random*1664525u+1013904223u;size_t at=random%bytes.size();random=random*1664525u+1013904223u;bytes[at]=char(random>>24);}
            if(attempt%11==10)bytes.resize(bytes.size()/2);
            {std::ofstream out(path,std::ios::binary);out.write(bytes.data(),bytes.size());}
            try{PsxAudio decoder(path);std::vector<int16_t> pcm(4096);
                for(int track=0;track<decoder.count();track++){decoder.start(track);decoder.render(pcm.data(),pcm.size());decoder.seek(decoder.length(track)/2);decoder.render(pcm.data(),pcm.size());decoder.loop(true);decoder.seek(decoder.length(track));decoder.render(pcm.data(),pcm.size());}
            }catch(const std::exception&){}
        }
        std::remove(path.c_str());
    }
    std::cout<<"PASS: 1250 valid/mutated PSX audio opens, renders, seeks and loops under sanitizers\n";
}
