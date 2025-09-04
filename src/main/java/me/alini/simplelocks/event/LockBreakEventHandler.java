package me.alini.simplelocks.event;

import me.alini.simplelocks.item.lock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class LockBreakEventHandler {

    @SubscribeEvent(priority = EventPriority.HIGHEST) // 确保优先拦截破坏
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (be == null) return;

        List<ItemStack> allItems = getAllItems(be);

        boolean hasAnyLock = false;
        for (ItemStack stack : allItems) {
            if (!stack.isEmpty() && lock.LOCK_ITEM.isPresent() && stack.getItem() == lock.LOCK_ITEM.get()) {
                hasAnyLock = true;
                var tag = stack.getTag();
                if (tag != null && tag.hasUUID("Owner")) {
                    var owner = tag.getUUID("Owner");
                    if (owner.equals(player.getUUID()) || player.hasPermissions(4)) {
                        return; // 自己的锁 / 管理员允许破坏
                    }
                }
            }
        }

        if (hasAnyLock) {
            event.setCanceled(true);
            player.sendSystemMessage(Component.literal("§c此容器已上锁，无法破坏！"));

            MinecraftServer server = player.getServer();
            if (server != null) {
                var pos = event.getPos();
                var level = (Level) event.getLevel();
                var dim = level.dimension().location();
                String msg = String.format(
                        "§c%s 试图破坏他人锁定的容器！§b位置: %s %d %d %d",
                        player.getName().getString(),
                        dim,
                        pos.getX(), pos.getY(), pos.getZ()
                );
                server.getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
            }
        }
    }

    /**
     * 统一获取方块实体的物品：
     * - 原版 Container
     * - Forge Capability (Metal Barrels 等)
     * - 反射兜底
     */
    private List<ItemStack> getAllItems(BlockEntity be) {
        List<ItemStack> items = new ArrayList<>();

        // 1. 原版 Container
        if (be instanceof Container container) {
            for (int i = 0; i < container.getContainerSize(); i++) {
                items.add(container.getItem(i));
            }
        }

        // 2. Forge Capability (Metal Barrels 等)
        be.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            for (int i = 0; i < handler.getSlots(); i++) {
                items.add(handler.getStackInSlot(i));
            }
        });

        // 3. 反射兜底
        if (items.isEmpty()) {
            items.addAll(getItemsFromCustomBlockEntity(be));
        }

        return items;
    }

    // 反射兜底，防止有些奇怪的模组容器
    private List<ItemStack> getItemsFromCustomBlockEntity(BlockEntity be) {
        List<ItemStack> items = new ArrayList<>();
        try {
            var fields = be.getClass().getDeclaredFields();
            for (var field : fields) {
                field.setAccessible(true);
                Object value = field.get(be);
                if (value instanceof ItemStack stack) {
                    items.add(stack);
                } else if (value instanceof List<?> list) {
                    for (Object obj : list) {
                        if (obj instanceof ItemStack stack) {
                            items.add(stack);
                        }
                    }
                } else if (value instanceof ItemStackHandler handler) {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        items.add(handler.getStackInSlot(i));
                    }
                } else if (value instanceof IItemHandler handler) {
                    for (int i = 0; i < handler.getSlots(); i++) {
                        items.add(handler.getStackInSlot(i));
                    }
                }
            }
        } catch (Exception ignored) {}
        return items;
    }

    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var menu = event.getContainer();

        MenuType<?> type;
        try {
            type = menu.getType();
        } catch (RuntimeException ignored) {
            return;
        }
        if (type != MenuType.GENERIC_9x3) return;
        if (menu.slots.isEmpty()) return;

        try {
            if (player.getEnderChestInventory() != menu.slots.get(0).container) return;
        } catch (RuntimeException ignored) {
            return;
        }

        boolean returned = false;
        for (var slot : menu.slots) {
            if (slot.container != player.getEnderChestInventory()) continue;
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && lock.LOCK_ITEM.isPresent() && stack.getItem() == lock.LOCK_ITEM.get()) {
                ItemStack lockCopy = stack.copy();
                boolean added = player.getInventory().add(lockCopy);
                if (!added) {
                    player.drop(lockCopy, false);
                }
                slot.set(ItemStack.EMPTY);
                returned = true;
            }
        }
        if (returned) {
            player.displayClientMessage(Component.literal("§c锁无法放入末影箱，已返还给你！"), true);
        }
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new LockBreakEventHandler());
    }
}
