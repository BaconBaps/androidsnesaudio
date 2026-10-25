#include "xsf_timing.h"
#include <iostream>
#include <stdexcept>
int main(){
    auto check=[](const char* text,int expected){int got=XsfTiming::milliseconds(text,12345);if(got!=expected)throw std::runtime_error(std::string(text)+": expected "+std::to_string(expected)+", got "+std::to_string(got));};
    check("0:6,052078",6052);check("2:2,44148399999999",122441);check("3:4,96118099999998",184961);
    check("2:03.500",123500);check("2:03,500",123500);check(" 02 : 03,5 \r",123500);
    check("1:02:03.456",3723456);check("123.5",123500);check(".5",500);check("1.",1000);
    check("0",0);check("0:00",0);check("0:01.2345",1235);check("0:59.9999",60000);check("24:00:00",86400000);
    for(auto text:{""," ","unknown","-1","NaN","inf","1e4","0x10","2:",":02","1:2:3:4","1.5:02","2:03 (loop)","99999999999999999999","24:00:01",",","1,,2"})check(text,12345);
    if(XsfTiming::milliseconds("1:01",5000,60000)!=5000||XsfTiming::milliseconds("0",5000,60000)!=0)throw std::runtime_error("Fade bounds/default");
    std::cout<<"PASS optional xSF timing: seconds, MM:SS, HH:MM:SS, decimal dot/comma, whitespace, rounding, bounds and invalid-tag fallback\n";
}
