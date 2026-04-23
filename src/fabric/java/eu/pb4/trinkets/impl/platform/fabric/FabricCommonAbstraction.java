package eu.pb4.trinkets.impl.platform.fabric;


import com.mojang.brigadier.CommandDispatcher;
import eu.pb4.trinkets.impl.platform.CommonAbstraction;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.function.BiConsumer;

public record FabricCommonAbstraction() implements CommonAbstraction {
    public static final CommonAbstraction INSTANCE = new FabricCommonAbstraction();

    @Override
    public void registerServerReloadListener(Identifier identifier, PreparableReloadListener instance, Identifier... requires) {
        var loader = ResourceLoader.get(PackType.SERVER_DATA);
        loader.registerReloadListener(identifier, instance);

        for (var r : requires) {
            loader.addListenerOrdering(r, identifier);
        }
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientboundPlayPayload(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
    }

    @Override
    public void registerCommand(BiConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext> consumer) {
        CommandRegistrationCallback.EVENT.register((a, b, _) -> consumer.accept(a, b));
    }

    @Override
    public void registerMobConversion(MobConversion conversion) {
        ServerLivingEntityEvents.MOB_CONVERSION.register(conversion::convert);
    }

    @Override
    public boolean isClient() {
        return FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT;
    }
}
