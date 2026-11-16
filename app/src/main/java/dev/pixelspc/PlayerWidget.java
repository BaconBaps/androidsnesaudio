package dev.pixelspc;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.SystemClock;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.widget.TextView;
import android.view.View;
import android.widget.RemoteViews;

/** Native launcher widget; the host animates the clock without periodic app wakeups. */
public class PlayerWidget extends AppWidgetProvider {
    private static final android.util.LruCache<String,Bitmap> skins=new android.util.LruCache<String,Bitmap>(8*1024*1024){@Override protected int sizeOf(String key,Bitmap bitmap){return bitmap.getByteCount();}};
    private static Bitmap cached(Context context,PlayerTheme theme,String part,java.util.function.Supplier<Bitmap> render){String key=theme.id+":"+theme.styleId+":"+theme.colors.hashCode()+":"+context.getResources().getDisplayMetrics().densityDpi+":"+context.getResources().getConfiguration().fontScale+":"+part;Bitmap bitmap=skins.get(key);if(bitmap==null){bitmap=render.get();skins.put(key,bitmap);}return bitmap;}
    @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids) { updateAll(context); }
    @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,android.os.Bundle options) { updateAll(context); }
    static void updateAll(Context context) {
        AppWidgetManager manager=AppWidgetManager.getInstance(context);
        int[] ids=manager.getAppWidgetIds(new ComponentName(context,PlayerWidget.class));
        for(int id:ids){android.os.Bundle options=manager.getAppWidgetOptions(id);
            int width=Math.max(180,Math.min(720,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,360)));
            int height=Math.max(160,Math.min(500,options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,220)));
            manager.updateAppWidget(id,views(context,PlaybackService.instance,width,height));
        }
    }
    static RemoteViews views(Context context,PlaybackService service) {
        return views(context,service,360,220);
    }
    private static RemoteViews views(Context context,PlaybackService service,int width,int height) {
        PlayerTheme theme=ThemeStore.selected(context).nowPlaying();PlayerSkin skin=new PlayerSkin(context,theme);
        RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.player_widget);
        views.setInt(R.id.widget_root,"setBackgroundColor",Color.TRANSPARENT);
        views.setImageViewBitmap(R.id.widget_chrome,cached(context,theme,"chassis:"+width+":"+height,()->paint(context,skin.chassis(theme.background),width-8,height-8)));
        views.setImageViewBitmap(R.id.widget_readout,cached(context,theme,"readout:"+width+":"+height,()->paint(context,skin.well(theme.value("readout",theme.background)),width-36,Math.max(32,height-138))));
        views.setImageViewBitmap(R.id.widget_band,cached(context,theme,"rail:"+width,()->paint(context,skin.rail(theme.secondary),Math.max(24,width-134),5)));
        for(int id:new int[]{R.id.widget_previous,R.id.widget_play,R.id.widget_next}) views.setInt(id,"setBackgroundColor",Color.TRANSPARENT);
        boolean loaded=service!=null && service.current!=null;
        boolean playing=loaded && service.playing;
        Library.Track track=loaded?service.current:null;
        views.setTextViewText(R.id.widget_title,loaded?track.title:context.getString(R.string.widget_empty));
        views.setTextViewText(R.id.widget_artist,loaded?(track.artist.isEmpty()?track.game:track.artist+" · "+track.game):context.getString(R.string.app_name));
        views.setTextColor(R.id.widget_heading,theme.heading); views.setTextColor(R.id.widget_title,theme.accent);
        views.setTextColor(R.id.widget_artist,theme.muted); views.setTextColor(R.id.widget_time,theme.accent); views.setTextColor(R.id.widget_duration,theme.accent);
        int position=loaded?service.position:0;
        views.setChronometer(R.id.widget_time,SystemClock.elapsedRealtime()-position,null,playing && service.seekTarget<0);
        views.setViewVisibility(R.id.widget_time,loaded?View.VISIBLE:View.GONE);
        views.setTextViewText(R.id.widget_duration,loaded?" / "+(service.repeat?"∞":MainActivity.clock(track.length)):"");
        views.setImageViewBitmap(R.id.widget_previous,cached(context,theme,"previous",()->key(context,theme,"|◀",56,false)));
        views.setImageViewBitmap(R.id.widget_play,cached(context,theme,playing?"pause":"play",()->key(context,theme,playing?"Ⅱ":"▶",64,true)));
        views.setImageViewBitmap(R.id.widget_next,cached(context,theme,"next",()->key(context,theme,"▶|",56,false)));
        views.setContentDescription(R.id.widget_play,context.getString(playing?R.string.widget_pause:R.string.widget_play));
        PendingIntent open=PendingIntent.getActivity(context,20,new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        for(int id:new int[]{R.id.widget_open,R.id.widget_title,R.id.widget_artist}) views.setOnClickPendingIntent(id,open);
        int[] buttons={R.id.widget_previous,R.id.widget_play,R.id.widget_next}; String[] actions={"previous","toggle","next"};
        for(int i=0;i<buttons.length;i++) views.setOnClickPendingIntent(buttons[i],loaded?PendingIntent.getForegroundService(context,30+i,new Intent(context,PlaybackService.class).setAction("widget:"+actions[i]),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT):open);
        return views;
    }
    private static Bitmap paint(Context context,Drawable drawable,int width,int height){
        float density=context.getResources().getDisplayMetrics().density;
        Bitmap bitmap=Bitmap.createBitmap(Math.max(1,Math.round(width*density)),Math.max(1,Math.round(height*density)),Bitmap.Config.ARGB_8888);
        drawable.setBounds(0,0,bitmap.getWidth(),bitmap.getHeight());drawable.draw(new Canvas(bitmap));return bitmap;
    }
    private static Bitmap key(Context context,PlayerTheme theme,String glyph,int width,boolean primary){
        TextView view=theme.chipAmp5()?new MatrixControl(context,theme.secondary):new TextView(context);
        view.setText(glyph);view.setTextColor(theme.buttonText(primary?theme.onAccent:theme.text));view.setGravity(android.view.Gravity.CENTER);
        if(!theme.chipAmp5()){view.setTypeface(Typeface.createFromAsset(context.getAssets(),"fonts/pixel.ttf"));view.setTextSize(primary?18:11);}
        view.setBackground(new PlayerSkin(context,theme).button(theme.buttonFace(primary?theme.accent:theme.background)));
        float density=context.getResources().getDisplayMetrics().density;int w=Math.round(width*density),h=Math.round(48*density);
        view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);
        Bitmap bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));return bitmap;
    }

}
