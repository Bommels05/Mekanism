package mekanism.common.integration.computer;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Function;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.math.FloatingLong;
import mekanism.api.security.SecurityMode;
import mekanism.api.text.EnumColor;
import mekanism.common.content.filter.FilterType;
import mekanism.common.content.filter.IFilter;
import mekanism.common.content.filter.IItemStackFilter;
import mekanism.common.content.filter.IModIDFilter;
import mekanism.common.content.filter.ITagFilter;
import mekanism.common.content.miner.MinerFilter;
import mekanism.common.content.miner.MinerItemStackFilter;
import mekanism.common.content.miner.MinerModIDFilter;
import mekanism.common.content.miner.MinerTagFilter;
import mekanism.common.content.oredictionificator.OredictionificatorFilter;
import mekanism.common.content.oredictionificator.OredictionificatorItemFilter;
import mekanism.common.content.qio.filter.QIOFilter;
import mekanism.common.content.qio.filter.QIOItemStackFilter;
import mekanism.common.content.qio.filter.QIOModIDFilter;
import mekanism.common.content.qio.filter.QIOTagFilter;
import mekanism.common.content.transporter.SorterFilter;
import mekanism.common.content.transporter.SorterItemStackFilter;
import mekanism.common.content.transporter.SorterModIDFilter;
import mekanism.common.content.transporter.SorterTagFilter;
import mekanism.common.integration.computer.TableType.Builder;
import mekanism.common.lib.frequency.Frequency;
import mekanism.common.util.RegistryUtils;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.common.util.Lazy;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides methods to get parameters from a computer integration and return converted values back. NB: new conversions should have an entry added to
 * {@link #convertType(Class)}
 *
 * getX methods may throw an exception if the param index does not exist or param is the wrong type. convert methods should not wrap results, as they will be used to
 * convert lists/maps
 */
public abstract class BaseComputerHelper {

    public static final Lazy<Map<Class<?>, TableType>> BUILTIN_TABLES = Lazy.of(BaseComputerHelper::getBuiltInTables);

    @NotNull
    private <T> T requireNonNull(int param, @Nullable T value) throws ComputerException {
        if (value == null) {
            throw new ComputerException("Invalid parameter at index " + param);
        }
        return value;
    }

    /**
     * Get an enum by string value
     *
     * @param param     param index
     * @param enumClazz Enum class
     *
     * @return the enum value
     *
     * @throws ComputerException if the param index does not exist, enum value doesn't exist or param is the wrong type.
     */
    @NotNull
    public <T extends Enum<T>> T getEnum(int param, Class<T> enumClazz) throws ComputerException {
        return requireNonNull(param, SpecialConverters.sanitizeStringToEnum(enumClazz, getString(param)));
    }

    public abstract boolean getBoolean(int param) throws ComputerException;

    public abstract byte getByte(int param) throws ComputerException;

    public abstract short getShort(int param) throws ComputerException;

    public abstract int getInt(int param) throws ComputerException;

    public abstract long getLong(int param) throws ComputerException;

    public abstract char getChar(int param) throws ComputerException;

    public abstract float getFloat(int param) throws ComputerException;

    public abstract double getDouble(int param) throws ComputerException;

    /**
     * Get a Floating Long from a positive double value (finite if supported by computer platform)
     *
     * @param param parameter index
     *
     * @return constant Floating Long or FloatingLong.ZERO
     *
     * @throws ComputerException if the param index does not exist or param is the wrong type.
     */
    public FloatingLong getFloatingLong(int param) throws ComputerException {
        double finiteDouble = getDouble(param);
        if (finiteDouble < 0) {
            return FloatingLong.ZERO;
        }
        return FloatingLong.createConst(finiteDouble);
    }

    @NotNull
    public abstract String getString(int param) throws ComputerException;

    @NotNull
    public abstract Map<?, ?> getMap(int param) throws ComputerException;

    /**
     * Convert a Map to an IFilter instance of the expected type
     *
     * @param param        param index
     * @param expectedType expected filter class (usually parent)
     *
     * @return the constructed filter, or null if conversion was invalid
     *
     * @throws ComputerException if the param index does not exist or param is the wrong type. (from getMap)
     */
    @Nullable
    public <FILTER extends IFilter<FILTER>> FILTER getFilter(int param, Class<FILTER> expectedType) throws ComputerException {
        return SpecialConverters.convertMapToFilter(expectedType, getMap(param));
    }

    /**
     * @param param param index
     *
     * @return ResourceLocation parsed from String or null
     *
     * @throws ComputerException if the param index does not exist or param is the wrong type.
     */
    @NotNull
    public ResourceLocation getResourceLocation(int param) throws ComputerException {
        return requireNonNull(param, ResourceLocation.tryParse(getString(param)));
    }

    /**
     * Get an Item instance from the registry by Resource Location (string)
     *
     * @param param param index
     *
     * @return Item instance or {@link Items#AIR} if item not found
     *
     * @throws ComputerException if the param index does not exist or param is the wrong type.
     */
    public Item getItem(int param) throws ComputerException {
        ResourceLocation itemName = getResourceLocation(param);
        return getItemFromResourceLocation(itemName);
    }

    @NotNull
    private static Item getItemFromResourceLocation(ResourceLocation itemName) {
        if (itemName == null) {
            return Items.AIR;
        }
        return BuiltInRegistries.ITEM.get(itemName);
    }

    public ItemStack getItemStack(int param) throws ComputerException {
        Map<?, ?> map = getMap(param);
        try {
            Item item = getItemFromResourceLocation(ResourceLocation.tryParse((String) map.get("name")));
            int count = SpecialConverters.getIntFromRaw(map.get("count"));
            String components = (String) map.get("components");
            if (components != null) {
                try {
                    DataComponentPatch dataComponents = DataComponentPatch.CODEC.decode(NbtOps.INSTANCE, NbtUtils.snbtToStructure(components))
                          .getOrThrow(ComputerException::new).getFirst();
                    return new ItemStack(item.builtInRegistryHolder(), count, dataComponents);
                } catch (CommandSyntaxException ex) {
                    throw new ComputerException("Invalid SNBT: " + ex.getMessage());
                }
            }
            return new ItemStack(item, count);
        } catch (ClassCastException ex) {
            throw new ComputerException("Invalid ItemStack at index " + param);
        }
    }

    /**
     * Signals that the method did not return a result (i.e. is void)
     *
     * @return Computer platform dependent.
     */
    public Object voidResult() {
        return null;
    }

    public Object convert(@Nullable FloatingLong result) {
        if (result == null) {
            return 0;
        } else if (result.getDecimal() == 0 && result.getValue() >= 0) {
            return result.longValue();
        }
        return result.doubleValue();
    }

    public Object convert(int i) {
        return i;
    }

    public Object convert(long i) {
        return i;
    }

    public Object convert(double d) {
        return d;
    }

    public Object convert(String s) {
        return s;
    }

    public Object convert(boolean b) {
        return b;
    }

    public <T> Object convert(@Nullable Collection<T> list, @NotNull Function<T, Object> converter) {
        if (list == null) {
            return Collections.emptyList();
        }
        List<Object> converted = new ArrayList<>(list.size());
        for (T el : list) {
            converted.add(converter.apply(el));
        }
        return converted;
    }

    public Object convert(@Nullable ResourceLocation rl) {
        return rl == null ? null : rl.toString();
    }

    public Object convert(@Nullable UUID uuid) {
        return uuid == null ? null : uuid.toString();
    }

    public Object convert(@Nullable ChemicalStack<?> stack) {
        if (stack == null) {
            return null;
        }
        Map<String, Object> wrapped = new HashMap<>(2);
        wrapped.put("name", convert(stack.getTypeRegistryName()));
        wrapped.put("amount", stack.getAmount());
        return wrapped;
    }

    public Object convert(@Nullable FluidStack stack) {
        if (stack == null) {
            return null;
        }
        return SpecialConverters.wrapStack(RegistryUtils.getName(stack.getFluid()), "amount", stack.getAmount(), stack.getComponentsPatch());
    }

    public Object convert(@Nullable ItemStack stack) {
        if (stack == null) {
            return null;
        }
        return SpecialConverters.wrapStack(RegistryUtils.getName(stack.getItem()), "count", stack.getCount(), stack.getComponentsPatch());
    }

    public Object convert(@Nullable BlockState state) {
        if (state == null) {
            return null;
        }

        Map<String, Object> wrapped = new HashMap<>(2);
        ResourceLocation name = RegistryUtils.getName(state.getBlock());
        if (name != null) {
            wrapped.put("block", convert(name));
        }
        Map<String, Object> stateData = new HashMap<>();
        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
            Property<?> property = entry.getKey();
            Object value = entry.getValue();
            if (!(property instanceof IntegerProperty) && !(property instanceof BooleanProperty)) {
                value = Util.getPropertyName(property, value);
            }
            stateData.put(property.getName(), value);
        }
        if (!stateData.isEmpty()) {
            wrapped.put("state", stateData);
        }
        return wrapped;
    }

    public Object convert(@Nullable Vec3i pos) {
        if (pos == null) {
            return null;
        }
        //BlockPos is covered by this case
        Map<String, Object> wrapped = new HashMap<>(3);
        wrapped.put("x", pos.getX());
        wrapped.put("y", pos.getY());
        wrapped.put("z", pos.getZ());
        return wrapped;
    }

    public Object convert(@Nullable GlobalPos globalPos) {
        if (globalPos == null) {
            return null;
        }
        Map<String, Object> wrapped = new HashMap<>(4);
        wrapped.put("x", globalPos.pos().getX());
        wrapped.put("y", globalPos.pos().getY());
        wrapped.put("z", globalPos.pos().getZ());
        wrapped.put("dimension", convert(globalPos.dimension().location()));
        return wrapped;
    }

    public Object convert(@Nullable Frequency frequency) {
        if (frequency == null) {
            return null;
        }
        Frequency.FrequencyIdentity identity = frequency.getIdentity();
        Map<String, Object> wrapped = new HashMap<>(2);
        wrapped.put("key", identity.key().toString());
        wrapped.put("security", convert(identity.securityMode()));
        return wrapped;
    }

    public Object convert(@Nullable Enum<?> res) {
        return res == null ? null : res.name();
    }

    protected Map<String, Object> convertFilterCommon(IFilter<?> result) {
        Map<String, Object> wrapped = new HashMap<>();
        wrapped.put("type", convert(result.getFilterType()));
        wrapped.put("enabled", result.isEnabled());
        switch (result) {
            case IItemStackFilter<?> itemFilter -> {
                ItemStack stack = itemFilter.getItemStack();
                wrapped.put("item", convert(stack.getItem()));
                if (!stack.isEmpty()) {
                    DataComponentPatch components = stack.getComponentsPatch();
                    if (!components.isEmpty()) {
                        wrapped.put("itemComponents", SpecialConverters.wrapComponents(components));
                    }
                }
            }
            case IModIDFilter<?> modIDFilter -> wrapped.put("modId", modIDFilter.getModID());
            case ITagFilter<?> tagFilter -> wrapped.put("tag", tagFilter.getTagName());
            default -> {
            }
        }
        return wrapped;
    }

    public Object convert(@Nullable MinerFilter<?> minerFilter) {
        if (minerFilter == null) {
            return null;
        }
        Map<String, Object> wrapped = convertFilterCommon(minerFilter);
        wrapped.put("requiresReplacement", minerFilter.requiresReplacement);
        wrapped.put("replaceTarget", convert(minerFilter.replaceTarget));
        return wrapped;
    }

    public Object convert(@Nullable SorterFilter<?> sorterFilter) {
        if (sorterFilter == null) {
            return null;
        }
        Map<String, Object> wrapped = convertFilterCommon(sorterFilter);
        wrapped.put("allowDefault", sorterFilter.allowDefault);
        wrapped.put("color", convert(sorterFilter.color));
        wrapped.put("size", sorterFilter.sizeMode);
        wrapped.put("min", sorterFilter.min);
        wrapped.put("max", sorterFilter.max);
        if (sorterFilter instanceof SorterItemStackFilter filter) {
            wrapped.put("fuzzy", filter.fuzzyMode);
        }
        return wrapped;
    }

    public Object convert(@Nullable QIOFilter<?> qioFilter) {
        if (qioFilter == null) {
            return null;
        }
        Map<String, Object> wrapped = convertFilterCommon(qioFilter);
        if (qioFilter instanceof QIOItemStackFilter filter) {
            wrapped.put("fuzzy", filter.fuzzyMode);
        }
        return wrapped;
    }

    public Object convert(@Nullable OredictionificatorFilter<?, ?, ?> filter) {
        if (filter == null) {
            return null;
        }
        Map<String, Object> wrapped = convertFilterCommon(filter);
        wrapped.put("target", filter.getFilterText());
        if (filter instanceof OredictionificatorItemFilter itemFilter) {
            wrapped.put("selected", convert(itemFilter.getResultElement()));
        }
        return wrapped;
    }

    public <KEY, VALUE> Object convert(@NotNull Map<KEY, VALUE> res, Function<KEY, Object> keyConverter, @NotNull Function<VALUE, Object> valueConverter) {
        Map<Object, Object> map = new HashMap<>(res.size());
        for (Entry<KEY, VALUE> entry : res.entrySet()) {
            map.put(keyConverter.apply(entry.getKey()), valueConverter.apply(entry.getValue()));
        }
        return map;
    }

    public Object convert(@Nullable Item item) {
        if (item == null) {
            return null;
        }
        return convert(RegistryUtils.getName(item));
    }

    public Object convert(@Nullable Convertable<?> convertable) {
        if (convertable == null) {
            return null;
        }
        return convertable.convert(this);
    }

    public Object convert(@Nullable MethodHelpData methodHelpData) {
        if (methodHelpData == null) {
            return null;
        }
        Map<String, Object> helpData = new HashMap<>();
        helpData.put("name", methodHelpData.methodName());
        if (methodHelpData.params() != null) {
            helpData.put("params", methodHelpData.params().stream().map(p -> {
                Map<String, Object> arg = new HashMap<>();
                arg.put("name", p.name());
                arg.put("type", p.type());
                if (p.values() != null) {
                    arg.put("values", p.values());
                }
                return arg;
            }).toList());
        }

        Map<String, Object> returns = new HashMap<>();
        returns.put("type", methodHelpData.returns().type());
        if (methodHelpData.returns().values() != null) {
            returns.put("values", methodHelpData.returns().values());
        }
        helpData.put("returns", returns);

        if (methodHelpData.description() != null) {
            helpData.put("description", methodHelpData.description());
        }
        return helpData;
    }

    /**
     * Convert a type to the converted version (what is exposed to the computer). Used on OpenComputers2
     *
     * @param clazz the unconverted type
     *
     * @return the converted type, or clazz if no conversion needed
     */
    public static Class<?> convertType(Class<?> clazz) {
        if (clazz == UUID.class || clazz == ResourceLocation.class || clazz == Item.class || Enum.class.isAssignableFrom(clazz)) {
            return String.class;
        }
        if (clazz == Frequency.class || clazz == GlobalPos.class || clazz == Vec3i.class || clazz == FluidStack.class || clazz == ItemStack.class || clazz == BlockState.class) {
            return Map.class;
        }
        if (ChemicalStack.class.isAssignableFrom(clazz) || IFilter.class.isAssignableFrom(clazz)) {
            return Map.class;
        }
        if (clazz == Convertable.class) {
            return Map.class;//technically can be anything, but so far only map used
        }
        return clazz;
    }

    private static Map<Class<?>, TableType> getBuiltInTables() {
        Map<Class<?>, TableType> types = new HashMap<>();

        TableType.builder(GlobalPos.class, "An xyz position with a dimension component")
              .addField("x", int.class, "The x component")
              .addField("y", int.class, "The y component")
              .addField("z", int.class, "The z component")
              .addField("dimension", ResourceLocation.class, "The dimension component")
              .build(types);

        TableType.builder(BlockPos.class, "An xyz position")
              .addField("x", int.class, "The x component")
              .addField("y", int.class, "The y component")
              .addField("z", int.class, "The z component")
              .build(types);

        TableType.builder(ItemStack.class, "A stack of Item(s)")
              .addField("name", Item.class, "The Item's registered name")
              .addField("count", int.class, "The count of items in the stack")
              .addField("components", String.class, "Any non default components of the item, in Command JSON format")
              .build(types);

        TableType.builder(FluidStack.class, "An amount of fluid")
              .addField("name", ResourceLocation.class, "The Fluid's registered name, e.g. minecraft:water")
              .addField("amount", int.class, "The amount in mB")
              .addField("components", String.class, "Any non default components of the fluid, in Command JSON format")
              .build(types);

        TableType.builder(ChemicalStack.class, "An amount of Gas/Fluid/Slurry/Pigment")
              .addField("name", Item.class, "The Chemical's registered name")
              .addField("amount", int.class, "The amount in mB")
              .build(types);

        TableType.builder(BlockState.class, "A Block State")
              .addField("block", String.class, "The Block's registered name, e.g. minecraft:sand")
              .addField("state", Map.class, "Any state parameters will be in Table format under this key. Not present if there are none")
              .build(types);

        TableType.builder(Frequency.class, "A frequency's identity")
              .addField("key", String.class, "Usually the name of the frequency entered in the GUI")
              .addField("security", SecurityMode.class, "Whether the Frequency is public, trusted, or private")
              .build(types);

        TableType.builder(IFilter.class, "Common Filter properties. Use the API Global to make constructing these a little easier.\nFilters are a combination of these base properties, an ItemStack or Mod Id or Tag component, and a device specific type.\nThe exception to that is an Oredictionificator filter, which does not have an item/mod/tag component.")
              .addField("type", FilterType.class, "The type of filter in this structure")
              .addField("enabled", boolean.class, "Whether the filter is enabled when added to a device")
              .build(types);

        TableType.builder(MinerFilter.class, "A Digital Miner filter")
              .extendedFrom(IFilter.class)
              .addField("requiresReplacement", boolean.class, "Whether the filter requires a replacement to be done before it will allow mining")
              .addField("replaceTarget", Item.class, "The name of the item block that will be used to replace a mined block")
              .build(types);

        TableType.builder(OredictionificatorItemFilter.class, "An Oredictionificator filter")
              .extendedFrom(IFilter.class)
              .addField("target", String.class, "The target tag to match (input)")
              .addField("selected", Item.class, "The selected output item's registered name. Optional for adding a filter")
              .build(types);

        TableType.builder(SorterFilter.class, "A Logistical Sorter filter")
              .extendedFrom(IFilter.class)
              .addField("allowDefault", boolean.class, "Allows the filtered item to travel to the default color destination")
              .addField("color", EnumColor.class, "The color configured, nil if none")
              .addField("size", boolean.class, "If Size Mode is enabled")
              .addField("min", int.class, "In Size Mode, the minimum to send")
              .addField("max", int.class, "In Size Mode, the maximum to send")
              .build(types);

        TableType.builder(QIOFilter.class, "A Quantum Item Orchestration filter")
              .extendedFrom(IFilter.class)
              .build(types);

        buildFilterVariants(types, SorterFilter.class, SorterItemStackFilter.class, SorterModIDFilter.class, SorterTagFilter.class, "Logistical Sorter", true);
        buildFilterVariants(types, MinerFilter.class, MinerItemStackFilter.class, MinerModIDFilter.class, MinerTagFilter.class, "Digital Miner", false);
        buildFilterVariants(types, QIOFilter.class, QIOItemStackFilter.class, QIOModIDFilter.class, QIOTagFilter.class, "QIO", true);

        return types;
    }

    private static <BASE> void buildFilterVariants(Map<Class<?>, TableType> types, Class<BASE> deviceFilterType, Class<? extends BASE> itemStackFilterClass, Class<? extends BASE> modIDFilterClass, Class<? extends BASE> tagFilterClass, String deviceName, boolean hasFuzzyItem) {
        Builder itemstackBuilder = TableType.builder(itemStackFilterClass, deviceName + " filter with ItemStack filter properties")
              .extendedFrom(deviceFilterType)
              .addField("item", Item.class, "The filtered item's registered name")
              .addField("itemComponents", String.class, "The Component data of the filtered item, optional");
        if (hasFuzzyItem) {
            itemstackBuilder.addField("fuzzy", boolean.class, "Whether Fuzzy mode is enabled (checks only the item name/type)");
        }
        itemstackBuilder.build(types);

        TableType.builder(modIDFilterClass, deviceName + " filter with Mod Id filter properties")
              .extendedFrom(deviceFilterType)
              .addField("modId", String.class, "The mod id to filter. e.g. mekansim")
              .build(types);

        TableType.builder(tagFilterClass, deviceName + " filter with Tag filter properties")
              .extendedFrom(deviceFilterType)
              .addField("tag", String.class, "The tag to filter. e.g. forge:ores")
              .build(types);
    }
}