package mekanism.common.recipe.impl;

import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.recipes.ItemStackToGasRecipe;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismRecipeSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

@NothingNullByDefault
public class GasConversionIRecipe extends ItemStackToGasRecipe implements ChemicalOutputInternal<Gas, GasStack> {

    public GasConversionIRecipe(ItemStackIngredient input, GasStack output) {
        super(input, output);
    }

    @Override
    public RecipeType<ItemStackToGasRecipe> getType() {
        return MekanismRecipeType.GAS_CONVERSION.get();
    }

    @Override
    public RecipeSerializer<GasConversionIRecipe> getSerializer() {
        return MekanismRecipeSerializers.GAS_CONVERSION.get();
    }

    @Override
    public String getGroup() {
        return "gas_conversion";
    }

    @Override
    public ItemStack getToastSymbol() {
        return MekanismBlocks.CREATIVE_CHEMICAL_TANK.getItemStack();
    }

    @Override
    public GasStack getOutputRaw() {
        return output;
    }
}