#include <jni.h>
#include <memory>
#include <algorithm>
#include <cstring>
#include "unarr.h"
static void error(JNIEnv*e,const char*text){e->ThrowNew(e->FindClass("java/io/IOException"),text);}
struct Rar {ar_stream* stream=nullptr;ar_archive* archive=nullptr;size_t remaining=0,size=0;~Rar(){ar_close_archive(archive);ar_close(stream);}};
extern "C" JNIEXPORT jlong JNICALL Java_dev_pixelspc_RarReader_open(JNIEnv*e,jclass,jstring file){
    std::unique_ptr<Rar> r(new Rar);const char*path=e->GetStringUTFChars(file,nullptr);if(!path)return 0;r->stream=ar_open_file(path);e->ReleaseStringUTFChars(file,path);
    if(!r->stream||(r->archive=ar_open_rar_archive(r->stream))==nullptr){error(e,"Cannot open RAR/RSN. Encrypted and RAR5 archives are not supported.");return 0;}return reinterpret_cast<jlong>(r.release());
}
extern "C" JNIEXPORT jbyteArray JNICALL Java_dev_pixelspc_RarReader_nextBytes(JNIEnv*e,jclass,jlong h){auto*r=reinterpret_cast<Rar*>(h);
    if(!ar_parse_entry(r->archive)){if(!ar_at_eof(r->archive))error(e,"Corrupt or unsupported RAR entry");return nullptr;}
    r->remaining=r->size=ar_entry_get_size(r->archive);const char*name=ar_entry_get_name(r->archive);if(!name){error(e,"Invalid RAR entry name");return nullptr;}size_t size=strlen(name);jbyteArray result=e->NewByteArray(size);if(result)e->SetByteArrayRegion(result,0,size,reinterpret_cast<const jbyte*>(name));return result;
}
extern "C" JNIEXPORT jlong JNICALL Java_dev_pixelspc_RarReader_size(JNIEnv*,jclass,jlong h){return reinterpret_cast<Rar*>(h)->size;}
extern "C" JNIEXPORT jint JNICALL Java_dev_pixelspc_RarReader_read(JNIEnv*e,jclass,jlong h,jbyteArray bytes,jint offset,jint length){auto*r=reinterpret_cast<Rar*>(h);if(length==0)return 0;if(!r->remaining)return -1;
    int count=std::min<size_t>(length,r->remaining);jbyte*buffer=e->GetByteArrayElements(bytes,nullptr);if(!buffer)return -1;bool ok=ar_entry_uncompress(r->archive,buffer+offset,count);e->ReleaseByteArrayElements(bytes,buffer,0);
    if(!ok){error(e,"RAR decompression or checksum failed");return -1;}r->remaining-=count;return count;
}
extern "C" JNIEXPORT void JNICALL Java_dev_pixelspc_RarReader_close(JNIEnv*,jclass,jlong h){delete reinterpret_cast<Rar*>(h);}
