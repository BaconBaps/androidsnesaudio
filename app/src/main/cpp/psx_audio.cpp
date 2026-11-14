#include "psx_audio.h"
#include <algorithm>
#include <array>
#include <cstring>
#include <fstream>
#include <map>
#include <stdexcept>

namespace {
const uint64_t MAX_FILE=128ull*1024*1024;
int clamp(int value){return std::max(-32768,std::min(32767,value));}
uint32_t be32(const uint8_t* p){return uint32_t(p[0])*16777216u+uint32_t(p[1])*65536u+uint32_t(p[2])*256u+p[3];}
uint32_t le32(const uint8_t* p){return uint32_t(p[3])*16777216u+uint32_t(p[2])*65536u+uint32_t(p[1])*256u+p[0];}
// Arithmetic division with floor, independent of signed right-shift behaviour.
int down(int value,int bits){int d=1<<bits;return value>=0?value/d:-int((uint32_t(-int64_t(value))+d-1)/d);}
int nibble(int value){value&=15;return value>=8?value-16:value;}
bool sync(const uint8_t* p){if(p[0]||p[11])return false;for(int i=1;i<11;i++)if(p[i]!=255)return false;return true;}
struct History {int a[2]={0,0},b[2]={0,0};};
struct Block {uint64_t offset,first;int bytes,frames;History history;bool known=false;};
struct Track {int rate=0,channels=0,bits=0,file=0,channel=0;uint64_t frames=0;int64_t repeat=-1;std::vector<Block> blocks;};
}
struct PsxAudio::Impl {
    std::ifstream input;uint64_t size=0;bool vag=false,looping=false;int selected=0;
    std::string title;std::vector<Track> tracks;
    uint64_t position=0; // output-frame position at 44.1 kHz
    int cached=-1;std::vector<std::array<int16_t,2>> pcm;
    void read(uint64_t at,uint8_t* out,size_t count){
        if(at>size||count>size-at)throw std::runtime_error("Truncated PlayStation audio");
        input.clear();input.seekg(at);input.read(reinterpret_cast<char*>(out),count);
        if(!input)throw std::runtime_error("Could not read PlayStation audio");
    }
    int sample(int encoded,int parameter,int channel,History& h,bool xa,int bits){
        static const int c0[]={0,60,115,98,122},c1[]={0,0,-52,-55,-60};
        int filter=parameter>>4,range=parameter&15;
        if(filter>=(xa?4:5))throw std::runtime_error("Invalid PlayStation ADPCM predictor");
        if(range>(bits==8?8:12))range=bits==8?8:9; // hardware reserved-range fallback
        int value=down(encoded*(bits==8?256:4096),range);
        int prediction=h.a[channel]*c0[filter]+h.b[channel]*c1[filter];
        value+=xa?down(prediction+32,6):prediction/64;
        h.b[channel]=h.a[channel];h.a[channel]=clamp(value);
        return clamp(value);
    }
    void decode(int track,int block,History& h){
        Track& t=tracks[track];Block& b=t.blocks[block];std::vector<uint8_t> bytes(b.bytes);read(b.offset,bytes.data(),bytes.size());
        pcm.clear();pcm.resize(b.frames);
        if(vag){
            for(int n=0;n<b.bytes/16;n++){
                const uint8_t* in=bytes.data()+n*16;
                for(int j=0;j<28;j++){int v=sample(nibble(in[2+j/2]>>(4*(j%2))),in[0],0,h,false,4);pcm[n*28+j]={{int16_t(v),int16_t(v)}};}
            }
        }else{
            int units=t.bits==8?4:8,perGroup=28*units/t.channels;
            for(int group=0;group<b.bytes/128;group++){
                const uint8_t* in=bytes.data()+group*128;
                for(int unit=0;unit<units;unit++){
                    int channel=t.channels==2?unit%2:0;
                    int start=group*perGroup+(unit/t.channels)*28;
                    int parameter=in[(t.bits==8?0:4)+unit];
                    for(int j=0;j<28;j++){
                        int encoded=t.bits==8?int(int8_t(in[16+j*4+unit])):nibble(in[16+j*4+unit/2]>>(4*(unit%2)));
                        int v=sample(encoded,parameter,channel,h,true,t.bits);pcm[start+j][channel]=v;if(t.channels==1)pcm[start+j][1]=v;
                    }
                }
            }
        }
    }
    void load(int block){
        if(block==cached)return;
        Track& t=tracks[selected];int from=block;while(from>0&&!t.blocks[from].known)from--;
        History h=t.blocks[from].history;
        for(int i=from;i<=block;i++){
            decode(selected,i,h);
            if(i+1<int(t.blocks.size())){t.blocks[i+1].history=h;t.blocks[i+1].known=true;}
        }
        cached=block;
    }
    std::array<int16_t,2> frame(uint64_t frame){
        Track& t=tracks[selected];frame=std::min(frame,t.frames-1);
        auto at=std::upper_bound(t.blocks.begin(),t.blocks.end(),frame,[](uint64_t f,const Block& b){return f<b.first;});
        int block=int(at-t.blocks.begin())-1;load(block);return pcm[size_t(frame-t.blocks[block].first)];
    }
    void add(Track& t,uint64_t offset,int bytes,int frames){Block b;b.offset=offset;b.bytes=bytes;b.frames=frames;b.first=t.frames;b.known=t.blocks.empty();t.blocks.push_back(b);t.frames+=frames;}
    void parseVag(){
        uint8_t header[48];read(0,header,48);
        if(std::memcmp(header,"VAGp",4))throw std::runtime_error("Only standard mono VAGp audio is supported");
        uint32_t version=be32(header+4),bytes=be32(header+12),rate=be32(header+16);
        if(version>0x20||header[30]>1||header[31]>1||rate<4000||rate>192000||!bytes||bytes%16||bytes>size-48)throw std::runtime_error("Invalid or unsupported VAG header (standard mono VAGp required)");
        Track t;t.rate=rate;t.channels=1;t.bits=4;
        uint64_t part=48;int partBytes=0;bool done=false;
        for(uint64_t chunk=48;chunk<48+uint64_t(bytes)&&!done;chunk+=4096){
            uint8_t buffer[4096];int amount=int(std::min<uint64_t>(4096,48+uint64_t(bytes)-chunk));read(chunk,buffer,amount);
            for(int n=0;n<amount;n+=16){
                uint64_t at=chunk+n;const uint8_t* block=buffer+n;int flag=block[1];
                if((block[0]>>4)>4||flag>7)throw std::runtime_error("Invalid VAG ADPCM block");
                if(flag==7){done=true;break;}
                if((flag&4)&&t.repeat<0)t.repeat=(at-48)/16*28;
                partBytes+=16;
                if(partBytes==4096||(flag&1)){add(t,part,partBytes,partBytes/16*28);part=at+16;partBytes=0;}
                if(flag&1){done=true;break;}
            }
        }
        if(partBytes)add(t,part,partBytes,partBytes/16*28);
        // Sector-aligned zero padding is common; concatenated/stereo variants
        // must not be mistaken for a standard mono file.
        for(uint64_t at=48+uint64_t(bytes);at<size;at+=4096){uint8_t padding[4096];int n=int(std::min<uint64_t>(4096,size-at));read(at,padding,n);for(int i=0;i<n;i++)if(padding[i])throw std::runtime_error("Unsupported VAG trailing data or stereo layout");}
        if(t.frames==0)throw std::runtime_error("VAG contains no audio blocks");
        tracks.push_back(std::move(t));
    }
    void parseXa(){
        uint8_t first[24];read(0,first,24);uint64_t base=0,bytes=size;
        if(!std::memcmp(first,"RIFF",4)){
            if(std::memcmp(first+8,"CDXA",4))throw std::runtime_error("XA RIFF container is not CDXA");
            uint64_t end=uint64_t(le32(first+4))+8;if(end>size||end<12)throw std::runtime_error("Truncated CDXA container");
            bool found=false;
            for(uint64_t at=12;at+8<=end;){uint8_t chunk[8];read(at,chunk,8);uint64_t n=le32(chunk+4);if(n>end-at-8)throw std::runtime_error("Invalid CDXA chunk size");if(!std::memcmp(chunk,"data",4)){base=at+8;bytes=n;found=true;break;}at+=8+n+(n&1);}
            if(!found)throw std::runtime_error("CDXA container has no data chunk");read(base,first,24);
        }
        int stride=sync(first)?2352:2336,sub=stride==2352?16:0,data=sub+8;
        if(!bytes||bytes%stride)throw std::runtime_error("XA requires complete 2352-byte or 2336-byte CD sectors");
        std::map<int,int> active;
        for(uint64_t at=base;at<base+bytes;at+=stride){
            uint8_t header[24];read(at,header,data);
            if(stride==2352&&(!sync(header)||header[15]!=2))throw std::runtime_error("Invalid XA Mode 2 sector");
            uint8_t* h=header+sub;if(std::memcmp(h,h+4,4))throw std::runtime_error("XA sector subheaders disagree");
            if(!(h[2]&4))continue;
            int code=h[3];if(h[1]>31||(code&0xaa))throw std::runtime_error("Unsupported XA audio coding information");
            int key=h[0]*256+h[1],index;
            auto it=active.find(key);
            if(it==active.end()){
                if(tracks.size()>=1000)throw std::runtime_error("XA exceeds 1000 streams");
                Track t;t.file=h[0];t.channel=h[1];t.channels=(code&1)?2:1;t.rate=(code&4)?18900:37800;t.bits=(code&16)?8:4;
                index=tracks.size();tracks.push_back(t);active[key]=index;
            }else index=it->second;
            Track& t=tracks[index];if(t.channels!=((code&1)?2:1)||t.rate!=((code&4)?18900:37800)||t.bits!=((code&16)?8:4))throw std::runtime_error("XA stream changes audio format without an end marker");
            int groups=(h[2]&0x20)?18:16;
            uint8_t audio[2304];read(at+data,audio,groups*128);
            for(int group=0;group<groups;group++)for(int unit=0;unit<(t.bits==8?4:8);unit++)if((audio[group*128+(t.bits==8?0:4)+unit]>>4)>3)throw std::runtime_error("Invalid XA ADPCM predictor");
            add(t,at+data,groups*128,groups*28*(t.bits==8?4:8)/t.channels);
            if(h[2]&0x80)active.erase(key);
        }
        if(tracks.empty())throw std::runtime_error("XA contains no supported audio streams");
    }
};
PsxAudio::PsxAudio(const std::string& path):p(new Impl){
    p->input.open(path,std::ios::binary);if(!p->input)throw std::runtime_error("Cannot open PlayStation audio");
    p->input.seekg(0,std::ios::end);auto size=p->input.tellg();if(size<24||uint64_t(size)>MAX_FILE)throw std::runtime_error("PlayStation audio must be between 24 bytes and 128 MiB");p->size=uint64_t(size);
    uint8_t header[4];p->read(0,header,4);auto dot=path.find_last_of('.');std::string ext=dot==std::string::npos?"":path.substr(dot);for(char& c:ext)if(c>='A'&&c<='Z')c+=32;
    p->vag=!std::memcmp(header,"VAGp",4)||ext==".vag";
    if(p->vag)p->parseVag();else p->parseXa();start(0);
}
PsxAudio::~PsxAudio()=default;
int PsxAudio::count()const{return int(p->tracks.size());}
std::vector<std::string> PsxAudio::info(int index)const{
    if(index<0||index>=count())throw std::runtime_error("Invalid PlayStation audio stream");
    const auto& t=p->tracks[index];std::string name=p->vag?"":"XA · file "+std::to_string(t.file)+" · channel "+std::to_string(t.channel)+" · stream "+std::to_string(index+1);
    return {name,"","","Sony PlayStation",""};
}
int PsxAudio::length(int index)const{if(index<0||index>=count())throw std::runtime_error("Invalid PlayStation audio stream");const auto& t=p->tracks[index];return int((t.frames*1000+t.rate-1)/t.rate);}
void PsxAudio::start(int index){if(index<0||index>=count())throw std::runtime_error("Invalid PlayStation audio stream");p->selected=index;p->position=0;p->cached=-1;}
void PsxAudio::loop(bool enabled){p->looping=enabled;}
void PsxAudio::seek(int ms){if(ms<0||ms>86400000)throw std::runtime_error("Invalid seek position");p->position=uint64_t(ms)*44100/1000;auto& t=p->tracks[p->selected];if(p->position*t.rate/44100<t.frames)p->frame(p->position*t.rate/44100);}
int PsxAudio::tell()const{return int(p->position*1000/44100);}
bool PsxAudio::render(int16_t* out,int samples){
    auto& t=p->tracks[p->selected];uint64_t total=(t.frames*44100+t.rate-1)/t.rate;
    bool ended=false;
    for(int i=0;i<samples/2;i++){
        if(p->position>=total){if(p->looping){p->position=t.repeat>=0?uint64_t(t.repeat)*44100/t.rate:0;}else{std::fill(out+i*2,out+samples,0);ended=true;break;}}
        uint64_t scaled=p->position*t.rate,frame=scaled/44100;int frac=scaled%44100;
        auto a=p->frame(frame),b=p->frame(frame+1);
        for(int ch=0;ch<2;ch++)out[i*2+ch]=int16_t((int64_t(a[ch])*(44100-frac)+int64_t(b[ch])*frac)/44100);
        p->position++;
    }
    return ended||(!p->looping&&p->position>=total);
}
