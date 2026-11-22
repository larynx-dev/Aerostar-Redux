package com.bega.aerostarcomp.client;

import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

public final class AeroStarCompPartials {
    public static final PartialModel ORBITAL_ASSEMBLER_LEVER = PartialModel.of(
        ResourceLocation.fromNamespaceAndPath("aerostarcomp", "block/orbital_physics_assembler/lever")
    );

    private AeroStarCompPartials() {
    }

    public static void init() {
    }
}
