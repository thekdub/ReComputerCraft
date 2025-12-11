package dan200.computer.shared;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;
import net.minecraft.server.*;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


public class TileEntityDiskDrive extends TileEntity implements IInventory, IPeripheral, INetworkedEntity, IDestroyableEntity {
	static final boolean $assertionsDisabled = !TileEntityDiskDrive.class.desiredAssertionStatus();
	private final HashSet<CraftHumanEntity> viewers = new HashSet<>();
	private ItemStack diskStack;
	private boolean m_firstFrame;
	private int m_contents;
	private int m_diskID;
	private String m_recordName;
	private String m_recordInfo;
	private boolean m_eject;
	private boolean m_recordQueued;
	private boolean m_recordPlaying;
	private boolean m_restartRecord;
	private final Map<IComputerAccess, TileEntityDiskDrive.ComputerInfo> m_computers;

	public TileEntityDiskDrive() {
		this.diskStack = null;
		this.m_firstFrame = true;
		this.m_contents = 0;
		this.m_diskID = -1;
		this.m_recordName = null;
		this.m_recordInfo = null;
		this.m_eject = false;
		this.m_recordQueued = false;
		this.m_recordPlaying = false;
		this.m_restartRecord = false;
		this.m_computers = new HashMap<>();
	}

	static Class<?> _mthclass$(String s) {
		try {
			return Class.forName(s);
		} catch (ClassNotFoundException var2) {
			throw new NoClassDefFoundError(var2.getMessage());
		}
	}

	public void m() {
		super.m();
		if (mod_ComputerCraft.isMultiplayerClient()) {
			ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
			computercraftpacket.packetType = 5;
			computercraftpacket.dataInt = new int[] {this.x, this.y, this.z};
			mod_ComputerCraft.sendToServer(computercraftpacket);
		}
	}

	@Override
	public void destroy() {
		this.ejectContents(true);
		synchronized (this) {
			if (this.m_recordPlaying) {
				this.playRecord(null, null);
			}
		}
	}

	public Packet d() {
		return null;
	}

	public void a(NBTTagCompound nbttagcompound) {
		super.a(nbttagcompound);
		NBTTagCompound nbttagcompound1 = nbttagcompound.getCompound("item");
		this.diskStack = ItemStack.a(nbttagcompound1);
		this.updateContents();
	}

	public void b(NBTTagCompound nbttagcompound) {
		super.b(nbttagcompound);
		NBTTagCompound item = new NBTTagCompound();
		if (this.diskStack != null) {
			this.diskStack.save(item);
		}

		nbttagcompound.set("item", item);
	}

	public void q_() {
		if (!mod_ComputerCraft.isMultiplayerClient() && this.m_firstFrame) {
			this.updateContents();
			mod_ComputerCraft.notifyBlockChange(this.world, this.x, this.y, this.z, mod_ComputerCraft.peripheral.id);
			this.m_firstFrame = false;
		}

		synchronized (this) {
			if (this.m_eject) {
				this.ejectContents(false);
				this.m_eject = false;
			}
		}

		synchronized (this) {
			if (this.m_recordPlaying != this.m_recordQueued || this.m_restartRecord) {
				this.m_restartRecord = false;
				if (this.m_recordQueued) {
					String s = this.getAudioDiscRecordName();
					if (s != null) {
						this.m_recordPlaying = true;
						this.playRecord(s, this.getAudioDiscRecordInfo());
					}
					else {
						this.m_recordQueued = false;
					}
				}
				else {
					this.playRecord(null, null);
					this.m_recordPlaying = false;
				}
			}
		}
	}

	public int getSize() {
		return 1;
	}

	public ItemStack getItem(int i) {
		return this.diskStack;
	}

	public ItemStack getStackInSlotOnClosing(int i) {
		ItemStack itemstack = this.diskStack;
		this.diskStack = null;
		return itemstack;
	}

