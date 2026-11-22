package larynx.dev.aerostar_redux.item;

import larynx.dev.aerostar_redux.block.AeroStarCompBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class AeroStarCompCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "aerostar_redux");
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register(
        "main",
        () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.aerostar_redux"))
                .icon(() -> new ItemStack(AeroStarCompBlocks.DIMENSIONAL_DRIVE.get()))
                .displayItems((params, output) -> AeroStarCompItems.ITEMS.getEntries().forEach(holder -> output.accept(holder.get())))
                .build()
    );
}
