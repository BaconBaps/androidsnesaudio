// Portable, bounded UTF-16LE -> UTF-8 adapter for libvgm GD3 tags (Android API 26).
// No system iconv dependency; other formats/codepages are intentionally not used.
#include <cstdint>
#include <cstdlib>
#include <cstring>
#include <string>
#include "utils/StrUtils.h"
struct _codepage_conversion {};
extern "C" UINT8 CPConv_Init(CPCONV** out,const char* from,const char* to){
    *out=nullptr;if(std::strcmp(from,"UTF-16LE")||std::strcmp(to,"UTF-8"))return 1;
    *out=new _codepage_conversion;return 0;
}
extern "C" void CPConv_Deinit(CPCONV* value){delete value;}
extern "C" UINT8 CPConv_StrConvert(CPCONV* converter,size_t* size,char** output,size_t length,const char* input){
    if(!converter||!size||!output||!input||!length)return 1;
    std::string result;result.reserve(length);
    for(size_t i=0;i+1<length;i+=2){
        uint32_t c=(uint8_t)input[i]|((uint8_t)input[i+1]<<8);
        if(c>=0xd800&&c<=0xdbff){
            uint32_t low=i+3<length?((uint8_t)input[i+2]|((uint8_t)input[i+3]<<8)):0;
            if(low>=0xdc00&&low<=0xdfff){c=0x10000+((c-0xd800)<<10)+(low-0xdc00);i+=2;}else c=0xfffd;
        }else if(c>=0xdc00&&c<=0xdfff)c=0xfffd;
        if(c<0x80)result+=(char)c;
        else if(c<0x800){result+=(char)(0xc0|(c>>6));result+=(char)(0x80|(c&63));}
        else if(c<0x10000){result+=(char)(0xe0|(c>>12));result+=(char)(0x80|((c>>6)&63));result+=(char)(0x80|(c&63));}
        else{result+=(char)(0xf0|(c>>18));result+=(char)(0x80|((c>>12)&63));result+=(char)(0x80|((c>>6)&63));result+=(char)(0x80|(c&63));}
    }
    if(!*output){*output=(char*)std::malloc(result.size()+1);if(!*output)return 1;}
    else if(*size<result.size()){*size=result.size();return 1;}
    std::memcpy(*output,result.data(),result.size());*size=result.size();return 0;
}
