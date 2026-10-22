package dev.pixelspc;

/** UI roles shared by the player, dialogs, system bars, and pixel spectrum. */
public final class PlayerTheme {
    public final String id,name,inspiration;
    public final boolean dark;
    public final int background,text,muted,accent,onAccent,border,surface,secondary;

    private PlayerTheme(String id,String name,String inspiration,boolean dark,int background,int text,int accent,int secondary) {
        this.id=id; this.name=name; this.inspiration=inspiration; this.dark=dark;
        this.background=background; this.text=text; this.accent=accent; this.secondary=secondary;
        muted=blend(text,background,.24f); border=blend(background,text,.22f);
        surface=blend(background,accent,dark?.14f:.06f);
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
        new PlayerTheme("dreamcast","Dreamcast","Amber dusk & orange spiral",true,0xff302012,0xfffff1d6,0xffffb45e,0xff87d2ed),
        new PlayerTheme("playstation","PlayStation","Steel blue shell & logo gold",false,0xffcbd7e4,0xff182e52,0xff174d9f,0xffd6ab30),
        new PlayerTheme("n64","Nintendo 64","Forest green · logo yellow & blue",true,0xff122d24,0xffedf4dd,0xffe9d455,0xff69a9f8),
        new PlayerTheme("gamecube","GameCube","Deep violet shell & mint highlights",true,0xff32194d,0xfff5eaff,0xffd2a8ff,0xff65e2b6),
        new PlayerTheme("desktop-98","Desktop ’98","Midnight metal · phosphor green & brass",true,0xff202232,0xffe4e9d7,0xffa8f238,0xffd8bc72)
    };
    public static PlayerTheme find(String id) { for(PlayerTheme t:ALL) if(t.id.equals(id)) return t; return ALL[0]; }
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
