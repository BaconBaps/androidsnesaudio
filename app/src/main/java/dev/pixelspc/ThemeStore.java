package dev.pixelspc;

import android.content.*;
import java.io.*;
import java.util.*;
import org.json.*;

/** Named palettes contain only opaque colours and a known built-in base. */
final class ThemeStore {
    static final int LIMIT=40,FILE_LIMIT=65536;
    static final Set<String> ROLES=new HashSet<>(Arrays.asList("background","text","muted","accent","secondary","border","surface","onAccent","heading","button","buttonText","libraryButton","readout","spectrumBackground","spectrumLow","spectrumMid","spectrumHigh","spectrumPeak"));
    static final class Draft {
        String id,name,base;final Map<String,Integer> colors=new HashMap<>();
        Draft(String id,String name,String base){this.id=id;this.name=name;this.base=base;}
        PlayerTheme theme(){return PlayerTheme.custom(id,name,base,colors);}
        Draft copy(){Draft d=new Draft(id,name,base);d.colors.putAll(colors);return d;}
    }
    private final SharedPreferences prefs;
    ThemeStore(Context context){prefs=context.getSharedPreferences("custom-themes",Context.MODE_PRIVATE);}
    static PlayerTheme selected(Context context){return find(context,context.getSharedPreferences("appearance",Context.MODE_PRIVATE).getString("theme","light"));}
    static PlayerTheme find(Context context,String id){for(Draft d:new ThemeStore(context).drafts())if(d.id.equals(id))return d.theme();return PlayerTheme.find(id);}
    List<Draft> drafts(){
        List<Draft> result=new ArrayList<>();try{JSONArray array=new JSONArray(prefs.getString("library","[]"));for(int i=0;i<Math.min(array.length(),LIMIT);i++){try{JSONObject item=array.getJSONObject(i);Draft d=decode(item);String id=item.getString("id");if(id.matches("custom-[a-f0-9-]{36}")){d.id=id;result.add(d);}}catch(Exception invalid){android.util.Log.w("ThemeStore","Skipping invalid saved theme",invalid);}}}catch(Exception ignored){/* Default themes remain available. */}return result;
    }
    List<PlayerTheme> all(){List<PlayerTheme> result=new ArrayList<>(Arrays.asList(PlayerTheme.ALL));for(Draft d:drafts())result.add(d.theme());return result;}
    static Draft from(PlayerTheme theme,boolean duplicate){Draft d=new Draft(duplicate||!theme.id.startsWith("custom-")?"custom-"+UUID.randomUUID():theme.id,duplicate||!theme.id.startsWith("custom-")?theme.name+" copy":theme.name,theme.baseId);d.colors.putAll(theme.colors);return d;}
    void save(Draft draft)throws Exception{synchronized(ThemeStore.class){
        validate(draft);List<Draft> drafts=drafts();boolean replaced=false;for(int i=0;i<drafts.size();i++)if(drafts.get(i).id.equals(draft.id)){drafts.set(i,draft.copy());replaced=true;break;}
        if(!replaced){if(drafts.size()>=LIMIT)throw new IOException("Save up to 40 custom themes");drafts.add(draft.copy());}write(drafts);
    }}
    void delete(String id)throws Exception{synchronized(ThemeStore.class){List<Draft> drafts=drafts();drafts.removeIf(d->d.id.equals(id));write(drafts);}}
    private void write(List<Draft> drafts)throws Exception{JSONArray array=new JSONArray();for(Draft d:drafts)array.put(encode(d).put("id",d.id));if(!prefs.edit().putString("library",array.toString()).commit())throw new IOException("Could not save themes");}
    static JSONObject encode(Draft d)throws JSONException{JSONObject colors=new JSONObject();for(Map.Entry<String,Integer> e:d.colors.entrySet())colors.put(e.getKey(),String.format(Locale.ROOT,"#%06X",e.getValue()&0xffffff));return new JSONObject().put("version",1).put("name",d.name).put("base",d.base).put("colors",colors);}
    static Draft decode(JSONObject json)throws Exception{
        if(json.getInt("version")!=1)throw new IOException("Unsupported theme version");Draft d=new Draft("custom-"+UUID.randomUUID(),json.getString("name"),json.getString("base"));JSONObject colors=json.getJSONObject("colors");if(colors.length()>128)throw new IOException("Too many theme colours");
        for(Iterator<String> keys=colors.keys();keys.hasNext();){String key=keys.next(),value=colors.getString(key);if(!value.matches("#[0-9a-fA-F]{6}"))throw new IOException("Theme colours must use #RRGGBB");d.colors.put(key,0xff000000|Integer.parseInt(value.substring(1),16));}validate(d);return d;
    }
    static void validate(Draft d)throws IOException{
        d.name=d.name.trim();if(d.name.isEmpty()||d.name.length()>80)throw new IOException("Use a theme name between 1 and 80 characters");boolean base=false;for(PlayerTheme t:PlayerTheme.ALL)base|=t.id.equals(d.base);if(!base)throw new IOException("Unknown base palette");
        for(Map.Entry<String,Integer> entry:d.colors.entrySet()){String role=entry.getKey();if(role.startsWith("player."))role=role.substring(7);else if(role.startsWith("library."))role=role.substring(8);if(!ROLES.contains(role)||(entry.getValue()>>>24)!=255)throw new IOException("Unsupported theme colour: "+entry.getKey());}
    }
    static Draft read(InputStream input)throws Exception{if(input==null)throw new IOException("Cannot read theme");ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1){if(bytes.size()+n>FILE_LIMIT)throw new IOException("Theme file exceeds 64 KiB");bytes.write(buffer,0,n);}return decode(new JSONObject(bytes.toString("UTF-8")));}
}
