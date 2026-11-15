package dev.pixelspc;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class HeaderDisplayTest {
    @Test public void realDotRendererProducesTitleGlitchesAndWaveInOneColour()throws Exception{
        android.content.Context c=RuntimeEnvironment.getApplication();PlayerTheme theme=PlayerTheme.find("phosphor");ChipLogo logo=new ChipLogo(c,theme);
        HeaderAnimation animation=org.robolectric.util.ReflectionHelpers.getField(logo,"animation");HeaderAnimation.Cycle cycle=animation.cycle();
        long[] times={0,cycle.glitches[0]+40,cycle.title+cycle.scatter+cycle.wave/2,cycle.title+cycle.scatter+cycle.wave+cycle.reform/2};
        String[] labels={"TITLE","GLITCH","WAVE","REFORM"};Bitmap sheet=Bitmap.createBitmap(1000,720,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(sheet);canvas.drawColor(theme.background);Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);paint.setTypeface(UiFonts.body(c));paint.setTextSize(18);paint.setColor(theme.text);
        Bitmap[] frames=new Bitmap[4];
        for(int i=0;i<4;i++){
            canvas.drawText(labels[i],12,i*180+22,paint);frames[i]=Bitmap.createBitmap(1000,148,Bitmap.Config.ARGB_8888);Canvas frame=new Canvas(frames[i]);Drawable screen=new PlayerSkin(c,theme).header(theme.nowPlaying().spectrumBackground);screen.setBounds(0,0,1000,148);screen.draw(frame);
            logo.renderFrame(frame,new RectF(18,12,895,136),times[i],true);Drawable gear=new DotGlyph(true,theme.headerInk());gear.setBounds(914,34,982,110);gear.draw(frame);canvas.drawBitmap(frames[i],0,i*180+30,null);
        }
        assertFalse(frames[0].sameAs(frames[1]));assertFalse(frames[0].sameAs(frames[2]));assertFalse(frames[2].sameAs(frames[3]));
        int lit=0;for(int y=0;y<148;y++)for(int x=0;x<895;x++)if(frames[2].getPixel(x,y)==theme.headerInk())lit++;assertTrue("Wave should contain visible foreground dots",lit>100);
        try(OutputStream out=new FileOutputStream("../docs/screenshots/header-pinball-sequence.png")){assertTrue(sheet.compress(Bitmap.CompressFormat.PNG,100,out));}
    }
}
