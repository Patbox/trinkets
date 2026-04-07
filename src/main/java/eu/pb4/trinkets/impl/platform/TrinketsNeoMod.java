package eu.pb4.trinkets.impl.platform;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("trinkets_updated")
public class TrinketsNeoMod {
    public TrinketsNeoMod(IEventBus modBus) {
        NeoServerAbstraction.EVENT_BUS.setValue(modBus);
        for (var a: NeoServerAbstraction.INSTANCE.lateActions()) {
            a.accept(modBus);
        }
        NeoServerAbstraction.INSTANCE.lateActions().clear();
    }
}
