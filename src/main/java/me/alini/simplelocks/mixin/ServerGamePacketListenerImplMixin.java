// src/main/java/me/alini/simplelocks/mixin/ServerGamePacketListenerImplMixin.java
package me.alini.simplelocks.mixin;

import me.alini.simplelocks.item.lock;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.player.Inventory;


@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

//    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
//    private void preventLockInEnderChest(ServerboundContainerClickPacket packet, CallbackInfo ci) {
//        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).player;
//        AbstractContainerMenu menu = player.containerMenu;
//        if (menu == null) return;
//        if (player.hasPermissions(4)) return; // OP 不受限制
//        if (menu.getType() == MenuType.GENERIC_9x3
//                && !menu.slots.isEmpty()
//                && player.getEnderChestInventory() == menu.slots.get(0).container) {
//            int slot = packet.getSlotNum();
//            if (slot >= 0 && slot < menu.slots.size()) {
//                ItemStack stack = menu.getSlot(slot).getItem();
//                if (!stack.isEmpty() && lock.LOCK_ITEM.isPresent() && stack.getItem() == lock.LOCK_ITEM.get()) {
//                    player.displayClientMessage(Component.literal("§c锁无法放入末影箱！"), true);
//                    ci.cancel();
//                }
//            }
//        }
//    }

    @Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
    private void checkPermissions(ServerboundContainerClickPacket packet, CallbackInfo ci) {
        ServerPlayer player = ((ServerGamePacketListenerImpl) (Object) this).player;
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == null) return;

        // 跳过玩家背包、创造背包、合成台、slots 为空的菜单
        if (menu instanceof net.minecraft.world.inventory.InventoryMenu
                || menu.slots.isEmpty()
                || menu instanceof net.minecraft.world.inventory.CraftingMenu) {
            return;
        }


        // 只检查容器本身的槽位
        var container = menu.getSlot(0).container;
        boolean hasMyLock = false;
        boolean hasAnyLock = false;
        if (lock.LOCK_ITEM.isPresent()) {
            for (var slot : menu.slots) {
                if (slot.container != container) continue;
                var stack = slot.getItem();
                if (!stack.isEmpty() && stack.getItem() == lock.LOCK_ITEM.get()) {
                    var tag = stack.getTag();
                    if (tag != null && tag.hasUUID("Owner")) {
                        hasAnyLock = true;
                        if (tag.getUUID("Owner").equals(player.getUUID())) {
                            hasMyLock = true;
                            break;
                        }
                    }
                }
            }
        }

        if (hasAnyLock && !hasMyLock && !player.hasPermissions(4)) {
            player.displayClientMessage(Component.literal("§c你没有权限操作容器里的物品！"), true);
            ci.cancel();
        }
    }
}