package dev.pixelspc;

/** UI roles shared by the player, dialogs, system bars, and pixel spectrum. */
public final class PlayerTheme {
    public final String id,name,inspiration;
    public final boolean dark;
    public final int background,text,muted,accent,onAccent,border,surface,secondary;

    private PlayerTheme(String id,String name,String inspiration,boolean dark,int background,int text,int accent,int secondary) {
        this.id=id; this.name=name; this.inspiration=inspiration; this.dark=dark;
        this.background=background; this.text=text; this.accent=accent; this.secondary=secondary;
        border=blend(background,text,.22f);
        surface=blend(background,accent,dark?.14f:.06f);
        int quiet=text;
        for(int step=0;step<=20;step++) {
            quiet=blend(text,background,.24f*(1-step/20f));
            if(contrast(background,quiet)>=4.5 && contrast(surface,quiet)>=4.5) break;
        }
        muted=quiet;
        onAccent=contrast(accent,0xff181820)>contrast(accent,0xffffffff)?0xff181820:0xffffffff;
    }
    public static final PlayerTheme[] ALL={
        new PlayerTheme("light","Pixel Light","The original lavender & cream",false,0xfff6f3eb,0xff302b40,0xff7453cc,0xffdce7bf),
        new PlayerTheme("dark","Pixel Dark","Deep ocean & electric teal",true,0xff08191e,0xffd6f3ef,0xff53d8d0,0xffffbd80),
        new PlayerTheme("snes","SNES","US hardware · gray & purple",false,0xffe7e5eb,0xff30283e,0xff6845aa,0xffc4b3df),
        new PlayerTheme("super-famicom","Super Famicom","Navy shell · vivid red & gold buttons",true,0xff101f32,0xffeef2f8,0xffff7186,0xfff4cc49),
        new PlayerTheme("nes","NES","Graphite shell & cartridge red",true,0xff303239,0xfff3f4f6,0xffff9b9b,0xffb9c1cd),
        new PlayerTheme("famicom","Famicom","Warm ivory & burgundy hardware",false,0xfff4e6cc,0xff4a2229,0xff922940,0xffd9b35a),
        new PlayerTheme("game-boy","Game Boy","Four shades of the original LCD",false,0xffd2dba0,0xff253b22,0xff38542a,0xff93a65b),
        new PlayerTheme("mega-drive","Mega Drive / Genesis","Black hardware · Sega blue & red",true,0xff111113,0xfff2eeee,0xff73acff,0xffe85b66),
        new PlayerTheme("gamecube","GameCube","Deep violet shell & mint highlights",true,0xff32194d,0xfff5eaff,0xffd2a8ff,0xff65e2b6),
        new PlayerTheme("desktop-98","Llama","Midnight metal · phosphor green & brass",true,0xff202232,0xffe4e9d7,0xffa8f238,0xffd8bc72)
    };
    public static PlayerTheme find(String id) { for(PlayerTheme t:ALL) if(t.id.equals(id)) return t; return ALL[0]; }
    /** A darker, contrast-safe version of the selected palette for the playback deck. */
    public PlayerTheme nowPlaying() {
        int bg=blend(background,0xff000000,dark?.55f:.82f);
        int lightAccent=accent;
        for(int step=0;step<=20;step++) {
            lightAccent=blend(accent,0xffffffff,step/20f);
            if(contrast(blend(bg,lightAccent,.14f),lightAccent)>=4.5) break;
        }
        return new PlayerTheme(id+"-player",name,inspiration,true,bg,dark?text:0xffedf2f4,lightAccent,secondary);
    }
    /** Quieter list surfaces with palette-matched text that keeps its contrast. */
    public PlayerTheme library() {
        int bg=blend(background,0xff000000,dark?.30f:.045f);
        int adjusted=accent;
        for(int step=0;step<=20;step++) {
            adjusted=blend(accent,dark?0xffffffff:0xff000000,step/20f);
            if(contrast(bg,adjusted)>=4.5 && contrast(blend(bg,adjusted,dark?.14f:.06f),adjusted)>=4.5) break;
        }
        return new PlayerTheme(id+"-library",name,inspiration,dark,bg,text,adjusted,secondary);
    }
    public int libraryButtonBackground() { return blend(background,0xffffffff,dark?.075f:.16f); }
    public String label() { return name+"\n"+inspiration; }
    static int blend(int a,int b,float amount) {
        int r=Math.round(((a>>16)&255)*(1-amount)+((b>>16)&255)*amount);
        int g=Math.round(((a>>8)&255)*(1-amount)+((b>>8)&255)*amount);
        int blue=Math.round((a&255)*(1-amount)+(b&255)*amount);
        return 0xff000000|(r<<16)|(g<<8)|blue;
    }
    private static double linear(int channel) { double c=channel/255.0; return c<=.04045?c/12.92:Math.pow((c+.055)/1.055,2.4); }
    static double luminance(int color) { return .2126*linear((color>>16)&255)+.7152*linear((color>>8)&255)+.0722*linear(color&255); }
    public static double contrast(int a,int b) { double x=luminance(a),y=luminance(b); return (Math.max(x,y)+.05)/(Math.min(x,y)+.05); }

}
