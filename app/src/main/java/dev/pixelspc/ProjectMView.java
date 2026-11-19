package dev.pixelspc;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.*;
import android.os.*;
import android.view.TextureView;
import java.util.function.Consumer;

/** TextureView keeps the renderer inside the app's normal menus, clipping and theming. */
final class ProjectMView extends TextureView implements TextureView.SurfaceTextureListener {
    private Session session,retiring;
    private boolean active,playing,visible=true;
    private VisualizerPcm audio;
    private Selection selection;
    private Consumer<String> error=message->{};
    private Runnable next=()->{};
    private boolean automatic;
    static final class Selection {
        final String file;final String[] textures;
        Selection(ProjectMPresets.Preset p,String[] t){file=p.file.getAbsolutePath();textures=t;}
    }
    ProjectMView(Context c){super(c);setSurfaceTextureListener(this);setOpaque(true);}
    void callbacks(Consumer<String> error,Runnable next){this.error=error;this.next=next;}
    void selection(Selection value){selection=value;if(session!=null){session.selection=value;session.wake();}}
    void automatic(boolean value){automatic=value;if(session!=null)session.automatic=value;}
    void state(boolean active,boolean playing,VisualizerPcm source){
        if(audio!=source){if(audio!=null)audio.enabled(false);audio=source;}
        this.active=active;this.playing=playing;refresh();
    }
    private void refresh(){
        boolean show=active&&visible&&isAvailable()&&isAttachedToWindow();
        if(audio!=null)audio.enabled(show&&playing);
        if(!show){stop();return;}
        if(session==null){session=new Session(getSurfaceTexture(),getWidth(),getHeight());session.start();}
        session.audio=audio;session.automatic=automatic;
        if(session.playing!=playing){session.playing=playing;session.wake();}
    }
    private void stop(){if(session!=null){retiring=session;session.close(false);session=null;}if(audio!=null)audio.enabled(false);}
    @Override public void onVisibilityAggregated(boolean shown){super.onVisibilityAggregated(shown);visible=shown;refresh();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();refresh();}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    @Override public void onSurfaceTextureAvailable(SurfaceTexture surface,int width,int height){refresh();}
    @Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface,int width,int height){stop();refresh();}
    @Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface){if(session!=null){retiring=session;session.close(true);session=null;return false;}return true;}
    @Override public void onSurfaceTextureUpdated(SurfaceTexture surface){}
    private final class Session {
        final java.util.concurrent.CountDownLatch finished=new java.util.concurrent.CountDownLatch(1);
        Session previous=retiring;
        private boolean releaseTexture;
        final SurfaceTexture texture;final int width,height;final HandlerThread thread=new HandlerThread("projectM renderer",android.os.Process.THREAD_PRIORITY_BACKGROUND);
        Handler handler;volatile boolean closed,playing,automatic;volatile Selection selection;volatile VisualizerPcm audio;
        EGLDisplay display=EGL14.EGL_NO_DISPLAY;EGLContext context=EGL14.EGL_NO_CONTEXT;EGLSurface surface=EGL14.EGL_NO_SURFACE;
        long engine,last,elapsed;Selection loaded;boolean pendingNext;final short[] pcm=new short[16384];
        Session(SurfaceTexture t,int w,int h){texture=t;float scale=Math.min(1,768f/Math.max(1,w));width=Math.max(16,Math.round(w*scale));height=Math.max(16,Math.round(h*scale));selection=ProjectMView.this.selection;playing=ProjectMView.this.playing;automatic=ProjectMView.this.automatic;audio=ProjectMView.this.audio;}
        void start(){thread.start();handler=new Handler(thread.getLooper());handler.post(()->{try{init();draw();}catch(Exception|LinkageError e){failed(e);}});}
        void wake(){if(handler!=null&&!closed)handler.post(()->{handler.removeCallbacks(frame);frame.run();});}
        final Runnable frame=()->{if(!closed)try{draw();}catch(Exception|LinkageError e){failed(e);}};
        void init()throws Exception{
            if(previous!=null){previous.finished.await();previous=null;}
            if(closed)return;
            texture.setDefaultBufferSize(width,height);
            display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);int[] version=new int[2];
            if(display==EGL14.EGL_NO_DISPLAY||!EGL14.eglInitialize(display,version,0,version,1))throw new Exception("OpenGL ES is unavailable");
            int[] attrs={EGL14.EGL_RENDERABLE_TYPE,0x40,EGL14.EGL_SURFACE_TYPE,EGL14.EGL_WINDOW_BIT,EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_NONE};
            EGLConfig[] configs=new EGLConfig[1];int[] count=new int[1];if(!EGL14.eglChooseConfig(display,attrs,0,configs,0,1,count,0)||count[0]==0)throw new Exception("This device needs OpenGL ES 3.0 for projectM");
            context=EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,new int[]{EGL14.EGL_CONTEXT_CLIENT_VERSION,3,EGL14.EGL_NONE},0);
            surface=EGL14.eglCreateWindowSurface(display,configs[0],texture,new int[]{EGL14.EGL_NONE},0);
            if(context==EGL14.EGL_NO_CONTEXT||surface==EGL14.EGL_NO_SURFACE||!EGL14.eglMakeCurrent(display,surface,surface,context))throw new Exception("Cannot start the projectM display");
            EGL14.eglSwapInterval(display,1);engine=ProjectMEngine.create(width,height);
        }
        void draw()throws Exception{
            if(closed||engine==0)return;
            long now=SystemClock.uptimeMillis();if(playing&&last>0)elapsed+=Math.min(100,now-last);last=now;
            Selection requested=selection;
            if(requested!=null&&requested!=loaded){
                boolean smooth=loaded!=null;
                // Remember failures too: a broken desktop preset must not retry every frame.
                loaded=requested;elapsed=0;pendingNext=false;
                try{ProjectMEngine.load(engine,requested.file,requested.textures,smooth);}catch(java.io.IOException e){postError("Preset could not be rendered: "+e.getMessage());}
            }
            int n=playing&&audio!=null?audio.drain(pcm):0;
            // Zeroes clear the previous audio envelope while paused or buffering.
            if(n==0){java.util.Arrays.fill(pcm,0,1024,(short)0);n=1024;}
            ProjectMEngine.render(engine,pcm,n);
            if(!EGL14.eglSwapBuffers(display,surface))throw new Exception("The projectM display was interrupted");
            if(automatic&&playing&&elapsed>=45000&&!pendingNext){pendingNext=true;post(()->{if(session==this&&!closed)next.run();});}
            if(playing&&!closed)handler.postDelayed(frame,33);
        }
        void postError(String message){post(()->{if(session==this&&!closed)error.accept(message);});}
        void failed(Throwable e){close(false);post(()->{if(session==this){session=null;active=false;if(audio!=null)audio.enabled(false);error.accept("projectM unavailable: "+(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage()));}});}
        synchronized void close(boolean release){
            releaseTexture|=release;
            if(closed){if(release&&finished.getCount()==0)texture.release();return;}
            closed=true;if(handler!=null)handler.post(()->{handler.removeCallbacksAndMessages(null);try{if(engine!=0){ProjectMEngine.destroy(engine);engine=0;}}finally{
            if(display!=EGL14.EGL_NO_DISPLAY){EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT);if(surface!=EGL14.EGL_NO_SURFACE)EGL14.eglDestroySurface(display,surface);if(context!=EGL14.EGL_NO_CONTEXT)EGL14.eglDestroyContext(display,context);EGL14.eglTerminate(display);EGL14.eglReleaseThread();}
            synchronized(this){if(releaseTexture)texture.release();finished.countDown();}thread.quitSafely();
        }});}
    }
}
