#include <jni.h>
#include <cstdint>
#include "gme/gme.h"

static void fail(JNIEnv* e, const char* s) { e->ThrowNew(e->FindClass("java/io/IOException"),s); }
extern "C" JNIEXPORT jlong JNICALL Java_dev_pixelspc_Engine_open(JNIEnv* e,jclass,jstring path) {
    const char* p=e->GetStringUTFChars(path,nullptr); Music_Emu* emu=nullptr;
    auto err=gme_open_file(p,&emu,44100); e->ReleaseStringUTFChars(path,p);
    if(err) { fail(e,err); return 0; } return reinterpret_cast<intptr_t>(emu);
}
extern "C" JNIEXPORT void JNICALL Java_dev_pixelspc_Engine_close(JNIEnv*,jclass,jlong h) { if(h) gme_delete(reinterpret_cast<Music_Emu*>(h)); }
extern "C" JNIEXPORT jint JNICALL Java_dev_pixelspc_Engine_count(JNIEnv*,jclass,jlong h) { return gme_track_count(reinterpret_cast<Music_Emu*>(h)); }
extern "C" JNIEXPORT jobjectArray JNICALL Java_dev_pixelspc_Engine_info(JNIEnv* e,jclass,jlong h,jint index) {
    gme_info_t* i=nullptr; auto err=gme_track_info(reinterpret_cast<Music_Emu*>(h),&i,index);
    if(err) { fail(e,err); return nullptr; }
    auto a=e->NewObjectArray(5,e->FindClass("java/lang/String"),nullptr);
    const char* values[]={i->song,i->game,i->author,i->system,i->copyright};
    for(int n=0;n<5;n++) { auto s=e->NewStringUTF(values[n]); e->SetObjectArrayElement(a,n,s); e->DeleteLocalRef(s); }
    gme_free_info(i); return a;
}
extern "C" JNIEXPORT jint JNICALL Java_dev_pixelspc_Engine_length(JNIEnv*,jclass,jlong h,jint index) {
    gme_info_t* i=nullptr; if(gme_track_info(reinterpret_cast<Music_Emu*>(h),&i,index)) return 180000;
    int ms=i->length>0?i->length:(i->intro_length+i->loop_length*2);
    gme_free_info(i); return ms>0?ms:180000;
}
extern "C" JNIEXPORT void JNICALL Java_dev_pixelspc_Engine_start(JNIEnv* e,jclass,jlong h,jint index,jint length) {
    auto emu=reinterpret_cast<Music_Emu*>(h); auto err=gme_start_track(emu,index);
    if(err) fail(e,err); else gme_set_fade(emu,length);
}
extern "C" JNIEXPORT jboolean JNICALL Java_dev_pixelspc_Engine_render(JNIEnv* e,jclass,jlong h,jshortArray pcm) {
    auto emu=reinterpret_cast<Music_Emu*>(h); auto p=e->GetShortArrayElements(pcm,nullptr);
    auto err=gme_play(emu,e->GetArrayLength(pcm),p); e->ReleaseShortArrayElements(pcm,p,0);
    if(err) fail(e,err); return gme_track_ended(emu);
}
extern "C" JNIEXPORT void JNICALL Java_dev_pixelspc_Engine_seek(JNIEnv* e,jclass,jlong h,jint ms) { auto err=gme_seek(reinterpret_cast<Music_Emu*>(h),ms); if(err) fail(e,err); }
extern "C" JNIEXPORT jint JNICALL Java_dev_pixelspc_Engine_tell(JNIEnv*,jclass,jlong h) { return gme_tell(reinterpret_cast<Music_Emu*>(h)); }
