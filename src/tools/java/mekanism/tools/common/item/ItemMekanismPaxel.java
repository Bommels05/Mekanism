package mekanism.tools.common.item;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.annotation.ParametersAreNonnullByDefault;
import mekanism.tools.common.ToolsTags;
import mekanism.tools.common.material.IPaxelMaterial;
import mekanism.tools.common.material.MaterialCreator;
import mekanism.tools.common.material.VanillaPaxelMaterialCreator;
import mekanism.tools.common.util.ToolsUtils;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ToolAction;
import net.neoforged.neoforge.common.ToolActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ParametersAreNonnullByDefault
public class ItemMekanismPaxel extends DiggerItem {

    private static final ToolAction PAXEL_DIG = ToolAction.get("paxel_dig");
    private static final Set<ToolAction> PAXEL_ACTIONS = Util.make(Collections.newSetFromMap(new IdentityHashMap<>()), actions -> {
        actions.add(PAXEL_DIG);
        actions.addAll(ToolActions.DEFAULT_PICKAXE_ACTIONS);
        actions.addAll(ToolActions.DEFAULT_SHOVEL_ACTIONS);
        actions.addAll(ToolActions.DEFAULT_AXE_ACTIONS);
    });

    private final IPaxelMaterial material;

    public ItemMekanismPaxel(MaterialCreator material, Item.Properties properties) {
        super(material, ToolsTags.Blocks.MINEABLE_WITH_PAXEL, properties.
              attributes(createAttributes(material, material.getPaxelDamage(), material.getPaxelAtkSpeed())));
        this.material = material;
    }

    public ItemMekanismPaxel(VanillaPaxelMaterialCreator material, Item.Properties properties) {
        super(material.getVanillaTier(), ToolsTags.Blocks.MINEABLE_WITH_PAXEL, properties.
              attributes(createAttributes(material.getVanillaTier(), material.getPaxelDamage(), material.getPaxelAtkSpeed())));
        this.material = material;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        ToolsUtils.addDurability(tooltip, stack);
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        return PAXEL_ACTIONS.contains(action);
    }

    /**
     * {@inheritDoc}
     *
     * Merged version of {@link net.minecraft.world.item.AxeItem#useOn(UseOnContext)} and {@link net.minecraft.world.item.ShovelItem#useOn(UseOnContext)}
     */
    @NotNull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState blockstate = world.getBlockState(blockpos);
        BlockState resultToSet = useAsAxe(blockstate, context);
        if (resultToSet == null) {
            //We cannot strip the item that was right-clicked, so attempt to use the paxel as a shovel
            if (context.getClickedFace() == Direction.DOWN) {
                return InteractionResult.PASS;
            }
            BlockState foundResult = blockstate.getToolModifiedState(context, ToolActions.SHOVEL_FLATTEN, false);
            if (foundResult != null && world.isEmptyBlock(blockpos.above())) {
                //We can flatten the item as a shovel
                world.playSound(player, blockpos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
                resultToSet = foundResult;
            } else if (blockstate.getBlock() instanceof CampfireBlock && blockstate.getValue(CampfireBlock.LIT)) {
                //We can use the paxel as a shovel to extinguish a campfire
                if (!world.isClientSide) {
                    world.levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, blockpos, 0);
                }
                CampfireBlock.dowse(player, world, blockpos, blockstate);
                resultToSet = blockstate.setValue(CampfireBlock.LIT, false);
            }
            if (resultToSet == null) {
                return InteractionResult.PASS;
            }
        }
        if (!world.isClientSide) {
            ItemStack stack = context.getItemInHand();
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, blockpos, stack);
            }
            world.setBlock(blockpos, resultToSet, Block.UPDATE_ALL_IMMEDIATE);
            world.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(player, resultToSet));
            if (player != null) {
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
            }
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    @Nullable
    private BlockState useAsAxe(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState resultToSet = state.getToolModifiedState(context, ToolActions.AXE_STRIP, false);
        if (resultToSet != null) {
            world.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
            return resultToSet;
        }
        resultToSet = state.getToolModifiedState(context, ToolActions.AXE_SCRAPE, false);
        if (resultToSet != null) {
            world.playSound(player, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
            world.levelEvent(player, LevelEvent.PARTICLES_SCRAPE, pos, 0);
            return resultToSet;
        }
        resultToSet = state.getToolModifiedState(context, ToolActions.AXE_WAX_OFF, false);
        if (resultToSet != null) {
            world.playSound(player, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
            world.levelEvent(player, LevelEvent.PARTICLES_WAX_OFF, pos, 0);
            return resultToSet;
        }
        return null;
    }

    @Override
    public int getEnchantmentValue() {
        return material.getPaxelEnchantability();
    }
}