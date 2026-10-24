#include "midi_decoder.h"
#define TSF_IMPLEMENTATION
#include "tsf.h"
#include <algorithm>
#include <array>
#include <cstring>
#include <fstream>
#include <map>
#include <stdexcept>

namespace {
const int RATE=44100;
void require(bool ok,const char* why){if(!ok)throw std::runtime_error(why);}
uint16_t le16(const uint8_t* p){return p[0]|uint16_t(p[1])<<8;}
uint32_t le32(const uint8_t* p){return le16(p)|uint32_t(le16(p+2))<<16;}
uint32_t be32(const uint8_t* p){return uint32_t(p[0])<<24|uint32_t(p[1])<<16|uint32_t(p[2])<<8|p[3];}
std::vector<uint8_t> readFile(const std::string& path,size_t limit){
    std::ifstream in(path,std::ios::binary|std::ios::ate);auto n=in.tellg();require(n>0&&uint64_t(n)<=limit,"Missing, empty or oversized audio file");
    std::vector<uint8_t> bytes(static_cast<size_t>(n));in.seekg(0);in.read(reinterpret_cast<char*>(bytes.data()),bytes.size());require(bool(in),"Could not read audio file");return bytes;
}
struct Slice {const uint8_t* p;size_t n;};
// Validate the complete RIFF tree and every cross-table index before handing
// user-supplied SF2 data to the synthesis library. Limit expansion as well as bytes.
size_t validate(const std::vector<uint8_t>& b){
    require(b.size()>=12&&!memcmp(b.data(),"RIFF",4)&&!memcmp(b.data()+8,"sfbk",4),"Choose an uncompressed SoundFont 2 (.sf2) bank");
    require(uint64_t(le32(b.data()+4))+8==b.size(),"Truncated SoundFont RIFF data");
    std::map<std::string,Slice> tables;size_t sampleCount=0;
    for(size_t at=12;at<b.size();){
        require(b.size()-at>=8,"Invalid SoundFont chunk");size_t n=le32(b.data()+at+4),end=at+8+n;require(end<=b.size(),"Truncated SoundFont chunk");
        if(!memcmp(b.data()+at,"LIST",4)){
            require(n>=4,"Invalid SoundFont list");std::string list(reinterpret_cast<const char*>(b.data()+at+8),4);
            for(size_t sub=at+12;sub<end;){
                require(end-sub>=8,"Invalid SoundFont table");size_t len=le32(b.data()+sub+4),next=sub+8+len;require(next<=end,"Truncated SoundFont table");
                std::string key(reinterpret_cast<const char*>(b.data()+sub),4);
                if(list=="pdta"){require(!tables.count(key),"Duplicate SoundFont table");tables[key]={b.data()+sub+8,len};}
                if(list=="sdta"&&key=="smpl"){require(!sampleCount&&len>=4&&len%2==0,"Invalid SoundFont samples");sampleCount=len/2;}
                sub=next+(len&1);require(sub<=end,"Invalid SoundFont padding");
            }
        }
        at=end+(n&1);require(at<=b.size(),"Invalid SoundFont padding");
    }
    std::map<std::string,int> counts;
    for(auto spec:std::vector<std::pair<std::string,int>>{{"phdr",38},{"pbag",4},{"pmod",10},{"pgen",4},{"inst",22},{"ibag",4},{"imod",10},{"igen",4},{"shdr",46}}){
        auto it=tables.find(spec.first);require(it!=tables.end()&&it->second.n>=size_t(spec.second)&&it->second.n%spec.second==0,"Missing or invalid SoundFont table");
        counts[spec.first]=it->second.n/spec.second;require(counts[spec.first]<=65536,"SoundFont has too many zones");
    }
    require(sampleCount&&counts["phdr"]>=2&&counts["phdr"]<=2049&&counts["inst"]>=2&&counts["shdr"]>=2,"SoundFont contains no playable presets");
    auto index=[&](const char* key,int stride,int offset,const char* target){
        int last=0;auto s=tables[key];for(int i=0;i<counts[key];i++){int v=le16(s.p+i*stride+offset);require(v>=last&&v<counts[target],"SoundFont table index lies outside its data");last=v;}
    };
    index("phdr",38,24,"pbag");index("inst",22,20,"ibag");index("pbag",4,0,"pgen");index("pbag",4,2,"pmod");index("ibag",4,0,"igen");index("ibag",4,2,"imod");
    uint64_t regionWork=0;
    for(int i=0;i<counts["pgen"]-1;i++){auto p=tables["pgen"].p+i*4;if(le16(p)==41){int k=le16(p+2);require(k<counts["inst"]-1,"Invalid SoundFont instrument reference");auto q=tables["inst"].p+k*22;int first=le16(q+20),last=le16(q+42);for(int bag=first;bag<last;bag++){auto zone=tables["ibag"].p+bag*4;regionWork+=le16(zone+4)-le16(zone);require(regionWork<=200000,"SoundFont preset expansion is too large");}}}
    require(regionWork<=200000,"SoundFont preset expansion is too large");
    for(int i=0;i<counts["igen"]-1;i++){auto p=tables["igen"].p+i*4;if(le16(p)==53)require(le16(p+2)<counts["shdr"]-1,"Invalid SoundFont sample reference");}
    for(int i=0;i<counts["shdr"]-1;i++){auto p=tables["shdr"].p+i*46;uint32_t start=le32(p+20),end=le32(p+24),a=le32(p+28),z=le32(p+32);require(start<end&&end<=sampleCount&&a<=sampleCount&&z<=sampleCount&&a<=z&&le32(p+36)>0&&!(le16(p+44)&0x8000),"Invalid or ROM-based SoundFont sample");}
    return sampleCount;
}
struct Reader {
    const uint8_t* p;const uint8_t* end;
    uint8_t byte(){require(p<end,"Truncated MIDI event");return *p++;}
    uint32_t var(){uint32_t v=0;for(int i=0;i<4;i++){auto c=byte();v=(v<<7)|(c&127);if(!(c&128))return v;}throw std::runtime_error("Invalid MIDI variable-length number");}
    void skip(size_t n){require(n<=size_t(end-p),"Truncated MIDI data");p+=n;}
};
struct Event {uint64_t tick,frame;uint32_t tempo;uint8_t status,a,b;};
// MIDI text is often unspecified legacy encoding; preserve ASCII/UTF-8 and
// replace invalid bytes instead of creating invalid JNI strings.
std::string text(const uint8_t* p,size_t n){
    n=std::min<size_t>(n,512);bool valid=true;
    for(size_t i=0;i<n&&valid;){uint8_t c=p[i++];if(c<128)continue;int more=c>=0xf0&&c<=0xf4?3:c>=0xe0&&c<0xf0?2:c>=0xc2&&c<0xe0?1:-1;if(more<0||i+more>n){valid=false;break;}uint32_t u=c&((1<<(6-more))-1);int total=more;while(more--){uint8_t v=p[i++];if((v&0xc0)!=0x80)valid=false;u=(u<<6)|(v&63);}if(u>0x10ffff||(u>=0xd800&&u<=0xdfff)||(total==2&&u<0x800)||(total==3&&u<0x10000))valid=false;}
    if(valid)return std::string(reinterpret_cast<const char*>(p),n);
    std::string out;for(size_t i=0;i<n;i++){uint8_t c=p[i];if(c>=32&&c<127)out+=char(c);else if(c>=128){out+=char(0xc0|(c>>6));out+=char(0x80|(c&63));}}return out;
}
}
struct MidiDecoder::Impl {
    std::vector<Event> events;std::string title,copyright;std::unique_ptr<tsf,decltype(&tsf_close)> synth{nullptr,tsf_close};
    uint64_t musicalEnd=0,position=0,cursor=0;size_t next=0;bool looping=false,released=false;std::string bank;
    explicit Impl(const std::string& path){
        auto file=readFile(path,16*1024*1024);auto p=file.data();size_t size=file.size();
        require(size>=14&&!memcmp(p,"MThd",4),"Not a Standard MIDI file");uint32_t header=be32(p+4);require(header>=6&&uint64_t(header)+8<=size,"Invalid MIDI header");
        int type=(p[8]<<8)|p[9],tracks=(p[10]<<8)|p[11],division=(p[12]<<8)|p[13];require(type<=1&&tracks>0&&tracks<=256&&(type!=0||tracks==1),"Only MIDI type 0 and type 1 are supported");
        require(division>0&&!(division&0x8000),"SMPTE-timed MIDI is not supported");size_t at=8+header;uint64_t endTick=0;
        for(int track=0;track<tracks;track++){
            require(size-at>=8&&!memcmp(p+at,"MTrk",4),"Missing MIDI track");uint32_t n=be32(p+at+4);at+=8;require(n<=size-at,"Truncated MIDI track");Reader r{p+at,p+at+n};at+=n;uint64_t tick=0;uint8_t running=0;bool ended=false;
            while(r.p<r.end){
                tick+=r.var();require(tick<=uint64_t(division)*60*60*24*20,"MIDI timeline is too long");uint8_t status=r.byte();
                if(status<128){require(running>=0x80&&running<0xf0,"Invalid MIDI running status");r.p--;status=running;}
                else if(status<0xf0)running=status;
                else running=0;
                require(events.size()<500000,"MIDI contains too many events");
                if(status==0xff){uint8_t meta=r.byte();uint32_t len=r.var();require(len<=size_t(r.end-r.p),"Truncated MIDI metadata");
                    if(meta==0x51){require(len==3,"Invalid MIDI tempo");uint32_t tempo=(r.p[0]<<16)|(r.p[1]<<8)|r.p[2];require(tempo>0,"Invalid MIDI tempo");events.push_back({tick,0,tempo,0,0,0});}
                    if(meta==3&&title.empty())title=text(r.p,len);if(meta==2&&copyright.empty())copyright=text(r.p,len);
                    r.skip(len);if(meta==0x2f){require(len==0,"Invalid MIDI end marker");ended=true;break;}
                }else if(status==0xf0||status==0xf7){r.skip(r.var());}
                else {require(status<0xf0,"Unsupported MIDI system event");uint8_t a=r.byte(),b=0;if((status&0xf0)!=0xc0&&(status&0xf0)!=0xd0)b=r.byte();require(a<128&&b<128,"Invalid MIDI channel data");events.push_back({tick,0,0,status,a,b});}
            }
            require(ended,"MIDI track has no end marker");endTick=std::max(endTick,tick);
        }
        require(!events.empty(),"MIDI contains no events");std::stable_sort(events.begin(),events.end(),[](const Event&a,const Event&b){return a.tick<b.tick;});
        uint64_t last=0;long double micros=0;uint32_t tempo=500000;
        for(auto& e:events){micros+=(e.tick-last)*static_cast<long double>(tempo)/division;last=e.tick;e.frame=uint64_t(micros*RATE/1000000);if(e.tempo)tempo=e.tempo;}
        micros+=(endTick-last)*static_cast<long double>(tempo)/division;require(micros>0&&micros<=86400000000.L,"MIDI duration must be between zero and 24 hours");musicalEnd=std::max<uint64_t>(1,uint64_t(micros*RATE/1000000));
    }
    void reset(){require(bool(synth),"Choose a SoundFont before playing MIDI");tsf_reset(synth.get());for(int i=0;i<synth->voiceNum;i++)synth->voices[i].playingPreset=-1;
        for(int ch=0;ch<16;ch++)tsf_channel_set_presetnumber(synth.get(),ch,0,ch==9);next=0;cursor=0;released=false;
    }
    void event(const Event&e){int ch=e.status&15;switch(e.status&0xf0){
        case 0x80:tsf_channel_note_off(synth.get(),ch,e.a);break;
        case 0x90:tsf_channel_note_on(synth.get(),ch,e.a,e.b/127.f);break;
        case 0xb0:tsf_channel_midi_control(synth.get(),ch,e.a,e.b);break;
        case 0xc0:if(!tsf_channel_set_presetnumber(synth.get(),ch,e.a,ch==9))tsf_channel_set_presetnumber(synth.get(),ch,0,ch==9);break;
        case 0xe0:tsf_channel_set_pitchwheel(synth.get(),ch,e.a|(e.b<<7));break;
    }}
    void generate(int16_t* out,int frames){
        require(bool(synth),"Choose a SoundFont before playing MIDI");
        while(frames){
            if(looping&&cursor>=musicalEnd)reset();
            while(next<events.size()&&events[next].frame<=cursor){event(events[next]);next++;}
            if(!looping&&cursor>=musicalEnd&&!released){for(int ch=0;ch<16;ch++){tsf_channel_set_sustain(synth.get(),ch,0);tsf_channel_note_off_all(synth.get(),ch);}released=true;}
            uint64_t boundary=next<events.size()?events[next].frame:UINT64_MAX;
            if(cursor<musicalEnd)boundary=std::min(boundary,musicalEnd);
            int n=int(std::min<uint64_t>(frames,std::min<uint64_t>(2048,boundary-cursor)));
            require(n>0,"MIDI scheduler did not advance");tsf_render_short(synth.get(),out,n,0);
            if(!looping&&cursor>=musicalEnd){for(int i=0;i<n;i++){double gain=std::max(0.,1.-double(cursor+i-musicalEnd)/(2*RATE));out[i*2]=int16_t(out[i*2]*gain);out[i*2+1]=int16_t(out[i*2+1]*gain);}}
            cursor+=n;position+=n;out+=n*2;frames-=n;
        }
    }
};
MidiDecoder::MidiDecoder(const std::string& path):p(new Impl(path)){}
MidiDecoder::~MidiDecoder()=default;
namespace {
std::unique_ptr<tsf,decltype(&tsf_close)> loadFont(const std::string& path){
    size_t count=validate(readFile(path,64*1024*1024));
    std::unique_ptr<tsf,decltype(&tsf_close)> f(tsf_load_filename(path.c_str()),tsf_close);
    require(bool(f),"Could not load this SoundFont");int regions=0;
    for(int i=0;i<f->presetNum;i++)for(int j=0;j<f->presets[i].regionNum;j++){
        auto& r=f->presets[i].regions[j];regions++;
        require(r.offset<r.end&&r.end<count,"SoundFont sample offsets are out of range");
        // Match TinySoundFont's doLoop condition. One-shot regions may carry
        // unused/reversed loop markers (including endLoop - 1 after loading).
        // They are never read by the renderer and must not reject the bank.
        if(r.loop_mode!=TSF_LOOPMODE_NONE && r.loop_start<r.loop_end)
            require(r.loop_end<count,"SoundFont loop offsets are out of range");
    }
    require(regions>0,"SoundFont contains no playable sample regions");return f;
}
}
void MidiDecoder::validateFont(const std::string& path){loadFont(path);}
void MidiDecoder::font(const std::string& path){
    auto f=loadFont(path);require(tsf_set_max_voices(f.get(),128)!=0,"Could not allocate SoundFont voices");
    tsf_set_output(f.get(),TSF_STEREO_INTERLEAVED,RATE,-10);p->synth=std::move(f);p->bank=path;
}
std::vector<std::string> MidiDecoder::info()const{return {p->title,"","","MIDI",p->copyright};}
int MidiDecoder::length()const{return int(p->musicalEnd*1000/RATE)+2000;}
void MidiDecoder::start(){p->reset();p->position=0;}
void MidiDecoder::loop(bool enabled){p->looping=enabled;}
int MidiDecoder::tell()const{return int(p->position*1000/RATE);}
void MidiDecoder::seek(int ms){uint64_t target=uint64_t(std::max(0,ms))*RATE/1000;if(target<p->position){p->reset();p->position=0;}std::array<int16_t,4096> scratch;while(p->position<target)p->generate(scratch.data(),int(std::min<uint64_t>(2048,target-p->position)));}
bool MidiDecoder::render(int16_t* out,int count){p->generate(out,count/2);return !p->looping&&p->cursor>=p->musicalEnd+2*RATE;}
