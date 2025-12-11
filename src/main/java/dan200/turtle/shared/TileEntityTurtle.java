package dan200.turtle.shared;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;
import dan200.computer.core.Terminal;
import dan200.computer.shared.*;
import dan200.turtle.api.FakePlayer;
import net.minecraft.server.*;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;


public class TileEntityTurtle extends TileEntity implements ITurtle, IInventory, INetworkedEntity, IComputerEntity, IPeripheral {
	static final boolean $assertionsDisabled = !TileEntityTurtle.class.desiredAssertionStatus();
	private static final int s_commandTime = 8;
	private static final int[] oppositeSide = new int[] {1, 0, 3, 2, 5, 4};
	private static final LinkedList<TileEntityTurtle.TurtleGrave> s_deadTurtles = new LinkedList<>();
	public boolean m_moved;
	private ItemStack[] m_inventory;
	private NetworkedComputerHelper m_computer;
	private TileEntityTurtle.State m_state;
	private TileEntityTurtle.ClientState m_clientState;
	private final int[] remapSide = new int[] {0, 1, 2, 3, 5, 4};

	public TileEntityTurtle() {
		this(0);
	}

	public TileEntityTurtle(int i) {
		this.m_inventory = new ItemStack[9];
		this.m_computer = new NetworkedComputerHelper(this, mod_CCTurtle.turtle, "turtle", 36, 12);
		this.m_clientState = new ClientState(i);
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_state = new TileEntityTurtle.State(this);
			this.m_computer.addPeripheralAsAPI(new TurtlePeripheral(this));
			if (this.hasModem()) {
				this.m_state.modem = new WirelessModemPeripheral(this);
				this.m_computer.setPeripheral(5, this.m_state.modem);
			}
		}

