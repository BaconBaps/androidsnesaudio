package dev.pixelspc;

final class PlayerStyle {
    static final String CLASSIC="classic",CHIPAMP5="chipamp5";
    static boolean valid(String id){return CLASSIC.equals(id)||CHIPAMP5.equals(id);}
    static String name(String id){return CHIPAMP5.equals(id)?"ChipAmp5":"Classic";}
    static String defaultTheme(String id){return CHIPAMP5.equals(id)?"chip5-dark":"light";}
}
