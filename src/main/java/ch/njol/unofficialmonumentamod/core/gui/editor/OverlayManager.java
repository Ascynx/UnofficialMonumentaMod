package ch.njol.unofficialmonumentamod.core.gui.editor;

import ch.njol.minecraft.config.Config;
import ch.njol.unofficialmonumentamod.UnofficialMonumentaModClient;
import ch.njol.unofficialmonumentamod.core.gui.customoverlay.CustomOverlay;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Future;
import net.minecraft.util.Util;

public class OverlayManager {
    public PersistentData data = new PersistentData();

    private static final String PersistentCachePosition = "monumenta/overlays.json";
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    private OverlayManager() {}

    private static final OverlayManager INSTANCE = new OverlayManager();
    public static OverlayManager getInstance() {
        return INSTANCE;
    }


    private PersistentData lastCheckedData;

    private static boolean shouldUpdateFileData = true;
    private Future<?> checkLocalDataFuture;

    public boolean shouldUpdateLocalData() {
        if (Objects.equals(lastCheckedData, new PersistentData())) {
            return true;
        }

        //if it is null, just check, if it is not null, then check whether it is done.
        return checkLocalDataFuture != null ? checkLocalDataFuture.isDone() && !Objects.equals(data, lastCheckedData) : !Objects.equals(data, lastCheckedData);
    }

    public boolean shouldSave() {
        try {
            if (!shouldUpdateFileData) {
                return false;
            }

            PersistentData localData = Config.readJsonFile(PersistentData.class, PersistentCachePosition);

            checkLocalDataFuture = Util.getIoWorkerExecutor().submit(() -> {
                lastCheckedData = data.clone();
            });

            return !Objects.equals(data, localData);
        } catch (Exception e) {
            UnofficialMonumentaModClient.LOGGER.error("Caught error whilst trying to reload overlay data", e);
            return true;
        }
    }

    public static class PersistentData {
        @Expose
        public HashMap<String, CustomOverlay> overlays = new HashMap<>();
        @Expose
        public HashMap<String, List<String>> groups = new HashMap<>();

        protected PersistentData clone() {
            //time to do a little bit of trolling
            return GSON.fromJson(GSON.toJson(this), PersistentData.class);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PersistentData other)) {
                return false;
            }

            //the Map Set implementation only checks if the first instance's keys is in the others, and does not check if the other's keys are in the original.
            if (!overlays.keySet().equals(other.overlays.keySet()) || !other.overlays.keySet().equals(overlays.keySet())) {
                return false;
            }

            if (!groups.keySet().equals(other.groups.keySet()) || !other.groups.keySet().equals(groups.keySet())) {
                return false;
            }

            out:
            for (CustomOverlay otherOverlayData: other.overlays.values()) {
                for (CustomOverlay overlayData: overlays.values()) {
                    if (overlayData.getName().equals(otherOverlayData.getName())) {
                        if (!Objects.equals(overlayData, otherOverlayData)) {
                            return false;
                        }
                        continue out;
                    }
                }
            }

            out:
            for (Map.Entry<String, List<String>> otherGroup: other.groups.entrySet()) {
                for (Map.Entry<String, List<String>> group: groups.entrySet()) {
                    if (group.getKey().equals(otherGroup.getKey())) {
                        if (!group.getValue().equals(otherGroup.getValue())) {
                            return false;
                        }
                        continue out;
                    }
                }
            }

            return true;
        }
    }
}
