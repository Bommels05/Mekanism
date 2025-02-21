package mekanism.common.recipe.lookup.cache.type;

import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalStack;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.api.recipes.ingredients.ChemicalStackIngredient;
import mekanism.common.recipe.ingredient.chemical.MultiChemicalStackIngredient;
import mekanism.common.recipe.ingredient.chemical.SingleChemicalStackIngredient;
import mekanism.common.recipe.ingredient.chemical.TaggedChemicalStackIngredient;
import net.minecraft.core.Holder;

public class ChemicalInputCache<CHEMICAL extends Chemical<CHEMICAL>, STACK extends ChemicalStack<CHEMICAL>, RECIPE extends MekanismRecipe>
      extends BaseInputCache<CHEMICAL, STACK, ChemicalStackIngredient<CHEMICAL, STACK>, RECIPE> {

    @Override
    public boolean mapInputs(RECIPE recipe, ChemicalStackIngredient<CHEMICAL, STACK> inputIngredient) {
        switch (inputIngredient) {
            case SingleChemicalStackIngredient<CHEMICAL, STACK> single -> addInputCache(single.getInputRaw(), recipe);
            case TaggedChemicalStackIngredient<CHEMICAL, STACK> tagged -> {
                for (Holder<CHEMICAL> input : tagged.getRawInput()) {
                    addInputCache(input, recipe);
                }
            }
            case MultiChemicalStackIngredient<CHEMICAL, STACK, ?> multi -> {
                return mapMultiInputs(recipe, multi);
            }
            default -> {
                //This should never really happen as we don't really allow for custom ingredients especially for networking,
                // but if it does add it as a fallback
                return true;
            }
        }
        return false;
    }

    @Override
    protected CHEMICAL createKey(STACK stack) {
        return stack.getChemical();
    }

    @Override
    public boolean isEmpty(STACK input) {
        return input.isEmpty();
    }
}