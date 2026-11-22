package larynx.dev.aerostar_redux.menu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AeroStarCompMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(BuiltInRegistries.MENU, "aerostar_redux");
    public static final DeferredHolder<MenuType<?>, MenuType<DimensionalDriveMenu>> DIMENSIONAL_DRIVE = MENU_TYPES.register(
        "dimensional_drive", () -> IMenuTypeExtension.create(DimensionalDriveMenu::new)
    );
}
