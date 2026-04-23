package eu.pb4.trinkets.impl.platform.neo;


import com.mojang.brigadier.CommandDispatcher;
import eu.pb4.trinkets.impl.platform.CommonAbstraction;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.ConversionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public record NeoCommonAbstraction(List<Consumer<IEventBus>> lateActions) implements CommonAbstraction {
    public static IEventBus EVENT_BUS = null;
    public static final NeoCommonAbstraction INSTANCE = new NeoCommonAbstraction(new ArrayList<>());

    @Override
    public void registerServerReloadListener(Identifier identifier, PreparableReloadListener instance, Identifier... requires) {
        NeoForge.EVENT_BUS.addListener(AddServerReloadListenersEvent.class, e -> {
            e.addListener(identifier, instance);

            for (var r : requires) {
                e.addDependency(r, identifier);
            }
        });
    }

    @Override
    public <T extends CustomPacketPayload> void registerClientboundPlayPayload(CustomPacketPayload.Type<T> type, StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        addLateAction(bus -> bus.addListener(RegisterPayloadHandlersEvent.class, e -> {
            e.registrar("1").playToClient(type, codec);
        }));
    }

    @Override
    public void registerCommand(BiConsumer<CommandDispatcher<CommandSourceStack>, CommandBuildContext> consumer) {
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> {
            consumer.accept(e.getDispatcher(), e.getBuildContext());
        });
    }

    @Override
    public void registerMobConversion(MobConversion conversion) {
        NeoForge.EVENT_BUS.addListener(LivingConversionEvent.Post.class, e -> {
            conversion.convert(e.getEntity(), e.getOutcome(), new ConversionParams(ConversionType.SINGLE, true, false, null));
        });
    }

    @Override
    public boolean isClient() {
        return FMLEnvironment.getDist().isClient();
    }

    public void addLateAction(Consumer<IEventBus> consumer) {
        if (EVENT_BUS != null) {
            consumer.accept(EVENT_BUS);
        } else {
            this.lateActions.add(consumer);
        }
    }
}
