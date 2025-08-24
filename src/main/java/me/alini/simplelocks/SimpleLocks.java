package me.alini.simplelocks;

import me.alini.simplelocks.event.LockBreakEventHandler;
import me.alini.simplelocks.item.lock;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(SimpleLocks.MODID)
public class SimpleLocks {
    public static final String MODID = "simplelocks";
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

    public SimpleLocks() {
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        lock.register(FMLJavaModLoadingContext.get().getModEventBus());
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::addCreative);
        LockBreakEventHandler.register();
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(lock.LOCK_ITEM);
        }
    }
}