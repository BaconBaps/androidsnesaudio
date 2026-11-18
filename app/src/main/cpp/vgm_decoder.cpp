#include "vgm_decoder.h"
#include <algorithm>
#include <cstring>
#include <limits>
#include <stdexcept>
#include <zlib.h>
#include "player/vgmplayer.hpp"
#include "utils/MemoryLoader.h"
#include "emu/SoundEmu.h"

namespace {
uint32_t le32(const uint8_t* b){return uint32_t(b[0])|(uint32_t(b[1])<<8)|(uint32_t(b[2])<<16)|(uint32_t(b[3])<<24);}
const uint16_t clocks[]={0x0c,0x10,0x2c,0x30,0x38,0x40,0x44,0x48,0x4c,0x50,0x54,0x58,0x5c,0x60,0x64,0x68,0x6c,0x70,0x74,0x80,0x84,0x88,0x8c,0x90,0x98,0x9c,0xa0,0xa4,0xa8,0xac,0xb0,0xb4,0xb8,0xc0,0xc4,0xc8,0xcc,0xd0,0xd8,0xdc,0xe0,0xe4,0xe8,0xec,0xf0,0xf4,0xf8,0xfc};
// Validate lengths before entering the upstream command/data-block handlers.
class CheckedVgmPlayer:public VGMPlayer {
public:
    static void validate(const std::vector<uint8_t>& bytes){
        uint32_t version=le32(&bytes[8]);size_t at=version>=0x150&&le32(&bytes[0x34])?size_t(le32(&bytes[0x34]))+0x34:0x40;
        if(at>=bytes.size())throw std::runtime_error("Invalid VGM command offset");
        size_t loop=le32(&bytes[0x1c])?size_t(le32(&bytes[0x1c]))+0x1c:0;bool loopValid=!loop;
        uint64_t totalRoms=0;uint32_t romSizes[128]={};
        while(at<bytes.size()){
            if(at==loop)loopValid=true;
            uint8_t cmd=bytes[at];if(cmd==0x66){if(!loopValid)throw std::runtime_error("Invalid VGM loop offset");return;}
            if(_CMD_INFO[cmd].func==&CheckedVgmPlayer::Cmd_unknown||_CMD_INFO[cmd].func==&CheckedVgmPlayer::Cmd_invalid)throw std::runtime_error("Unsupported VGM command "+std::to_string(cmd));
            uint64_t length=_CMD_INFO[cmd].cmdLen;
            if(cmd==0x67){
                if(bytes.size()-at<7||bytes[at+1]!=0x66)throw std::runtime_error("Truncated VGM data block");
                uint32_t payload=le32(&bytes[at+3])&0x7fffffff;length=uint64_t(payload)+7;
                if(length>bytes.size()-at)throw std::runtime_error("Truncated VGM sample data");
                uint8_t type=bytes[at+2];
                if((type&0xc0)==0x80){
                    if(payload<8)throw std::runtime_error("Invalid VGM ROM block");
                    uint32_t size=le32(&bytes[at+7]),offset=le32(&bytes[at+11]);
                    if(size>64u*1024*1024||offset>size||payload-8>size-offset)throw std::runtime_error("VGM sample ROM exceeds bounds");
                    unsigned bank=(type&0x3f)*2+(le32(&bytes[at+3])>>31);if(size>romSizes[bank]){totalRoms+=size-romSizes[bank];romSizes[bank]=size;}if(totalRoms>128u*1024*1024)throw std::runtime_error("VGM sample ROMs exceed 128 MB");
                }
                if(type>=0x40&&type<0x7f&&(payload<5||le32(&bytes[at+8])>64u*1024*1024))throw std::runtime_error("Expanded VGM PCM exceeds bounds");
            }
            if(!length||length>bytes.size()-at)throw std::runtime_error("Truncated VGM command stream");at+=length;
        }
        throw std::runtime_error("VGM end command is missing");
    }
    void checkDevices(){for(unsigned i=0;i<_CHIP_COUNT;i++)if(GetHeaderChipClock(i)&&!SndEmu_GetDevDecl(_DEV_LIST[i],nullptr,0))throw std::runtime_error("Unsupported VGM sound chip "+std::to_string(i));}
};
std::string tag(VGMPlayer& player,const char* key){const char* const* tags=player.GetTags();if(tags)for(size_t n=0;tags[n]&&tags[n+1];n+=2)if(!std::strcmp(tags[n],key))return tags[n+1];return "";}
}
struct VgmDecoder::State {
    std::vector<uint8_t> bytes;DATA_LOADER* loader=nullptr;std::unique_ptr<CheckedVgmPlayer> player;
    std::vector<WAVE_32BS> buffer;std::vector<std::string> tags;int duration=180000;int64_t position=0,fadeAt=180000LL*44100/1000;
    bool looping=false,hasLoop=false;int gain=0x10000;
    ~State(){if(player){player->Stop();player.reset();}if(loader)DataLoader_Deinit(loader);}
};
std::unique_ptr<VgmDecoder> VgmDecoder::openIfNeeded(const std::string& path){
    gzFile file=gzopen(path.c_str(),"rb");if(!file)throw std::runtime_error("Cannot open VGM file");
    struct Close{gzFile f;~Close(){gzclose(f);}} close{file};
    std::vector<uint8_t> bytes(256);int count=gzread(file,bytes.data(),bytes.size());
    if(count<64||std::memcmp(bytes.data(),"Vgm ",4))throw std::runtime_error("Invalid VGM header");bytes.resize(count);
    uint32_t version=le32(&bytes[8]),data=version>=0x150&&le32(&bytes[0x34])?0x34u+le32(&bytes[0x34]):0x40;
    if(data<0x40)throw std::runtime_error("Invalid VGM data offset");
    if(data>0x64&&bytes.size()>=0x64&&(le32(&bytes[0x60])&0x3fffffff))throw std::runtime_error("YMF278B (OPL4) VGM is not supported by this build");
    bool wider=false;for(size_t i=3;i<sizeof(clocks)/sizeof(*clocks);i++)if(clocks[i]+4<=std::min<size_t>(bytes.size(),data)&&(le32(&bytes[clocks[i]])&0x3fffffff))wider=true;
    if(!wider)return nullptr; // Keep existing console playback and its channel mixer.
    uint8_t block[16384];while((count=gzread(file,block,sizeof(block)))>0){if(bytes.size()+count>64u*1024*1024)throw std::runtime_error("Expanded VGM exceeds 64 MB");bytes.insert(bytes.end(),block,block+count);}
    if(count<0)throw std::runtime_error("Cannot decompress VGM file");
    CheckedVgmPlayer::validate(bytes);
    return std::unique_ptr<VgmDecoder>(new VgmDecoder(std::move(bytes)));
}
VgmDecoder::VgmDecoder(std::vector<uint8_t> bytes):p(new State){
    p->bytes=std::move(bytes);p->loader=MemoryLoader_Init(p->bytes.data(),p->bytes.size());
    if(!p->loader||DataLoader_Load(p->loader))throw std::runtime_error("Cannot load VGM data");
    DataLoader_ReadAll(p->loader);if(DataLoader_GetSize(p->loader)!=p->bytes.size())throw std::runtime_error("Incomplete VGM data");
    p->player.reset(new CheckedVgmPlayer);p->player->SetSampleRate(44100);
    if(p->player->LoadFile(p->loader))throw std::runtime_error("Unsupported or damaged VGM file");
    p->tags={tag(*p->player,"TITLE"),tag(*p->player,"GAME"),tag(*p->player,"ARTIST"),tag(*p->player,"SYSTEM"),tag(*p->player,"DATE")};
    PLR_SONG_INFO info{};if(p->player->GetSongInfo(info))throw std::runtime_error("Cannot read VGM information");
    p->gain=info.volGain;p->hasLoop=p->player->GetLoopTicks()>0;
    uint64_t ticks=uint64_t(p->player->GetTotalTicks())+p->player->GetLoopTicks();
    if(ticks)p->duration=std::min<uint64_t>(86400000,ticks*1000/44100);
    p->player->checkDevices();start();
    std::vector<PLR_DEV_INFO> devices;if(p->player->GetSongDeviceInfo(devices)==0xff)throw std::runtime_error("Cannot inspect VGM sound chips");
    if(devices.empty())throw std::runtime_error("VGM file declares no supported sound chips");
    for(const auto& device:devices)if(!device.devDecl||!device.core||!device.smplRate)throw std::runtime_error("Unsupported VGM sound chip (device "+std::to_string(device.type)+")");
}
VgmDecoder::~VgmDecoder()=default;
std::vector<std::string> VgmDecoder::info()const{return p->tags;}
int VgmDecoder::length()const{return p->duration;}
void VgmDecoder::start(){p->player.reset(new CheckedVgmPlayer);p->player->SetSampleRate(44100);if(p->player->LoadFile(p->loader))throw std::runtime_error("Cannot reload VGM data");if(p->player->Start())throw std::runtime_error("Cannot start VGM sound chips");p->position=0;p->fadeAt=int64_t(p->duration)*44100/1000;}
void VgmDecoder::loop(bool enabled,int length){p->looping=enabled;p->fadeAt=std::max(p->position,int64_t(length)*44100/1000);}
int VgmDecoder::tell()const{return std::min<int64_t>(86400000,p->position*1000/44100);}
bool VgmDecoder::render(int16_t* output,int samples){
    int frames=samples/2;p->buffer.assign(frames,{});p->player->Render(frames,p->buffer.data());
    for(int frame=0;frame<frames;frame++,p->position++){
        int64_t fade=p->looping?352800:std::max<int64_t>(0,std::min<int64_t>(352800,p->fadeAt+352800-p->position));
        for(int side=0;side<2;side++){
            int64_t value=(int64_t(side?p->buffer[frame].R:p->buffer[frame].L)*p->gain)>>24;
            value=value*fade/352800;output[frame*2+side]=std::max<int64_t>(-32768,std::min<int64_t>(32767,value));
        }
    }
    return (!p->hasLoop&&(p->player->GetState()&PLAYSTATE_END))||(!p->looping&&p->position>=p->fadeAt+352800);
}
void VgmDecoder::seek(int ms){
    int64_t target=int64_t(ms)*44100/1000;if(target<p->position)start();
    // Emulate and discard, preserving oscillator/sample state. The app bounds seeks to 250 ms steps.
    std::vector<int16_t> discard(4096);while(p->position<target){int frames=std::min<int64_t>(2048,target-p->position);render(discard.data(),frames*2);}
}
