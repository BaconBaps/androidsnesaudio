#pragma once
#include <cstdint>
#include <string>

namespace XsfTiming {
// Timing is optional metadata. Locale-independent fixed-point parsing avoids
// rejecting usable sound programs for an unknown/malformed length or fade.
inline int milliseconds(std::string value,int fallback,int maximum=86400000){
    auto trim=[](std::string s){auto a=s.find_first_not_of(" \t\r\n"),b=s.find_last_not_of(" \t\r\n");return a==std::string::npos?std::string():s.substr(a,b-a+1);};
    value=trim(value);if(value.empty()||value.size()>64)return fallback;
    int64_t total=0;size_t at=0;int parts=0;
    while(at<value.size()){
        if(++parts>3)return fallback;
        size_t colon=value.find(':',at);bool last=colon==std::string::npos;
        std::string token=trim(value.substr(at,last?std::string::npos:colon-at));
        if(token.empty())return fallback;
        int64_t whole=0;size_t p=0;
        while(p<token.size()&&token[p]>='0'&&token[p]<='9'){whole=whole*10+token[p++]-'0';if(whole>86400)return fallback;}
        bool digits=p>0;int fraction=0;
        if(p<token.size()&&last&&(token[p]=='.'||token[p]==',')){
            p++;int count=0;bool round=false;
            while(p<token.size()&&token[p]>='0'&&token[p]<='9'){
                if(count<3)fraction=fraction*10+token[p]-'0';else if(count==3)round=token[p]>='5';
                count++;p++;
            }
            if(!digits&&count==0)return fallback;
            for(int n=count;n<3;n++)fraction*=10;
            if(round)fraction++;
        }else if(!digits)return fallback;
        if(p!=token.size())return fallback;
        total=total*60+whole*1000+fraction;
        if(total>maximum)return fallback;
        if(last)return int(total);
        at=colon+1;if(at==value.size())return fallback;
    }
    return fallback;
}
}
