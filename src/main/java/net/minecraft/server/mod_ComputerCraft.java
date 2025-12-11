package net.minecraft.server;

import dan200.computer.shared.*;
import forge.*;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class mod_ComputerCraft extends NetworkMod implements IGuiHandler, IConnectionHandler, IPacketHandler, ISaveEventHandler {
	public static final int terminal_defaultWidth = 50;
	public static final int terminal_defaultHeight = 18;
	public static final int terminal_width = 50;
	public static final int terminal_height = 18;
	@MLProp
	public static int computerBlockID = 207;
	@MLProp
	public static int diskDriveBlockID = 208;
	@MLProp
	public static int diskItemID = 4000;
	@MLProp
	public static int diskDriveGUIID = 100;
	@MLProp
	public static int enableAPI_http = 0;
	@MLProp
	public static int modem_range = 64;
	@MLProp
	public static int modem_rangeDuringStorm = 1;
	public static mod_ComputerCraft instance;
	public static BlockComputer computer;
	public static ItemComputer itemComputer;
	public static BlockPeripheral peripheral;
	public static ItemPeripheral itemPeripheral;
	public static ItemStack diskDrive;
	public static ItemStack wirelessModem;
	public static ItemStack monitor;
	public static ItemDisk disk;

	public mod_ComputerCraft() {
		instance = this;
		System.out.println("Loading ComputerCraft: To change block IDs, modify config/mod_ComputerCraft.cfg");
		System.out.println("ComputerCraft: computerBlockID " + computerBlockID);
		System.out.println("ComputerCraft: diskDriveBlockID " + diskDriveBlockID);
		System.out.println("ComputerCraft: diskItemID " + diskItemID);
		computer = new BlockComputer(computerBlockID);
		computer.c(1.0F).a("computer").j();
		itemComputer = new ItemComputer(computer.id - 256);
		itemComputer.a("computer");
		ModLoader.registerBlock(computer);
		Item.byId[computer.id] = itemComputer;
		ModLoader.addRecipe(new ItemStack(itemComputer, 1), "XXX", "XYX", "XZX", 'X', Block.STONE, 'Y', Item.REDSTONE, 'Z', Block.THIN_GLASS);
		ModLoader.registerTileEntity(TileEntityComputer.getComputerClass(), "computer");
		peripheral = new BlockPeripheral(diskDriveBlockID);
		peripheral.c(1.0F).a("diskdrive").j();
		itemPeripheral = new ItemPeripheral(peripheral.id - 256);
		itemPeripheral.a("diskdrive");
		ModLoader.registerBlock(peripheral);
		Item.byId[peripheral.id] = itemPeripheral;
		diskDrive = new ItemStack(itemPeripheral, 1, 0);
		ModLoader.addRecipe(diskDrive, "XXX", "XYX", "XYX", 'X', Block.STONE, 'Y', Item.REDSTONE);
		wirelessModem = new ItemStack(itemPeripheral, 1, 1);
		ModLoader.addRecipe(wirelessModem, "XXX", "XYX", "XXX", 'X', Block.STONE, 'Y', Block.REDSTONE_TORCH_ON);
		monitor = new ItemStack(itemPeripheral, 1, 2);
		ModLoader.addRecipe(monitor, "XXX", "XYX", "XXX", 'X', Block.STONE, 'Y', Block.THIN_GLASS);
		disk = new ItemDisk(diskItemID);
		disk.a("disk");
		ModLoader.addRecipe(new ItemStack(disk, 1), "X", "Y", 'X', Item.REDSTONE, 'Y', Item.PAPER);
		ModLoader.setInGameHook(this, true, true);
		MinecraftForge.setGuiHandler(this, this);
		MinecraftForge.registerConnectionHandler(this);
		MinecraftForge.registerSaveHandler(this);
		ItemDisk.loadLabels();
		ItemComputer.loadLabels();
	}

	public static void notifyBlockChange(World world, int i, int j, int k, int l) {
		world.update(i, j, k, l);
	}

	public static String getRecordInfo(ItemRecord itemrecord, ItemStack itemstack) {
		return "C418 - " + itemrecord.a;
	}

	public static void playRecord(String s, String s1, World world, int i, int j, int k) {
	}

	public static File getBaseDir() {
		return new File(".");
	}

	public static File getModDir() {
		return new File("./mods/ComputerCraft");
	}

	public static File getWorldDir(World world) {
		return new File("./world");
	}

	public static void openComputerGUI(EntityHuman entityhuman, TileEntityComputer tileEntity) {
		if (!LittleBlocksInterop.isLittleWorld(tileEntity.world)) {
			ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
			computercraftpacket.packetType = 1;
			computercraftpacket.dataInt = new int[] {tileEntity.x, tileEntity.y, tileEntity.z};
			sendToPlayer(entityhuman, computercraftpacket);
		}
	}

	public static void openDiskDriveGUI(EntityHuman entityhuman, TileEntityDiskDrive tileEntity) {
		if (!LittleBlocksInterop.isLittleWorld(tileEntity.world)) {
			entityhuman.openGui(instance, diskDriveGUIID, tileEntity.world, tileEntity.x, tileEntity.y, tileEntity.z);
		}
	}

	public static void sendToPlayer(EntityHuman entityhuman, ComputerCraftPacket computercraftpacket) {
		if (entityhuman instanceof EntityPlayer) {
			Packet packet = computercraftpacket.toPacket();
			((EntityPlayer) entityhuman).netServerHandler.sendPacket(packet);
		}
	}

	public static void sendToAllPlayers(ComputerCraftPacket computercraftpacket) {
		Packet packet = computercraftpacket.toPacket();
		ModLoader.getMinecraftServerInstance().serverConfigurationManager.sendAll(packet);
	}

	public static void sendToServer(ComputerCraftPacket computercraftpacket) {
		throw new UnsupportedOperationException("Cannot send from server to server");
	}

	private static World getPlayerWorld(EntityHuman entityhuman) {
		for (World world : DimensionManager.getWorlds()) {
			if (world.players.contains(entityhuman)) {
				return world;
			}
		}

		return null;
	}

	public static boolean isMultiplayerClient() {
		return false;
	}

	public static boolean isMultiplayerServer() {
		return true;
	}

	public void load() {
	}

	public void modsLoaded() {
		super.modsLoaded();
		ModLoader.registerTileEntity(TileEntityComputer.getComputerClass(), "computer");
		ModLoader.registerTileEntity(TileEntityDiskDrive.class, "diskdrive");
		ModLoader.registerTileEntity(TileEntityWirelessModem.class, "wirelessmodem");
		ModLoader.registerTileEntity(TileEntityMonitor.class, "monitor");
	}

	public String getVersion() {
		return "1.33";
	}

	public String getPriorities() {
		return "after:mod_RedPowerCore;after:mod_RedPowerArray;after:mod_RedPowerLighting;after:mod_RedPowerLogic;after:mod_RedPowerMachine;after:mod_RedPowerWiring;after:mod_RedPowerWorld";
	}

	public boolean clientSideRequired() {
		return true;
	}

	public boolean serverSideRequired() {
		return false;
	}

	private void handlePacket(ComputerCraftPacket computercraftpacket, EntityPlayer entityPlayer) {
		World world = getPlayerWorld(entityPlayer);
		if (world != null) {
			if (computercraftpacket.packetType == 11) {
				for (int i = 0; i < computercraftpacket.dataInt.length; i++) {
					int l = computercraftpacket.dataInt[i];
					ItemDisk.sendDiskLabelToPlayer(l, entityPlayer);
				}
			}
			else if (computercraftpacket.packetType == 15) {
				for (int j = 0; j < computercraftpacket.dataInt.length; j++) {
					int i1 = computercraftpacket.dataInt[j];
					ItemComputer.sendComputerLabelToPlayer(i1, entityPlayer);
				}
			}
			else {
				int k = computercraftpacket.dataInt[0];
				int j1 = computercraftpacket.dataInt[1];
				int k1 = computercraftpacket.dataInt[2];
				TileEntity tileEntity = world.getTileEntity(k, j1, k1);
				if (tileEntity instanceof INetworkedEntity) {
					INetworkedEntity networkedentity = (INetworkedEntity) tileEntity;
					networkedentity.handlePacket(computercraftpacket, entityPlayer);
				}
			}
		}
	}

	public Object getGuiElement(int i, EntityHuman entityhuman, World world, int x, int y, int z) {
		TileEntity tileEntity = world.getTileEntity(x, y, z);
		if (i == diskDriveGUIID && tileEntity instanceof TileEntityDiskDrive) {
			TileEntityDiskDrive diskDrive = (TileEntityDiskDrive) tileEntity;
			return new ContainerDiskDrive(entityhuman.inventory, diskDrive);
		}
		else {
			return null;
		}
	}

	public void onConnect(NetworkManager networkmanager) {
	}

	public void onLogin(NetworkManager network, Packet1Login packet) {
		MessageManager.getInstance().registerChannel(network, this, "mod_ComputerCraf");
	}

	public void onDisconnect(NetworkManager network, String s, Object[] args) {
	}

	public void onPacketData(NetworkManager network, String s, byte[] byte0) {
		if (s.equals("mod_ComputerCraf")) {
			try {
				ComputerCraftPacket computercraftpacket = ComputerCraftPacket.parse(byte0);
				EntityPlayer entityPlayer = ((NetServerHandler) network.getNetHandler()).getPlayerEntity();
				this.handlePacket(computercraftpacket, entityPlayer);
			} catch (IOException var6) {
			}
		}
	}

	public void onWorldLoad(World world) {
	}

	public void onWorldSave(World world) {
	}

	public void onChunkLoad(World world, Chunk chunk) {
	}

	public void onChunkSaveData(World world, Chunk chunk, NBTTagCompound data) {
	}

	public void onChunkLoadData(World world, Chunk chunk, NBTTagCompound data) {
	}

	public void onChunkUnload(World world, Chunk chunk) {
		for (Object tileEntity : chunk.tileEntities.values()) {
			if (tileEntity instanceof IComputerEntity) {
				IComputerEntity computer = (IComputerEntity) tileEntity;
				computer.unload();
			}
		}
	}
}
