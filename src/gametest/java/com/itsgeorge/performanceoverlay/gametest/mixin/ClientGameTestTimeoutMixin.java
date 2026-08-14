package com.itsgeorge.performanceoverlay.gametest.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(targets = "net.fabricmc.fabric.impl.client.gametest.util.ClientGameTestImpl", remap = false)
public abstract class ClientGameTestTimeoutMixin {
    @ModifyConstant(method = "waitForWorldLoad", constant = @Constant(intValue = 1200), remap = false)
    private static int extendWorldLoadTimeout(int originalTimeoutTicks) {
        return 3600;
    }
}
