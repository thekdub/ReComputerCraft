package net.minecraft.server;

import dan200.computer.shared.LittleBlocksInterop;
import dan200.turtle.shared.BlockTurtle;
import dan200.turtle.shared.ContainerTurtle;
import dan200.turtle.shared.ItemTurtle;
import dan200.turtle.shared.TileEntityTurtle;
import forge.IGuiHandler;
import forge.MinecraftForge;
import forge.NetworkMod;


public class mod_CCTurtle extends NetworkMod implements IGuiHandler {
	@MLProp
	public static int turtleBlockID = 209;
	@MLProp
	public static int turtleGUIID = 101;
	@MLProp
	public static String turtle_fakeplayer_method = "perturtle";
	@MLProp
	public static boolean turtle_fakeplayer_dologin = false;
	public static mod_CCTurtle instance;
	public static BlockTurtle turtle;
	public static ItemTurtle itemTurtle;

	public mod_CCTurtle() {
		instance = this;
	}

	public static void openTurtleGUI(EntityHuman entityhuman, TileEntityTurtle turtle) {
		if (!LittleBlocksInterop.isLittleWorld(turtle.world)) {
			entityhuman.openGui(instance, turtleGUIID, turtle.world, turtle.x, turtle.y, turtle.z);
		}
	}

	public static void playBlockSound(World world, float f, float f1, float f2, Block block) {
	}

	public void load() {
	}

	public void modsLoaded() {
		super.modsLoaded();
		System.out.println("ComputerCraft: turtleBlockID " + turtleBlockID);
		turtle = new BlockTurtle(turtleBlockID);
		turtle.c(1.0F).a("turtle").j();
		itemTurtle = new ItemTurtle(turtle.id - 256);
		itemTurtle.a("turtle");
		ModLoader.registerBlock(turtle);
		ModLoader.registerTileEntity(TileEntityTurtle.getTurtleClass(), "turtle");
		Item.byId[turtle.id] = itemTurtle;
		ItemStack itemstack = new ItemStack(turtle, 1, 0);
		ModLoader.addRecipe(itemstack, "AAA", "ABA", "ACA", 'A', Item.IRON_INGOT, 'B', mod_ComputerCraft.computer, 'C', Block.CHEST);
		ItemStack itemstack1 = new ItemStack(turtle, 1, 1);
		ModLoader.addRecipe(itemstack1, "AB", 'A', itemstack, 'B', Item.DIAMOND_PICKAXE);
		ModLoader.addRecipe(itemstack1, "BA", 'A', itemstack, 'B', Item.DIAMOND_PICKAXE);
		ItemStack itemstack2 = new ItemStack(turtle, 1, 2);
		ModLoader.addRecipe(itemstack2, "AB", 'A', itemstack, 'B', mod_ComputerCraft.wirelessModem);
		ModLoader.addRecipe(itemstack2, "BA", 'A', itemstack, 'B', mod_ComputerCraft.wirelessModem);
		ItemStack itemstack3 = new ItemStack(turtle, 1, 3);
		ModLoader.addRecipe(itemstack3, "ABC", 'A', mod_ComputerCraft.wirelessModem, 'B', mod_ComputerCraft.computer, 'C', Item.DIAMOND_PICKAXE);
		ModLoader.addRecipe(itemstack3, "CBA", 'A', mod_ComputerCraft.wirelessModem, 'B', mod_ComputerCraft.computer, 'C', Item.DIAMOND_PICKAXE);
		ModLoader.addRecipe(itemstack3, "AB", 'A', itemstack2, 'B', Item.DIAMOND_PICKAXE);
		ModLoader.addRecipe(itemstack3, "BA", 'A', itemstack2, 'B', Item.DIAMOND_PICKAXE);
		ModLoader.addRecipe(itemstack3, "AB", 'A', itemstack1, 'B', mod_ComputerCraft.wirelessModem);
		ModLoader.addRecipe(itemstack3, "BA", 'A', itemstack1, 'B', mod_ComputerCraft.wirelessModem);
		ModLoader.setInGameHook(this, true, true);
		MinecraftForge.setGuiHandler(this, this);
	}

	public String getVersion() {
		return "1.33";
	}

	public String getPriorities() {
		return "after:mod_ComputerCraft;after:mod_RedPowerCore;after:mod_RedPowerArray;after:mod_RedPowerLighting;after:mod_RedPowerLogic;after:mod_RedPowerMachine;after:mod_RedPowerWiring;after:mod_RedPowerWorld";
	}

	public boolean clientSideRequired() {
		return true;
	}

	public boolean serverSideRequired() {
		return false;
	}

	public Object getGuiElement(int i, EntityHuman entityhuman, World world, int j, int k, int l) {
		TileEntity tileEntity = world.getTileEntity(j, k, l);
		if (i == turtleGUIID && tileEntity instanceof TileEntityTurtle) {
			TileEntityTurtle turtle = (TileEntityTurtle) tileEntity;
			return new ContainerTurtle(entityhuman.inventory, turtle);
		}
		else {
			return null;
		}
	}
}
