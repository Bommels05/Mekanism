package mekanism.common.recipe.impl;

import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.PaintingRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient.PigmentStackIngredient;
import mekanism.api.recipes.ingredients.ItemStackIngredient;
import mekanism.common.recipe.MekanismRecipeType;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.registries.MekanismRecipeSerializers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

@NothingNullByDefault
public class PaintingIRecipe extends PaintingRecipe implements ItemStackOutputInternal {

    public PaintingIRecipe(ItemStackIngredient itemInput, PigmentStackIngredient pigmentInput, ItemStack output) {
        super(itemInput, pigmentInput, output);
    }

    @Override
    public RecipeType<PaintingRecipe> getType() {
        return MekanismRecipeType.PAINTING.get();
    }

    @Override
    public RecipeSerializer<PaintingIRecipe> getSerializer() {
        return MekanismRecipeSerializers.PAINTING.get();
    }

    @Override
    public String getGroup() {
        return MekanismBlocks.PAINTING_MACHINE.getName();
    }

    @Override
    public ItemStack getToastSymbol() {
        return MekanismBlocks.PAINTING_MACHINE.getItemStack();
    }

    @Override
    public ItemStack getOutputRaw() {
        return output;
    }
}