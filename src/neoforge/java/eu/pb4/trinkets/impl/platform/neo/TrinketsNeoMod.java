package eu.pb4.trinkets.impl.platform.neo;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("trinkets_updated")
public class TrinketsNeoMod {
    public TrinketsNeoMod(IEventBus modBus) {
        NeoCommonAbstraction.EVENT_BUS = modBus;
        for (var a : NeoCommonAbstraction.INSTANCE.lateActions()) {
            a.accept(modBus);
        }
        NeoCommonAbstraction.INSTANCE.lateActions().clear();
    }
}
