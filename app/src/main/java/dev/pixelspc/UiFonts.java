package dev.pixelspc;

import android.content.Context;
import android.graphics.Typeface;

/** Bundled OFL fonts: available offline, with existing display fonts kept separate. */
final class UiFonts {
    private static Typeface body,heading,brand;
    static synchronized Typeface body(Context c){if(body==null)body=c.getResources().getFont(R.font.chakra_petch);return body;}
    static synchronized Typeface heading(Context c){if(heading==null)heading=c.getResources().getFont(R.font.jersey_15);return heading;}
    static synchronized Typeface brand(Context c){if(brand==null)brand=c.getResources().getFont(R.font.pixelify_sans);return brand;}
}
