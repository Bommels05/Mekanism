package mekanism.common.block;

import mekanism.common.advancements.MekanismCriteriaTriggers;
import mekanism.common.block.interfaces.IHasTileEntity;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.IStateStorage;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.registries.MekanismAttachmentTypes;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismTileEntityTypes;
import mekanism.common.tile.TileEntityCardboardBox;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BlockCardboardBox extends BlockMekanism implements IStateStorage, IHasTileEntity<TileEntityCardboardBox> {

    public BlockCardboardBox() {
        super(BlockBehaviour.Properties.of().strength(0.5F, 0.6F).mapColor(MapColor.WOOD));
    }

    @NotNull
    @Override
    @Deprecated
    public InteractionResult use(@NotNull BlockState state, @NotNull Level world, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand,
          @NotNull BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        } else if (!canReplace(world, player, pos, state)) {
            return InteractionResult.FAIL;
        }
        if (!world.isClientSide) {
            TileEntityCardboardBox box = WorldUtils.getTileEntity(TileEntityCardboardBox.class, world, pos);
            if (box != null && box.hasData(MekanismAttachmentTypes.BLOCK_DATA)) {
                box.getData(MekanismAttachmentTypes.BLOCK_DATA).placeIntoWorld(world, pos);
                //TODO: Do we need to call setPlacedBy or not bother given we are setting the blockstate to what it was AND setting any tile data
                //adjustedState.getBlock().setPlacedBy(world, pos, data.blockState, player, new ItemStack(adjustedState.getBlock()));
                popResource(world, pos, MekanismBlocks.CARDBOARD_BOX.getItemStack());
                MekanismCriteriaTriggers.UNBOX_CARDBOARD_BOX.value().trigger((ServerPlayer) player);
            }
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    private static boolean canReplace(Level world, Player player, BlockPos pos, BlockState state) {
        //Check if the player is allowed to use the cardboard box in the given position
        if (world.mayInteract(player, pos)) {
            //If they are then check if they can "break" the cardboard block that is in that spot
            if (!NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(world, pos, state, player)).isCanceled()) {
                //If they can then we need to see if they are allowed to "place" the unboxed block in the given position
                //TODO: Once forge fixes https://github.com/MinecraftForge/MinecraftForge/issues/7609 use block snapshots
                // and fire a place event to see if the player is able to "place" the cardboard box
                return true;
            }
        }
        return false;
    }

    @NotNull
    @Override
    public ItemStack getCloneItemStack(@NotNull BlockState state, HitResult target, @NotNull LevelReader world, @NotNull BlockPos pos, Player player) {
        ItemStack itemStack = new ItemStack(this);
        TileEntityCardboardBox tile = WorldUtils.getTileEntity(TileEntityCardboardBox.class, world, pos);
        if (tile != null && tile.hasData(MekanismAttachmentTypes.BLOCK_DATA)) {
            itemStack.setData(MekanismAttachmentTypes.BLOCK_DATA, tile.getData(MekanismAttachmentTypes.BLOCK_DATA));
        }
        return itemStack;
    }

    @Override
    public void setPlacedBy(@NotNull Level world, @NotNull BlockPos pos, @NotNull BlockState state, @Nullable LivingEntity placer, @NotNull ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (stack.hasData(MekanismAttachmentTypes.BLOCK_DATA)) {
            TileEntityCardboardBox box = WorldUtils.getTileEntity(TileEntityCardboardBox.class, world, pos);
            if (box != null) {
                box.setData(MekanismAttachmentTypes.BLOCK_DATA, stack.getData(MekanismAttachmentTypes.BLOCK_DATA));
            }
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(@NotNull BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        if (state != null && context.getItemInHand().hasData(MekanismAttachmentTypes.BLOCK_DATA)) {
            return state.setValue(BlockStateHelper.storageProperty, true);
        }
        return state;
    }

    @Override
    public TileEntityTypeRegistryObject<TileEntityCardboardBox> getTileType() {
        return MekanismTileEntityTypes.CARDBOARD_BOX;
    }
}