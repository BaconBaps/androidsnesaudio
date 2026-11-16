package dev.pixelspc;

import android.content.Context;
import android.graphics.Typeface;

/** Bundled OFL fonts: available offline, with existing display fonts kept separate. */
final class UiFonts {
    private static Typeface body,heading;
    static synchronized Typeface body(Context c){if(body==null)body=c.getResources().getFont(R.font.pixelify_sans);return body;}
    static synchronized Typeface heading(Context c){if(heading==null)heading=c.getResources().getFont(R.font.jersey_15);return heading;}
    static Typeface brand(Context c){return body(c);}
}