		this.m_moved = false;
	}

	public TileEntityTurtle(int i, int j) {
		this(i);
		this.setDir(j);
		this.m_clientState.ready = false;
	}

	public static Class getTurtleClass() {
		if (RedPowerInterop.isRedPowerInstalled()) {
			try {
				return Class.forName("dan200.turtle.shared.RedPowerTileEntityTurtle");
			} catch (ClassNotFoundException var1) {
				System.out.println("ComputerCraft: Exception loading dan200.turtle.shared.RedPowerTileEntityTurtle");
				System.out.println("ComputerCraft: Turtles will not have RedPower support");
			}
		}

		return TileEntityTurtle.class;
	}

	public void transferStateFrom(TileEntityTurtle tileentityturtle) {
		this.m_inventory = tileentityturtle.m_inventory;
		this.m_computer = tileentityturtle.m_computer;
		this.m_computer.setOwner(this);
		this.m_clientState = tileentityturtle.m_clientState;
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_state = tileentityturtle.m_state;
			this.m_state.latestOwner = this;
			if (this.m_state.modem != null) {
				this.m_state.modem.setOwnerAndPos(this, this.x, this.y, this.z);
			}
		}

		this.m_moved = false;
	}

	public int getDir() {
		return this.m_clientState.dir;
	}

	public void setDir(int i) {
		this.m_clientState.dir = i;
		switch (i) {
			case 2:
				this.m_clientState.rot = 180.0F;
				break;
			case 3:
				this.m_clientState.rot = 0.0F;
				break;
			case 4:
				this.m_clientState.rot = 90.0F;
				break;
			default:
				this.m_clientState.rot = 270.0F;
		}

		this.m_clientState.lastRot = this.m_clientState.rot;
		this.m_clientState.ready = true;
	}

	public boolean hasPick() {
		return (this.m_clientState.subType & 1) > 0;
	}

	public boolean hasModem() {
		return (this.m_clientState.subType & 2) > 0;
	}

	public boolean isModemActive() {
		return this.m_clientState.modemLight;
	}

	@Override
	public void unload() {
		if (!this.m_moved) {
			if (!this.m_clientState.destroyed) {
				this.m_computer.destroy();
				this.m_clientState.destroyed = true;
			}
		}
	}

	@Override
	public void destroy() {
		if (!this.m_moved) {
			if (!this.m_clientState.destroyed) {
				this.m_computer.destroy();
				if (!mod_ComputerCraft.isMultiplayerClient()) {
					for (ItemStack itemstack = this.takeItemStack(); itemstack != null; itemstack = this.takeItemStack()) {
						this.dropStack(itemstack, -1);
					}
				}

				this.m_clientState.destroyed = true;
			}
		}
	}

	@Override
	public int issueCommand(int i) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			synchronized (this.m_state) {
				this.m_state.commandQueue.offer(i);
				this.m_state.commandsIssued++;
				return this.m_state.commandsIssued;
			}
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public int getSelectedSlot() {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			synchronized (this.m_inventory) {
				return this.m_state.selectedSlot;
			}
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public int getSlotItemCount(int i) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			synchronized (this.m_inventory) {
				ItemStack itemstack = this.m_inventory[i];
				return itemstack != null ? itemstack.count : 0;
			}
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public int getSlotSpaceRemaining(int i) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			synchronized (this.m_inventory) {
				ItemStack itemstack = this.m_inventory[i];
				int j = this.getMaxStackSize();
				return itemstack != null ? Math.min(itemstack.getMaxStackSize(), j) - itemstack.count : j;
			}
		}
		else {
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public void addListener(ITurtleListener iturtlelistener) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			if (!$assertionsDisabled && this.m_state.listener != null) {
				throw new AssertionError();
			}

			this.m_state.listener = iturtlelistener;
		}
	}

	@Override
	public boolean isDestroyed() {
		return this.m_clientState.destroyed;
	}

	@Override
	public Terminal getTerminal() {
		return this.m_computer.getTerminal();
	}

	public boolean isTerminalReady() {
		return this.m_computer.isTerminalReady();
	}

	@Override
	public void pressKey(char c, int i) {
		this.m_computer.pressKey(c, i);
	}

	@Override
	public void typeString(String s) {
		this.m_computer.typeString(s);
	}

	@Override
	public void turnOn() {
		this.m_computer.turnOn();
	}

	@Override
	public boolean isOn() {
		return this.m_computer.isOn();
	}

	@Override
	public void shutdown() {
		this.m_computer.shutdown();
	}

	@Override
	public void reboot() {
		this.m_computer.reboot();
	}

	@Override
	public void terminate() {
		this.m_computer.terminate();
	}

	@Override
	public int getComputerID() {
		return this.m_computer.getComputerID();
	}

	@Override
	public void setComputerID(int i) {
		this.m_computer.setComputerID(i);
	}

	@Override
	public boolean getPowerOutput(int i) {
		return (!this.hasModem() || i != 4) && this.m_computer.getPowerOutput(this.remapSide[i]);
	}

	@Override
	public void setPowerInput(int i, boolean flag) {
		if (!this.hasModem() || i != 4) {
			this.m_computer.setPowerInput(this.remapSide[i], flag);
		}
	}

	@Override
	public int getBundledPowerOutput(int i) {
		return this.hasModem() && i == 4 ? 0 : this.m_computer.getBundledPowerOutput(this.remapSide[i]);
	}

	@Override
	public void setBundledPowerInput(int i, int j) {
		if (!this.hasModem() || i != 4) {
			this.m_computer.setBundledPowerInput(this.remapSide[i], j);
		}
	}

	@Override
	public void setPeripheral(int i, IPeripheral iperipheral) {
		if (!this.hasModem() || i != 4) {
			this.m_computer.setPeripheral(this.remapSide[i], iperipheral);
		}
	}

	@Override
	public String getType() {
		return this.m_computer.getType();
	}

	@Override
	public String[] getMethodNames() {
		return this.m_computer.getMethodNames();
	}

	@Override
	public Object[] callMethod(IComputerAccess icomputeraccess, int i, Object[] obj) throws Exception {
		return this.m_computer.callMethod(icomputeraccess, i, obj);
	}

	@Override
	public boolean canAttachToSide(int i) {
		return this.m_computer.canAttachToSide(i);
	}

	@Override
	public void attach(IComputerAccess icomputeraccess, String s) {
		this.m_computer.attach(icomputeraccess, s);
	}

	@Override
	public void detach(IComputerAccess icomputeraccess) {
		this.m_computer.detach(icomputeraccess);
	}

	public void a(NBTTagCompound nbttagcompound) {
		super.a(nbttagcompound);
		NBTTagList nbttaglist = nbttagcompound.getList("Items");
		this.m_inventory = new ItemStack[this.getSize()];

		for (int i = 0; i < nbttaglist.size(); i++) {
			NBTTagCompound nbttagcompound1 = (NBTTagCompound) nbttaglist.get(i);
			int k = nbttagcompound1.getByte("Slot") & 255;
			if (k < this.m_inventory.length) {
				this.m_inventory[k] = ItemStack.a(nbttagcompound1);
			}
		}

		if (!mod_ComputerCraft.isMultiplayerClient()) {
			int j = 0;
			synchronized (this.m_state) {
				this.m_state.commandQueue.clear();

				while (nbttagcompound.get("command" + j) != null) {
					int l = nbttagcompound.getInt("command" + j);
					this.m_state.commandQueue.add(l);
					j++;
				}
			}

			this.m_state.commandsIssued = nbttagcompound.getInt("commandsIssued");
			this.m_state.commandsProcessed = nbttagcompound.getInt("commandsProcessed");
			this.m_state.selectedSlot = nbttagcompound.getInt("selectedSlot");
		}

		this.m_clientState.ready = nbttagcompound.getBoolean("ready");
		this.m_clientState.subType = nbttagcompound.getInt("subType");
		this.m_clientState.dir = nbttagcompound.getInt("dir");
		this.m_clientState.offsetDir = nbttagcompound.getInt("offsetDir");
		this.m_clientState.animation = nbttagcompound.getInt("animation");
		this.m_clientState.animationProgress = nbttagcompound.getInt("animationProgress");
		this.m_clientState.rot = nbttagcompound.getFloat("rot");
		this.m_clientState.lastRot = nbttagcompound.getFloat("lastRot");
		this.m_clientState.offset = nbttagcompound.getFloat("offset");
		this.m_clientState.lastOffset = nbttagcompound.getFloat("lastOffset");
		this.m_clientState.toolRot = nbttagcompound.getFloat("toolRot");
		this.m_clientState.lastToolRot = nbttagcompound.getFloat("lastToolRot");
		this.m_computer.readFromNBT(nbttagcompound);
		if (!mod_ComputerCraft.isMultiplayerClient() && this.hasModem() && this.m_state.modem == null) {
			this.m_state.modem = new WirelessModemPeripheral(this);
			this.m_computer.setPeripheral(5, this.m_state.modem);
		}
	}

	public void b(NBTTagCompound nbttagcompound) {
		super.b(nbttagcompound);
		NBTTagList nbttaglist = new NBTTagList();

		for (int i = 0; i < this.m_inventory.length; i++) {
			if (this.m_inventory[i] != null) {
				NBTTagCompound nbttagcompound1 = new NBTTagCompound();
				nbttagcompound1.setByte("Slot", (byte) i);
				this.m_inventory[i].save(nbttagcompound1);
				nbttaglist.add(nbttagcompound1);
			}
		}

		nbttagcompound.set("Items", nbttaglist);
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			int j = 0;
			synchronized (this.m_state) {
				for (int k : this.m_state.commandQueue) {
					nbttagcompound.setInt("command" + j, k);
					j++;
				}
			}

			nbttagcompound.setInt("commandsIssued", this.m_state.commandsIssued);
			nbttagcompound.setInt("commandsProcessed", this.m_state.commandsProcessed);
			nbttagcompound.setInt("selectedSlot", this.m_state.selectedSlot);
		}

		nbttagcompound.setBoolean("ready", this.m_clientState.ready);
		nbttagcompound.setInt("subType", this.m_clientState.subType);
		nbttagcompound.setInt("dir", this.m_clientState.dir);
		nbttagcompound.setInt("offsetDir", this.m_clientState.offsetDir);
		nbttagcompound.setInt("animation", this.m_clientState.animation);
		nbttagcompound.setInt("animationProgress", this.m_clientState.animationProgress);
		nbttagcompound.setFloat("rot", this.m_clientState.rot);
		nbttagcompound.setFloat("lastRot", this.m_clientState.lastRot);
		nbttagcompound.setFloat("offset", this.m_clientState.offset);
		nbttagcompound.setFloat("lastOffset", this.m_clientState.lastOffset);
		nbttagcompound.setFloat("toolRot", this.m_clientState.toolRot);
		nbttagcompound.setFloat("lastToolRot", this.m_clientState.lastToolRot);
		this.m_computer.writeToNBT(nbttagcompound);
	}

	public int getOffsetDir() {
		return this.m_clientState.offsetDir;
	}

	public float getRenderAngle(float f) {
		return this.m_clientState.lastRot + (this.m_clientState.rot - this.m_clientState.lastRot) * f;
	}

	public float getRenderOffset(float f) {
		return this.m_clientState.lastOffset + (this.m_clientState.offset - this.m_clientState.lastOffset) * f;
	}

	public float getToolAngle(float f) {
		return this.m_clientState.lastToolRot + (this.m_clientState.toolRot - this.m_clientState.lastToolRot) * f;
	}

	public boolean shouldRender() {
		return !this.m_moved;
	}

	public void m() {
		super.m();
		this.m_computer.requestUpdate();
		if (mod_ComputerCraft.isMultiplayerClient()) {
			long l = System.currentTimeMillis();
			Iterator<TileEntityTurtle.TurtleGrave> it = s_deadTurtles.iterator();
			int i = -1;
			boolean flag = false;

			while (it.hasNext()) {
				TileEntityTurtle.TurtleGrave turtlegrave = it.next();
				if (l - turtlegrave.timeOfDeath > 1000L) {
					it.remove();
				}
				else {
					World world = turtlegrave.world.get();
					if (world == null) {
						it.remove();
					}
					else if (i < 0 && this.world == world && turtlegrave.dir == this.m_clientState.dir) {
						if (turtlegrave.x == this.x && turtlegrave.z == this.z) {
							if (turtlegrave.y == this.y + 1) {
								i = 0;
								flag = turtlegrave.modemLight;
							}
							else if (turtlegrave.y == this.y - 1) {
								i = 1;
								flag = turtlegrave.modemLight;
							}
						}
						else if (turtlegrave.y == this.y) {
							if (turtlegrave.x == this.x - Facing.b[this.m_clientState.dir] && turtlegrave.z == this.z - Facing.d[this.m_clientState.dir]) {
								i = this.m_clientState.dir;
								flag = turtlegrave.modemLight;
							}
							else if (turtlegrave.x == this.x + Facing.b[this.m_clientState.dir] && turtlegrave.z == this.z + Facing.d[this.m_clientState.dir]) {
								i = this.getOppositeDir(this.m_clientState.dir);
								flag = turtlegrave.modemLight;
							}
						}
					}
				}
			}

			if (i >= 0) {
				this.m_clientState.offsetDir = i;
				this.m_clientState.modemLight = flag;
				this.startAnimation(0);
			}
		}
	}

	public void j() {
		if (mod_ComputerCraft.isMultiplayerClient()) {
			s_deadTurtles.addFirst(new TurtleGrave(this));
		}

		super.j();
	}

	public Packet d() {
		return null;
	}

	private int getOppositeDir(int i) {
		return oppositeSide[i];
	}

	private boolean isBlockInWorld(int i) {
		return i >= 0 && i < this.world.getHeight();
	}

	private boolean canPlaceInBlock(int i) {
		return i > 0 && i < this.world.getHeight() - 1;
	}

	private boolean move(int i) {
		int j = this.x;
		int k = this.y;
		int l = this.z;
		int i1 = this.x + Facing.b[i];
		int j1 = this.y + Facing.c[i];
		int k1 = this.z + Facing.d[i];
		if (!this.canPlaceInBlock(j1)) {
			return false;
		}
		else {
			int l1 = this.world.getTypeId(i1, j1, k1);
			if (l1 != 0
					&& l1 != Block.STATIONARY_WATER.id
					&& l1 != Block.WATER.id
					&& l1 != Block.STATIONARY_LAVA.id
					&& l1 != Block.LAVA.id
					&& l1 != Block.FIRE.id
					&& l1 != Block.SNOW.id) {
				Block block = Block.byId[l1];
				if (!block.isBlockReplaceable(this.world, i1, j1, k1)) {
					return false;
				}
			}

			CraftPlayer fakePlayer = FakePlayer.getBukkitEntity(this.world, this.getTurtleName());
			if (fakePlayer != null) {
				CraftWorld craftWorld = this.world.getWorld();
				CraftServer craftServer = this.world.getServer();
				PlayerMoveEvent event = new PlayerMoveEvent(fakePlayer, new Location(craftWorld, j, k, l), new Location(craftWorld, i1, j1, k1));
				craftServer.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					return false;
				}
			}

			AxisAlignedBB axisalignedbb = mod_CCTurtle.turtle.e(this.world, i1, j1, k1);
			if (axisalignedbb != null && !this.world.containsEntity(axisalignedbb)) {
				return false;
			}
			else {
				this.m_moved = true;
				this.world.setRawTypeId(j, k, l, 0);
				int i2 = ((this.m_clientState.dir - 2 & 3) << 2) + (this.m_clientState.subType & 3);
				this.world.setRawTypeIdAndData(i1, j1, k1, mod_CCTurtle.turtle.id, i2);
				TileEntity tileEntity = this.world.getTileEntity(i1, j1, k1);
				if (tileEntity instanceof TileEntityTurtle) {
					TileEntityTurtle tileentityturtle = (TileEntityTurtle) tileEntity;
					this.m_clientState.offsetDir = i;
					tileentityturtle.transferStateFrom(this);
					tileentityturtle.startAnimation(0);
					tileentityturtle.updateAnimation();
				}

				mod_ComputerCraft.notifyBlockChange(this.world, j, k, l, 0);
				mod_ComputerCraft.notifyBlockChange(this.world, i1, j1, k1, mod_CCTurtle.turtle.id);
				return true;
			}
		}
	}

	private boolean dig(int i) {
		if (!this.hasPick()) {
			return false;
		}
		else {
			int j = this.x + Facing.b[i];
			int k = this.y + Facing.c[i];
			int l = this.z + Facing.d[i];
			if (!this.isBlockInWorld(k)) {
				return false;
			}
			else {
				int i1 = this.world.getTypeId(j, k, l);
				if (i1 != 0 && i1 != Block.BEDROCK.id) {
					Player fakePlayer = FakePlayer.getBukkitEntity(this.world, this.getTurtleName());
					if (fakePlayer != null) {
						CraftWorld craftWorld = this.world.getWorld();
						CraftServer craftServer = this.world.getServer();
						BlockBreakEvent event = new BlockBreakEvent(craftWorld.getBlockAt(j, k, l), fakePlayer);
						craftServer.getPluginManager().callEvent(event);
						if (event.isCancelled()) {
							return false;
						}
					}

					for (ItemStack itemstack : this.getBlockDropped(i1, this.world, j, k, l)) {
						boolean flag = this.storeItemStack(itemstack);
						if (!flag) {
							this.dropStack(itemstack, this.getOppositeDir(this.m_clientState.dir));
						}
					}

					Block block = Block.byId[i1];
					this.world.triggerEffect(2001, j, k, l, block.id + (this.world.getData(j, k, l) << 12));
					this.world.setTypeId(j, k, l, 0);
					return true;
				}
				else {
					return false;
				}
			}
		}
	}

	private boolean place(int i) {
		int j = this.x + Facing.b[i];
		int k = this.y + Facing.c[i];
		int l = this.z + Facing.d[i];
		if (!this.canPlaceInBlock(k)) {
			return false;
		}
		else {
			int i1 = this.getOppositeDir(i);
			ItemStack itemstack = this.takePlaceableItem(j, k, l, i1);
			if (itemstack == null) {
				return false;
			}
			else {
				Item item = Item.byId[itemstack.id];
				Block block = Block.byId[itemstack.id];
				if (!this.world.setTypeIdAndData(j, k, l, itemstack.id, item.filterData(itemstack.getData()))) {
					return false;
				}
				else {
					if (this.world.getTypeId(j, k, l) == itemstack.id) {
						EntityPlayer fakePlayer = FakePlayer.get(this.world, this.getTurtleName());
						if (fakePlayer != null) {
							BlockState replacedBlockState = CraftBlockState.getBlockState(this.world, j, k, l);
							BlockPlaceEvent event = CraftEventFactory.callBlockPlaceEvent(this.world, fakePlayer, replacedBlockState, j, k, l);
							if (event.isCancelled() || !event.canBuild()) {
								return false;
							}
						}

						block.postPlace(this.world, j, k, l, i1);
						if (item instanceof ItemComputer) {
							((ItemComputer) item).setupComputerAfterPlacement(itemstack, this.world, j, k, l);
						}
					}

					mod_CCTurtle.playBlockSound(this.world, j + 0.5F, k + 0.5F, l + 0.5F, block);
					return true;
				}
			}
		}
	}

	private boolean drop(int i) {
		ItemStack itemstack = this.takeItemStack();
		if (itemstack == null) {
			return false;
		}
		else {
			this.dropStack(itemstack, i);
			return true;
		}
	}

	private boolean detect(int i) {
		int j = this.x + Facing.b[i];
		int k = this.y + Facing.c[i];
		int l = this.z + Facing.d[i];
		if (!this.isBlockInWorld(k)) {
			return false;
		}
		else {
			int i1 = this.world.getTypeId(j, k, l);
			if (i1 != 0
					&& i1 != Block.STATIONARY_WATER.id
					&& i1 != Block.WATER.id
					&& i1 != Block.STATIONARY_LAVA.id
					&& i1 != Block.LAVA.id
					&& i1 != Block.FIRE.id
					&& i1 != Block.SNOW.id) {
				Block block = Block.byId[i1];
				return !block.isBlockReplaceable(this.world, j, k, l);
			}

			return false;
		}
	}

	private boolean compare(int i) {
		int j = -1;
		int k = -1;
		synchronized (this.m_inventory) {
			ItemStack itemstack = this.m_inventory[this.m_state.selectedSlot];
			if (itemstack == null || itemstack.id == 0) {
				return !this.detect(i);
			}

			int j1 = itemstack.id;
			Item item = Item.byId[j1];
			if (item instanceof ItemBlock) {
				j = j1;
				if (item.e()) {
					k = item.filterData(itemstack.getData());
				}
			}
		}

		int l = this.x + Facing.b[i];
		int i1 = this.y + Facing.c[i];
		int k1 = this.z + Facing.d[i];
		int l1 = 0;
		int i2 = 0;
		if (this.isBlockInWorld(i1)) {
			l1 = this.world.getTypeId(l, i1, k1);
			i2 = this.world.getData(l, i1, k1);
		}

		return j == l1 && (k == -1 || k == i2);
	}

	private void dropStack(ItemStack itemstack, int i) {
		double d = this.x + 0.5;
		double d1 = this.y + 0.5;
		double d2 = this.z + 0.5;
		double d3 = 0.0;
		double d4 = 0.0;
		double d5 = 0.0;
		if (i >= 0) {
			d3 = Facing.b[i] * 0.35;
			d4 = Facing.c[i] * 0.35;
			d5 = Facing.d[i] * 0.35;
			d += d3;
			d1 += d4;
			d2 += d5;
		}

		EntityItem entityitem = new EntityItem(this.world, d, d1, d2, itemstack);
		entityitem.motX = d3 * 0.35 + this.world.random.nextFloat() * 0.2 - 0.1;
		entityitem.motY = d4 * 0.55 + this.world.random.nextFloat() * 0.2 - 0.1;
		entityitem.motZ = d5 * 0.35 + this.world.random.nextFloat() * 0.2 - 0.1;
		entityitem.pickupDelay = 30;
		CraftPlayer fakePlayer = FakePlayer.getBukkitEntity(this.world, this.getTurtleName());
		if (fakePlayer != null) {
			CraftServer craftServer = this.world.getServer();
			PlayerDropItemEvent event = new PlayerDropItemEvent(fakePlayer, (org.bukkit.entity.Item) entityitem.getBukkitEntity());
			craftServer.getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return;
			}
		}

		this.world.addEntity(entityitem);
		if (i >= 0) {
			this.world.makeSound(d, d1, d2, "random.pop", 0.2F, ((this.world.random.nextFloat() - this.world.random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
		}
	}

	private ArrayList<ItemStack> getBlockDropped(int i, World world, int j, int k, int l) {
		Block block = Block.byId[i];
		int i1 = world.getData(j, k, l);
		return block.getBlockDropped(world, j, k, l, i1, 0);
	}

	public void q_() {
		if (!this.m_moved && !this.m_clientState.destroyed) {
			this.m_computer.update();
			if (!mod_ComputerCraft.isMultiplayerClient()) {
				if (this.m_state.modem != null && this.m_state.modem.pollChanged()) {
					this.m_clientState.modemLight = this.m_state.modem.isActive();
					if (mod_ComputerCraft.isMultiplayerServer()) {
						ComputerCraftPacket computercraftpacket = this.createModemLightPacket();
						mod_ComputerCraft.sendToAllPlayers(computercraftpacket);
					}
				}

				this.updateCommands();
			}

			this.updateAnimation();
		}
	}

	private void updateCommands() {
		if (this.m_clientState.animation == -1) {
			int i = -1;
			synchronized (this.m_state) {
				if (this.m_state.commandQueue.peek() != null) {
					i = this.m_state.commandQueue.remove();
				}
			}

			if (i >= 0) {
				boolean flag = true;
				if (i >= 14 && i <= 22) {
					synchronized (this.m_inventory) {
						this.m_state.selectedSlot = i - 14;
					}

					this.update();
				}
				else {
					switch (i) {
						case 1:
							flag = this.move(this.m_clientState.dir);
							break;
						case 2:
							flag = this.move(this.getOppositeDir(this.m_clientState.dir));
							break;
						case 3:
							flag = this.move(1);
							break;
						case 4:
							flag = this.move(0);
							break;
						case 5:
							switch (this.m_clientState.dir) {
								case 2:
									this.m_clientState.dir = 4;
									break;
								case 3:
									this.m_clientState.dir = 5;
									break;
								case 4:
									this.m_clientState.dir = 3;
									break;
								case 5:
									this.m_clientState.dir = 2;
							}

							mod_ComputerCraft.notifyBlockChange(this.world, this.x, this.y, this.z, mod_CCTurtle.turtle.id);
							mod_CCTurtle.turtle.refreshInput(this.world, this.x, this.y, this.z);
							this.startAnimation(1);
							break;
						case 6:
							switch (this.m_clientState.dir) {
								case 2:
									this.m_clientState.dir = 5;
									break;
								case 3:
									this.m_clientState.dir = 4;
									break;
								case 4:
									this.m_clientState.dir = 2;
									break;
								case 5:
									this.m_clientState.dir = 3;
							}

							mod_ComputerCraft.notifyBlockChange(this.world, this.x, this.y, this.z, mod_CCTurtle.turtle.id);
							mod_CCTurtle.turtle.refreshInput(this.world, this.x, this.y, this.z);
							this.startAnimation(2);
							break;
						case 7:
							flag = this.dig(this.m_clientState.dir);
							if (flag) {
								this.startAnimation(3);
							}
							break;
						case 8:
							flag = this.dig(1);
							if (flag) {
								this.startAnimation(3);
							}
							break;
						case 9:
							flag = this.dig(0);
							if (flag) {
								this.startAnimation(3);
							}
							break;
						case 10:
							flag = this.place(this.m_clientState.dir);
							if (flag) {
								this.startAnimation(4);
							}
							break;
						case 11:
							flag = this.place(1);
							if (flag) {
								this.startAnimation(4);
							}
							break;
						case 12:
							flag = this.place(0);
							if (flag) {
								this.startAnimation(4);
							}
							break;
						case 13:
							flag = this.drop(this.m_clientState.dir);
							if (flag) {
								this.startAnimation(4);
							}
						case 23:
							flag = this.detect(this.m_clientState.dir);
							break;
						case 24:
							flag = this.detect(1);
							break;
						case 25:
							flag = this.detect(0);
							break;
						case 26:
							flag = this.compare(this.m_clientState.dir);
							break;
						case 27:
							flag = this.compare(1);
							break;
						case 28:
							flag = this.compare(0);
						default:
							break;
					}
				}

				this.m_state.commandsProcessed++;
				if (this.m_state.listener != null) {
					this.m_state.listener.commandProcessed(this.m_state.commandsProcessed, flag);
				}
			}
		}
	}

	private void startAnimation(int i) {
		if (!mod_ComputerCraft.isMultiplayerClient() || i != 0 || this.m_clientState.animation != 0) {
			this.m_clientState.animation = i;
			this.m_clientState.animationProgress = 0;
			if (mod_ComputerCraft.isMultiplayerClient()) {
				this.m_clientState.offset = 0.0F;
				this.m_clientState.lastOffset = 0.0F;
				switch (i) {
					case 0:
						this.m_clientState.offset = -1.0F;
						this.m_clientState.lastOffset = -1.0F;
						break;
					case 1:
						this.m_clientState.rot += 90.0F;
						this.m_clientState.lastRot = this.m_clientState.rot;
						break;
					case 2:
						this.m_clientState.rot -= 90.0F;
						this.m_clientState.lastRot = this.m_clientState.rot;
				}
			}
			else if (i == 0) {
				float f = this.m_clientState.offset - this.m_clientState.lastOffset;
				this.m_clientState.offset = -1.0F;
				this.m_clientState.lastOffset = -1.0F - f;
			}

			if (mod_ComputerCraft.isMultiplayerServer() && i != 4) {
				ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
				computercraftpacket.packetType = 50;
				computercraftpacket.dataInt = new int[] {this.x, this.y, this.z, i, this.m_clientState.dir, this.m_clientState.offsetDir};
				mod_ComputerCraft.sendToAllPlayers(computercraftpacket);
			}
		}
	}

	private void updateAnimation() {
		if (!this.m_moved) {
			this.m_clientState.lastRot = this.m_clientState.rot;
			this.m_clientState.lastOffset = this.m_clientState.offset;
			this.m_clientState.lastToolRot = this.m_clientState.toolRot;
			if (this.m_clientState.animation >= 0) {
				switch (this.m_clientState.animation) {
					case 0:
						this.m_clientState.offset += 0.125F;
						break;
					case 1:
						this.m_clientState.rot -= 11.25F;
						break;
					case 2:
						this.m_clientState.rot += 11.25F;
						break;
					case 3:
						float f = (this.m_clientState.animationProgress + 1) / 8.0F;
						this.m_clientState.toolRot = 45.0F * (float) Math.sin(f * Math.PI);
				}

				this.m_clientState.animationProgress++;
				if (this.m_clientState.animationProgress >= 8) {
					this.m_clientState.animation = -1;
					this.m_clientState.animationProgress = 0;
				}
			}

			if (this.m_clientState.rot >= 360.0F) {
				this.m_clientState.rot -= 360.0F;
				this.m_clientState.lastRot -= 360.0F;
			}
			else if (this.m_clientState.rot < 0.0F) {
				this.m_clientState.rot += 360.0F;
				this.m_clientState.lastRot += 360.0F;
			}
		}
	}

	public int getSize() {
		return 9;
	}

	public ItemStack getItem(int i) {
		synchronized (this.m_inventory) {
			return this.m_inventory[i];
		}
	}

	public ItemStack splitWithoutUpdate(int i) {
		synchronized (this.m_inventory) {
			ItemStack itemstack = this.m_inventory[i];
			this.m_inventory[i] = null;
			return itemstack;
		}
	}

	public ItemStack splitStack(int i, int j) {
		synchronized (this.m_inventory) {
			if (this.m_inventory[i] == null) {
				return null;
			}
			else if (this.m_inventory[i].count <= j) {
				ItemStack itemstack = this.m_inventory[i];
				this.m_inventory[i] = null;
				this.update();
				return itemstack;
			}
			else {
				ItemStack itemstack1 = this.m_inventory[i].a(j);
				if (this.m_inventory[i].count == 0) {
					this.m_inventory[i] = null;
				}

				this.update();
				return itemstack1;
			}
		}
	}

	public void setItem(int i, ItemStack itemstack) {
		synchronized (this.m_inventory) {
			this.m_inventory[i] = itemstack;
			this.update();
		}
	}

	public String getName() {
		return "Turtle";
	}

	public int getMaxStackSize() {
		return 64;
	}

	public void setMaxStackSize(int i) {
	}

	private boolean storeItemStack(ItemStack itemstack) {
		int i = this.getMaxStackSize();
		boolean flag = false;
		synchronized (this.m_inventory) {
			for (int j = 0; j < 9; j++) {
				int k = (this.m_state.selectedSlot + j) % 9;
				if (this.m_inventory[k] == null) {
					if (itemstack.count <= i) {
						this.m_inventory[k] = itemstack;
						this.update();
						return true;
					}

					this.m_inventory[k] = itemstack.a(i);
					flag = true;
				}
				else if (this.m_inventory[k].id == itemstack.id
						&& this.m_inventory[k].isStackable()
						&& (!this.m_inventory[k].usesData() || this.m_inventory[k].getData() == itemstack.getData())) {
					int i1 = Math.min(this.m_inventory[k].getMaxStackSize(), i) - this.m_inventory[k].count;
					if (i1 >= itemstack.count) {
						this.m_inventory[k].count = this.m_inventory[k].count + itemstack.count;
						this.update();
						return true;
					}

					if (i1 > 0) {
						itemstack.count -= i1;
						this.m_inventory[k].count += i1;
						flag = true;
					}
				}
			}
		}

		if (flag) {
			this.update();
		}

		return false;
	}

	private ItemStack takeItemStack() {
		boolean flag = false;
		synchronized (this.m_inventory) {
			for (int i = 0; i < 9; i++) {
				int j = (this.m_state.selectedSlot + i) % 9;
				if (this.m_inventory[j] != null && this.m_inventory[j].count <= 0) {
					this.m_inventory[j] = null;
					flag = true;
				}

				if (this.m_inventory[j] != null) {
					ItemStack itemstack = this.m_inventory[j];
					this.m_inventory[j] = null;
					this.update();
					return itemstack;
				}
			}
		}

		if (flag) {
			this.update();
		}

		return null;
	}

	private ItemStack takePlaceableItem(int i, int j, int k, int l) {
		boolean flag = false;
		synchronized (this.m_inventory) {
			for (int i1 = 0; i1 < 9; i1++) {
				int j1 = (this.m_state.selectedSlot + i1) % 9;
				if (this.m_inventory[j1] != null && this.m_inventory[j1].count <= 0) {
					this.m_inventory[j1] = null;
					flag = true;
				}

				if (this.m_inventory[j1] != null) {
					int k1 = this.m_inventory[j1].id;
					if (k1 > 0 && k1 < 256) {
						Item item = Item.byId[k1];
						if (item instanceof ItemBlock && this.world.mayPlace(k1, i, j, k, false, l)) {
							if (this.m_inventory[j1].count == 1) {
								ItemStack itemstack = this.m_inventory[j1];
								this.m_inventory[j1] = null;
								this.update();
								return itemstack;
							}

							ItemStack itemstack1 = this.m_inventory[j1].a(1);
							this.update();
							return itemstack1;
						}
					}
				}
			}
		}

		if (flag) {
			this.update();
		}

		return null;
	}

	public TileEntityTurtle getRealSelf() {
		TileEntityTurtle tileentityturtle = this;
		if (this.m_clientState.destroyed) {
			return null;
		}
		else {
			if (!mod_ComputerCraft.isMultiplayerClient() && this.m_moved) {
				tileentityturtle = this.m_state.latestOwner;
			}

			return tileentityturtle.world.getTileEntity(tileentityturtle.x, tileentityturtle.y, tileentityturtle.z) != tileentityturtle ? null : tileentityturtle;
		}
	}

	public boolean a(EntityHuman entityhuman) {
		TileEntityTurtle tileentityturtle = this.getRealSelf();
		if (tileentityturtle != null) {
			return LittleBlocksInterop.isLittleWorld(tileentityturtle.world)
					? entityhuman.e(tileentityturtle.x / 8.0 + 0.5, tileentityturtle.y / 8.0 + 0.5, tileentityturtle.z / 8.0 + 0.5) <= 64.0
					: entityhuman.e(tileentityturtle.x + 0.5, tileentityturtle.y + 0.5, tileentityturtle.z + 0.5) <= 64.0;
		}
		else {
			return false;
		}
	}

	public void f() {
	}

	public void g() {
	}

	public void updateClient(EntityHuman entityhuman) {
		this.m_computer.updateClient(entityhuman);
	}

	@Override
	public void handlePacket(ComputerCraftPacket computercraftpacket, EntityHuman entityhuman) {
		this.m_computer.handlePacket(computercraftpacket, entityhuman);
		if (mod_ComputerCraft.isMultiplayerServer()) {
			if (computercraftpacket.packetType == 5) {
				ComputerCraftPacket computercraftpacket1 = new ComputerCraftPacket();
				computercraftpacket1.packetType = 50;
				computercraftpacket1.dataInt = new int[] {
						this.x, this.y, this.z, this.m_clientState.animation, this.m_clientState.dir, this.m_clientState.offsetDir
				};
				mod_ComputerCraft.sendToPlayer(entityhuman, computercraftpacket1);
				ComputerCraftPacket computercraftpacket2 = this.createModemLightPacket();
				mod_ComputerCraft.sendToPlayer(entityhuman, computercraftpacket2);
			}
		}
		else {
			switch (computercraftpacket.packetType) {
				case 13:
					this.m_clientState.modemLight = computercraftpacket.dataInt[3] > 0;
					break;
				case 50:
					this.m_clientState.offsetDir = computercraftpacket.dataInt[5];
					this.setDir(computercraftpacket.dataInt[4]);
					this.startAnimation(computercraftpacket.dataInt[3]);
			}
		}
	}

	private ComputerCraftPacket createModemLightPacket() {
		ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
		computercraftpacket.packetType = 13;
		computercraftpacket.dataInt = new int[] {this.x, this.y, this.z, this.m_clientState.modemLight ? 1 : 0};
		return computercraftpacket;
	}

	public String getTurtleName() {
		return "[ComputerCraft] Turtle " + this.getComputerID();
	}

	public ItemStack[] getContents() {
		return null;
	}

	private static class ClientState {
		public int subType;
		public boolean destroyed;
		public boolean ready = false;
		public int animation;
		public int animationProgress;
		public int dir;
		public int offsetDir;
		public float rot;
		public float lastRot;
		public float offset;
		public float lastOffset;
		public float toolRot;
		public float lastToolRot;
		public boolean modemLight;

		ClientState(int i) {
			this.subType = i;
			this.destroyed = false;
			this.animation = -1;
			this.animationProgress = 0;
			this.dir = 3;
			this.offsetDir = this.dir;
			this.rot = 0.0F;
			this.lastRot = this.rot;
			this.offset = 0.0F;
			this.lastOffset = this.offset;
			this.toolRot = 0.0F;
			this.lastToolRot = 0.0F;
			this.modemLight = false;
		}
	}

	private class State {
		public TileEntityTurtle latestOwner;
		public WirelessModemPeripheral modem;
		public ITurtleListener listener;
		public LinkedList<Integer> commandQueue;
		public int commandsIssued;
		public int commandsProcessed;
		public int selectedSlot;

		State(TileEntityTurtle tileentityturtle1) {
			this.latestOwner = tileentityturtle1;
			this.listener = null;
			this.commandQueue = new LinkedList<>();
			this.commandsIssued = 0;
			this.commandsProcessed = 0;
			this.selectedSlot = 0;
		}
	}

	private static class TurtleAnimation {
		public static final int Move = 0;
		public static final int TurnLeft = 1;
		public static final int TurnRight = 2;
		public static final int Dig = 3;
		public static final int Wait = 4;
	}

	private static class TurtleGrave {
		public WeakReference<World> world;
		public int x;
		public int y;
		public int z;
		public int dir;
		public long timeOfDeath;
		public boolean modemLight;

		TurtleGrave(TileEntityTurtle tileentityturtle1) {
			this.world = new WeakReference<>(tileentityturtle1.world);
			this.x = tileentityturtle1.x;
			this.y = tileentityturtle1.y;
			this.z = tileentityturtle1.z;
			this.dir = tileentityturtle1.m_clientState.dir;
			this.timeOfDeath = System.currentTimeMillis();
			this.modemLight = tileentityturtle1.m_clientState.modemLight;
		}
	}
}
