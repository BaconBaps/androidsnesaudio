#include "xsf_timing.h"
#include "psf_decoder.h"
#include "cpuintrf.h"
#include "psx.h"
#include "peops/spu.h"
#include <zlib.h>
#include <algorithm>
#include <condition_variable>
#include <cstring>
#include <deque>
#include <dirent.h>
#include <fstream>
#include <functional>
#include <future>
#include <map>
#include <mutex>
#include <set>
#include <stdexcept>
#include <thread>
#include <limits.h>
#include <strings.h>

thread_local int64_t chip_psf_budget=20000000;
thread_local int psf_refresh=60;
thread_local bool stop_flag=false;
// PSF2 is deliberately unsupported. The PS1 hardware file references these
// entry points, but the version-1 loader never starts the PS2 execution path.
uint32_t psf2_load_elf(uint8_t*,uint32_t){throw std::runtime_error("PSF2 is not supported");}
uint32_t psf2_load_file(const char*,uint8_t*,uint32_t){return 0xffffffff;}
uint32_t psf2_get_loadaddr(){return 0;}
void psf2_set_loadaddr(uint32_t){}
void SPU2readDMA4Mem(uint32_t,int){}
void SPU2writeDMA4Mem(uint32_t,int){}
void SPU2readDMA7Mem(uint32_t,int){}
void SPU2writeDMA7Mem(uint32_t,int){}
void SPU2interruptDMA4(){}
void SPU2interruptDMA7(){}
void SPU2write(unsigned long,uint16_t){}
uint16_t SPU2read(unsigned long){return 0;}

namespace {
uint32_t le32(const uint8_t* p){return p[0]|uint32_t(p[1])<<8|uint32_t(p[2])<<16|uint32_t(p[3])<<24;}
std::string lower(std::string s){for(char& c:s)if(c>='A'&&c<='Z')c+=32;return s;}
std::string parent(const std::string& s){auto pos=s.find_last_of('/');return pos==std::string::npos?".":s.substr(0,pos);}
std::string canonical(const std::string& s){char buffer[PATH_MAX];if(!realpath(s.c_str(),buffer))throw std::runtime_error("Missing PSF library: "+s.substr(s.find_last_of('/')+1)+". Import the complete ZIP set.");return buffer;}
std::string trim(std::string s){auto a=s.find_first_not_of(" \t\r\n"),b=s.find_last_not_of(" \t\r\n");return a==std::string::npos?"":s.substr(a,b-a+1);}
// Keep references inside this import. Case-insensitive lookup accommodates
// Windows-authored sets while archive extraction rejects case collisions.
std::string resolve(const std::string& base,std::string ref,const std::string& root){
    std::replace(ref.begin(),ref.end(),'\\','/');
    if(ref.empty()||ref[0]=='/'||ref.find(':')!=std::string::npos||ref.find('\0')!=std::string::npos)throw std::runtime_error("Invalid PSF library path");
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
        if(path!=root&&path.compare(0,root.size()+1,root+"/")!=0)throw std::runtime_error("PSF library escapes its import folder");
    }
    path=canonical(path);
    if(path.compare(0,root.size()+1,root+"/")!=0)throw std::runtime_error("PSF library escapes its import folder");
    return path;
}
std::string cp1252(const std::string& input){
    static const uint16_t extended[]={0x20ac,0x81,0x201a,0x192,0x201e,0x2026,0x2020,0x2021,0x2c6,0x2030,0x160,0x2039,0x152,0x8d,0x17d,0x8f,0x90,0x2018,0x2019,0x201c,0x201d,0x2022,0x2013,0x2014,0x2dc,0x2122,0x161,0x203a,0x153,0x9d,0x17e,0x178};
    std::string out;for(uint8_t c:input){uint32_t u=c<128||c>=160?c:extended[c-128];if(u<128)out+=char(u);else if(u<2048){out+=char(0xc0|(u>>6));out+=char(0x80|(u&63));}else{out+=char(0xe0|(u>>12));out+=char(0x80|((u>>6)&63));out+=char(0x80|(u&63));}}return out;
}
struct Segment{uint32_t address,pc,gp,sp;std::vector<uint8_t> data;};
struct Program{
    std::vector<Segment> segments;std::map<std::string,std::string> tags;std::set<std::string> stack;
    size_t expanded=0,files=0;std::string root;
    void load(const std::string& path,int depth){
        if(depth>16||++files>64||!stack.insert(path).second)throw std::runtime_error("PSF library cycle or dependency limit exceeded");
        std::ifstream input(path,std::ios::binary|std::ios::ate);auto size=input.tellg();
        if(size<16||size>32*1024*1024)throw std::runtime_error("Invalid or oversized PSF file");
        std::vector<uint8_t> file(static_cast<size_t>(size));input.seekg(0);input.read(reinterpret_cast<char*>(file.data()),file.size());
        if(!input||memcmp(file.data(),"PSF\1",4))throw std::runtime_error("Only original PlayStation PSF version 1 is supported");
        uint32_t reserved=le32(file.data()+4),compressed=le32(file.data()+8);
        uint64_t offset=16ull+reserved,end=offset+compressed;
        if(end>file.size()||compressed==0||crc32(0,file.data()+offset,compressed)!=le32(file.data()+12))throw std::runtime_error("Corrupt PSF data or checksum");
        std::map<std::string,std::string> own;
        if(file.size()>end+5&&!memcmp(file.data()+end,"[TAG]",5)){
            std::string text(reinterpret_cast<char*>(file.data()+end+5),file.size()-end-5);
            if(text.size()>1024*1024)throw std::runtime_error("PSF tags are too large");size_t at=0;
            while(at<text.size()){size_t n=text.find('\n',at);if(n==std::string::npos)n=text.size();auto line=text.substr(at,n-at);auto eq=line.find('=');if(eq!=std::string::npos){auto key=lower(trim(line.substr(0,eq)));if(own.count(key)&&key.compare(0,4,"_lib")==0)throw std::runtime_error("Duplicate PSF library tag");own[key]=trim(line.substr(eq+1));}at=n+1;}
        }
        if(own["utf8"]!="1")for(auto& tag:own)tag.second=cp1252(tag.second);
        if(own.count("_lib"))load(resolve(parent(path),own["_lib"],root),depth+1);
        std::vector<uint8_t> exe(2*1024*1024+2048);uLongf bytes=exe.size();
        if(uncompress(exe.data(),&bytes,file.data()+offset,compressed)!=Z_OK||bytes<2048||memcmp(exe.data(),"PS-X EXE",8))throw std::runtime_error("Invalid PlayStation executable in PSF");
        expanded+=bytes;if(expanded>32*1024*1024)throw std::runtime_error("PSF libraries expand beyond the size limit");
        uint32_t address=le32(exe.data()+0x18)&0x1fffffff,pc=le32(exe.data()+0x10),sp=le32(exe.data()+0x30);
        if(address>2*1024*1024||bytes-2048>2*1024*1024-address||(pc&&((pc&0x1fffffff)>=2*1024*1024)))throw std::runtime_error("PSF executable lies outside PlayStation RAM");
        segments.push_back({address,pc,le32(exe.data()+0x14),sp,std::vector<uint8_t>(exe.begin()+2048,exe.begin()+bytes)});
        // Root metadata overrides library metadata. Auxiliary libraries do not
        // replace the outer track's own tags.
        for(auto& tag:own)tags[tag.first]=tag.second;
        auto primary=tags;
        for(int n=2;n<=64;n++){auto key="_lib"+std::to_string(n);if(own.count(key))load(resolve(parent(path),own[key],root),depth+1);}
        tags=primary;stack.erase(path);
    }
};
}

