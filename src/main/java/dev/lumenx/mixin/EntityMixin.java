package dev.lumenx.mixin;

import dev.lumenx.Glow;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Подменяет цвет контура свечения для целей наших модулей. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
    private void lumenx$outlineColor(CallbackInfoReturnable<Integer> cir) {
        Integer c = Glow.colorFor((Entity) (Object) this);
        if (c != null) cir.setReturnValue(c);
    }
}
