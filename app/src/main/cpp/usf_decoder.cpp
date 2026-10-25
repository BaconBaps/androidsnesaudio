#include "xsf_timing.h"
#include "usf_decoder.h"
extern "C" {
#include "usf/usf.h"
}
#include <zlib.h>
#include <algorithm>
#include <cstring>
#include <dirent.h>
#include <fstream>
#include <map>
#include <set>
#include <stdexcept>
#include <limits.h>
#include <strings.h>
namespace {
uint32_t le32(const uint8_t* p){return p[0]|uint32_t(p[1])<<8|uint32_t(p[2])<<16|uint32_t(p[3])<<24;}
std::string lower(std::string s){for(char& c:s)if(c>='A'&&c<='Z')c+=32;return s;}
std::string parent(const std::string& s){auto pos=s.find_last_of('/');return pos==std::string::npos?".":s.substr(0,pos);}
std::string canonical(const std::string& s){char buffer[PATH_MAX];if(!realpath(s.c_str(),buffer))throw std::runtime_error("Missing USF library: "+s.substr(s.find_last_of('/')+1)+". Import the complete ZIP set.");return buffer;}
std::string trim(std::string s){auto a=s.find_first_not_of(" \t\r\n"),b=s.find_last_not_of(" \t\r\n");return a==std::string::npos?"":s.substr(a,b-a+1);}
// Keep references inside this import. Case-insensitive lookup accommodates
// Windows-authored sets while archive extraction rejects case collisions.
std::string resolve(const std::string& base,std::string ref,const std::string& root){
    std::replace(ref.begin(),ref.end(),'\\','/');
    if(ref.empty()||ref[0]=='/'||ref.find(':')!=std::string::npos||ref.find('\0')!=std::string::npos)throw std::runtime_error("Invalid USF library path");
    std::string path=base;size_t at=0;
    while(at<=ref.size()){
        size_t end=ref.find('/',at);if(end==std::string::npos)end=ref.size();std::string part=ref.substr(at,end-at);at=end+1;
        if(part.empty()||part==".")continue;
        if(part=="..")path=parent(path);
        else {
            DIR* dir=opendir(path.c_str());std::string found;
            if(dir){while(auto e=readdir(dir))if(!strcasecmp(e->d_name,part.c_str())){found=e->d_name;break;}closedir(dir);}
            path+="/"+(found.empty()?part:found);
        }
        if(path!=root&&path.compare(0,root.size()+1,root+"/")!=0)throw std::runtime_error("USF library escapes its import folder");
    }
    path=canonical(path);
    if(path.compare(0,root.size()+1,root+"/")!=0)throw std::runtime_error("USF library escapes its import folder");
    return path;
}
std::string cp1252(const std::string& input){
    static const uint16_t extended[]={0x20ac,0x81,0x201a,0x192,0x201e,0x2026,0x2020,0x2021,0x2c6,0x2030,0x160,0x2039,0x152,0x8d,0x17d,0x8f,0x90,0x2018,0x2019,0x201c,0x201d,0x2022,0x2013,0x2014,0x2dc,0x2122,0x161,0x203a,0x153,0x9d,0x17e,0x178};
    std::string out;for(uint8_t c:input){uint32_t u=c<128||c>=160?c:extended[c-128];if(u<128)out+=char(u);else if(u<2048){out+=char(0xc0|(u>>6));out+=char(0x80|(u&63));}else{out+=char(0xe0|(u>>12));out+=char(0x80|((u>>6)&63));out+=char(0x80|(u&63));}}return out;
}
struct Program {
    std::vector<uint8_t> rom,state=std::vector<uint8_t>(0x80275c);
    std::map<std::string,std::string> tags;std::set<std::string> stack;
    size_t files=0,bytes=0;std::string root;bool hasState=false;
    void section(const uint8_t* data,size_t size){
        size_t at=0;
        auto word=[&](){if(at+4>size)throw std::runtime_error("Truncated USF sparse section");uint32_t v=le32(data+at);at+=4;return v;};
        for(int kind=0;kind<2;kind++){
            uint32_t magic=word();if(magic==0)continue;
            if(magic!=0x34365253)throw std::runtime_error("Invalid USF sparse section marker");
            for(uint32_t count=word();count;count=word()){
                uint32_t offset=word();uint64_t end=uint64_t(offset)+count;
                size_t limit=kind?0x80275c:64*1024*1024;
                if(count>size-at||end>limit)throw std::runtime_error("USF sparse patch is outside its memory limits");
                auto& dest=kind?state:rom;if(dest.size()<end)dest.resize(end);
                memcpy(dest.data()+offset,data+at,count);at+=count;if(kind)hasState=true;
            }
        }
        if(at!=size)throw std::runtime_error("Unexpected data after USF sparse sections");
    }
    void load(const std::string& path,int depth){
        if(depth>16||++files>64||!stack.insert(path).second)throw std::runtime_error("USF library cycle or dependency limit exceeded");
        std::ifstream input(path,std::ios::binary|std::ios::ate);auto size=input.tellg();
        if(size<16||size>32*1024*1024)throw std::runtime_error("Invalid or oversized USF file");
        bytes+=size;if(bytes>128*1024*1024)throw std::runtime_error("USF dependency set exceeds 128 MiB");
        std::vector<uint8_t> file(static_cast<size_t>(size));input.seekg(0);input.read(reinterpret_cast<char*>(file.data()),file.size());
        if(!input||memcmp(file.data(),"PSF\x21",4))throw std::runtime_error("Expected a Nintendo 64 USF (PSF version 0x21)");
        uint32_t reserved=le32(file.data()+4),compressed=le32(file.data()+8);
        uint64_t offset=16ull+reserved,end=offset+compressed;
        if(end>file.size()||crc32(0,file.data()+offset,compressed)!=le32(file.data()+12))throw std::runtime_error("Corrupt USF data or checksum");
        // USF executable data belongs in the reserved sparse section, not the
        // compressed PSF program area. Accept a zlib-encoded empty area too.
        if(compressed){uint8_t empty;uLongf n=1;if(uncompress(&empty,&n,file.data()+offset,compressed)!=Z_OK||n)throw std::runtime_error("USF executable section must be empty");}
        std::map<std::string,std::string> own;
        if(file.size()>end){
            if(file.size()<end+5||memcmp(file.data()+end,"[TAG]",5))throw std::runtime_error("Invalid USF tag block");
            std::string text(reinterpret_cast<char*>(file.data()+end+5),file.size()-end-5);
            if(text.size()>1024*1024)throw std::runtime_error("USF tags are too large");
            size_t at=0;while(at<text.size()){size_t next=text.find('\n',at);if(next==std::string::npos)next=text.size();auto line=text.substr(at,next-at);auto eq=line.find('=');if(eq!=std::string::npos){auto key=lower(trim(line.substr(0,eq)));if(own.count(key)&&key.compare(0,4,"_lib")==0)throw std::runtime_error("Duplicate USF library tag");own[key]=trim(line.substr(eq+1));}at=next+1;}
        }
        if(own["utf8"]!="1")for(auto& tag:own)tag.second=cp1252(tag.second);
        if(own.count("_lib"))load(resolve(parent(path),own["_lib"],root),depth+1);
        if(reserved)section(file.data()+16,reserved);
        for(auto& tag:own)tags[tag.first]=tag.second;
        auto primary=tags;
        for(int n=2;n<=64;n++){auto key="_lib"+std::to_string(n);if(own.count(key))load(resolve(parent(path),own[key],root),depth+1);}
        tags=primary;stack.erase(path);
    }
    void validate(){
        if(!hasState||le32(state.data())!=0x23d8a6c8)throw std::runtime_error("USF has no valid N64 save state. Import the complete set with its .usflib files.");
        auto ram=le32(state.data()+4);if(ram!=0x400000&&ram!=0x800000)throw std::runtime_error("USF save state has an invalid RAM size");
        state.resize(ram+0x275c);
    }
};
}
struct UsfDecoder::Impl {
    Program program;std::vector<uint8_t> emulator;int duration=180000,fade=5000;int64_t position=0,fadeAt=0;bool looping=false;
    explicit Impl(const std::string& path){
        std::string real=canonical(path),dir=parent(real);program.root=dir;
        for(std::string at=dir;!at.empty()&&at!="/";at=parent(at))if(at.substr(at.find_last_of('/')+1)=="psf")program.root=at;
        program.load(real,0);program.validate();
        duration=XsfTiming::milliseconds(program.tags["length"],180000);if(duration<=0)duration=180000;
        fade=XsfTiming::milliseconds(program.tags["fade"],5000,60000);
        fadeAt=int64_t(duration)*44100/1000;
    }
    ~Impl(){if(!emulator.empty())usf_shutdown(emulator.data());}
    void reset(){
        if(emulator.empty()){
            emulator.resize(usf_get_state_size());usf_clear(emulator.data());
            if(!usf_allocations_ok(emulator.data(),0))throw std::runtime_error("Not enough memory for the N64 decoder");
            if(!program.rom.empty())usf_upload_rom(emulator.data(),program.rom.data(),program.rom.size());
            usf_upload_save_state(emulator.data(),program.state.data(),program.state.size());
            if(!usf_allocations_ok(emulator.data(),!program.rom.empty()))throw std::runtime_error("Not enough memory for the N64 soundtrack");
            usf_set_compare(emulator.data(),program.tags.count("_enablecompare")?1:0);
            usf_set_fifo_full(emulator.data(),program.tags.count("_enablefifofull")?1:0);
            // Low-level RSP preserves compatibility across game audio drivers.
            usf_set_hle_audio(emulator.data(),0);
        }else usf_restart(emulator.data());
        position=0;
    }
    void generate(int16_t* out,int frames,bool discard){
        if(emulator.empty())throw std::runtime_error("Start the USF track before rendering");
        int16_t scratch[4096];
        while(frames>0){
            int count=std::min(frames,2048);int16_t* buffer=discard?scratch:out;
            usf_set_cpu_budget(emulator.data(),64000000);
            if(auto err=usf_render_resampled(emulator.data(),buffer,count,44100))throw std::runtime_error(err);
            if(!discard&&!looping)for(int i=0;i<count;i++)if(position+i>=fadeAt){double gain=fade==0?0:std::max(0.,1.-double(position+i-fadeAt)/(int64_t(fade)*44100/1000));buffer[i*2]=int16_t(buffer[i*2]*gain);buffer[i*2+1]=int16_t(buffer[i*2+1]*gain);}
            position+=count;frames-=count;if(!discard)out+=count*2;
        }
    }
};
UsfDecoder::UsfDecoder(const std::string& path):p(new Impl(path)){}
UsfDecoder::~UsfDecoder()=default;
std::vector<std::string> UsfDecoder::info()const{auto tag=[this](const char* key){auto it=p->program.tags.find(key);return it==p->program.tags.end()?std::string():it->second.substr(0,4096);};return {tag("title"),tag("game"),tag("artist"),"Nintendo 64",tag("copyright")};}
int UsfDecoder::length()const{return p->duration+p->fade;}
void UsfDecoder::start(){p->reset();p->fadeAt=int64_t(p->duration)*44100/1000;}
void UsfDecoder::loop(bool enabled){p->looping=enabled;if(!enabled)p->fadeAt=p->position>int64_t(p->duration+p->fade)*44100/1000?p->position:int64_t(p->duration)*44100/1000;}
int UsfDecoder::tell()const{return int(p->position*1000/44100);}
void UsfDecoder::seek(int ms){int64_t target=int64_t(std::max(0,ms))*44100/1000;if(target<p->position)p->reset();while(p->position<target)p->generate(nullptr,int(std::min<int64_t>(4096,target-p->position)),true);}
bool UsfDecoder::render(int16_t* out,int count){if(count%2)throw std::runtime_error("Stereo output requires pairs of samples");p->generate(out,count/2,false);return !p->looping&&p->position>=p->fadeAt+int64_t(p->fade)*44100/1000;}
