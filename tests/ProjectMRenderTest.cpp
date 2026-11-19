// Headless GLES smoke test: the same projectM API and settings as the Android bridge.
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <GLES3/gl3.h>
#include <projectM-4/projectM.h>
#include <cassert>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <fstream>
#include <string>
#include <thread>
#include <vector>
static std::string error;
static void failed(const char*,const char* message,void*){error=message?message:"Unknown preset error";}
int main(int argc,char** argv){
    assert(argc>=3);
    auto platform=(PFNEGLGETPLATFORMDISPLAYEXTPROC)eglGetProcAddress("eglGetPlatformDisplayEXT");assert(platform);
    EGLDisplay display=platform(EGL_PLATFORM_SURFACELESS_MESA,EGL_DEFAULT_DISPLAY,nullptr);EGLint major,minor;assert(eglInitialize(display,&major,&minor));assert(eglBindAPI(EGL_OPENGL_ES_API));
    EGLint attrs[]={EGL_SURFACE_TYPE,EGL_PBUFFER_BIT,EGL_RENDERABLE_TYPE,EGL_OPENGL_ES3_BIT,EGL_RED_SIZE,8,EGL_GREEN_SIZE,8,EGL_BLUE_SIZE,8,EGL_NONE};
    EGLConfig config;EGLint count;assert(eglChooseConfig(display,attrs,&config,1,&count)&&count);
    EGLint ctx[]={EGL_CONTEXT_CLIENT_VERSION,3,EGL_NONE},size[]={EGL_WIDTH,512,EGL_HEIGHT,192,EGL_NONE};
    EGLContext context=eglCreateContext(display,config,EGL_NO_CONTEXT,ctx);assert(context!=EGL_NO_CONTEXT);EGLSurface surface=eglCreatePbufferSurface(display,config,size);assert(eglMakeCurrent(display,surface,surface,context));
    printf("Renderer: %s · %s\n",glGetString(GL_RENDERER),glGetString(GL_VERSION));
    projectm_handle engine=projectm_create();assert(engine);projectm_set_window_size(engine,512,192);projectm_set_mesh_size(engine,32,24);projectm_set_fps(engine,30);projectm_set_preset_locked(engine,true);projectm_set_soft_cut_duration(engine,0.1);projectm_set_preset_switch_failed_event_callback(engine,failed,nullptr);
    const char* textures[]={argv[1]};projectm_set_texture_search_paths(engine,textures,1);
    std::vector<int16_t> pcm(2940);std::vector<unsigned char> pixels(512*192*4);int ok=0,unsupported=0;
    for(int i=2;i<argc;i++){
        error.clear();projectm_load_preset_file(engine,argv[i],false);
        if(!error.empty()){fprintf(stderr,"Unsupported preset %s: %s\n",argv[i],error.c_str());unsupported++;continue;}
        for(int frame=0;frame<12;frame++){
            for(int n=0;n<1470;n++){double t=(frame*1470+n)/44100.0;pcm[2*n]=(int16_t)(20000*sin(t*440*6.283185));pcm[2*n+1]=(int16_t)(16000*sin(t*330*6.283185));}
            projectm_pcm_add_int16(engine,pcm.data(),1470,PROJECTM_STEREO);projectm_opengl_render_frame(engine);glFinish();
            std::this_thread::sleep_for(std::chrono::milliseconds(34));
        }
        glReadPixels(0,0,512,192,GL_RGBA,GL_UNSIGNED_BYTE,pixels.data());int lit=0;for(size_t p=0;p<pixels.size();p+=4)if(pixels[p]+pixels[p+1]+pixels[p+2]>20)lit++;
        printf("%s: %d lit pixels\n",argv[i],lit);if(i<5)assert(lit>10);ok++;
        if(i==2&&std::getenv("CHIPAMP_VIZ_CAPTURE")){std::ofstream image(std::getenv("CHIPAMP_VIZ_CAPTURE"),std::ios::binary);image<<"P6\n512 192\n255\n";for(int y=191;y>=0;y--)for(int x=0;x<512;x++)image.write((char*)&pixels[(y*512+x)*4],3);}
    }
    error.clear();projectm_load_preset_file(engine,"/missing.milk",false);assert(!error.empty());projectm_opengl_render_frame(engine);projectm_destroy(engine);
    engine=projectm_create();assert(engine);projectm_set_window_size(engine,512,192);projectm_opengl_render_frame(engine);projectm_destroy(engine);
    assert(ok>=3);printf("Passed: %d rendered; %d unsupported; missing-preset recovery and recreate passed\n",ok,unsupported);
    eglMakeCurrent(display,EGL_NO_SURFACE,EGL_NO_SURFACE,EGL_NO_CONTEXT);eglDestroySurface(display,surface);eglDestroyContext(display,context);eglTerminate(display);
}
