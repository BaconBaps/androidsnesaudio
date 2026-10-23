package dev.pixelspc;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;

/** Native launcher widget; the host animates the clock without periodic app wakeups. */
public class PlayerWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids) { updateAll(context); }
    @Override public void onAppWidgetOptionsChanged(Context context,AppWidgetManager manager,int id,android.os.Bundle options) { updateAll(context); }
    static void updateAll(Context context) {
        AppWidgetManager manager=AppWidgetManager.getInstance(context);
        int[] ids=manager.getAppWidgetIds(new ComponentName(context,PlayerWidget.class));
        if(ids.length!=0) manager.updateAppWidget(ids,views(context,PlaybackService.instance));
    }
    static RemoteViews views(Context context,PlaybackService service) {
        PlayerTheme theme=PlayerTheme.find(context.getSharedPreferences("appearance",Context.MODE_PRIVATE).getString("theme","light")).nowPlaying();
        RemoteViews views=new RemoteViews(context.getPackageName(),R.layout.player_widget);
        views.setInt(R.id.widget_root,"setBackgroundColor",theme.background);
        views.setInt(R.id.widget_band,"setBackgroundColor",theme.secondary);
        for(int id:new int[]{R.id.widget_previous,R.id.widget_play,R.id.widget_next}) views.setInt(id,"setBackgroundResource",R.drawable.widget_key);
        boolean loaded=service!=null && service.current!=null;
        boolean playing=loaded && service.playing;
        Library.Track track=loaded?service.current:null;
        views.setTextViewText(R.id.widget_title,loaded?track.title:context.getString(R.string.widget_empty));
        views.setTextViewText(R.id.widget_artist,loaded?(track.artist.isEmpty()?track.game:track.artist+" · "+track.game):context.getString(R.string.app_name));
        views.setTextColor(R.id.widget_heading,theme.accent); views.setTextColor(R.id.widget_title,theme.accent);
        views.setTextColor(R.id.widget_artist,theme.muted); views.setTextColor(R.id.widget_time,theme.accent); views.setTextColor(R.id.widget_duration,theme.accent);
        int position=loaded?service.position:0;
        views.setChronometer(R.id.widget_time,SystemClock.elapsedRealtime()-position,null,playing && service.seekTarget<0);
        views.setViewVisibility(R.id.widget_time,loaded?View.VISIBLE:View.GONE);
        views.setTextViewText(R.id.widget_duration,loaded?" / "+(service.repeat?"∞":MainActivity.clock(track.length)):"");
        views.setImageViewResource(R.id.widget_play,playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play);
        views.setContentDescription(R.id.widget_play,context.getString(playing?R.string.widget_pause:R.string.widget_play));
        PendingIntent open=PendingIntent.getActivity(context,20,new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        for(int id:new int[]{R.id.widget_open,R.id.widget_title,R.id.widget_artist}) views.setOnClickPendingIntent(id,open);
        int[] buttons={R.id.widget_previous,R.id.widget_play,R.id.widget_next}; String[] actions={"previous","toggle","next"};
        for(int i=0;i<buttons.length;i++) views.setOnClickPendingIntent(buttons[i],loaded?PendingIntent.getForegroundService(context,30+i,new Intent(context,PlaybackService.class).setAction("widget:"+actions[i]),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT):open);
        return views;
    }
}
