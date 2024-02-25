package dev.luna5ama.fornax.mixin.core;

import dev.luna5ama.fornax.FornaxMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixinCore {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void Inject$init$RETURN(RunArgs args, CallbackInfo ci) {
        FornaxMod.INSTANCE.init();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void Inject$close$HEAD(CallbackInfo ci) {
        FornaxMod.INSTANCE.shutdown();
    }
}
