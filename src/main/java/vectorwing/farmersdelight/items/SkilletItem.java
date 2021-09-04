package vectorwing.farmersdelight.items;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.*;
import net.minecraft.item.crafting.CampfireCookingRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import vectorwing.farmersdelight.FarmersDelight;
import vectorwing.farmersdelight.tile.SkilletTileEntity;
import vectorwing.farmersdelight.utils.TextUtils;
import vectorwing.farmersdelight.utils.tags.ModTags;

import javax.annotation.Nullable;
import java.util.Optional;

public class SkilletItem extends BlockItem
{
	public static final ItemTier SKILLET_TIER = ItemTier.IRON;
	private final Multimap<Attribute, AttributeModifier> toolAttributes;

	public SkilletItem(Block blockIn, Item.Properties builderIn) {
		super(blockIn, builderIn.defaultMaxDamage(SKILLET_TIER.getMaxUses()));
		float attackDamage = 4.5F + SKILLET_TIER.getAttackDamage();
		ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
		builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Tool modifier", (double) attackDamage, AttributeModifier.Operation.ADDITION));
		builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", (double) -3.0F, AttributeModifier.Operation.ADDITION));
		this.toolAttributes = builder.build();
	}

	@Mod.EventBusSubscriber(modid = FarmersDelight.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
	public static class SkilletEvents
	{
		@SubscribeEvent
		public static void onSkilletKnockback(LivingKnockBackEvent event) {
			LivingEntity attacker = event.getEntityLiving().getAttackingEntity();
			ItemStack tool = attacker != null ? attacker.getHeldItem(Hand.MAIN_HAND) : ItemStack.EMPTY;
			if (tool.getItem() instanceof SkilletItem) {
				event.setStrength(event.getOriginalStrength() * 2.0F);
			}
		}
	}

	private static boolean isPlayerNearHeatSource(PlayerEntity player, IWorldReader worldIn) {
		if (player.isBurning()) {
			return true;
		}
		BlockPos pos = player.getPosition();
		for (BlockPos nearbyPos : BlockPos.getAllInBoxMutable(pos.add(-1, -1, -1), pos.add(1, 1, 1))) {
			if (worldIn.getBlockState(nearbyPos).isIn(ModTags.HEAT_SOURCES)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.NONE;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		int fireAspectLevel = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_ASPECT, stack);
		int cookingTimeReduction = 0;
		if (fireAspectLevel > 0) {
			cookingTimeReduction = ((MathHelper.clamp(fireAspectLevel, 0, 2) * 20) + 20);
		}
		return 120 - cookingTimeReduction;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
		ItemStack heldStack = playerIn.getHeldItem(handIn);
		if (isPlayerNearHeatSource(playerIn, worldIn)) {
			if (getCookingRecipeForOffhandItem(playerIn).isPresent()) {
				playerIn.setActiveHand(handIn);
				return ActionResult.resultConsume(heldStack);
			} else {
				playerIn.sendStatusMessage(TextUtils.getTranslation("item.skillet.how_to_cook"), true);
			}
		}
		return ActionResult.resultPass(heldStack);
	}

	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World worldIn, LivingEntity entityLiving) {
		if (entityLiving instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity) entityLiving;
			Optional<CampfireCookingRecipe> cookingRecipe = getCookingRecipeForOffhandItem(player);

			cookingRecipe.ifPresent((recipe) -> {
				ItemStack resultStack = recipe.getCraftingResult(new Inventory());
				if (!player.inventory.addItemStackToInventory(resultStack)) {
					player.dropItem(resultStack, false);
				}
				player.getHeldItem(Hand.OFF_HAND).shrink(1);
			});
			worldIn.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.BLOCK_NOTE_BLOCK_BELL, SoundCategory.BLOCKS, 1.0F, 1.0F);
		}
		return stack;
	}

	public static Optional<CampfireCookingRecipe> getCookingRecipeForOffhandItem(LivingEntity living) {
		ItemStack heldStack = living.getHeldItem(Hand.OFF_HAND);
		if (heldStack.isEmpty()) {
			return Optional.empty();
		}

		World world = living.getEntityWorld();
		return world.getRecipeManager().getRecipe(IRecipeType.CAMPFIRE_COOKING, new Inventory(heldStack), world);
	}

	@Override
	protected boolean onBlockPlaced(BlockPos pos, World worldIn, @Nullable PlayerEntity player, ItemStack stack, BlockState state) {
		super.onBlockPlaced(pos, worldIn, player, stack, state);
		TileEntity tileEntity = worldIn.getTileEntity(pos);
		if (tileEntity instanceof SkilletTileEntity) {
			((SkilletTileEntity) tileEntity).setSkilletItem(stack);
			return true;
		}
		return false;
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack repair) {
		return SKILLET_TIER.getRepairMaterial().test(repair) || super.getIsRepairable(toRepair, repair);
	}

	public boolean onBlockDestroyed(ItemStack stack, World worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving) {
		if (!worldIn.isRemote && state.getBlockHardness(worldIn, pos) != 0.0F) {
			stack.damageItem(1, entityLiving, (entity) -> {
				entity.sendBreakAnimation(EquipmentSlotType.MAINHAND);
			});
		}

		return true;
	}

	@Override
	public ActionResultType tryPlace(BlockItemUseContext context) {
		PlayerEntity player = context.getPlayer();
		if (player != null && player.isSneaking()) {
			return super.tryPlace(context);
		}
		return ActionResultType.PASS;
	}

	@Override
	public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		stack.damageItem(1, attacker, (user) -> user.sendBreakAnimation(EquipmentSlotType.MAINHAND));
		return true;
	}

	public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlotType equipmentSlot) {
		return equipmentSlot == EquipmentSlotType.MAINHAND ? this.toolAttributes : super.getAttributeModifiers(equipmentSlot);
	}
}
