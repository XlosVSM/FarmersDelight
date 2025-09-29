package vectorwing.farmersdelight.integration.jei;

import com.google.common.collect.Lists;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;
import vectorwing.farmersdelight.FarmersDelight;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.crafting.CuttingBoardRecipe;
import vectorwing.farmersdelight.common.registry.ModItems;
import vectorwing.farmersdelight.common.registry.ModRecipeTypes;

import java.util.List;
import java.util.Optional;

public class FDRecipes
{
	private final RecipeManager recipeManager;

	public FDRecipes() {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;

		if (level != null) {
			this.recipeManager = level.getRecipeManager();
		} else {
			throw new NullPointerException("minecraft world must not be null.");
		}
	}

	public List<CookingPotRecipe> getCookingPotRecipes() {
		return recipeManager.getAllRecipesFor(ModRecipeTypes.COOKING.get()).stream().toList();
	}

	public List<CuttingBoardRecipe> getCuttingBoardRecipes() {
		return recipeManager.getAllRecipesFor(ModRecipeTypes.CUTTING.get()).stream().toList();
	}

	public List<CraftingRecipe> getSpecialWheatDoughRecipe() {
		Optional<? extends Recipe<?>> specialRecipe = recipeManager.byKey(new ResourceLocation(FarmersDelight.MODID, "wheat_dough_from_water"));
		List<CraftingRecipe> recipes = Lists.newArrayList();

		specialRecipe.ifPresent((recipe) -> {
			NonNullList<Ingredient> inputs = NonNullList.of(
					Ingredient.EMPTY,
					Ingredient.of(Items.WHEAT),
					Ingredient.of(Items.WATER_BUCKET)
			);
			ItemStack output = new ItemStack(ModItems.WHEAT_DOUGH.get());

			ResourceLocation id = recipe.getId();
			recipes.add(new ShapelessRecipe(id, "fd_dough", CraftingBookCategory.MISC, output, inputs));
		});

		return recipes;
	}
}
