package eu.pb4.trinkets.mixin.datafixer;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.V1460;

/**
 * This is main schema where Minecraft defines most post-flattening data formats.
 * Trinkets injects here adding support for basic datafixing in case of other mods supporting it or just general vanilla nbt
 * format changes.
 *
 * @author Patbox
 */
@Mixin(V1460.class)
public class V1460Mixin {
	@Unique
	private static Schema schema;

	/*
	 * We need to capture schema, so it is available in lambda mixins
	 */
	@Inject(method = "registerTypes", at = @At("HEAD"))
	private void captureSchema(Schema schemax, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes, CallbackInfo ci) {
		schema = schemax;
	}

	/*
	 * Inject trinket's schema into player data definition (lambda$registerTypes$2) and generic entity data definition (lambda$registerTypes$6)
	 * Optionals are ignored if it doesn't fit data definitions.
	 */
	@ModifyReturnValue(method = {"lambda$registerTypes$2", "lambda$registerTypes$6"}, at = @At("RETURN"))
	private static TypeTemplate attachTrinketFixer(TypeTemplate original) {
		// Add schema for trinkets to existing datafixers
		return DSL.allWithRemainder(
				// cardinal_components might not exist, so add it as an optional field.
				DSL.optional(DSL.field("cardinal_components",
						// trinkets:trinkets might not exist, so add it as an optional field.
						DSL.optionalFields("trinkets:trinkets",
								// Define it as (optional) compound list / map (Map<String, Further Definition>). Keys are slot groups.
								DSL.optional(DSL.compoundList(
										// Define it as (optional) compound list / map (Map<String, Further Definition>). Keys are slot types.
										DSL.optional(DSL.compoundList(
												// Define optional Items field, which is an optional list of ITEM_STACK. Other data is just copied over.
												DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(schema)))
										))
								))
						)
				)), original
		);
	}
}
