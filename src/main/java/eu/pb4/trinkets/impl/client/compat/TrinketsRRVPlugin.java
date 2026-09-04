package eu.pb4.trinkets.impl.client.compat;

import cc.cassian.rrv.api.ReliableRecipeViewerClientPlugin;
import cc.cassian.rrv.api.event.OverlayManagementEvents;
import cc.cassian.rrv.common.overlay.BlockingGuiComponent;
import eu.pb4.trinkets.impl.TrinketsMain;
import eu.pb4.trinkets.impl.client.TrinketScreen;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TrinketsRRVPlugin implements ReliableRecipeViewerClientPlugin {
    private final List<Identifier> areas = new ArrayList<>();

    @Override
    public void onIntegrationInitialize() {
        OverlayManagementEvents.registerExclusionArea((screen, instance, partialTicks) -> {
            if (screen instanceof TrinketScreen) {
                int i = 0;
                for (var area : TrinketsExclusionAreas.create(screen)) {
                    instance.setExclusionArea(new BlockingGuiComponent(this.getId(i++), area));
                }
            }
        });
    }

    private Identifier getId(int i) {
        while (i >= this.areas.size()) {
            this.areas.add(Identifier.fromNamespaceAndPath(TrinketsMain.NAMESPACE, "exclusion_area/" + this.areas.size()));
        }

        return this.areas.get(i);
    }
}