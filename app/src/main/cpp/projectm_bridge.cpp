#include <jni.h>
#include <projectM-4/projectM.h>
#include <stdexcept>
#include <string>
#include <vector>

// Every call is confined to the view's EGL thread. Never called by audio output.
struct Visualizer {
    projectm_handle engine=nullptr;
    std::string error;
    ~Visualizer(){if(engine)projectm_destroy(engine);}
};
static void fail(JNIEnv* env,const std::string& message){env->ThrowNew(env->FindClass("java/io/IOException"),message.c_str());}
static Visualizer* instance(jlong handle){if(!handle)throw std::runtime_error("Visualizer is closed");return reinterpret_cast<Visualizer*>(handle);}
static std::string string(JNIEnv* env,jstring text){if(!text)return {};const char* p=env->GetStringUTFChars(text,nullptr);if(!p)throw std::runtime_error("Cannot read preset path");std::string s(p);env->ReleaseStringUTFChars(text,p);return s;}
extern "C" JNIEXPORT jlong JNICALL Java_dev_pixelspc_ProjectMEngine_create(JNIEnv* env,jclass,jint width,jint height){
    Visualizer* v=nullptr;
    try{
        v=new Visualizer();v->engine=projectm_create();if(!v->engine)throw std::runtime_error("projectM needs OpenGL ES 3.0");
        projectm_set_window_size(v->engine,width,height);projectm_set_mesh_size(v->engine,32,24);
        projectm_set_fps(v->engine,30);projectm_set_preset_locked(v->engine,true);
        projectm_set_hard_cut_enabled(v->engine,false);projectm_set_soft_cut_duration(v->engine,2);
        projectm_set_preset_switch_failed_event_callback(v->engine,[](const char*,const char* message,void* data){static_cast<Visualizer*>(data)->error=message?message:"Preset could not be loaded";},v);
        return reinterpret_cast<jlong>(v);
    }catch(const std::exception& e){delete v;fail(env,e.what());return 0;}
}
extern "C" JNIEXPORT void JNICALL Java_dev_pixelspc_ProjectMEngine_load(JNIEnv* env,jclass,jlong handle,jstring preset,jobjectArray paths,jboolean smooth){
    try{
        auto* v=instance(handle);v->error.clear();std::string file=string(env,preset);
        std::vector<std::string> names;std::vector<const char*> pointers;
        jsize count=paths?env->GetArrayLength(paths):0;
        for(jsize i=0;i<count;i++){auto text=static_cast<jstring>(env->GetObjectArrayElement(paths,i));names.push_back(string(env,text));env->DeleteLocalRef(text);}
        for(auto& name:names)pointers.push_back(name.c_str());
        projectm_set_texture_search_paths(v->engine,pointers.data(),pointers.size());
        projectm_load_preset_file(v->engine,file.c_str(),smooth);
        if(!v->error.empty())throw std::runtime_error(v->error);
    }catch(const std::exception& e){fail(env,e.what());}
}
extern "C" JNIEXPORT void JNICALL Java_dev_pixelspc_ProjectMEngine_render(JNIEnv* env,jclass,jlong handle,jshortArray samples,jint count){
    try{
        auto* v=instance(handle);
        if(count<0||count%2||!samples||count>env->GetArrayLength(samples)||count>16384)throw std::runtime_error("Invalid visualizer PCM");
        if(count){jshort* pcm=env->GetShortArrayElements(samples,nullptr);if(!pcm)return;projectm_pcm_add_int16(v->engine,reinterpret_cast<int16_t*>(pcm),count/2,PROJECTM_STEREO);env->ReleaseShortArrayElements(samples,pcm,JNI_ABORT);}
        projectm_opengl_render_frame(v->engine);
    }catch(const std::exception& e){fail(env,e.what());}
}
extern "C" JNIEXPORT void JNICALL Java_dev_pixelspc_ProjectMEngine_destroy(JNIEnv*,jclass,jlong handle){delete reinterpret_cast<Visualizer*>(handle);}
