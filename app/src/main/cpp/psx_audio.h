#pragma once
#include <cstdint>
#include <memory>
#include <string>
#include <vector>

/** Disk-backed PlayStation XA sector streams and standard mono Sony VAGp. */
class PsxAudio {
public:
    explicit PsxAudio(const std::string& path);
    ~PsxAudio();
    int count() const;
    std::vector<std::string> info(int index) const;
    int length(int index) const;
    void start(int index);
    void loop(bool enabled);
    void seek(int ms);
    int tell() const;
    bool render(int16_t* pcm,int samples);
private:
    struct Impl;
    std::unique_ptr<Impl> p;
};
