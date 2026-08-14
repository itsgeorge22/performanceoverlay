package com.itsgeorge.performanceoverlay.gametest.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class ClientServiceIsolationMixin {
    @Inject(method = "allowsRealms", at = @At("HEAD"), cancellable = true)
    private void disableRealms(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }

    @Inject(method = "allowsTelemetry", at = @At("HEAD"), cancellable = true)
    private void disableTelemetry(CallbackInfoReturnable<Boolean> callback) {
        callback.setReturnValue(false);
    }
}
