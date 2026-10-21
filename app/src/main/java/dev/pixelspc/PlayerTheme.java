package dev.pixelspc;

/** UI roles shared by the player, dialogs, system bars, and pixel spectrum. */
public final class PlayerTheme {
    public final String id,name,inspiration;
    public final boolean dark;
    public final int background,text,muted,accent,onAccent,border,surface,secondary,chipText;

    private PlayerTheme(String id,String name,String inspiration,boolean dark,int background,int text,int accent,int secondary) {
        this.id=id; this.name=name; this.inspiration=inspiration; this.dark=dark;
        this.background=background; this.text=text; this.accent=accent; this.secondary=secondary;
        muted=blend(text,background,.24f); border=blend(background,text,.22f);
        surface=blend(background,accent,dark?.18f:.06f);
        onAccent=contrast(accent,0xff181820)>contrast(accent,0xffffffff)?0xff181820:0xffffffff;
        chipText=contrast(secondary,0xff181820)>contrast(secondary,0xffffffff)?0xff181820:0xffffffff;
    }
    public static final PlayerTheme[] ALL={
        new PlayerTheme("light","Pixel Light","The original lavender & cream",false,0xfff6f3eb,0xff302b40,0xff7453cc,0xffdce7bf),
        new PlayerTheme("dark","Pixel Dark","Midnight ink & soft lavender",true,0xff181622,0xfff0eafa,0xffbda2ff,0xffb5d99b),
        new PlayerTheme("snes","SNES","US hardware · gray & purple",false,0xffe7e5eb,0xff30283e,0xff6845aa,0xffc4b3df),
        new PlayerTheme("super-famicom","Super Famicom","Button colors · red, blue, green & yellow",false,0xffeeeee9,0xff272c32,0xffb62c36,0xffe7c64a),
        new PlayerTheme("nes","NES","Charcoal, light gray & cartridge red",false,0xffe7e6e2,0xff292929,0xffb72a36,0xffbfc0c2),
        new PlayerTheme("famicom","Famicom","Warm ivory & burgundy hardware",false,0xfff4e6cc,0xff4a2229,0xff922940,0xffd9b35a),
        new PlayerTheme("game-boy","Game Boy","Four shades of the original LCD",false,0xffd2dba0,0xff253b22,0xff38542a,0xff93a65b),
        new PlayerTheme("mega-drive","Mega Drive / Genesis","Black hardware & red lettering",true,0xff19191b,0xfff2eeee,0xffff737d,0xffaeb3bd),
        new PlayerTheme("dreamcast","Dreamcast","Ivory hardware & orange spiral",false,0xfff5f1e9,0xff363230,0xffad4011,0xff9cc6df),
        new PlayerTheme("playstation","PlayStation","Cool gray hardware & logo blue",false,0xffe3e5e6,0xff252d3c,0xff244c9d,0xffb5cc92),
        new PlayerTheme("n64","Nintendo 64","Charcoal hardware & colorful logo",true,0xff1d2125,0xfff2f3ed,0xffefd34f,0xff76bb78),
        new PlayerTheme("gamecube","GameCube","Indigo hardware & lavender cube",true,0xff211d3c,0xfff2edff,0xffb6a2f5,0xff9ed5ce)
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
