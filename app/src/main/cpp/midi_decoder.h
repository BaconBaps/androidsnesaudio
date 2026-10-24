#pragma once
#include <memory>
#include <string>
#include <vector>
#include <cstdint>
class MidiDecoder {
    struct Impl;std::unique_ptr<Impl> p;
public:
    explicit MidiDecoder(const std::string& path);
    ~MidiDecoder();
    static void validateFont(const std::string& path);
    void font(const std::string& path);
    std::vector<std::string> info()const;
    int length()const;
    void start();void loop(bool enabled);void seek(int ms);int tell()const;
    bool render(int16_t* out,int count);
};
