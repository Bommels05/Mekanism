package mekanism.common.recipe.serializer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import mekanism.api.JsonConstants;
import mekanism.api.SerializerHelper;
import mekanism.api.chemical.ChemicalType;
import mekanism.api.codec.DependentMapCodec;
import mekanism.api.recipes.ChemicalCrystallizerRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.api.recipes.ingredients.creator.IngredientCreatorAccess;
import mekanism.common.Mekanism;
import mekanism.common.recipe.impl.ChemicalCrystallizerIRecipe;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public class ChemicalCrystallizerRecipeSerializer implements RecipeSerializer<ChemicalCrystallizerIRecipe> {

    private final IFactory<ChemicalCrystallizerIRecipe> factory;
    private Codec<ChemicalCrystallizerIRecipe> codec;

    public ChemicalCrystallizerRecipeSerializer(IFactory<ChemicalCrystallizerIRecipe> factory) {
        this.factory = factory;
    }

    private static final MapCodec<ChemicalType> chemicalTypeMapCodec = ChemicalType.CODEC.fieldOf(JsonConstants.CHEMICAL_TYPE);
    @SuppressWarnings("unchecked")
    private static final MapCodec<ChemicalStackIngredient<?,?>> chemicalStackIngredientMapEncoder = new DependentMapCodec<>(JsonConstants.INPUT, type -> (Codec<ChemicalStackIngredient<?, ?>>) IngredientCreatorAccess.getCreatorForType(type).codec(), chemicalTypeMapCodec, ChemicalType::getTypeFor);

    @NotNull
    @Override
    public Codec<ChemicalCrystallizerIRecipe> codec() {
        if (codec == null) {
            codec = RecordCodecBuilder.create(instance-> instance.group(
                  chemicalStackIngredientMapEncoder.forGetter(ChemicalCrystallizerRecipe::getInput),
                  SerializerHelper.ITEMSTACK_CODEC.fieldOf(JsonConstants.OUTPUT).forGetter(ChemicalCrystallizerIRecipe::getOutputRaw)
            ).apply(instance, factory::create));
        }
        return codec;
    }

    @Override
    public ChemicalCrystallizerIRecipe fromNetwork(@NotNull FriendlyByteBuf buffer) {
        try {
            ChemicalType chemicalType = buffer.readEnum(ChemicalType.class);
            ChemicalStackIngredient<?, ?> inputIngredient = IngredientCreatorAccess.getCreatorForType(chemicalType).read(buffer);
            ItemStack output = buffer.readItem();
            return this.factory.create(inputIngredient, output);
        } catch (Exception e) {
            Mekanism.logger.error("Error reading boxed chemical to itemstack recipe from packet.", e);
            throw e;
        }
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull ChemicalCrystallizerIRecipe recipe) {
        try {
            recipe.write(buffer);
        } catch (Exception e) {
            Mekanism.logger.error("Error writing boxed chemical to itemstack recipe to packet.", e);
            throw e;
        }
    }

    @FunctionalInterface
    public interface IFactory<RECIPE extends ChemicalCrystallizerRecipe> {

        RECIPE create(ChemicalStackIngredient<?, ?> input, ItemStack output);
    }
}