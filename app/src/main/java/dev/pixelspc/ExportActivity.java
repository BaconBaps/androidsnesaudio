package dev.pixelspc;

import android.app.*;
import android.content.*;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import android.view.*;
import android.widget.*;
import java.io.*;

/** Save-only export: document picker grants access to one user-chosen WAV destination. */
public class ExportActivity extends Activity {
    private EditText start,duration;
    private Switch fade;
    private TextView save,status,whole;
    private ProgressBar progress;
    private Job job;
    private WavExporter.Options pending;
    private boolean choosing;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        PlayerTheme theme=PlayerTheme.find(getSharedPreferences("appearance",MODE_PRIVATE).getString("theme","light"));
        getWindow().setStatusBarColor(theme.background); getWindow().setNavigationBarColor(theme.background);
        getWindow().getDecorView().setSystemUiVisibility(theme.dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|(Build.VERSION.SDK_INT>=27?View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR:0));
        PlayerSkin skin=new PlayerSkin(this);
        LinearLayout body=new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setPadding(dp(20),dp(20),dp(20),dp(20)); body.setBackgroundColor(theme.background);
        body.setOnApplyWindowInsetsListener((v,insets)->{ body.setPadding(dp(20),dp(20)+insets.getSystemWindowInsetTop(),dp(20),dp(20)+insets.getSystemWindowInsetBottom()); return insets.consumeSystemWindowInsets(); });
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.addView(body); setContentView(scroll);
        TextView heading=label(body,"EXPORT AUDIO",18,theme.accent); heading.setTypeface(Typeface.createFromAsset(getAssets(),"fonts/pixel.ttf"));
        label(body,getIntent().getStringExtra("title"),22,theme.text);
        label(body,"Save a WAV file for a ringtone, notification, alarm, or sharing. Phone settings stay unchanged.",14,theme.muted);
        label(body,"Start · minutes:seconds (up to 60:00)",14,theme.text);
        start=input(body,"Export start",state==null?"0:00":state.getString("start","0:00"),theme);
        label(body,"Duration · minutes:seconds (up to 10:00)",14,theme.text);
        duration=input(body,"Export duration",state==null?"0:30":state.getString("duration","0:30"),theme);
        whole=button(body,"USE FULL TRACK",theme,skin);
        whole.setOnClickListener(v->{ start.setText(R.string.export_start_zero); duration.setText(MainActivity.clock((int)Math.min(WavExporter.MAX_DURATION_MS,((long)getIntent().getIntExtra("length",180000)+999)/1000*1000))); });
        if(getIntent().getIntExtra("length",180000)>WavExporter.MAX_DURATION_MS) { whole.setEnabled(false); whole.setText(R.string.export_full_too_long); }
        fade=new Switch(this); fade.setText(R.string.export_fade); fade.setTextColor(theme.text); fade.setChecked(state==null || state.getBoolean("fade",true)); body.addView(fade,new LinearLayout.LayoutParams(-1,dp(52)));
        status=label(body,"Stereo · 44.1 kHz · 16-bit WAV\n30 seconds uses about 5 MB. Your music can keep playing while the file is prepared.",14,theme.muted);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(100); progress.setVisibility(View.GONE); body.addView(progress,new LinearLayout.LayoutParams(-1,dp(28)));
        save=button(body,"SAVE WAV…",theme,skin); save.setOnClickListener(v->chooseDestination());
        TextView cancel=button(body,"CANCEL",theme,skin); cancel.setOnClickListener(v->cancelAndFinish());
        if(state!=null && state.containsKey("pendingStart")) {
            pending=new WavExporter.Options(state.getInt("pendingStart"),state.getInt("pendingDuration"),state.getBoolean("pendingFade")); choosing=state.getBoolean("choosing");
        }
        job=(Job)getLastNonConfigurationInstance();
        if(job!=null) { job.owner=this; updateJob(); } else setInputsEnabled(!choosing);
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private TextView label(LinearLayout body,String value,int size,int color) {
        TextView view=new TextView(this); view.setText(value); view.setTextSize(size); view.setTextColor(color); view.setPadding(0,dp(10),0,dp(8)); body.addView(view,new LinearLayout.LayoutParams(-1,-2)); return view;
    }
    private EditText input(LinearLayout body,String description,String value,PlayerTheme theme) {
        EditText view=new EditText(this); view.setSingleLine(true); view.setText(value); view.setTextColor(theme.text); view.setContentDescription(description);
        view.setInputType(android.text.InputType.TYPE_CLASS_DATETIME|android.text.InputType.TYPE_DATETIME_VARIATION_TIME);
        body.addView(view,new LinearLayout.LayoutParams(-1,dp(52))); return view;
    }
    private TextView button(LinearLayout body,String text,PlayerTheme theme,PlayerSkin skin) {
        TextView view=label(body,text,14,theme.accent); view.setGravity(Gravity.CENTER); view.setBackground(skin.button(theme.background)); view.setClickable(true); view.setFocusable(true); view.setMinHeight(dp(48)); return view;
    }
    private void setInputsEnabled(boolean enabled) { start.setEnabled(enabled); duration.setEnabled(enabled); fade.setEnabled(enabled); save.setEnabled(enabled); whole.setEnabled(enabled && getIntent().getIntExtra("length",180000)<=WavExporter.MAX_DURATION_MS); }
    private void chooseDestination() {
        try { pending=new WavExporter.Options(WavExporter.parseTime(start.getText().toString()),WavExporter.parseTime(duration.getText().toString()),fade.isChecked()); }
        catch(IllegalArgumentException e) { status.setText(e.getMessage()); return; }
        choosing=true; setInputsEnabled(false);
        startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/wav").putExtra(Intent.EXTRA_TITLE,WavExporter.filename(getIntent().getStringExtra("title"))),1);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=1) return;
        choosing=false;
        if(result!=RESULT_OK || data==null || data.getData()==null || pending==null) { setInputsEnabled(true); return; }
        job=new Job(getApplicationContext(),getIntent(),data.getData(),pending); job.owner=this; setInputsEnabled(false); updateJob(); job.thread.start();
    }
    private void updateJob() {
        if(job==null) return;
        setInputsEnabled(job.done && !choosing); progress.setVisibility(job.done?View.GONE:View.VISIBLE); progress.setProgress(job.percent);
        status.setText(job.done?job.result:job.percent==0?"Preparing audio… Longer start offsets can take a moment.":"Rendering audio… "+job.percent+"%");
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out); out.putString("start",start.getText().toString()); out.putString("duration",duration.getText().toString()); out.putBoolean("fade",fade.isChecked()); out.putBoolean("choosing",choosing);
        if(pending!=null) { out.putInt("pendingStart",pending.startMs); out.putInt("pendingDuration",pending.durationMs); out.putBoolean("pendingFade",pending.fade); }
    }
    @Override public Object onRetainNonConfigurationInstance() { return job; }
    private void cancelAndFinish() { if(job!=null && !job.done) job.thread.interrupt(); finish(); }
    @Override public void onBackPressed() { cancelAndFinish(); }
    @Override protected void onDestroy() {
        if(job!=null) { job.owner=null; if(!isChangingConfigurations() && !job.done) job.thread.interrupt(); }
        super.onDestroy();
    }
    private static final class Job implements Runnable {
        final Context context; final Intent source; final Uri destination; final WavExporter.Options options;
        final Handler main=new Handler(Looper.getMainLooper()); final Thread thread=new Thread(this,"WAV export");
        volatile ExportActivity owner; volatile int percent; volatile boolean done; volatile String result;
        Job(Context context,Intent source,Uri destination,WavExporter.Options options) { this.context=context; this.source=source; this.destination=destination; this.options=options; }
        private void update() { main.post(()->{ ExportActivity activity=owner; if(activity!=null && !activity.isDestroyed()) activity.updateJob(); }); }
        public void run() {
            File temporary=null;
            try {
                if(new StatFs(context.getCacheDir().getPath()).getAvailableBytes()<WavExporter.dataSize(options)+8*1024*1024) throw new IOException("Not enough free space to prepare this WAV.");
                temporary=File.createTempFile("audio-export-",".wav",context.getCacheDir());
                try(OutputStream output=new BufferedOutputStream(new FileOutputStream(temporary))) {
                    WavExporter.write(WavExporter.nativeDecoder(source.getStringExtra("path"),source.getIntExtra("index",0),source.getIntExtra("length",180000)),output,options,p->{ percent=p; update(); });
                }
                WavExporter.checkCancelled();
                try(InputStream input=new FileInputStream(temporary); OutputStream output=context.getContentResolver().openOutputStream(destination,"wt")) {
                    if(output==null) throw new IOException("The selected location could not be opened.");
                    byte[] bytes=new byte[32768]; int count;
                    while((count=input.read(bytes))!=-1) { WavExporter.checkCancelled(); output.write(bytes,0,count); }
                }
                result="WAV saved to your chosen location. You can now share it or choose it in your phone’s sound settings.";
            } catch(Exception e) {
                boolean removed=false;
                try { removed=DocumentsContract.deleteDocument(context.getContentResolver(),destination); } catch(Exception ignored) { /* Provider may not support deletion. */ }
                result=(thread.isInterrupted() || e instanceof InterruptedIOException)?"Export cancelled.":"Could not export audio: "+e.getMessage();
                if(!removed) result+=" If an empty or partial WAV was created, remove it from the selected folder.";
            } finally { if(temporary!=null) temporary.delete(); done=true; update(); }
        }
    }
}