	public ItemStack splitStack(int i, int j) {
		if (this.diskStack == null) {
			return null;
		}
		else if (this.diskStack.count <= j) {
			ItemStack itemstack = this.diskStack;
			this.setItem(0, null);
			return itemstack;
		}
		else {
			ItemStack itemstack1 = this.diskStack.a(j);
			if (this.diskStack.count == 0) {
				this.setItem(0, null);
			}
			else {
				this.setItem(0, this.diskStack);
			}

			return itemstack1;
		}
	}

	public void setItem(int i, ItemStack itemstack) {
		if (mod_ComputerCraft.isMultiplayerClient()) {
			this.diskStack = itemstack;
			this.update();
		}
		else {
			boolean flag = false;
			synchronized (this) {
				if (this.diskStack != null) {
					for (IComputerAccess icomputeraccess : this.m_computers.keySet()) {
						this.unmountDisk(icomputeraccess);
					}
				}

				if (this.m_recordPlaying) {
					this.playRecord(null, null);
					this.m_recordPlaying = false;
					this.m_recordQueued = false;
				}

				this.diskStack = itemstack;
				this.update();
				int j = this.m_contents;
				this.updateContents();
				flag = j != this.m_contents;
				if (this.diskStack != null) {
					for (IComputerAccess icomputeraccess1 : this.m_computers.keySet()) {
						this.mountDisk(icomputeraccess1);
					}
				}
			}

			if (flag) {
				mod_ComputerCraft.notifyBlockChange(this.world, this.x, this.y, this.z, mod_ComputerCraft.peripheral.id);
				if (mod_ComputerCraft.isMultiplayerServer()) {
					ComputerCraftPacket computercraftpacket = this.createDiskLightPacket();
					mod_ComputerCraft.sendToAllPlayers(computercraftpacket);
				}
			}
		}
	}

	public String getName() {
		return "Disk Drive";
	}

	public int getMaxStackSize() {
		return 64;
	}

	public void setMaxStackSize(int i) {
	}

	public boolean a(EntityHuman entityhuman) {
		if (this.world.getTileEntity(this.x, this.y, this.z) != this) {
			return false;
		}
		else {
			return LittleBlocksInterop.isLittleWorld(this.world)
					? entityhuman.e(this.x / 8.0 + 0.5, this.y / 8.0 + 0.5, this.z / 8.0 + 0.5) <= 64.0
					: entityhuman.e(this.x + 0.5, this.y + 0.5, this.z + 0.5) <= 64.0;
		}
	}

	public void f() {
	}

	public void g() {
	}

	@Override
	public String getType() {
		return "drive";
	}

	@Override
	public String[] getMethodNames() {
		return new String[] {"isPresent", "getLabel", "setLabel", "hasData", "getMountPath", "hasAudio", "getAudioTitle", "playAudio", "stopAudio", "eject"};
	}

	@Override
	public Object[] callMethod(IComputerAccess icomputeraccess, int i, Object[] aobj) throws Exception {
		switch (i) {
			case 0:
				return new Object[] {this.hasAnything()};
			case 1:
				String s = null;
				synchronized (this) {
					int k = this.getDataDiskID();
					if (k > 0) {
						s = ItemDisk.getDiskLabel(k);
					}
					else {
						String s3 = this.getAudioDiscRecordInfo();
						if (s3 != null) {
							s = s3;
						}
					}
				}

				return new Object[] {s};
			case 2:
				int j = this.getDataDiskID();
				if (j > 0) {
					String s2 = null;
					if (aobj.length > 0) {
						if (aobj[0] != null && !(aobj[0] instanceof String)) {
							throw new Exception("Expected string");
						}

						s2 = (String) aobj[0];
					}

					ItemDisk.setDiskLabel(j, s2);
				}

				return null;
			case 3:
				return new Object[] {this.getDataDiskID() >= 0};
			case 4:
				synchronized (this) {
					TileEntityDiskDrive.ComputerInfo computerinfo = this.m_computers.get(icomputeraccess);
					if (computerinfo != null) {
						return new Object[] {computerinfo.mountLoc};
					}

					return null;
				}
			case 5:
				return new Object[] {this.getAudioDiscRecordName() != null};
			case 6:
				String s1 = this.getAudioDiscRecordInfo();
				if (s1 != null) {
					return new Object[] {s1};
				}

				return null;
			case 7:
				synchronized (this) {
					if (this.getAudioDiscRecordName() != null) {
						this.m_recordQueued = true;
						this.m_restartRecord = this.m_recordPlaying;
					}

					return null;
				}
			case 8:
				synchronized (this) {
					this.m_recordQueued = false;
					this.m_restartRecord = false;
					return null;
				}
			case 9:
				synchronized (this) {
					this.m_eject = true;
					return null;
				}
			default:
				if (!$assertionsDisabled) {
					throw new AssertionError();
				}
				else {
					return null;
				}
		}
	}

