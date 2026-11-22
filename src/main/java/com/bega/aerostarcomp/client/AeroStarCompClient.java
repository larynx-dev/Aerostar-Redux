package com.bega.aerostarcomp.client;

import com.bega.aerostarcomp.menu.AeroStarCompMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(
    modid = "aerostarcomp",
    value = {Dist.CLIENT}
)
public final class AeroStarCompClient {
    private AeroStarCompClient() {
    }

    @SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(AeroStarCompMenuTypes.DIMENSIONAL_DRIVE.get(), DimensionalDriveScreen::new);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        AeroStarCompPartials.init();
    }
}
