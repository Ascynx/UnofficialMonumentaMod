package ch.njol.unofficialmonumentamod.discordrpc;

import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.mixins.PlayerListHudAccessor;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.reflect.Field;

import static ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient.readJsonFile;
import static ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient.writeJsonFile;
import static ch.njol.unofficialmonumentamod.Utils.getUrl;
import static java.lang.Integer.parseInt;

public class Locations {


    //add shards with locations here
    public ArrayList<String> VALLEY;
    public ArrayList<String> PLOTS;
    public ArrayList<String> ISLES;

    private static final String CACHE_FILE_PATH = "unofficial-monumenta-mod-locations.json";
    private String update_commit;
    private static final String UPDATE_GIST_URL = "https://api.github.com/gists/4b1602b907da62a9cca6f135fd334737";//put new locations in that gist

    public static class locationFile {
        //add the shards here too
        public String[] VALLEY;
        public String[] PLOTS;
        public String[] ISLES;

        public String update_commit;
    }


    public Locations() {
        for (Field f: this.getClass().getDeclaredFields()) {
            if (f.getType() != java.util.ArrayList.class) continue;
                try {
                    f.set(this, new ArrayList<>());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
        }
    }

    public static String getShard() {
        MinecraftClient mc = MinecraftClient.getInstance();

        Text header = ((PlayerListHudAccessor) mc.inGameHud.getPlayerListWidget()).getHeader();

        String shard = null;

        for (Text text: header.getSiblings()) {
            if (text.getString().matches("<.*>")) {
                //player shard
                shard = text.getString().substring(1, text.getString().length()-1);
            }
        }

        return shard;
    }

    private void addToShard(String addition, String shard) {
        addToShard(new String[]{addition}, shard);
    }

    private void addToShard(String[] additions, String shard) {
        try {
            ArrayList<String> location = getLocations(shard.toUpperCase());
            if (location != null) {//shard's list exist
                for (String addition: additions) {
                    if (!addition.matches("\\((?<X1>-*[0-9]*):(?<Z1>-?[0-9]*)\\)\\((?<X2>-?[0-9]*):(?<Z2>-?[0-9]*)\\)/(?<name>.*)")) continue;//only adds correctly made locations
                    location.add(addition);
                };
                this.getClass().getField(shard.toUpperCase()).set(this, location);
            };
        } catch (Exception ignored) {}
    }

    private void update() {
        try {
            URL url = new URL(UPDATE_GIST_URL);
            String content = getUrl(url);

            JsonParser jsonParser = new JsonParser();

            JsonObject json =  jsonParser.parse(content).getAsJsonObject();

            JsonObject jsonContent = jsonParser.parse(json.get("files").getAsJsonObject().get("Locations.json").getAsJsonObject().get("content").getAsString()).getAsJsonObject();
            Gson gson = new Gson();
            Type type = new TypeToken<String[]>(){}.getType();

            for (Map.Entry<String, JsonElement> entry: jsonContent.entrySet()) {
                addToShard((String[]) gson.fromJson(entry.getValue(), type), entry.getKey());
            }

            this.update_commit = jsonParser.parse(getUrl(new URL(UPDATE_GIST_URL + "/commits"))).getAsJsonArray().get(0).getAsJsonObject().get("version").getAsString();

        }catch (Exception nE) {
            nE.printStackTrace();
        }

        writeJsonFile(this, CACHE_FILE_PATH);
    }

    public void load() {
        try {
            locationFile cache = readJsonFile(locationFile.class, CACHE_FILE_PATH);

            if (UnofficialMonumentaModClient.options.locationUpdate) {
                JsonParser jsonParser = new JsonParser();
                if (jsonParser.parse(getUrl(new URL(UPDATE_GIST_URL + "/commits"))).getAsJsonArray().get(0).getAsJsonObject().get("version").getAsString() != cache.update_commit) {
                    update();
                    return;
                }
            }

            for (Field f: cache.getClass().getFields()) {
                for (Field parentField: this.getClass().getFields()) {
                    if (f.getName().equals(parentField.getName()) && f.getType() == parentField.getType()) {
                        parentField.set(this, f.get(cache));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            // file doesn't exist,
           update();
        } catch (IOException | JsonParseException | IllegalAccessException e) {
            e.printStackTrace();
        }
        /*
          locations that will be included:
          cities (ex: Ta'eldim)
          World bosses (ex: Kaul Arena)
          Important locations (ex: Player Market)

           (+X:-Z)(-X:+Z)/Name
         */
    }

    private ArrayList<String> getLocations(String shard) throws IllegalAccessException {
        for (Field f: this.getClass().getFields()) {
            if (f.getName().equals(shard.toUpperCase()) && f.getType().getTypeName().equals(this.VALLEY.getClass().getTypeName()))
                    return (ArrayList<String>) f.get(this);
        }

        return null;
    }

    public String getLocation(double X, double Z, String shard) {
        try {
            ArrayList<String> locations = getLocations(shard);
            if (Objects.isNull(locations)) return shard;

            for (String location : locations) {
                if (location.matches("\\((?<X1>-*[0-9]*):(?<Z1>-?[0-9]*)\\)\\((?<X2>-?[0-9]*):(?<Z2>-?[0-9]*)\\)/(?<name>.*)")) {//skips badly made locations
                    Pattern locationTest = Pattern.compile("\\((?<X1>-*[0-9]*):(?<Z1>-?[0-9]*)\\)\\((?<X2>-?[0-9]*):(?<Z2>-?[0-9]*)\\)/(?<name>.*)");
                    Matcher matcher = locationTest.matcher(location);
                    matcher.matches();//runs the pattern

                    int X1 = parseInt(matcher.group("X1"));
                    int Z1 = parseInt(matcher.group("Z1"));

                    int X2 = parseInt(matcher.group("X2"));
                    int Z2 = parseInt(matcher.group("Z2"));

                    String locationName = matcher.group("name");

                    if ((X >= X1 && X <= X2) || (X <= X1 && X >= X2)) {
                        //X is between X1 and X2

                        if ((Z >= Z1 && Z <= Z2) || (Z <= Z1 && Z >= Z2)) {
                            //Z is between Z1 and Z2 (coordinates are between the two limits)

                            return locationName;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

            return shard;//doesn't exist in lists
        }

        return shard;//didn't find coordinates in existing lists
    }


}
