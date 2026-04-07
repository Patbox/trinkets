package eu.pb4.trinkets.impl.platform;

import com.mojang.brigadier.CommandDispatcher;
import dev.yumi.mc.core.api.YumiMods;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface CommonAbstraction {
    boolean IS_FABRIC = YumiMods.get().isModLoaded("fabricloader") && !YumiMods.get().isModLoaded("connector");

    static CommonAbstraction get() {
        return IS_FABRIC ? new FabricServerAbstraction() : NeoServerAbstraction.INSTANCE;
    }

    void registerServerReloadListener(Identifier identifier, PreparableReloadListener instance, Identifier... requires);

    <T extends CustomPacketPayload> void registerClientboundPlayPayload(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec);

    void registerCommand(BiConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext> consumer);

    void registerMobConversion(MobConversion conversion);

    interface MobConversion {
        void convert(LivingEntity from, LivingEntity to, ConversionParams params);
    }
}
