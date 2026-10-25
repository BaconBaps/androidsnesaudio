package dev.pixelspc;

import android.graphics.*;
import android.graphics.drawable.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class PlayerSkinTest {
    private Bitmap render(Drawable d,int w,int h) {
        Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888); d.setBounds(0,0,w,h); d.draw(new Canvas(b)); return b;
    }
    @Test public void rasterBordersCompileAsNinePatchesAndKeepTheirCornersWhenStretched() {
        int[] ids={R.drawable.skin_chassis,R.drawable.skin_panel,R.drawable.skin_well,R.drawable.skin_key,R.drawable.skin_key_down,R.drawable.skin_rail};
        for(int id:ids) assertTrue(RuntimeEnvironment.getApplication().getDrawable(id) instanceof NinePatchDrawable);
        PlayerSkin skin=new PlayerSkin(RuntimeEnvironment.getApplication());
        Bitmap small=render(skin.well(0xff202232),80,48),large=render(skin.well(0xff202232),420,240);
        for(int y=0;y<6;y++) for(int x=0;x<6;x++) {
            assertEquals(small.getPixel(x,y),large.getPixel(x,y));
            assertEquals(small.getPixel(79-x,47-y),large.getPixel(419-x,239-y));
        }
        assertEquals(0xff202232,large.getPixel(210,120));
        int trackHeight=Math.round(10*RuntimeEnvironment.getApplication().getResources().getDisplayMetrics().density);
        assertEquals(trackHeight,skin.progress(PlayerTheme.ALL[0]).getMinimumHeight());
    }
    @Test public void pressedControlsChangeTheirEdgingWithoutMovingTheirContent() {
        Drawable button=new PlayerSkin(RuntimeEnvironment.getApplication()).button(0xff53d8d0);
        Bitmap up=render(button,120,80);
        button.setState(new int[]{android.R.attr.state_pressed}); Bitmap down=render(button,120,80);
        assertFalse(up.sameAs(down)); assertEquals(up.getPixel(60,40),down.getPixel(60,40));
        button.setState(new int[]{}); assertTrue(up.sameAs(render(button,120,80)));
    }
    @Test public void reactorFramesKeepCornersAndCustomThemesKeepTheMechanicalSkin() throws Exception {
        android.content.Context c=RuntimeEnvironment.getApplication();PlayerTheme reactor=PlayerTheme.find("reactor");
        int[] frames={R.drawable.reactor_chassis,R.drawable.reactor_panel,R.drawable.reactor_well,R.drawable.reactor_key,R.drawable.reactor_key_down,R.drawable.reactor_rail};
        for(int id:frames)assertTrue(c.getDrawable(id) instanceof NinePatchDrawable);
        PlayerSkin skin=new PlayerSkin(c,reactor);
        Bitmap small=render(c.getDrawable(R.drawable.reactor_well),100,80),large=render(c.getDrawable(R.drawable.reactor_well),500,300);
        for(int y=0;y<8;y++)for(int x=0;x<8;x++){
            assertEquals(small.getPixel(x,y),large.getPixel(x,y));
            assertEquals(small.getPixel(99-x,79-y),large.getPixel(499-x,299-y));
        }
        ThemeStore.Draft draft=ThemeStore.from(reactor,false);draft.colors.put("player.spectrumHigh",0xffaaffcc);
        PlayerTheme restored=ThemeStore.read(new java.io.ByteArrayInputStream(ThemeStore.encode(draft).toString().getBytes("UTF-8"))).theme();
        assertTrue(restored.reactor());assertTrue(restored.nowPlaying().reactor());assertTrue(restored.library().reactor());
        assertEquals(0xffaaffcc,restored.nowPlaying().spectrumHigh);assertEquals(reactor.nowPlaying().spectrumLow,restored.nowPlaying().spectrumLow);
        assertFalse(render(skin.button(reactor.accent),120,80).sameAs(render(new PlayerSkin(c).button(reactor.accent),120,80)));
    }
    @Test public void lightThemeFrameShadowsStaySoft() {
        PlayerSkin skin=new PlayerSkin(RuntimeEnvironment.getApplication());
        for(String id:new String[]{"light","snes","famicom","game-boy"}) {
            PlayerTheme theme=PlayerTheme.find(id);
            Drawable button=skin.button(theme.background); button.setState(new int[]{android.R.attr.state_enabled});
            Bitmap frame=render(button,160,100);
            for(int y=0;y<100;y++) for(int x=0;x<160;x++)
                assertTrue(id+" frame contrast",PlayerTheme.contrast(theme.background,frame.getPixel(x,y))<3);
        }
    }
    @Test public void textureKeepsThemeTextReadableAcrossEveryPalette() {
        PlayerSkin skin=new PlayerSkin(RuntimeEnvironment.getApplication());
        for(PlayerTheme theme:PlayerTheme.ALL) {
            skin=new PlayerSkin(RuntimeEnvironment.getApplication(),theme);
            Bitmap chassis=render(skin.chassis(theme.background),80,80),selected=render(skin.selection(theme.surface),80,80);
            for(int y=24;y<56;y++) for(int x=24;x<56;x++) {
                int bg=chassis.getPixel(x,y);
                assertTrue(theme.name+" muted text",PlayerTheme.contrast(bg,theme.muted)>=4.5);
                assertTrue(theme.name+" accent",PlayerTheme.contrast(bg,theme.accent)>=4.5);
                assertTrue(theme.name+" selected track",PlayerTheme.contrast(selected.getPixel(x,y),theme.accent)>=4.5);
            }
        }
    }
}