	@Override
	public boolean canAttachToSide(int i) {
		return true;
	}

	@Override
	public void attach(IComputerAccess icomputeraccess, String s) {
		this.m_computers.put(icomputeraccess, new ComputerInfo(s));
		this.mountDisk(icomputeraccess);
	}

	@Override
	public void detach(IComputerAccess icomputeraccess) {
		this.unmountDisk(icomputeraccess);
		this.m_computers.remove(icomputeraccess);
	}

	private synchronized void mountDisk(IComputerAccess icomputeraccess) {
		if (this.diskStack != null) {
			TileEntityDiskDrive.ComputerInfo computerinfo = this.m_computers.get(icomputeraccess);
			if (!$assertionsDisabled && computerinfo == null) {
				throw new AssertionError();
			}

			if (this.m_contents == 2) {
				if (this.m_diskID <= 0) {
					this.m_diskID = icomputeraccess.createNewSaveDir("computer/disk");
					ItemDisk.setDiskID(this.diskStack, this.m_diskID);
				}

				computerinfo.mountLoc = icomputeraccess.mountSaveDir("disk", "computer/disk", this.m_diskID, false);
			}

			icomputeraccess.queueEvent("disk", new Object[] {computerinfo.side});
		}
	}

	private synchronized void unmountDisk(IComputerAccess icomputeraccess) {
		if (this.diskStack != null) {
			TileEntityDiskDrive.ComputerInfo computerinfo = this.m_computers.get(icomputeraccess);
			if (!$assertionsDisabled && computerinfo == null) {
				throw new AssertionError();
			}

			if (computerinfo.mountLoc != null) {
				icomputeraccess.unmount(computerinfo.mountLoc);
				computerinfo.mountLoc = null;
			}

			icomputeraccess.queueEvent("disk_eject", new Object[] {computerinfo.side});
		}
	}

	private synchronized void updateContents() {
		if (this.diskStack != null) {
			Item item = Item.byId[this.diskStack.id];
			if (item instanceof ItemDisk) {
				this.m_contents = 2;
				this.m_diskID = ItemDisk.getDiskID(this.diskStack);
			}
			else if (item instanceof ItemRecord) {
				ItemRecord itemrecord = (ItemRecord) item;
				this.m_contents = 3;
				this.m_recordName = itemrecord.a;
				this.m_recordInfo = mod_ComputerCraft.getRecordInfo(itemrecord, this.diskStack);
			}
			else {
				this.m_contents = 1;
			}
		}
		else {
			this.m_contents = 0;
		}
	}

	public synchronized boolean hasAnything() {
		return this.m_contents != 0;
	}

	public synchronized boolean hasDisk() {
		return this.m_contents == 2 || this.m_contents == 3;
	}

