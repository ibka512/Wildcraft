package dev.wildcraft.test;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Feed packaged recipe inputs through Minecraft's real recipe/menu path. */
final class SurvivalRecipes {
    static final List<String> ALL=List.of("paraglider","cooking_pot","battery","charger","machine_body","fan","wing","wheel","rocket","spring","stabilizer","buoyancy","fabricator","spent_rocket_recycling");
    record Pattern(List<ItemStack> inputs,Item output){}
    static Pattern read(String name){
        if(name.equals("compass"))return vanilla(Items.COMPASS,new String[]{" I ","IRI"," I "},Map.of('I',Items.IRON_INGOT,'R',Items.REDSTONE));
        if(name.equals("bow"))return vanilla(Items.BOW,new String[]{" SR","S R"," SR"},Map.of('S',Items.STICK,'R',Items.STRING));
        if(name.equals("iron_sword"))return vanilla(Items.IRON_SWORD,new String[]{" I "," I "," S "},Map.of('I',Items.IRON_INGOT,'S',Items.STICK));
        if(name.equals("redstone_block"))return vanilla(Items.REDSTONE_BLOCK,new String[]{"RRR","RRR","RRR"},Map.of('R',Items.REDSTONE));
        try(var in=SurvivalRecipes.class.getResourceAsStream("/data/wildcraft/recipe/"+name+".json")){
            if(in==null)throw new AssertionError("Missing packaged recipe "+name);
            var json=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            var output=item(json.getAsJsonObject("result").get("id").getAsString());var list=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));
            if(json.has("pattern")){
                var rows=json.getAsJsonArray("pattern");var keys=json.getAsJsonObject("key");
                for(int y=0;y<rows.size();y++){var row=rows.get(y).getAsString();for(int x=0;x<row.length();x++)if(row.charAt(x)!=' ')list.set(y*3+x,new ItemStack(item(keys.get(String.valueOf(row.charAt(x))).getAsString())));}
            }else{int i=0;for(var element:json.getAsJsonArray("ingredients"))list.set(i++,new ItemStack(item(element.getAsString())));}
            return new Pattern(List.copyOf(list),output);
        }catch(IOException ex){throw new UncheckedIOException(ex);}
    }
    private static Pattern vanilla(Item out,String[] rows,Map<Character,Item> keys){var list=new ArrayList<ItemStack>();for(var row:rows)for(char c:row.toCharArray())list.add(c==' '?ItemStack.EMPTY:new ItemStack(keys.get(c)));return new Pattern(List.copyOf(list),out);}
    private static Item item(String id){return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));}
}
