package me.alini.simplelocks.event;

import me.alini.simplelocks.item.lock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

public class LockBreakEventHandler {
    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (!(be instanceof Container container)) return;

        boolean hasAnyLock = false;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == lock.LOCK_ITEM.get()) {
                hasAnyLock = true;
                var tag = stack.getTag();
                if (tag != null && tag.hasUUID("Owner")) {
                    var owner = tag.getUUID("Owner");
                    if (owner.equals(player.getUUID()) || player.hasPermissions(4)) {
                        // 有一把属于自己或是OP，允许破坏
                        return;
                    }
                }
            }
        }
        if (hasAnyLock) {
            event.setCanceled(true);
            MinecraftServer server = player.getServer();
            if (server != null) {
                var pos = event.getPos();
                var level = (Level) event.getLevel();
                var dim = level.dimension().location();
                String msg = String.format(
                        "§c%s 试图破坏他人锁定的容器，疑似盗窃！§b位置: %s %d %d %d",
                        player.getName().getString(),
                        dim,
                        pos.getX(), pos.getY(), pos.getZ()
                );
                server.getPlayerList().broadcastSystemMessage(
                        Component.literal(msg),
                        false
                );
            }
        }
    }
    @SubscribeEvent
    public void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var menu = event.getContainer();

        // 防御性判断，防止 getType() 抛异常并静默处理
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