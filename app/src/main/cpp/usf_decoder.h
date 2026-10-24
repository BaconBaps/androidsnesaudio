#pragma once
#include <cstdint>
#include <memory>
#include <string>
#include <vector>
class UsfDecoder {
public:
    explicit UsfDecoder(const std::string& path);
    ~UsfDecoder();
    std::vector<std::string> info() const;
    int length() const;
    void start();
    void loop(bool enabled);
    void seek(int ms);
    int tell() const;
    bool render(int16_t* pcm,int samples);
private:
    struct Impl;
    std::unique_ptr<Impl> p;
};
