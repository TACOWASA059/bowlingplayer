package com.github.tacowasa059.bowlingplayergame.game;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Factory and identification helpers for the special game items. Items are plain
 * vanilla items tagged with an NBT marker so they can be recognised on use without
 * registering custom items (keeping the add-on data-pack free and loader agnostic).
 */
public final class GameItems {
    public static final String TAG_KEY = "BpgItem";

    public static final String ID_COBWEB = "cobweb";
    public static final String ID_DETECTOR = "detector";
    public static final String ID_SPEED = "speed";

    private GameItems() {
    }

    public static ItemStack steak(int count) {
        return new ItemStack(Items.COOKED_BEEF, count);
    }

    public static ItemStack cobweb(int count) {
        ItemStack stack = new ItemStack(Items.COBWEB, count);
        mark(stack, ID_COBWEB);
        stack.setHoverName(Component.literal("設置用クモの巣").withStyle(ChatFormatting.WHITE));
        return stack;
    }

    public static ItemStack detector() {
        ItemStack stack = new ItemStack(Items.ECHO_SHARD);
        mark(stack, ID_DETECTOR);
        stack.setHoverName(Component.literal("探知機").withStyle(ChatFormatting.GOLD));
        return stack;
    }

    public static ItemStack speed() {
        ItemStack stack = new ItemStack(Items.FEATHER);
        mark(stack, ID_SPEED);
        stack.setHoverName(Component.literal("加速の羽").withStyle(ChatFormatting.YELLOW));
        return stack;
    }

    private static void mark(ItemStack stack, String id) {
        stack.getOrCreateTag().putString(TAG_KEY, id);
    }

    /** Returns the game item id of the stack, or empty string if it is not a game item. */
    public static String idOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return "";
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_KEY)) {
            return "";
        }
        return tag.getString(TAG_KEY);
    }
}
