package mekanism.common.block.machine;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import mekanism.api.IMekWrench;
import mekanism.api.Upgrade;
import mekanism.api.block.IBlockSound;
import mekanism.api.block.IHasInventory;
import mekanism.api.block.IHasModel;
import mekanism.api.block.IHasSecurity;
import mekanism.api.block.IHasTileEntity;
import mekanism.api.block.ISupportsComparator;
import mekanism.api.block.ISupportsRedstone;
import mekanism.api.block.ISupportsUpgrades;
import mekanism.common.Mekanism;
import mekanism.common.base.IActiveState;
import mekanism.common.block.BlockMekanismContainer;
import mekanism.common.block.interfaces.IHasGui;
import mekanism.common.block.states.BlockStateHelper;
import mekanism.common.block.states.IStateActive;
import mekanism.common.block.states.IStateFacing;
import mekanism.common.config.MekanismConfig;
import mekanism.common.integration.wrenches.Wrenches;
import mekanism.common.inventory.container.ContainerProvider;
import mekanism.common.inventory.container.tile.filter.list.LogisticalSorterContainer;
import mekanism.common.tile.TileEntityLogisticalSorter;
import mekanism.common.tile.base.MekanismTileEntityTypes;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MultipartUtils;
import mekanism.common.util.SecurityUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.particles.RedstoneParticleData;
import net.minecraft.state.DirectionProperty;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IEnviromentBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class BlockLogisticalSorter extends BlockMekanismContainer implements IHasModel, IHasGui<TileEntityLogisticalSorter>, ISupportsUpgrades, IStateFacing, IStateActive,
      IBlockSound, IHasInventory, ISupportsRedstone, IHasTileEntity<TileEntityLogisticalSorter>, ISupportsComparator, IHasSecurity {

    private static final SoundEvent SOUND_EVENT = new SoundEvent(new ResourceLocation(Mekanism.MODID, "tile.machine.logisticalsorter"));

    private static final VoxelShape[] bounds = new VoxelShape[6];

    static {
        VoxelShape sorter = MultipartUtils.combineAndSimplify(
              Block.makeCuboidShape(5, 5, 1, 11, 11, 15),//pipe
              Block.makeCuboidShape(3, 3, 14, 13, 13, 15),//connectorBack
              Block.makeCuboidShape(2, 2, 15, 14, 14, 16),//portBackLarge
              Block.makeCuboidShape(4, 4.5, 16, 12, 12, 17),//portBack
              Block.makeCuboidShape(3, 3, 0, 13, 13, 1),//portFront
              Block.makeCuboidShape(11, 6.5, 3.5, 12, 9.5, 11.5),//panel1
              Block.makeCuboidShape(4, 6.5, 3.5, 5, 9.5, 11.5),//panel2
              Block.makeCuboidShape(7, 3, 4, 9, 5, 5),//pistonConnector1
              Block.makeCuboidShape(7, 11, 4, 9, 13, 5),//pistonConnector2
              Block.makeCuboidShape(7, 3.5, 1, 9, 5.5, 4),//pistonBrace1
              Block.makeCuboidShape(7, 10.5, 1, 9, 12.5, 4),//pistonBrace2
              Block.makeCuboidShape(7, 3, 9.01, 9, 5, 14.01),//pistonBase1
              Block.makeCuboidShape(7, 11, 9.01, 9, 13, 14.01),//pistonBase2
              Block.makeCuboidShape(7.5, 3.5, 5.01, 8.5, 4.5, 10.01),//pistonBar1
              Block.makeCuboidShape(7.5, 11.5, 5.01, 8.5, 12.5, 10.01),//pistonBar2
              Block.makeCuboidShape(4.5, 4.5, 1, 11.5, 11.5, 2),//ring1
              Block.makeCuboidShape(4.5, 4.5, 3, 11.5, 11.5, 4),//ring2
              Block.makeCuboidShape(4.5, 4.5, 5, 11.5, 11.5, 6),//ring3
              Block.makeCuboidShape(4.5, 4.5, 7, 11.5, 11.5, 8),//ring4
              Block.makeCuboidShape(4.5, 4.5, 9, 11.5, 11.5, 10),//ring5
              Block.makeCuboidShape(4.5, 4.5, 11, 11.5, 11.5, 12),//ring6
              Block.makeCuboidShape(4.5, 4.5, 13, 11.5, 11.5, 14),//ring7
              Block.makeCuboidShape(11.5, 7.5, 6.5, 12.5, 8.5, 7.5),//led1
              Block.makeCuboidShape(11.5, 7.5, 4.5, 12.5, 8.5, 5.5),//led2
              Block.makeCuboidShape(3.5, 7.5, 4.5, 4.5, 8.5, 5.5),//led3
              Block.makeCuboidShape(3.5, 7.5, 6.5, 4.5, 8.5, 7.5)//led4
        );
        sorter = MultipartUtils.rotate(sorter, Direction.NORTH);
        for (Direction side : EnumUtils.DIRECTIONS) {
            bounds[side.ordinal()] = MultipartUtils.rotate(sorter, side.getOpposite());
        }
    }

    public BlockLogisticalSorter() {
        super(Block.Properties.create(Material.IRON).hardnessAndResistance(3.5F, 16F));
    }

    @Nonnull
    @Override
    public DirectionProperty getFacingProperty() {
        return BlockStateHelper.facingProperty;
    }

    //TODO: updatePostPlacement?? for rotating to a block if not attached to any container yet

    @Override
    public void setTileData(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack, @Nonnull TileEntityMekanism tile) {
        if (tile instanceof TileEntityLogisticalSorter) {
            TileEntityLogisticalSorter transporter = (TileEntityLogisticalSorter) tile;
            if (!transporter.hasConnectedInventory()) {
                BlockPos tilePos = tile.getPos();
                for (Direction dir : EnumUtils.DIRECTIONS) {
                    TileEntity tileEntity = MekanismUtils.getTileEntity(world, tilePos.offset(dir));
                    if (InventoryUtils.isItemHandler(tileEntity, dir)) {
                        transporter.setFacing(dir.getOpposite());
                        break;
                    }
                }
            }
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState state, World world, BlockPos pos, Random random) {
        TileEntityMekanism tileEntity = MekanismUtils.getTileEntity(TileEntityMekanism.class, world, pos);
        if (tileEntity != null && MekanismUtils.isActive(world, pos) && ((IActiveState) tileEntity).renderUpdate() && MekanismConfig.client.machineEffects.get()) {
            float xRandom = (float) pos.getX() + 0.5F;
            float yRandom = (float) pos.getY() + 0.0F + random.nextFloat() * 6.0F / 16.0F;
            float zRandom = (float) pos.getZ() + 0.5F;
            float iRandom = 0.52F;
            float jRandom = random.nextFloat() * 0.6F - 0.3F;
            Direction side = tileEntity.getDirection();

            switch (side) {
                case WEST:
                    world.addParticle(ParticleTypes.SMOKE, xRandom - iRandom, yRandom, zRandom + jRandom, 0.0D, 0.0D, 0.0D);
                    world.addParticle(RedstoneParticleData.REDSTONE_DUST, xRandom - iRandom, yRandom, zRandom + jRandom, 0.0D, 0.0D, 0.0D);
                    break;
                case EAST:
                    world.addParticle(ParticleTypes.SMOKE, xRandom + iRandom, yRandom, zRandom + jRandom, 0.0D, 0.0D, 0.0D);
                    world.addParticle(RedstoneParticleData.REDSTONE_DUST, xRandom + iRandom, yRandom, zRandom + jRandom, 0.0D, 0.0D, 0.0D);
                    break;
                case NORTH:
                    world.addParticle(ParticleTypes.SMOKE, xRandom + jRandom, yRandom, zRandom - iRandom, 0.0D, 0.0D, 0.0D);
                    world.addParticle(RedstoneParticleData.REDSTONE_DUST, xRandom + jRandom, yRandom, zRandom - iRandom, 0.0D, 0.0D, 0.0D);
                    break;
                case SOUTH:
                    world.addParticle(ParticleTypes.SMOKE, xRandom + jRandom, yRandom, zRandom + iRandom, 0.0D, 0.0D, 0.0D);
                    world.addParticle(RedstoneParticleData.REDSTONE_DUST, xRandom + jRandom, yRandom, zRandom + iRandom, 0.0D, 0.0D, 0.0D);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public int getLightValue(BlockState state, IEnviromentBlockReader world, BlockPos pos) {
        if (MekanismConfig.client.enableAmbientLighting.get()) {
            TileEntity tileEntity = MekanismUtils.getTileEntity(world, pos);
            if (tileEntity instanceof IActiveState && ((IActiveState) tileEntity).lightUpdate() && ((IActiveState) tileEntity).wasActiveRecently()) {
                return MekanismConfig.client.ambientLightingLevel.get();
            }
        }
        return 0;
    }

    @Override
    public boolean onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
        if (world.isRemote) {
            return true;
        }
        //TODO: Make this be moved into the logistical sorter tile
        TileEntityLogisticalSorter tileEntity = MekanismUtils.getTileEntity(TileEntityLogisticalSorter.class, world, pos);
        if (tileEntity == null) {
            return false;
        }
        ItemStack stack = player.getHeldItem(hand);
        if (!stack.isEmpty()) {
            IMekWrench wrenchHandler = Wrenches.getHandler(stack);
            if (wrenchHandler != null) {
                if (wrenchHandler.canUseWrench(stack, player, hit.getPos())) {
                    if (SecurityUtils.canAccess(player, tileEntity)) {
                        if (player.isSneaking()) {
                            MekanismUtils.dismantleBlock(state, world, pos);
                            return true;
                        }
                        Direction change = tileEntity.getDirection().rotateY();
                        if (!tileEntity.hasConnectedInventory()) {
                            for (Direction dir : EnumUtils.DIRECTIONS) {
                                TileEntity tile = MekanismUtils.getTileEntity(world, pos.offset(dir));
                                if (InventoryUtils.isItemHandler(tile, dir)) {
                                    change = dir.getOpposite();
                                    break;
                                }
                            }
                        }
                        tileEntity.setFacing(change);
                        world.notifyNeighborsOfStateChange(pos, this);
                    } else {
                        SecurityUtils.displayNoAccess(player);
                    }
                    return true;
                }
            }
        }
        return tileEntity.openGui(player);
    }

    @OnlyIn(Dist.CLIENT)
    @Nonnull
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    @Deprecated
    public float getPlayerRelativeBlockHardness(BlockState state, @Nonnull PlayerEntity player, @Nonnull IBlockReader world, @Nonnull BlockPos pos) {
        return SecurityUtils.canAccess(player, MekanismUtils.getTileEntity(world, pos)) ? super.getPlayerRelativeBlockHardness(state, player, world, pos) : 0.0F;
    }

    @Override
    public float getExplosionResistance(BlockState state, IWorldReader world, BlockPos pos, @Nullable Entity exploder, Explosion explosion) {
        //TODO: This is how it was before, but should it be divided by 5 like in Block.java
        return blockResistance;
    }

    @Override
    @Deprecated
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean isMoving) {
        if (!world.isRemote) {
            TileEntity tileEntity = MekanismUtils.getTileEntity(world, pos);
            if (tileEntity instanceof TileEntityMekanism) {
                ((TileEntityMekanism) tileEntity).onNeighborChange(neighborBlock);
            }
            if (tileEntity instanceof TileEntityLogisticalSorter) {
                TileEntityLogisticalSorter sorter = (TileEntityLogisticalSorter) tileEntity;
                if (!sorter.hasConnectedInventory()) {
                    for (Direction dir : EnumUtils.DIRECTIONS) {
                        TileEntity tile = MekanismUtils.getTileEntity(world, pos.offset(dir));
                        if (InventoryUtils.isItemHandler(tile, dir)) {
                            sorter.setFacing(dir.getOpposite());
                            return;
                        }
                    }
                }
            }
        }
    }

    @Nonnull
    @Override
    @Deprecated
    public VoxelShape getShape(BlockState state, IBlockReader world, BlockPos pos, ISelectionContext context) {
        return bounds[getDirection(state).ordinal()];
        //return VoxelShapes.create(MultipartUtils.rotate(LOGISTICAL_SORTER_BOUNDS.offset(-0.5, -0.5, -0.5), getDirection(state)).offset(0.5, 0.5, 0.5));
    }

    @Nonnull
    @Override
    public SoundEvent getSoundEvent() {
        return SOUND_EVENT;
    }

    @Override
    public INamedContainerProvider getProvider(TileEntityLogisticalSorter tile) {
        return new ContainerProvider("mekanism.container.logistical_sorter", (i, inv, player) -> new LogisticalSorterContainer(i, inv, tile));
    }

    @Override
    public TileEntityType<TileEntityLogisticalSorter> getTileType() {
        return MekanismTileEntityTypes.LOGISTICAL_SORTER.getTileEntityType();
    }

    @Nonnull
    @Override
    public Set<Upgrade> getSupportedUpgrade() {
        return EnumSet.of(Upgrade.MUFFLING);
    }
}