package com.zrollus.bd.mixin;

import com.zrollus.bd.BuildersDelight;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class ExampleMixin {
    @Inject(at = @At("HEAD"), method = "init()V", require = 0)
    private void init(CallbackInfo info) {
        BuildersDelight.LOGGER.info("This line is printed by an example mod mixin!");
    }
}