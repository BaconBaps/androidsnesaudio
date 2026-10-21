package dev.pixelspc;

import java.text.Normalizer;
import java.util.*;

/** Conservative filename/metadata join; sequel numbers remain significant. */
public final class ArtworkNames {
    public static String display(String name) {
        return name.replaceFirst("(?i)\\.(png|jpe?g)$","")
            .replaceAll("(?i)[ _-]+marquee$","")
            .replaceAll("\\[[^]]*]","")
            .replaceAll("(?i)\\((?:u|e|j|usa|europe|japan|world|v[0-9.]+|rev[ .0-9]+)\\)","")
            .replaceAll("\\s+"," ").trim();
    }
    public static String key(String name) {
        String s=Normalizer.normalize(display(name),Normalizer.Form.NFKD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT)
            .replace("&"," and ").replaceAll("[^\\p{L}\\p{N}]+"," ").trim();
        List<String> words=new ArrayList<>(Arrays.asList(s.split("\\s+")));
        if(!words.isEmpty() && words.get(0).equals("the")) words.remove(0);
        if(!words.isEmpty() && words.get(words.size()-1).equals("the")) words.remove(words.size()-1);
        String[] roman={"ii","iii","iv","v","vi","vii","viii","ix"};
        for(int i=0;i<words.size();i++) for(int n=0;n<roman.length;n++) if(words.get(i).equals(roman[n])) { words.set(i,Integer.toString(n+2)); break; }
        return String.join(" ",words);
    }
}
