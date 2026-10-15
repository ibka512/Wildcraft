package dev.wildcraft.client.combat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.wildcraft.Wildcraft;
import java.nio.file.*;
import net.fabricmc.loader.api.FabricLoader;

/** Independent preferences; existing focus configuration and saved gameplay data are untouched. */
public final class CombatOptions {
    private static final Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("wildcraft-combat.json");
    private static CombatOptions current=new CombatOptions();
    public int schema=1, range=1, follow=1, bowSkill=0, dodgeSkill=0;
    public boolean hold=false, neutral=false, jumpDodge=false;
    public static CombatOptions get(){return current;}
    public static void load(){
        if(!Files.exists(FILE))return;
        try(var reader=Files.newBufferedReader(FILE)){
            var read=JSON.fromJson(reader,CombatOptions.class);
            if(read!=null&&read.schema==1){read.range=Math.clamp(read.range,0,2);read.follow=Math.clamp(read.follow,0,3);read.bowSkill=Math.clamp(read.bowSkill,0,2);read.dodgeSkill=Math.clamp(read.dodgeSkill,0,2);current=read;}
            else Wildcraft.LOGGER.warn("Unsupported combat preferences; original file retained");
        }catch(java.io.IOException|RuntimeException ex){Wildcraft.LOGGER.warn("Cannot read combat preferences; defaults retained",ex);}
    }
    public static void save(){
        try{Files.createDirectories(FILE.getParent());var temp=FILE.resolveSibling(FILE.getFileName()+".tmp");
            Files.writeString(temp,JSON.toJson(current)+"\n");Files.move(temp,FILE,StandardCopyOption.REPLACE_EXISTING);
        }catch(java.io.IOException ex){Wildcraft.LOGGER.warn("Cannot save combat preferences",ex);}
    }
}
