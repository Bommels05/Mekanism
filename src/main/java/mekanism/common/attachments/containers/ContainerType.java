package mekanism.common.attachments.containers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mekanism.api.DataHandlerUtils;
import mekanism.api.NBTConstants;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.api.chemical.infuse.IInfusionTank;
import mekanism.api.chemical.pigment.IPigmentTank;
import mekanism.api.chemical.slurry.ISlurryTank;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.energy.IStrictEnergyHandler;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.api.heat.IHeatCapacitor;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.Mekanism;
import mekanism.common.attachments.containers.chemical.gas.AttachedGases;
import mekanism.common.attachments.containers.chemical.gas.ComponentBackedGasHandler;
import mekanism.common.attachments.containers.chemical.infuse.AttachedInfuseTypes;
import mekanism.common.attachments.containers.chemical.infuse.ComponentBackedInfusionHandler;
import mekanism.common.attachments.containers.chemical.pigment.AttachedPigments;
import mekanism.common.attachments.containers.chemical.pigment.ComponentBackedPigmentHandler;
import mekanism.common.attachments.containers.chemical.slurry.AttachedSlurries;
import mekanism.common.attachments.containers.chemical.slurry.ComponentBackedSlurryHandler;
import mekanism.common.attachments.containers.creator.IContainerCreator;
import mekanism.common.attachments.containers.energy.AttachedEnergy;
import mekanism.common.attachments.containers.energy.ComponentBackedEnergyHandler;
import mekanism.common.attachments.containers.fluid.AttachedFluids;
import mekanism.common.attachments.containers.fluid.ComponentBackedFluidHandler;
import mekanism.common.attachments.containers.heat.AttachedHeat;
import mekanism.common.attachments.containers.heat.ComponentBackedHeatHandler;
import mekanism.common.attachments.containers.item.AttachedItems;
import mekanism.common.attachments.containers.item.ComponentBackedItemHandler;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.capabilities.IMultiTypeCapability;
import mekanism.common.config.IMekanismConfig;
import mekanism.common.integration.energy.EnergyCompatUtils;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.tile.base.TileEntityMekanism;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class ContainerType<CONTAINER extends INBTSerializable<CompoundTag>, ATTACHED extends IAttachedContainers<?, ATTACHED>,
      HANDLER extends ComponentBackedHandler<?, CONTAINER, ATTACHED>> {

    private static final List<ContainerType<?, ?, ?>> TYPES_INTERNAL = new ArrayList<>();
    public static final List<ContainerType<?, ?, ?>> TYPES = Collections.unmodifiableList(TYPES_INTERNAL);

    public static final ContainerType<IEnergyContainer, AttachedEnergy, ComponentBackedEnergyHandler> ENERGY = new ContainerType<>(MekanismDataComponents.ATTACHED_ENERGY,
          NBTConstants.ENERGY_CONTAINERS, NBTConstants.CONTAINER, ComponentBackedEnergyHandler::new, Capabilities.STRICT_ENERGY,
          TileEntityMekanism::getEnergyContainers, TileEntityMekanism::collectEnergyContainers, TileEntityMekanism::applyEnergyContainers, TileEntityMekanism::canHandleEnergy) {
        @Override
        @SuppressWarnings("unchecked")
        public void registerItemCapabilities(RegisterCapabilitiesEvent event, Item item, boolean exposeWhenStacked, IMekanismConfig... requiredConfigs) {
            EnergyCompatUtils.registerItemCapabilities(event, item, (ICapabilityProvider<ItemStack, Void, IStrictEnergyHandler>) getCapabilityProvider(exposeWhenStacked, requiredConfigs));
        }
    };
    public static final ContainerType<IInventorySlot, AttachedItems, ComponentBackedItemHandler> ITEM = new ContainerType<>(MekanismDataComponents.ATTACHED_ITEMS,
          NBTConstants.ITEMS, NBTConstants.SLOT, ComponentBackedItemHandler::new, Capabilities.ITEM, TileEntityMekanism::getInventorySlots,
          TileEntityMekanism::collectInventorySlots, TileEntityMekanism::applyInventorySlots, TileEntityMekanism::hasInventory);
    public static final ContainerType<IExtendedFluidTank, AttachedFluids, ComponentBackedFluidHandler> FLUID = new ContainerType<>(MekanismDataComponents.ATTACHED_FLUIDS,
          NBTConstants.FLUID_TANKS, NBTConstants.TANK, ComponentBackedFluidHandler::new, Capabilities.FLUID, TileEntityMekanism::getFluidTanks,
          TileEntityMekanism::collectFluidTanks, TileEntityMekanism::applyFluidTanks, TileEntityMekanism::canHandleFluid);
    public static final ContainerType<IGasTank, AttachedGases, ComponentBackedGasHandler> GAS = new ContainerType<>(MekanismDataComponents.ATTACHED_GASES,
          NBTConstants.GAS_TANKS, NBTConstants.TANK, ComponentBackedGasHandler::new, Capabilities.GAS, TileEntityMekanism::getGasTanks,
          TileEntityMekanism::collectGasTanks, TileEntityMekanism::applyGasTanks, TileEntityMekanism::canHandleGas);
    public static final ContainerType<IInfusionTank, AttachedInfuseTypes, ComponentBackedInfusionHandler> INFUSION = new ContainerType<>(
          MekanismDataComponents.ATTACHED_INFUSE_TYPES, NBTConstants.INFUSION_TANKS, NBTConstants.TANK, ComponentBackedInfusionHandler::new, Capabilities.INFUSION,
          TileEntityMekanism::getInfusionTanks, TileEntityMekanism::collectInfusionTanks, TileEntityMekanism::applyInfusionTanks, TileEntityMekanism::canHandleInfusion);
    public static final ContainerType<IPigmentTank, AttachedPigments, ComponentBackedPigmentHandler> PIGMENT = new ContainerType<>(MekanismDataComponents.ATTACHED_PIGMENTS,
          NBTConstants.PIGMENT_TANKS, NBTConstants.TANK, ComponentBackedPigmentHandler::new, Capabilities.PIGMENT,
          TileEntityMekanism::getPigmentTanks, TileEntityMekanism::collectPigmentTanks, TileEntityMekanism::applyPigmentTanks, TileEntityMekanism::canHandlePigment);
    public static final ContainerType<ISlurryTank, AttachedSlurries, ComponentBackedSlurryHandler> SLURRY = new ContainerType<>(MekanismDataComponents.ATTACHED_SLURRIES,
          NBTConstants.SLURRY_TANKS, NBTConstants.TANK, ComponentBackedSlurryHandler::new, Capabilities.SLURRY, TileEntityMekanism::getSlurryTanks,
          TileEntityMekanism::collectSlurryTanks, TileEntityMekanism::applySlurryTanks, TileEntityMekanism::canHandleSlurry);
    public static final ContainerType<IHeatCapacitor, AttachedHeat, ComponentBackedHeatHandler> HEAT = new ContainerType<>(MekanismDataComponents.ATTACHED_HEAT,
          NBTConstants.HEAT_CAPACITORS, NBTConstants.CONTAINER, ComponentBackedHeatHandler::new, null, TileEntityMekanism::getHeatCapacitors,
          TileEntityMekanism::collectHeatCapacitors, TileEntityMekanism::applyHeatCapacitors, TileEntityMekanism::canHandleHeat);

    //TODO - 1.20.5: Re-evaluate this codec implementation
    public static final Codec<ContainerType<?, ?, ?>> CODEC = BuiltInRegistries.DATA_COMPONENT_TYPE.byNameCodec().comapFlatMap(componentType -> {
        for (ContainerType<?, ?, ?> type : TYPES) {
            if (type.component.value() == componentType) {
                return DataResult.success(type);
            }
        }
        return DataResult.error(() -> "Data Component type " + BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(componentType) + " does not have a corresponding container type");
    }, containerType -> containerType.component.get());

    private final Map<Item, Lazy<? extends IContainerCreator<? extends CONTAINER, ATTACHED>>> knownDefaultCreators = new Reference2ObjectOpenHashMap<>();
    private final Function<ItemStack, HANDLER> handlerConstructor;
    private final BiFunction<TileEntityMekanism, @Nullable Direction, List<CONTAINER>> containersFromTile;
    private final CopyFromTile<CONTAINER, ATTACHED> copyFromTile;
    private final CopyToTile<CONTAINER, ATTACHED> copyToTile;
    private final DeferredHolder<DataComponentType<?>, DataComponentType<ATTACHED>> component;
    @Nullable
    private final IMultiTypeCapability<? super HANDLER, ?> capability;
    private final Predicate<TileEntityMekanism> canHandle;
    private final String containerTag;
    private final String containerKey;

    private ContainerType(DeferredHolder<DataComponentType<?>, DataComponentType<ATTACHED>> component, String containerTag, String containerKey,
          Function<ItemStack, HANDLER> handlerConstructor, @Nullable IMultiTypeCapability<? super HANDLER, ?> capability,
          BiFunction<TileEntityMekanism, @Nullable Direction, List<CONTAINER>> containersFromTile, CopyFromTile<CONTAINER, ATTACHED> copyFromTile,
          CopyToTile<CONTAINER, ATTACHED> copyToTile, Predicate<TileEntityMekanism> canHandle) {
        TYPES_INTERNAL.add(this);
        this.component = component;
        this.containerTag = containerTag;
        this.containerKey = containerKey;
        this.handlerConstructor = handlerConstructor;
        this.containersFromTile = containersFromTile;
        this.copyFromTile = copyFromTile;
        this.copyToTile = copyToTile;
        this.capability = capability;
        this.canHandle = canHandle;
    }

    public DeferredHolder<DataComponentType<?>, DataComponentType<ATTACHED>> getComponentType() {
        return component;
    }

    @Nullable
    public ResourceLocation getComponentName() {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.get());
    }

    public String getTag() {
        return containerTag;
    }

    /**
     * Adds some containers as default and exposes it as a capability that requires the given configs if the specified bus is present.
     */
    public void addDefaultCreators(@Nullable IEventBus eventBus, Item item, Supplier<? extends IContainerCreator<? extends CONTAINER, ATTACHED>> defaultCreator,
          IMekanismConfig... requiredConfigs) {
        knownDefaultCreators.put(item, Lazy.of(defaultCreator));
        if (eventBus != null && capability != null) {
            eventBus.addListener(RegisterCapabilitiesEvent.class, event -> registerItemCapabilities(event, item, false, requiredConfigs));
        }
    }

    public void registerItemCapabilities(RegisterCapabilitiesEvent event, Item item, boolean exposeWhenStacked, IMekanismConfig... requiredConfigs) {
        if (capability != null) {
            //TODO - 1.20.5: Re-evaluate this
            event.registerItem((ItemCapability) capability.item(), getCapabilityProvider(exposeWhenStacked, requiredConfigs), item);
        }
    }

    //TODO - 1.20.5: Do we want to have create in the name instead of get
    public List<CONTAINER> getAttachmentContainersIfPresent(ItemStack stack) {
        HANDLER handler = createHandlerIfData(stack);
        return handler == null ? Collections.emptyList() : handler.getContainers();
    }

    public int getContainerCount(ItemStack stack) {
        ATTACHED attached = stack.get(component);
        if (attached == null) {
            Lazy<? extends IContainerCreator<? extends CONTAINER, ATTACHED>> containerCreator = knownDefaultCreators.get(stack.getItem());
            return containerCreator == null ? 0 : containerCreator.get().totalContainers();
        }
        return attached.size();
    }

    @Nullable//TODO - 1.20.5: Re-evaluate
    public HANDLER createHandlerIfData(ItemStack stack) {
        ATTACHED attached = stack.get(component);
        return attached == null ? null : handlerConstructor.apply(stack);
    }

    @Nullable
    public HANDLER createHandler(ItemStack stack) {
        //TODO - 1.20.5: Do we want local callers to just directly access the handler constructor as we wouldn't be exposing the cap
        // if we didn't have any creators?
        ATTACHED attached = stack.get(component);
        if (attached == null) {
            Lazy<? extends IContainerCreator<? extends CONTAINER, ATTACHED>> lazy = knownDefaultCreators.get(stack.getItem());
            if (lazy == null) {
                return null;
            }
            IContainerCreator<? extends CONTAINER, ATTACHED> containerCreator = lazy.get();
            int count = containerCreator.totalContainers();
            if (count == 0) {
                return null;
            }
            attached = containerCreator.initStorage(count);
            stack.set(component, attached);
        }
        return handlerConstructor.apply(stack);
    }

    public CONTAINER createContainer(ItemStack attachedTo, int containerIndex) {
        Lazy<? extends IContainerCreator<? extends CONTAINER, ATTACHED>> creator = knownDefaultCreators.get(attachedTo.getItem());
        if (creator != null) {
            return creator.get().create(this, attachedTo, containerIndex);
        }
        throw new IllegalArgumentException("No known containers for item " + attachedTo.getItem());
    }

    protected ICapabilityProvider<ItemStack, Void, ? super HANDLER> getCapabilityProvider(boolean exposeWhenStacked, IMekanismConfig... requiredConfigs) {
        if (exposeWhenStacked) {
            return getCapabilityProvider(requiredConfigs);
        } else if (requiredConfigs.length == 0) {
            return (stack, context) -> stack.getCount() == 1 ? createHandler(stack) : null;
        }
        //Only expose the capabilities if the required configs are loaded
        return (stack, context) -> stack.getCount() == 1 && hasRequiredConfigs(requiredConfigs) ? createHandler(stack) : null;
    }

    protected ICapabilityProvider<ItemStack, Void, ? super HANDLER> getCapabilityProvider(IMekanismConfig... requiredConfigs) {
        if (requiredConfigs.length == 0) {
            return (stack, context) -> createHandler(stack);
        }
        //Only expose the capabilities if the required configs are loaded
        return (stack, context) -> hasRequiredConfigs(requiredConfigs) ? createHandler(stack) : null;
    }

    private static boolean hasRequiredConfigs(IMekanismConfig... requiredConfigs) {
        for (IMekanismConfig requiredConfig : requiredConfigs) {
            if (!requiredConfig.isLoaded()) {
                return false;
            }
        }
        return true;
    }

    public boolean supports(ItemStack stack) {
        return stack.has(component) || knownDefaultCreators.containsKey(stack.getItem());
    }

    public void addDefault(ItemLike item, DataComponentPatch.Builder builder) {
        Lazy<? extends IContainerCreator<? extends CONTAINER, ATTACHED>> lazy = knownDefaultCreators.get(item);
        if (lazy != null) {
            //Supports the type
            IContainerCreator<? extends CONTAINER, ATTACHED> containerCreator = lazy.get();
            int count = containerCreator.totalContainers();
            if (count > 0) {
                builder.set(component.get(), containerCreator.initStorage(count));
            }
        }
    }

    public static boolean anySupports(ItemLike itemLike) {
        for (ContainerType<?, ?, ?> type : TYPES) {
            if (type.knownDefaultCreators.containsKey(itemLike.asItem())) {
                return true;
            }
        }
        return false;
    }

    private ListTag save(HolderLookup.Provider provider, List<CONTAINER> containers) {
        return DataHandlerUtils.writeContents(provider, containers, containerKey);
    }

    private void read(HolderLookup.Provider provider, List<CONTAINER> containers, @Nullable ListTag storedContainers) {
        if (storedContainers != null) {
            DataHandlerUtils.readContents(provider, containers, storedContainers, containerKey);
        }
    }

    public void saveTo(HolderLookup.Provider provider, CompoundTag tag, TileEntityMekanism tile) {
        saveTo(provider, tag, getContainers(tile));
    }

    public void saveTo(HolderLookup.Provider provider, CompoundTag tag, List<CONTAINER> containers) {
        ListTag serialized = save(provider, containers);
        if (!serialized.isEmpty()) {
            tag.put(containerTag, serialized);
        }
    }

    public void readFrom(HolderLookup.Provider provider, CompoundTag tag, TileEntityMekanism tile) {
        readFrom(provider, tag, getContainers(tile));
    }

    public void readFrom(HolderLookup.Provider provider, CompoundTag tag, List<CONTAINER> containers) {
        read(provider, containers, tag.getList(containerTag, Tag.TAG_COMPOUND));
    }

    public void copyToStack(HolderLookup.Provider provider, List<CONTAINER> containers, ItemStack stack) {
        HANDLER handler = createHandler(stack);
        if (handler != null) {
            read(provider, handler.getContainers(), save(provider, containers));
            stack.set(component, handler.getAttached());
            if (stack.getCount() > 1) {
                Mekanism.logger.error("Copied {} to a stack ({}). This might lead to duplication of data.", getComponentName(), stack);
            }
        }
    }

    public void copyToTile(TileEntityMekanism tile, BlockEntity.DataComponentInput input) {
        ATTACHED attachedData = input.get(component);
        if (attachedData != null) {
            copyToTile.copy(tile, input, getContainers(tile), attachedData);
        }
    }

    public void copyFromStack(HolderLookup.Provider provider, ItemStack stack, List<CONTAINER> containers) {
        HANDLER handler = createHandler(stack);
        if (handler != null) {
            read(provider, containers, save(provider, handler.getContainers()));
        }
    }

    public void copyFromTile(TileEntityMekanism tile, DataComponentMap.Builder builder) {
        List<CONTAINER> containers = getContainers(tile);
        if (!containers.isEmpty()) {
            ATTACHED attachedData = copyFromTile.copy(tile, builder, containers);
            if (attachedData != null) {
                builder.set(component, attachedData);
            }
        }
    }

    public boolean canHandle(TileEntityMekanism tile) {
        return canHandle.test(tile);
    }

    public List<CONTAINER> getContainers(TileEntityMekanism tile) {
        return containersFromTile.apply(tile, null);
    }

    @FunctionalInterface
    public interface CopyToTile<CONTAINER extends INBTSerializable<CompoundTag>, ATTACHED extends IAttachedContainers<?, ATTACHED>> {

        void copy(TileEntityMekanism tile, BlockEntity.DataComponentInput input, List<CONTAINER> containers, ATTACHED attachedData);
    }

    @FunctionalInterface
    public interface CopyFromTile<CONTAINER extends INBTSerializable<CompoundTag>, ATTACHED extends IAttachedContainers<?, ATTACHED>> {

        @Nullable
        ATTACHED copy(TileEntityMekanism tile, DataComponentMap.Builder builder, List<CONTAINER> containers);
    }
}