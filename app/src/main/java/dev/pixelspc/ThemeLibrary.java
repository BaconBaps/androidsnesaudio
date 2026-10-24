package dev.pixelspc;

import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Saved themes are visual cards, with editing/import/export kept separate from audio files. */
final class ThemeLibrary {
    static void show(MainActivity host){
        LinearLayout body=ThemeEditor.column(host),tools=new LinearLayout(host);TextView create=ThemeEditor.key(host,"CREATE"),load=ThemeEditor.key(host,"IMPORT");tools.addView(create,new LinearLayout.LayoutParams(0,ThemeEditor.dp(host,44),1));tools.addView(load,new LinearLayout.LayoutParams(0,ThemeEditor.dp(host,44),1));body.addView(tools);
        ScrollView scroll=new ScrollView(host);GridLayout grid=new GridLayout(host);grid.setColumnCount(2);scroll.addView(grid);body.addView(scroll,new LinearLayout.LayoutParams(-1,ThemeEditor.dp(host,320)));
        Dialog dialog=ThemeEditor.window(host,"SAVED THEMES",body);List<ThemeStore.Draft> drafts=new ThemeStore(host).drafts();
        if(drafts.isEmpty()){TextView empty=ThemeEditor.text(host,"Create a palette from your current theme, or import a shared theme file.",15,ThemeEditor.FG,false);empty.setPadding(12,24,12,24);grid.addView(empty);}
        for(int i=0;i<drafts.size();i++){ThemeStore.Draft draft=drafts.get(i);ThemePreview preview=new ThemePreview(host,draft.theme(),draft.id.equals(ThemeStore.selected(host).id));GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(i/2),GridLayout.spec(i%2,1f));p.width=0;p.height=ThemeEditor.dp(host,150);p.setMargins(4,8,4,8);grid.addView(preview,p);preview.setContentDescription("Manage "+draft.name+" theme");preview.setOnClickListener(v->{dialog.dismiss();actions(host,draft);});}
        if(!drafts.isEmpty()&&drafts.size()%2==1){android.view.View spacer=new android.view.View(host);GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(drafts.size()/2),GridLayout.spec(1,1f));p.width=0;p.height=ThemeEditor.dp(host,150);grid.addView(spacer,p);}
        TextView close=ThemeEditor.key(host,"CLOSE");body.addView(close,new LinearLayout.LayoutParams(-1,ThemeEditor.dp(host,44)));close.setOnClickListener(v->dialog.dismiss());create.setOnClickListener(v->{dialog.dismiss();host.beginThemeEdit(ThemeStore.from(ThemeStore.selected(host),true));});load.setOnClickListener(v->{dialog.dismiss();host.importTheme();});ThemeEditor.show(dialog);
    }
    private static void action(MainActivity host,LinearLayout body,Dialog dialog,String name,Runnable run){TextView button=ThemeEditor.key(host,name);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,ThemeEditor.dp(host,44));p.topMargin=ThemeEditor.dp(host,name.equals("DELETE")?20:6);body.addView(button,p);button.setOnClickListener(v->{dialog.dismiss();run.run();});}
    private static void actions(MainActivity host,ThemeStore.Draft draft){
        LinearLayout body=ThemeEditor.column(host);Dialog dialog=ThemeEditor.window(host,draft.name,body);
        action(host,body,dialog,"USE",()->host.selectTheme(draft.id));action(host,body,dialog,"EDIT",()->host.beginThemeEdit(draft.copy()));
        action(host,body,dialog,"DUPLICATE",()->host.beginThemeEdit(ThemeStore.from(draft.theme(),true)));action(host,body,dialog,"RENAME",()->rename(host,draft));action(host,body,dialog,"EXPORT",()->host.exportTheme(draft));
        action(host,body,dialog,"DELETE",()->{LinearLayout confirm=ThemeEditor.column(host);confirm.addView(ThemeEditor.text(host,"Delete “"+draft.name+"”?",16,ThemeEditor.FG,false));Dialog prompt=ThemeEditor.window(host,"DELETE THEME?",confirm);action(host,confirm,prompt,"DELETE",()->{try{boolean active=ThemeStore.selected(host).id.equals(draft.id);new ThemeStore(host).delete(draft.id);if(active)host.selectTheme(draft.base);show(host);}catch(Exception e){error(host,e.getMessage());}});action(host,confirm,prompt,"CANCEL",()->{});ThemeEditor.show(prompt);});
        action(host,body,dialog,"BACK",()->show(host));ThemeEditor.show(dialog);
    }
    private static void rename(MainActivity host,ThemeStore.Draft draft){
        LinearLayout body=ThemeEditor.column(host);EditText name=new EditText(new ContextThemeWrapper(host,android.R.style.Theme_Material_Dialog_Alert));name.setTextColor(ThemeEditor.FG);name.setSingleLine(true);name.setText(draft.name);name.setContentDescription("Theme name");name.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(80)});body.addView(name);Dialog dialog=ThemeEditor.window(host,"RENAME THEME",body);TextView save=ThemeEditor.key(host,"SAVE");body.addView(save,new LinearLayout.LayoutParams(-1,ThemeEditor.dp(host,44)));save.setOnClickListener(v->{try{draft.name=name.getText().toString();new ThemeStore(host).save(draft);if(ThemeStore.selected(host).id.equals(draft.id))host.selectTheme(draft.id);dialog.dismiss();show(host);}catch(Exception e){name.setError(e.getMessage());}});ThemeEditor.show(dialog);
    }
    static void error(MainActivity host,String message){LinearLayout body=ThemeEditor.column(host);body.addView(ThemeEditor.text(host,message==null?"Could not load theme":message,15,ThemeEditor.FG,false));Dialog dialog=ThemeEditor.window(host,"THEME UNAVAILABLE",body);TextView close=ThemeEditor.key(host,"CLOSE");body.addView(close,new LinearLayout.LayoutParams(-1,ThemeEditor.dp(host,44)));close.setOnClickListener(v->dialog.dismiss());ThemeEditor.show(dialog);}
}
