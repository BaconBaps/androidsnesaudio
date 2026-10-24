#include <jni.h>
#include <cstdint>
#include <fstream>
#include <memory>
#include <stdexcept>
#include <vector>
#include "gme/gme.h"
#include "psf_decoder.h"
#include "usf_decoder.h"
#include "midi_decoder.h"

static jstring utf8(JNIEnv*,const std::string&);
static void fail(JNIEnv* e,const char* text){
    if(e->ExceptionCheck())return;
    jclass cls=e->FindClass("java/io/IOException");jmethodID ctor=e->GetMethodID(cls,"<init>","(Ljava/lang/String;)V");
    jstring message=utf8(e,text);if(e->ExceptionCheck())return;
    jobject error=e->NewObject(cls,ctor,message);if(error)e->Throw(static_cast<jthrowable>(error));
    e->DeleteLocalRef(message);e->DeleteLocalRef(cls);e->DeleteLocalRef(error);
}
struct Decoder {
    Music_Emu* gme=nullptr;std::unique_ptr<PsfDecoder> psf;std::unique_ptr<UsfDecoder> usf;std::unique_ptr<MidiDecoder> midi;
    ~Decoder(){if(gme)gme_delete(gme);}
};
static Decoder& get(jlong h){if(!h)throw std::runtime_error("Decoder is closed");return *reinterpret_cast<Decoder*>(h);}
static void checked(gme_err_t error){if(error)throw std::runtime_error(error);}
static jstring utf8(JNIEnv* e,const std::string& text){
    jbyteArray bytes=e->NewByteArray(text.size());if(!bytes)return nullptr;
    e->SetByteArrayRegion(bytes,0,text.size(),reinterpret_cast<const jbyte*>(text.data()));
    jclass cls=e->FindClass("java/lang/String");jmethodID ctor=e->GetMethodID(cls,"<init>","([BLjava/lang/String;)V");jstring encoding=e->NewStringUTF("UTF-8");
    jstring value=static_cast<jstring>(e->NewObject(cls,ctor,bytes,encoding));e->DeleteLocalRef(bytes);e->DeleteLocalRef(encoding);e->DeleteLocalRef(cls);return value;
}
extern "C" JNIEXPORT jlong JNICALL Java_dev_pixelspc_Engine_open(JNIEnv* e,jclass,jstring path){
    jclass cls=e->FindClass("java/lang/String");jmethodID encode=e->GetMethodID(cls,"getBytes","(Ljava/lang/String;)[B");
    jstring charset=e->NewStringUTF("UTF-8");jbyteArray bytes=static_cast<jbyteArray>(e->CallObjectMethod(path,encode,charset));
    e->DeleteLocalRef(charset);e->DeleteLocalRef(cls);if(e->ExceptionCheck()||!bytes)return 0;
    std::string file(e->GetArrayLength(bytes),'\0');if(!file.empty())e->GetByteArrayRegion(bytes,0,file.size(),reinterpret_cast<jbyte*>(&file[0]));e->DeleteLocalRef(bytes);
    try{
        std::unique_ptr<Decoder> d(new Decoder);std::ifstream input(file,std::ios::binary);char magic[4]={};input.read(magic,4);
        auto dot=file.find_last_of('.');std::string ext=dot==std::string::npos?"":file.substr(dot);for(char& c:ext)if(c>='A'&&c<='Z')c+=32;
        if((std::string(magic,3)=="PSF"&&uint8_t(magic[3])==0x21)||ext==".usf"||ext==".miniusf")d->usf.reset(new UsfDecoder(file));else if(ext==".mid"||ext==".midi")d->midi.reset(new MidiDecoder(file));else if(std::string(magic,3)=="PSF"||ext==".psf"||ext==".minipsf")d->psf.reset(new PsfDecoder(file));else checked(gme_open_file(file.c_str(),&d->gme,44100));
        return reinterpret_cast<jlong>(d.release());
    }catch(const std::exception& ex){fail(e,ex.what());return 0;}
}
extern "C" JNIEXPORT void JNICALL Java_dev_pixelspc_Engine_close(JNIEnv*,jclass,jlong h){delete reinterpret_cast<Decoder*>(h);}
extern "C" JNIEXPORT jint JNICALL Java_dev_pixelspc_Engine_count(JNIEnv* e,jclass,jlong h){try{auto&d=get(h);return (d.usf||d.psf||d.midi)?1:gme_track_count(d.gme);}catch(const std::exception&ex){fail(e,ex.what());return 0;}}
extern "C" JNIEXPORT jobjectArray JNICALL Java_dev_pixelspc_Engine_info(JNIEnv* e,jclass,jlong h,jint index){
    try{
        auto&d=get(h);std::vector<std::string> values;
        if(d.usf){if(index)throw std::runtime_error("Invalid USF track index");values=d.usf->info();}
        else if(d.midi){if(index)throw std::runtime_error("Invalid MIDI track index");values=d.midi->info();}
        else if(d.psf){if(index)throw std::runtime_error("Invalid PSF track index");values=d.psf->info();}
        else{gme_info_t* i=nullptr;checked(gme_track_info(d.gme,&i,index));values={i->song,i->game,i->author,i->system,i->copyright};gme_free_info(i);}
        auto out=e->NewObjectArray(5,e->FindClass("java/lang/String"),nullptr);if(!out)return nullptr;
        for(int n=0;n<5;n++){auto value=utf8(e,values[n]);if(e->ExceptionCheck())return nullptr;e->SetObjectArrayElement(out,n,value);e->DeleteLocalRef(value);}return out;
    }catch(const std::exception&ex){fail(e,ex.what());return nullptr;}
}
extern "C" JNIEXPORT jint JNICALL Java_dev_pixelspc_Engine_length(JNIEnv*e,jclass,jlong h,jint index){
    try{auto&d=get(h);if(d.usf)return d.usf->length();if(d.midi)return d.midi->length();if(d.psf)return d.psf->length();gme_info_t*i=nullptr;checked(gme_track_info(d.gme,&i,index));int ms=i->length>0?i->length:i->intro_length+i->loop_length*2;gme_free_info(i);return ms>0?ms:180000;}catch(const std::exception&ex){fail(e,ex.what());return 180000;}
}
extern "C" JNIEXPORT void JNICALL Java_dev_pixelspc_Engine_start(JNIEnv*e,jclass,jlong h,jint index,jint length){
    try{auto&d=get(h);if(d.usf){if(index)throw std::runtime_error("Invalid USF track index");d.usf->start();}else if(d.midi){if(index)throw std::runtime_error("Invalid MIDI track index");d.midi->start();}else if(d.psf){if(index)throw std::runtime_error("Invalid PSF track index");d.psf->start();}else{checked(gme_start_track(d.gme,index));gme_set_fade(d.gme,length);}}catch(const std::exception&ex){fail(e,ex.what());}
}
extern "C" JNIEXPORT void JNICALL Java_dev_pixelspc_Engine_setLoop(JNIEnv*e,jclass,jlong h,jboolean enabled,jint length){
    try{auto&d=get(h);if(d.usf)d.usf->loop(enabled);else if(d.midi)d.midi->loop(enabled);else if(d.psf)d.psf->loop(enabled);else{gme_ignore_silence(d.gme,enabled?1:0);int at=gme_tell(d.gme);gme_set_fade(d.gme,enabled?-1:std::max(at,length));}}catch(const std::exception&ex){fail(e,ex.what());}
}
extern "C" JNIEXPORT jboolean JNICALL Java_dev_pixelspc_Engine_render(JNIEnv*e,jclass,jlong h,jshortArray pcm){
    try{auto&d=get(h);int count=e->GetArrayLength(pcm);if(count<=0||count>1048576||count%2)throw std::runtime_error("Invalid stereo output buffer");
        jshort* out=e->GetShortArrayElements(pcm,nullptr);if(!out)return false;bool ended=false;
        try{if(d.usf)ended=d.usf->render(out,count);else if(d.midi)ended=d.midi->render(out,count);else if(d.psf)ended=d.psf->render(out,count);else{checked(gme_play(d.gme,count,out));ended=gme_track_ended(d.gme);}}catch(...){e->ReleaseShortArrayElements(pcm,out,0);throw;}
        e->ReleaseShortArrayElements(pcm,out,0);return ended;
    }catch(const std::exception&ex){fail(e,ex.what());return false;}
}
extern "C" JNIEXPORT void JNICALL Java_dev_pixelspc_Engine_seek(JNIEnv*e,jclass,jlong h,jint ms){try{auto&d=get(h);if(ms<0||ms>86400000)throw std::runtime_error("Invalid seek position");if(d.usf)d.usf->seek(ms);else if(d.midi)d.midi->seek(ms);else if(d.psf)d.psf->seek(ms);else checked(gme_seek(d.gme,ms));}catch(const std::exception&ex){fail(e,ex.what());}}
extern "C" JNIEXPORT jint JNICALL Java_dev_pixelspc_Engine_tell(JNIEnv*e,jclass,jlong h){try{auto&d=get(h);return d.usf?d.usf->tell():d.midi?d.midi->tell():d.psf?d.psf->tell():gme_tell(d.gme);}catch(const std::exception&ex){fail(e,ex.what());return 0;}}

extern "C" JNIEXPORT void JNICALL Java_dev_pixelspc_Engine_soundFont(JNIEnv*e,jclass,jlong h,jstring path){
    const char* file=e->GetStringUTFChars(path,nullptr);if(!file)return;
    try{auto& d=get(h);if(d.midi)d.midi->font(file);}catch(const std::exception&ex){fail(e,ex.what());}
    e->ReleaseStringUTFChars(path,file);
}
extern "C" JNIEXPORT void JNICALL Java_dev_pixelspc_Engine_validateSoundFont(JNIEnv*e,jclass,jstring path){
    const char* file=e->GetStringUTFChars(path,nullptr);if(!file)return;
    try{MidiDecoder::validateFont(file);}catch(const std::exception&ex){fail(e,ex.what());}
    e->ReleaseStringUTFChars(path,file);
}
