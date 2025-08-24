package me.alini.simplelocks.item;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;

public class lock {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "simplelocks");
    // 用 createLockItem 方法注册
    public static final RegistryObject<Item> LOCK_ITEM = ITEMS.register("lock_item", lock::createLockItem);

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }

    public static Item createLockItem() {
        return new Item(new Item.Properties().stacksTo(1)) {
            @Override
            public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                ItemStack stack = player.getItemInHand(hand);
                if (!level.isClientSide && player.isShiftKeyDown()) {
                    CompoundTag tag = stack.getOrCreateTag();
                    if (!tag.contains("Owner")) {
                        tag.putUUID("Owner", player.getUUID());
                        tag.putString("OwnerName", player.getName().getString());
                        player.displayClientMessage(Component.literal("已绑定到 " + player.getName().getString()), true);
                    }
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }

            @Override
            @OnlyIn(Dist.CLIENT)
            public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("OwnerName")) {
                    tooltip.add(Component.literal("绑定玩家: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(tag.getString("OwnerName")).withStyle(ChatFormatting.GREEN)));
                    tooltip.add(Component.literal("绑定UUID: ")
                            .withStyle(ChatFormatting.GRAY)
                            .append(Component.literal(tag.getUUID("Owner").toString()).withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC)));
                } else {
                    tooltip.add(Component.literal("未绑定").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    tooltip.add(Component.literal("潜行 + 右键 可绑定")
                            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                }
            }
            @Override
            public net.minecraft.world.item.Rarity getRarity(ItemStack stack) {
                return net.minecraft.world.item.Rarity.EPIC;
            }
        };
    }
}