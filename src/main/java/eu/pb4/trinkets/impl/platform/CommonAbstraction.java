package eu.pb4.trinkets.impl.platform;

import com.mojang.brigadier.CommandDispatcher;
import dev.yumi.mc.core.api.YumiMods;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.Util;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.BiConsumer;

public interface CommonAbstraction {
    boolean IS_FABRIC = YumiMods.get().isModLoaded("fabricloader") && !YumiMods.get().isModLoaded("connector");

    CommonAbstraction INSTANCE = Util.make(() -> {
        try {
            return (CommonAbstraction) Class.forName(
                    "eu.pb4.trinkets.impl.platform." +
                            (CommonAbstraction.IS_FABRIC ? "fabric.FabricCommonAbstraction" : "neo.NeoCommonAbstraction")).getField("INSTANCE").get(null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    });

    static CommonAbstraction get() {
        return INSTANCE;
    }

    void registerServerReloadListener(Identifier identifier, PreparableReloadListener instance, Identifier... requires);

    <T extends CustomPacketPayload> void registerClientboundPlayPayload(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec);

    void registerCommand(BiConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext> consumer);

    void registerMobConversion(MobConversion conversion);

    interface MobConversion {
        void convert(LivingEntity from, LivingEntity to, ConversionParams params);
    }
}