struct PsfDecoder::Impl {
    Program program;int duration=180000,fade=5000;bool looping=false;int64_t position=0,fadeAt=0;
    std::thread worker;std::mutex mutex;std::condition_variable ready;std::function<void()> request;bool quitting=false,initialized=false;
    std::deque<int16_t> samples;
    static thread_local Impl* owner;
    explicit Impl(const std::string& path){
        std::string real=canonical(path),dir=parent(real);program.root=dir;
        // ArchiveImporter puts PSF files below <private set>/psf. A dependency
        // may use ../ within that PSF tree, never outside the import.
        for(std::string at=dir;!at.empty()&&at!="/";at=parent(at))if(at.substr(at.find_last_of('/')+1)=="psf"){program.root=at;}
        program.load(real,0);
        if(std::none_of(program.segments.begin(),program.segments.end(),[](const Segment& s){return s.pc!=0;}))throw std::runtime_error("PSF has no executable entry point or is missing its library");
        duration=XsfTiming::milliseconds(program.tags["length"],180000);if(duration<=0)duration=180000;
        fade=XsfTiming::milliseconds(program.tags["fade"],5000,60000);
        fadeAt=int64_t(duration)*44100/1000;
    }
    ~Impl(){finish();}
    void invoke(std::function<void()> fn){
        if(!worker.joinable())worker=std::thread([this]{std::unique_lock<std::mutex> lock(mutex);while(true){ready.wait(lock,[this]{return quitting||bool(request);});if(quitting)break;auto fn=std::move(request);request=nullptr;lock.unlock();fn();lock.lock();}lock.unlock();if(initialized)SPUclose();initialized=false;});
        auto task=std::make_shared<std::packaged_task<void()>>(std::move(fn));auto future=task->get_future();
        {std::lock_guard<std::mutex> lock(mutex);request=[task]{(*task)();};}ready.notify_one();future.get();
    }
    void finish(){if(worker.joinable()){{std::lock_guard<std::mutex> lock(mutex);quitting=true;}ready.notify_one();worker.join();quitting=false;}samples.clear();position=0;}
    void reset(){
        finish();invoke([this]{
            owner=this;chip_psf_budget=20000000;stop_flag=false;psf_refresh=program.tags["_refresh"]=="50"?50:60;
            memset(psx_ram,0,sizeof(psx_ram));memset(psx_scratch,0,sizeof(psx_scratch));
            uint32_t pc=0,gp=0,sp=0;
            for(auto& segment:program.segments){if(!pc&&segment.pc){pc=segment.pc;gp=segment.gp;sp=segment.sp;}if(!segment.data.empty())memcpy(reinterpret_cast<uint8_t*>(psx_ram)+segment.address,segment.data.data(),segment.data.size());}
            if(!pc)throw std::runtime_error("PSF has no executable entry point");
            if(program.tags["game"]=="Chocobo Dungeon 2"&&psx_ram[0xbc090/4]==0x0802f040){psx_ram[0xbc090/4]=0;psx_ram[0xbc094/4]=0x0802f040;psx_ram[0xbc098/4]=0;}
            mips_init();mips_reset(nullptr);union cpuinfo info;info.i=pc;mips_set_info(CPUINFO_INT_PC,&info);
            info.i=sp?sp:0x801fff00;mips_set_info(CPUINFO_INT_REGISTER+MIPS_R29,&info);mips_set_info(CPUINFO_INT_REGISTER+MIPS_R30,&info);
            info.i=gp;mips_set_info(CPUINFO_INT_REGISTER+MIPS_R28,&info);
            psx_hw_init();SPUinit();SPUopen();initialized=true;setendless(1);setlength(-1,0);mips_execute(5000);
        });
    }
    static void audio(const void* data,int bytes){if(!data||bytes<=0)return;auto p=static_cast<const int16_t*>(data);owner->samples.insert(owner->samples.end(),p,p+bytes/2);}
    void generate(int16_t* out,int frames,bool discard){
        if(!initialized)throw std::runtime_error("Start the PSF track before rendering");
        chip_psf_budget=std::min<int64_t>(128000000,std::max<int64_t>(8000000,int64_t(frames)*4096));
        for(int frame=0;frame<frames;frame++){
            if(samples.empty()){
                for(int n=0;n<735;n++){psx_hw_slice();SPUasync(384,audio);}psx_hw_frame();
                if(samples.empty())throw std::runtime_error("PSF emulator produced no audio frames");
            }
            double gain=1;
            if(!looping&&position>=fadeAt)gain=fade==0?0:std::max(0.,1.-double(position-fadeAt)/(int64_t(fade)*44100/1000));
            for(int channel=0;channel<2;channel++){int16_t v=samples.front();samples.pop_front();if(!discard)out[frame*2+channel]=int16_t(v*gain);}
            position++;
        }
    }
};
thread_local PsfDecoder::Impl* PsfDecoder::Impl::owner=nullptr;
PsfDecoder::PsfDecoder(const std::string& path):p(new Impl(path)){}
PsfDecoder::~PsfDecoder()=default;
std::vector<std::string> PsfDecoder::info()const{auto tag=[this](const char* key){auto it=p->program.tags.find(key);return it==p->program.tags.end()?std::string():it->second.substr(0,4096);};return {tag("title"),tag("game"),tag("artist"),"Sony PlayStation",tag("copyright")};}
int PsfDecoder::length()const{return p->duration+p->fade;}
void PsfDecoder::start(){p->reset();p->fadeAt=int64_t(p->duration)*44100/1000;}
void PsfDecoder::loop(bool enabled){p->looping=enabled;if(!enabled)p->fadeAt=p->position>int64_t(p->duration+p->fade)*44100/1000?p->position:int64_t(p->duration)*44100/1000;}
int PsfDecoder::tell()const{return int(p->position*1000/44100);}
void PsfDecoder::seek(int ms){int64_t target=int64_t(std::max(0,ms))*44100/1000;if(target<p->position)p->reset();p->invoke([this,target]{while(p->position<target)p->generate(nullptr,int(std::min<int64_t>(4096,target-p->position)),true);});}
bool PsfDecoder::render(int16_t* out,int count){if(count%2)throw std::runtime_error("Stereo output requires pairs of samples");p->invoke([this,out,count]{p->generate(out,count/2,false);});return !p->looping&&p->position>=p->fadeAt+int64_t(p->fade)*44100/1000;}