	public void ejectContents(boolean flag) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			if (this.diskStack != null) {
				ItemStack itemstack = this.diskStack;
				this.setItem(0, null);
				int i = 0;
				int j = 0;
				if (!flag) {
					int k = this.world.getData(this.x, this.y, this.z);
					switch (k) {
						case 2:
							j = -1;
							break;
						case 3:
							j = 1;
							break;
						case 4:
							i = -1;
							break;
						case 5:
							i = 1;
					}
				}

				double d = this.x + 0.5 + i * 0.5;
				double d1 = this.y + 0.75;
				double d2 = this.z + 0.5 + j * 0.5;
				EntityItem entityitem = new EntityItem(this.world, d, d1, d2, itemstack);
				entityitem.motX = i * 0.15;
				entityitem.motY = 0.0;
				entityitem.motZ = j * 0.15;
				this.world.addEntity(entityitem);
				if (!flag) {
					this.world.triggerEffect(1000, this.x, this.y, this.z, 0);
				}
			}
		}
	}

	private synchronized int getDataDiskID() {
		return this.m_contents == 2 ? this.m_diskID : -1;
	}

	private synchronized String getAudioDiscRecordName() {
		return this.m_contents == 3 ? this.m_recordName : null;
	}

	private synchronized String getAudioDiscRecordInfo() {
		return this.m_contents == 3 ? this.m_recordInfo : null;
	}

	private void playRecord(String s, String s1) {
		if (mod_ComputerCraft.isMultiplayerServer()) {
			ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
			computercraftpacket.packetType = 7;
			computercraftpacket.dataInt = new int[] {this.x, this.y, this.z};
			if (s != null) {
				computercraftpacket.dataString = new String[] {s, s1};
			}

			mod_ComputerCraft.sendToAllPlayers(computercraftpacket);
		}
		else {
			mod_ComputerCraft.playRecord(s, s1, this.world, this.x, this.y, this.z);
		}
	}

	private ComputerCraftPacket createDiskLightPacket() {
		ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
		computercraftpacket.packetType = 8;
		synchronized (this) {
			computercraftpacket.dataInt = new int[] {this.x, this.y, this.z, this.m_contents};
			return computercraftpacket;
		}
	}

	public void updateClient(EntityHuman entityhuman) {
		if (mod_ComputerCraft.isMultiplayerServer()) {
			ComputerCraftPacket computercraftpacket = this.createDiskLightPacket();
			mod_ComputerCraft.sendToPlayer(entityhuman, computercraftpacket);
		}
	}

	@Override
	public void handlePacket(ComputerCraftPacket computercraftpacket, EntityHuman entityhuman) {
		if (mod_ComputerCraft.isMultiplayerServer()) {
			if (computercraftpacket.packetType == 5) {
				this.updateClient(entityhuman);
			}
		}
		else {
			switch (computercraftpacket.packetType) {
				case 7:
					if (computercraftpacket.dataString != null && computercraftpacket.dataString.length > 0) {
						this.playRecord(computercraftpacket.dataString[0], computercraftpacket.dataString[1]);
					}
					else {
						this.playRecord(null, null);
					}
					break;
				case 8:
					synchronized (this) {
						this.m_contents = computercraftpacket.dataInt[3];
					}

					mod_ComputerCraft.notifyBlockChange(this.world, this.x, this.y, this.z, mod_ComputerCraft.peripheral.id);
			}
		}
	}

	public ItemStack[] getContents() {
		return null;
	}

	public void onOpen(CraftHumanEntity craftHumanEntity) {
		this.viewers.add(craftHumanEntity);
	}

	public void onClose(CraftHumanEntity craftHumanEntity) {
		this.viewers.remove(craftHumanEntity);
	}

	public List<HumanEntity> getViewers() {
		return new ArrayList<>(this.viewers);
	}

	public ItemStack splitWithoutUpdate(int i) {
		return null;
	}

	private static class ComputerInfo {
		String side;
		String mountLoc;

		private ComputerInfo(String s) {
			this.side = s;
			this.mountLoc = null;
		}
	}
}
