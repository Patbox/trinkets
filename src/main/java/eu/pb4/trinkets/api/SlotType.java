package eu.pb4.trinkets.api;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.ApiStatus;

import java.util.Set;

@ApiStatus.NonExtendable
public interface SlotType {
    MutableComponent getTranslation();
    String getId();
    String group();
    String name();
    int order();
    int amount();
    Identifier icon();
    Set<Identifier> quickMovePredicates();
    Set<Identifier> validatorPredicates();
    Set<Identifier> tooltipPredicates();
    TrinketDropRule dropRule();
}
