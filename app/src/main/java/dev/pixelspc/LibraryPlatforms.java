package dev.pixelspc;

import java.util.*;

/** Groups imports by metadata, without guessing that every VGM belongs to one console. */
final class LibraryPlatforms {
    static final class Group {
        final String name;
        final List<Library.Game> games=new ArrayList<>();
        Group(String name) { this.name=name; }
        int trackCount() { int count=0; for(Library.Game game:games) count+=game.tracks.size(); return count; }
    }
    static String canonical(String system) {
        if(system==null || system.trim().isEmpty()) return "Other systems";
        String s=system.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();
        if(s.contains("super nintendo") || s.contains("super famicom") || s.equals("snes")) return "Super Nintendo";
        if(s.contains("game boy advance") || s.contains("gameboy advance")) return "Game Boy Advance";
        if(s.contains("game boy") || s.contains("gameboy")) return "Game Boy";
        if(s.equals("nes") || s.contains("nintendo nes") || s.contains("nintendo entertainment system") || s.equals("famicom")) return "NES / Famicom";
        if(s.contains("mega drive") || s.contains("megadrive") || s.equals("genesis") || s.equals("sega genesis")) return "Mega Drive / Genesis";
        if(s.equals("sony playstation") || s.equals("playstation") || s.equals("ps1") || s.equals("psx")) return "PlayStation";
        if(s.equals("n64") || s.equals("nintendo 64") || s.equals("nintendo64")) return "Nintendo 64";
        if(s.contains("game gear")) return "Game Gear";
        if(s.contains("master system")) return "Master System";
        if(s.contains("pc engine") || s.contains("turbografx")) return "PC Engine / TurboGrafx-16";
        return system.trim();
    }
    static String platform(Library.Game game) {
        Set<String> systems=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> tags=new HashSet<>();
        for(Library.Track track:game.tracks) if(tags.add(track.system)) { String s=canonical(track.system); if(!s.equals("Other systems")) systems.add(s); }
        return systems.isEmpty()?"Other systems":systems.size()==1?systems.iterator().next():"Multiple systems";
    }
    static List<Group> group(List<Library.Game> games) {
        Map<String,Group> groups=new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for(Library.Game game:games) { String name=platform(game); groups.computeIfAbsent(name,Group::new).games.add(game); }
        List<Group> result=new ArrayList<>(groups.values());
        for(Group group:result) group.games.sort(Comparator.comparing(g->g.name,String.CASE_INSENSITIVE_ORDER));
        return result;
    }
}
