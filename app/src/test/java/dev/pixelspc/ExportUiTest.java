package dev.pixelspc;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=35,qualifiers="w411dp-h891dp-xhdpi")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class ExportUiTest {
    private View find(View view,String label) {
        if(label.contentEquals(view.getContentDescription()==null?"":view.getContentDescription())) return view;
        if(view instanceof TextView && label.contentEquals(((TextView)view).getText())) return view;
        if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) { View found=find(((ViewGroup)view).getChildAt(i),label); if(found!=null) return found; }
        return null;
    }
    @Test public void saveUsesDocumentPickerAndKeepsClipSettingsAcrossRecreation() {
        Intent track=new Intent(RuntimeEnvironment.getApplication(),ExportActivity.class).putExtra("title","A track").putExtra("path","fixture.spc").putExtra("length",180000);
        try(org.robolectric.android.controller.ActivityController<ExportActivity> c=Robolectric.buildActivity(ExportActivity.class,track).setup()) {
            View decor=c.get().getWindow().getDecorView();
            ((EditText)find(decor,"Export start")).setText("1:20"); ((EditText)find(decor,"Export duration")).setText("0:12");
            c.recreate(); decor=c.get().getWindow().getDecorView();
            assertEquals("1:20",((EditText)find(decor,"Export start")).getText().toString());
            try {
                decor.measure(View.MeasureSpec.makeMeasureSpec(822,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1782,View.MeasureSpec.EXACTLY)); decor.layout(0,0,822,1782);
                android.graphics.Bitmap image=android.graphics.Bitmap.createBitmap(822,1782,android.graphics.Bitmap.Config.ARGB_8888); decor.draw(new android.graphics.Canvas(image));
                java.io.File file=new java.io.File(System.getProperty("user.dir"),"../docs/screenshots/export-audio.png");
                try(java.io.OutputStream out=new java.io.FileOutputStream(file)) { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out); }
            } catch(java.io.IOException e) { throw new AssertionError(e); }
            find(decor,"SAVE WAV…").performClick();
            Intent picker=Shadows.shadowOf(c.get()).getNextStartedActivityForResult().intent;
            assertEquals(Intent.ACTION_CREATE_DOCUMENT,picker.getAction()); assertEquals("audio/wav",picker.getType());
            assertEquals("A track.wav",picker.getStringExtra(Intent.EXTRA_TITLE));
        }
    }
    @Test public void invalidDurationDoesNotLaunchPickerAndFullTrackUsesMetadata() {
        Intent track=new Intent(RuntimeEnvironment.getApplication(),ExportActivity.class).putExtra("title","A track").putExtra("length",238000);
        try(org.robolectric.android.controller.ActivityController<ExportActivity> c=Robolectric.buildActivity(ExportActivity.class,track).setup()) {
            View decor=c.get().getWindow().getDecorView(); ((EditText)find(decor,"Export duration")).setText("99:99"); find(decor,"SAVE WAV…").performClick();
            assertNull(Shadows.shadowOf(c.get()).getNextStartedActivityForResult());
            find(decor,"USE FULL TRACK").performClick(); assertEquals("3:58",((EditText)find(decor,"Export duration")).getText().toString());
        }
    }
}
