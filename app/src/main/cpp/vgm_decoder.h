#pragma once
#include <memory>
#include <string>
#include <vector>
#include <cstdint>
class VgmDecoder {
public:
    static std::unique_ptr<VgmDecoder> openIfNeeded(const std::string& path);
    ~VgmDecoder();
    std::vector<std::string> info()const;
    int length()const;
    void start();
    void loop(bool enabled,int length);
    bool render(int16_t* output,int samples);
    void seek(int ms);
    int tell()const;
private:
    struct State;std::unique_ptr<State> p;
    explicit VgmDecoder(std::vector<uint8_t> bytes);
};
