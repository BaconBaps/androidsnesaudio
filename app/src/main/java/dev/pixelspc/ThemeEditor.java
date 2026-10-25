package dev.pixelspc;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.json.*;

/** Frozen real-view preview with a fixed, readable editing shell and pixel colour tools. */
final class ThemeEditor {
    static final int BG=0xff151b26,FG=0xffedf1f5,ACCENT=0xffc5f06b;
    static final class State {
        ThemeStore.Draft draft;List<Library.Track> tracks=new ArrayList<>();Library.Track current;int position;boolean playing,repeat,shuffle;float[] levels=new float[AudioSpectrum.BANDS],waveform=new float[AudioWaveform.SAMPLES];
        JSONObject json()throws Exception{JSONArray wave=new JSONArray();for(float value:waveform)wave.put(value);JSONArray bars=new JSONArray();for(float f:levels)bars.put((double)f);return new JSONObject().put("draft",ThemeStore.encode(draft)).put("draftId",draft.id).put("position",position).put("playing",playing).put("repeat",repeat).put("shuffle",shuffle).put("current",current==null?JSONObject.NULL:current.json()).put("levels",bars).put("waveform",wave);}
        static State read(String text)throws Exception{JSONObject o=new JSONObject(text);State s=new State();s.draft=ThemeStore.decode(o.getJSONObject("draft"));String id=o.optString("draftId");if(id.matches("custom-[a-f0-9-]{36}"))s.draft.id=id;s.position=o.getInt("position");s.playing=o.getBoolean("playing");s.repeat=o.getBoolean("repeat");s.shuffle=o.getBoolean("shuffle");if(!o.isNull("current"))s.current=Library.Track.from(o.getJSONObject("current"));JSONArray bars=o.getJSONArray("levels");for(int i=0;i<Math.min(bars.length(),s.levels.length);i++)s.levels[i]=(float)bars.getDouble(i);JSONArray wave=o.optJSONArray("waveform");if(wave!=null)for(int i=0;i<Math.min(wave.length(),s.waveform.length);i++)s.waveform[i]=(float)wave.getDouble(i);return s;}
    }
    static final class Target {final View view;final String title;final String[] roles;Target(View view,String title,String...roles){this.view=view;this.title=title;this.roles=roles;}}
    static int dp(Context c,float n){return (int)(c.getResources().getDisplayMetrics().density*n+.5f);}
    static TextView text(Context c,String value,int size,int color,boolean pixel){TextView v=new TextView(c);v.setText(value);v.setTextSize(size);v.setTextColor(color);if(pixel){v.setTypeface(Typeface.createFromAsset(c.getAssets(),"fonts/pixel.ttf"));v.setSingleLine(true);v.setEllipsize(android.text.TextUtils.TruncateAt.END);}v.setGravity(Gravity.CENTER_VERTICAL);return v;}
    static TextView key(Context c,String title){TextView v=text(c,title,8,FG,true);v.setSingleLine(false);v.setHorizontallyScrolling(false);v.setEllipsize(null);v.setMaxLines(2);v.setGravity(Gravity.CENTER);v.setBackground(new PlayerSkin(c).button(BG));v.setPadding(dp(c,6),0,dp(c,6),0);v.setContentDescription(title);v.setFocusable(true);v.setClickable(true);return v;}
    static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}
    static Dialog window(Context c,String title,View content){
        Dialog dialog=new Dialog(new ContextThemeWrapper(c,android.R.style.Theme_Material_Dialog_Alert));dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout body=column(c);body.setPadding(dp(c,14),dp(c,12),dp(c,14),dp(c,12));body.setBackground(new PlayerSkin(c).chassis(BG));TextView heading=text(c,title,10,ACCENT,true);body.addView(heading,new LinearLayout.LayoutParams(-1,dp(c,40)));body.addView(content,new LinearLayout.LayoutParams(-1,-2));dialog.setContentView(body);if(c instanceof MainActivity)((MainActivity)c).trackEditorDialog(dialog);dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));return dialog;
    }
    static void show(Dialog d){d.show();d.getWindow().setLayout(Math.min(d.getContext().getResources().getDisplayMetrics().widthPixels-dp(d.getContext(),24),dp(d.getContext(),460)),-2);}
    static void install(MainActivity host,LinearLayout root,List<Target> targets){
        root.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);root.setOnApplyWindowInsetsListener(null);
        LinearLayout shell=column(host);shell.setBackgroundColor(BG);shell.setOnApplyWindowInsetsListener((v,insets)->{shell.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());return insets.consumeSystemWindowInsets();});
        LinearLayout bar=new LinearLayout(host);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(host,6),0,dp(host,6),0);TextView cancel=key(host,"×");cancel.setContentDescription("Cancel theme edit");bar.addView(cancel,new LinearLayout.LayoutParams(dp(host,44),dp(host,44)));
        TextView mode=text(host,"THEME EDIT",9,ACCENT,true);bar.addView(mode,new LinearLayout.LayoutParams(0,dp(host,48),1));TextView elements=key(host,"PARTS"),save=key(host,"SAVE");elements.setContentDescription("Choose theme element");save.setContentDescription("Save custom theme");bar.addView(elements,new LinearLayout.LayoutParams(dp(host,68),dp(host,44)));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(host,64),dp(host,44));p.leftMargin=dp(host,6);bar.addView(save,p);shell.addView(bar,new LinearLayout.LayoutParams(-1,dp(host,52)));
        TextView hint=text(host,"Frozen preview · tap a part to paint it",12,FG,false);hint.setGravity(Gravity.CENTER);shell.addView(hint,new LinearLayout.LayoutParams(-1,dp(host,24)));
        FrameLayout frame=new FrameLayout(host);if(root.getParent()!=null)((ViewGroup)root.getParent()).removeView(root);frame.addView(root,new FrameLayout.LayoutParams(-1,-1));Overlay overlay=new Overlay(host,frame,targets);frame.addView(overlay,new FrameLayout.LayoutParams(-1,-1));shell.addView(frame,new LinearLayout.LayoutParams(-1,0,1));host.setContentView(shell);
        cancel.setOnClickListener(v->host.cancelThemeEdit());save.setOnClickListener(v->host.saveThemeEdit());elements.setOnClickListener(v->parts(host,targets));
    }
    // Honour all ancestor clips, including the scrolling playlist viewport.
    static Rect visibleBounds(FrameLayout frame,View view,Rect result){
        if(!view.getLocalVisibleRect(result)){result.setEmpty();return result;}
        try{frame.offsetDescendantRectToMyCoords(view,result);if(!result.intersect(0,0,frame.getWidth(),frame.getHeight()))result.setEmpty();}
        catch(IllegalArgumentException e){result.setEmpty();}return result;
    }
    private static final class Overlay extends View {
        final MainActivity host;final FrameLayout frame;final List<Target> targets;final Paint paint=new Paint();final Rect rectangle=new Rect();
        Overlay(MainActivity host,FrameLayout frame,List<Target> targets){super(host);this.host=host;this.frame=frame;this.targets=new ArrayList<>(targets);setContentDescription("Frozen theme preview. Use Parts or tap an element to edit colours.");setFocusable(true);setClickable(true);paint.setColor(0x779de2ef);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(host,1));paint.setPathEffect(new DashPathEffect(new float[]{dp(host,4),dp(host,5)},0));}
        Rect bounds(Target target){return visibleBounds(frame,target.view,rectangle);}
        @Override protected void onDraw(Canvas canvas){for(Target target:targets){Rect r=bounds(target);if(r.width()>0&&r.height()>0)canvas.drawRect(r,paint);}}
        @Override public boolean onTouchEvent(MotionEvent event){if(event.getAction()==MotionEvent.ACTION_UP){Target selected=null;long area=Long.MAX_VALUE;for(Target t:targets){Rect r=bounds(t);long size=(long)r.width()*r.height();if(r.contains((int)event.getX(),(int)event.getY())&&size<area){selected=t;area=size;}}if(selected!=null)picker(host,selected);performClick();}return true;}
        @Override public boolean performClick(){super.performClick();return true;}
    }
    private static void parts(MainActivity host,List<Target> targets){
        LinearLayout list=column(host);ScrollView scroll=new ScrollView(host);scroll.addView(list);Dialog dialog=window(host,"CHOOSE A PART",scroll);
        Set<String> seen=new HashSet<>();for(Target target:targets)if(seen.add(target.title)){TextView row=key(host,target.title);list.addView(row,new LinearLayout.LayoutParams(-1,dp(host,46)));row.setOnClickListener(v->{dialog.dismiss();picker(host,target);});}
        show(dialog);dialog.getWindow().setLayout(Math.min(host.getResources().getDisplayMetrics().widthPixels-dp(host,24),dp(host,460)),(int)(host.getResources().getDisplayMetrics().heightPixels*.75f));
    }
    static String label(String role){String name=role.substring(role.lastIndexOf('.')+1);switch(name){case "button":return "Face";case "buttonText":return "Label";case "spectrumLow":return "Low";case "spectrumMid":return "Middle";case "spectrumHigh":return "High";case "spectrumPeak":return "Peaks";case "spectrumBackground":return "Screen";case "onAccent":return "On accent";case "libraryButton":return "Face";default:return Character.toUpperCase(name.charAt(0))+name.substring(1);}}
    static int color(ThemeStore.Draft draft,String role){PlayerTheme t=draft.theme();String name=role;if(role.startsWith("player.")){t=t.nowPlaying();name=role.substring(7);}else if(role.startsWith("library.")){t=t.library();name=role.substring(8);}switch(name){case "background":return t.background;case "surface":return t.surface;case "text":return t.text;case "muted":return t.muted;case "accent":return t.accent;case "secondary":return t.secondary;case "border":return t.border;case "heading":return t.heading;case "button":return t.buttonFace(t.background);case "buttonText":return t.buttonText(t.text);case "onAccent":return t.onAccent;case "libraryButton":return t.libraryButtonBackground();case "spectrumBackground":return t.spectrumBackground;case "spectrumLow":return t.spectrumLow;case "spectrumMid":return t.spectrumMid;case "spectrumHigh":return t.spectrumHigh;case "spectrumPeak":return t.spectrumPeak;default:return t.value(name,t.background);}}
    static void picker(MainActivity host,Target target){
        ThemeStore.Draft draft=host.editDraft();if(draft==null)return;Map<String,Integer> before=new HashMap<>(draft.colors);LinearLayout body=column(host);ScrollView content=new ScrollView(host);content.addView(body);Dialog dialog=window(host,target.title,content);
        HorizontalScrollView tabsScroll=new HorizontalScrollView(host);LinearLayout tabs=new LinearLayout(host);tabsScroll.addView(tabs);body.addView(tabsScroll,new LinearLayout.LayoutParams(-1,dp(host,44)));
        TextView value=text(host,"",12,FG,false);value.setGravity(Gravity.CENTER);body.addView(value,new LinearLayout.LayoutParams(-1,dp(host,30)));
        final String[] selected={target.roles[0]};float[] hsv=new float[3];Color.colorToHSV(color(draft,selected[0]),hsv);
        ColorSquare square=new ColorSquare(host,hsv);square.setContentDescription("Saturation and brightness");body.addView(square,new LinearLayout.LayoutParams(-1,dp(host,150)));
        HueStrip hue=new HueStrip(host,hsv);hue.setContentDescription("Hue");LinearLayout.LayoutParams hueParams=new LinearLayout.LayoutParams(-1,dp(host,32));hueParams.topMargin=dp(host,8);body.addView(hue,hueParams);
        Runnable apply=()->{int color=Color.HSVToColor(hsv);value.setText(String.format(Locale.ROOT,"%s · #%06X",label(selected[0]),color&0xffffff));square.updateHue();square.invalidate();hue.invalidate();host.paintTheme(selected[0],color);};square.changed=apply;hue.changed=apply;
        List<TextView> chips=new ArrayList<>();Runnable refresh=()->{for(int i=0;i<chips.size();i++){boolean active=target.roles[i].equals(selected[0]);chips.get(i).setTextColor(active?ACCENT:FG);}Color.colorToHSV(color(draft,selected[0]),hsv);value.setText(String.format(Locale.ROOT,"%s · #%06X",label(selected[0]),color(draft,selected[0])&0xffffff));square.updateHue();square.invalidate();hue.invalidate();};
        for(String role:target.roles){TextView chip=key(host,label(role));tabs.addView(chip,new LinearLayout.LayoutParams(dp(host,104),dp(host,40)));chips.add(chip);chip.setOnClickListener(v->{selected[0]=role;refresh.run();});}
        GridLayout swatches=new GridLayout(host);swatches.setColumnCount(8);int[] colors={0xff101019,0xff303543,0xff747d8c,0xfff6f3eb,0xffe9556f,0xffffbe55,0xffbcf06b,0xff52d9d1,0xff74a9ff,0xffba8bea,0xffffa4ca,0xff754532,0xffe9d499,0xff304b30,0xff194551,0xffffffff};
        for(int i=0;i<colors.length;i++){int color=colors[i];TextView swatch=text(host,"",1,color,false);swatch.setBackgroundColor(color);swatch.setContentDescription(String.format(Locale.ROOT,"Paint #%06X",color&0xffffff));GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(i/8),GridLayout.spec(i%8,1f));p.width=0;p.height=dp(host,28);p.setMargins(dp(host,2),dp(host,3),dp(host,2),dp(host,3));swatches.addView(swatch,p);swatch.setOnClickListener(v->{Color.colorToHSV(color,hsv);apply.run();});}body.addView(swatches);
        LinearLayout buttons=new LinearLayout(host);TextView undo=key(host,"UNDO"),done=key(host,"DONE");buttons.addView(undo,new LinearLayout.LayoutParams(0,dp(host,44),1));buttons.addView(done,new LinearLayout.LayoutParams(0,dp(host,44),1));body.addView(buttons);undo.setOnClickListener(v->{draft.colors.clear();draft.colors.putAll(before);host.repaintTheme();refresh.run();});done.setOnClickListener(v->dialog.dismiss());refresh.run();show(dialog);
    }
    private static final class ColorSquare extends View {
        final float[] hsv;final Paint paint=new Paint();Runnable changed;LinearGradient saturation,value;float cachedHue=-1;final float[] pure={0,1,1};
        ColorSquare(Context c,float[] hsv){super(c);this.hsv=hsv;setLayerType(View.LAYER_TYPE_SOFTWARE,null);}
        void updateHue(){if(getWidth()>0&&cachedHue!=hsv[0]){pure[0]=hsv[0];saturation=new LinearGradient(0,0,getWidth(),0,Color.WHITE,Color.HSVToColor(pure),Shader.TileMode.CLAMP);cachedHue=hsv[0];}}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);cachedHue=-1;value=new LinearGradient(0,0,0,h,0x00000000,Color.BLACK,Shader.TileMode.CLAMP);updateHue();}
        @Override protected void onDraw(Canvas c){paint.setStyle(Paint.Style.FILL);paint.setShader(saturation);c.drawRect(0,0,getWidth(),getHeight(),paint);paint.setShader(value);c.drawRect(0,0,getWidth(),getHeight(),paint);paint.setShader(null);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(3);paint.setColor(Color.WHITE);float x=hsv[1]*getWidth(),y=(1-hsv[2])*getHeight();c.drawRect(x-5,y-5,x+5,y+5,paint);}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN)getParent().requestDisallowInterceptTouchEvent(true);if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL)getParent().requestDisallowInterceptTouchEvent(false);if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE||e.getAction()==MotionEvent.ACTION_UP){hsv[1]=Math.max(0,Math.min(1,e.getX()/getWidth()));hsv[2]=1-Math.max(0,Math.min(1,e.getY()/getHeight()));if(changed!=null)changed.run();if(e.getAction()==MotionEvent.ACTION_UP)performClick();}return true;}
        @Override public boolean performClick(){super.performClick();return true;}
    }
    private static final class HueStrip extends View {
        final float[] hsv;final Paint paint=new Paint();Runnable changed;LinearGradient gradient;
        HueStrip(Context c,float[] hsv){super(c);this.hsv=hsv;}
        @Override protected void onSizeChanged(int w,int h,int oldw,int oldh){super.onSizeChanged(w,h,oldw,oldh);gradient=new LinearGradient(0,0,w,0,new int[]{Color.RED,Color.YELLOW,Color.GREEN,Color.CYAN,Color.BLUE,Color.MAGENTA,Color.RED},null,Shader.TileMode.CLAMP);}
        @Override protected void onDraw(Canvas c){paint.setShader(gradient);c.drawRect(0,0,getWidth(),getHeight(),paint);paint.setShader(null);paint.setColor(Color.WHITE);float x=hsv[0]/360*getWidth();c.drawRect(x-2,0,x+2,getHeight(),paint);}
        @Override public boolean onTouchEvent(MotionEvent e){if(e.getAction()==MotionEvent.ACTION_DOWN)getParent().requestDisallowInterceptTouchEvent(true);if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL)getParent().requestDisallowInterceptTouchEvent(false);if(e.getAction()==MotionEvent.ACTION_DOWN||e.getAction()==MotionEvent.ACTION_MOVE||e.getAction()==MotionEvent.ACTION_UP){hsv[0]=Math.max(0,Math.min(359.99f,e.getX()/getWidth()*360));if(changed!=null)changed.run();if(e.getAction()==MotionEvent.ACTION_UP)performClick();}return true;}
        @Override public boolean performClick(){super.performClick();return true;}
    }
}
