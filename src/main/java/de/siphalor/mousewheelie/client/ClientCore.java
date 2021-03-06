package de.siphalor.mousewheelie.client;

import de.siphalor.amecs.api.KeyModifiers;
import de.siphalor.mousewheelie.MouseWheelie;
import de.siphalor.mousewheelie.client.keybinding.PickToolKeyBinding;
import de.siphalor.mousewheelie.client.keybinding.ScrollKeyBinding;
import de.siphalor.mousewheelie.client.keybinding.SortKeyBinding;
import de.siphalor.mousewheelie.client.util.accessors.IContainerScreen;
import de.siphalor.mousewheelie.client.util.accessors.IScrollableRecipeBook;
import de.siphalor.mousewheelie.client.util.accessors.ISpecialScrollableScreen;
import de.siphalor.mousewheelie.client.util.inventory.ToolPicker;
import de.siphalor.tweed.client.TweedClothBridge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.fabricmc.fabric.api.event.client.player.ClientPickBlockGatherCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.tools.FabricToolTags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.*;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;

public class ClientCore implements ClientModInitializer {
	private static final MinecraftClient CLIENT = MinecraftClient.getInstance();

	public static final String KEY_BINDING_CATEGORY = "key.categories." + MouseWheelie.MOD_ID;
	public static final FabricKeyBinding SORT_KEY_BINDING = new SortKeyBinding(new Identifier(MouseWheelie.MOD_ID, "sort_inventory"), InputUtil.Type.MOUSE, 2, KEY_BINDING_CATEGORY, KeyModifiers.NONE);
	public static final FabricKeyBinding SCROLL_UP_KEYBINDING = new ScrollKeyBinding(new Identifier(MouseWheelie.MOD_ID, "scroll_up"), KEY_BINDING_CATEGORY, false);
	public static final FabricKeyBinding SCROLL_DOWN_KEYBINDING = new ScrollKeyBinding(new Identifier(MouseWheelie.MOD_ID, "scroll_down"), KEY_BINDING_CATEGORY, true);
	public static final FabricKeyBinding PICK_TOOL_KEYBINDING = new PickToolKeyBinding(new Identifier(MouseWheelie.MOD_ID, "pick_tool"), InputUtil.Type.KEYSYM, -1, KEY_BINDING_CATEGORY, KeyModifiers.NONE);

	public static TweedClothBridge tweedClothBridge;

	public static boolean awaitSlotUpdate = false;

	@Override
	public void onInitializeClient() {
		KeyBindingRegistry.INSTANCE.addCategory(KEY_BINDING_CATEGORY);
		KeyBindingRegistry.INSTANCE.register(SORT_KEY_BINDING);
		KeyBindingRegistry.INSTANCE.register(SCROLL_UP_KEYBINDING);
		KeyBindingRegistry.INSTANCE.register(SCROLL_DOWN_KEYBINDING);
		KeyBindingRegistry.INSTANCE.register(PICK_TOOL_KEYBINDING);

		ClientPickBlockGatherCallback.EVENT.register((player, result) -> {
			Item item = player.getMainHandStack().getItem();
			if(ClientCore.isTool(item) || ClientCore.isWeapon(item) && Config.holdToolPick.value) {
				if(result.getType() == HitResult.Type.BLOCK && result instanceof BlockHitResult) {
					ToolPicker toolPicker = new ToolPicker(player.inventory);
					int index = toolPicker.findToolFor(player.world.getBlockState(((BlockHitResult) result).getBlockPos()));
					return index == -1 ? ItemStack.EMPTY : player.inventory.getInvStack(index);
				} else {
					ToolPicker toolPicker = new ToolPicker(player.inventory);
					int index = toolPicker.findWeapon();
                    return index == -1 ? ItemStack.EMPTY : player.inventory.getInvStack(index);
				}
			}
			return ItemStack.EMPTY;
		});

		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getStackInHand(hand);
			EquipmentSlot equipmentSlot = MobEntity.getPreferredEquipmentSlot(stack);
			if(equipmentSlot != EquipmentSlot.MAINHAND && equipmentSlot != EquipmentSlot.OFFHAND) {
				ItemStack equipmentStack = player.getEquippedStack(equipmentSlot);
				if(!equipmentStack.isEmpty()) {
					player.setStackInHand(hand, equipmentStack);
					player.setEquippedStack(equipmentSlot, stack);
					return ActionResult.SUCCESS;
				}
			}
			return ActionResult.PASS;
		});

		Config.initialize();

		tweedClothBridge = new TweedClothBridge(Config.configFile);
	}

	public static boolean isTool(Item item) {
		return item instanceof ToolItem || item instanceof ShearsItem || FabricToolTags.AXES.contains(item) || FabricToolTags.HOES.contains(item) || FabricToolTags.PICKAXES.contains(item) || FabricToolTags.SHOVELS.contains(item);
	}

	public static boolean isWeapon(Item item) {
		return item instanceof RangedWeaponItem || item instanceof TridentItem || item instanceof SwordItem || FabricToolTags.SWORDS.contains(item);
	}

	public static double getMouseX() {
		return CLIENT.mouse.getX() * (double) CLIENT.window.getScaledWidth() / (double) CLIENT.window.getWidth();
	}

	public static double getMouseY() {
		return CLIENT.mouse.getY() * (double) CLIENT.window.getScaledHeight() / (double) CLIENT.window.getHeight();
	}

	public static boolean triggerScroll(double mouseX, double mouseY, double scrollY) {
		double scrollAmount = scrollY * CLIENT.options.mouseWheelSensitivity;
		if(CLIENT.currentScreen instanceof ISpecialScrollableScreen) {
			if(((ISpecialScrollableScreen) CLIENT.currentScreen).mouseWheelie_onMouseScrolledSpecial(mouseX, mouseY, scrollAmount)) {
				return true;
			}
		}
		if(CLIENT.currentScreen instanceof IContainerScreen) {
			if(((IContainerScreen) CLIENT.currentScreen).mouseWheelie_onMouseScroll(mouseX, mouseY, scrollAmount)) {
				return true;
			}
		}
		if(CLIENT.currentScreen instanceof IScrollableRecipeBook) {
			//noinspection RedundantIfStatement
			if(((IScrollableRecipeBook) CLIENT.currentScreen).mouseWheelie_onMouseScrollRecipeBook(mouseX, mouseY, scrollAmount)) {
				return true;
			}
		}
		return false;
	}
}
