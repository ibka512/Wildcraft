package dev.wildcraft.client.datagen;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.data.*;

/** Native item model selection reads saved meal kind; no new items or data migration. */
public final class SecondArtModelProvider implements DataProvider {
    private final Path root;
    public SecondArtModelProvider(FabricPackOutput output){root=output.getOutputFolder().resolve("assets/wildcraft");}
    @Override public String getName(){return "Wildcraft adopted second art models";}
    @Override public CompletableFuture<?> run(CachedOutput cache){
        var writes=new ArrayList<CompletableFuture<?>>();var entries=new JsonArray();
        String[] kinds={"vegetable","warming","cooling","recovery"};
        for(int i=0;i<kinds.length;i++){
            String name="meal_"+kinds[i];
            writes.add(DataProvider.saveStable(cache,JsonParser.parseString("{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"wildcraft:item/"+name+"\"}}"),root.resolve("models/item/"+name+".json")));
            var entry=new JsonObject();entry.addProperty("threshold",i);entry.add("model",model(name));entries.add(entry);
        }
        var select=new JsonObject();select.addProperty("type","minecraft:range_dispatch");select.addProperty("property","wildcraft:meal_kind");select.add("entries",entries);select.add("fallback",model("meal_vegetable"));
        var item=new JsonObject();item.add("model",select);writes.add(DataProvider.saveStable(cache,item,root.resolve("items/meal.json")));
        var batteryEntries=new JsonArray();
        for(int i=0;i<=18;i++){
            String name="battery_charge_"+i;
            writes.add(DataProvider.saveStable(cache,JsonParser.parseString("{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"wildcraft:item/"+name+"\"}}"),root.resolve("models/item/"+name+".json")));
            var e=new JsonObject();e.addProperty("threshold",(int)Math.ceil(i*1000.0/18));e.add("model",model(name));batteryEntries.add(e);
        }
        var charge=new JsonObject();charge.addProperty("type","minecraft:range_dispatch");charge.addProperty("property","wildcraft:battery_energy");charge.add("entries",batteryEntries);charge.add("fallback",model("battery_charge_0"));
        var guiCase=new JsonObject();guiCase.addProperty("when","gui");guiCase.add("model",charge);var cases=new JsonArray();cases.add(guiCase);
        var batterySelect=new JsonObject();batterySelect.addProperty("type","minecraft:select");batterySelect.addProperty("property","minecraft:display_context");batterySelect.add("cases",cases);
        batterySelect.add("fallback",JsonParser.parseString("{\"type\":\"minecraft:special\",\"base\":\"wildcraft:item/battery_3d\",\"model\":{\"type\":\"wildcraft:battery\"}}"));
        var battery=new JsonObject();battery.add("model",batterySelect);writes.add(DataProvider.saveStable(cache,battery,root.resolve("items/battery.json")));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }
    private static JsonObject model(String name){var m=new JsonObject();m.addProperty("type","minecraft:model");m.addProperty("model","wildcraft:item/"+name);return m;}
}
