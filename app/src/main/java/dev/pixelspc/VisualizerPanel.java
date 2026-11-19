package dev.pixelspc;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.*;
import android.widget.*;
import java.io.IOException;
import java.util.*;

/** Existing pixel displays plus an optional, lifecycle-bound projectM screen. */
final class VisualizerPanel extends FrameLayout {
    final PixelVisualizer pixels;
    private final ProjectMView projectm;
    private final TextView label;
    private final SharedPreferences prefs;
    private final ProjectMPresets store;
    private PlayerTheme theme=PlayerTheme.ALL[0];
    private boolean active,playing,frozen;
    private VisualizerPcm audio;
    private List<ProjectMPresets.Preset> presets=Collections.emptyList();
    private String presetId="";
    VisualizerPanel(Context c){
        super(c);prefs=c.getSharedPreferences("appearance",0);store=new ProjectMPresets(c);
        projectm=new ProjectMView(c);addView(projectm,new LayoutParams(-1,-1));projectm.setVisibility(GONE);
        pixels=new PixelVisualizer(c);addView(pixels,new LayoutParams(-1,-1));
        label=new TextView(c);label.setTypeface(Typeface.createFromAsset(c.getAssets(),"fonts/pixel.ttf"));label.setTextSize(6);label.setGravity(Gravity.CENTER_VERTICAL);label.setPadding(dp(10),0,dp(8),0);LayoutParams lp=new LayoutParams(-1,dp(23),Gravity.TOP);addView(label,lp);label.setVisibility(GONE);label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        pixels.modeChanged=this::refresh;
        projectm.setOnClickListener(v->pixels.selectMode(0));projectm.setOnLongClickListener(v->{pixels.selectMode(0);return true;});
        projectm.callbacks(message->{Toast.makeText(c,message,Toast.LENGTH_LONG).show();if(message.startsWith("projectM unavailable"))pixels.selectMode(0);},this::nextPreset);
        refreshPresets();refresh();
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    void setPlayerTheme(PlayerTheme t){theme=t;setBackground(new PlayerSkin(getContext(),t).display(t.spectrumBackground));pixels.setPlayerTheme(t);label.setTextColor(t.heading);label.setBackgroundColor((t.spectrumBackground&0xffffff)|0xdd000000);}
    void activityActive(boolean value){active=value;refresh();}
    int mode(){return pixels.mode();}
    void selectMode(int value){frozen=false;pixels.selectMode(value);}
    void setAudioSource(VisualizerPcm source){audio=source;refresh();}
    void setFrame(float[] levels,float[] waveform,boolean playing,boolean loaded){frozen=false;this.playing=playing;pixels.setFrame(levels,waveform,playing,loaded);refresh();}
    void setFrozenFrame(float[] levels,float[] waveform){frozen=true;pixels.setFrozenFrame(levels,waveform);refresh();}
    void refreshPresets(){try{presets=store.all();}catch(IOException e){Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_LONG).show();presets=Collections.emptyList();}presetId="";choosePreset(prefs.getString("projectm-preset",""));}
    List<ProjectMPresets.Preset> presets(){return presets;}
    String selectedPreset(){return presetId;}
    void choosePreset(String id){
        ProjectMPresets.Preset choice=null;for(ProjectMPresets.Preset p:presets)if(p.id.equals(id))choice=p;
        if(choice==null&&!presets.isEmpty())choice=presets.get(0);
        if(choice==null)return;if(choice.id.equals(presetId))return;
        presetId=choice.id;prefs.edit().putString("projectm-preset",presetId).apply();
        label.setText("PROJECTM · "+choice.name.toUpperCase(Locale.ROOT));
        projectm.selection(new ProjectMView.Selection(choice,store.texturePaths(choice)));
    }
    void nextPreset(){if(presets.isEmpty())return;int at=0;for(int i=0;i<presets.size();i++)if(presets.get(i).id.equals(presetId))at=i;choosePreset(presets.get((at+1)%presets.size()).id);}
    boolean automatic(){return prefs.getBoolean("projectm-cycle",true);}
    void automatic(boolean value){prefs.edit().putBoolean("projectm-cycle",value).apply();projectm.automatic(value);}
    private void refresh(){
        boolean show=mode()==4&&!frozen;
        int edge=show?dp(4):0;setPadding(edge,edge,edge,edge);
        pixels.setVisibility(show?GONE:VISIBLE);projectm.setVisibility(show?VISIBLE:GONE);label.setVisibility(show?VISIBLE:GONE);
        projectm.setContentDescription("PROJECTM visualizer. Tap to return to spectrum. Presets are in Player menu, Visualizers.");
        projectm.automatic(automatic());projectm.state(active&&show,playing,audio);
    }
}
