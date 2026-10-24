#pragma once
#include <cstdint>
#include <memory>
#include <string>
#include <vector>
class PsfDecoder {
public:
    explicit PsfDecoder(const std::string& path);
    ~PsfDecoder();
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
