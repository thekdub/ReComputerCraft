package dan200.computer.shared;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;
import dan200.computer.core.Computer;
import dan200.computer.core.IComputerEnvironment;
import dan200.computer.core.Terminal;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.TileEntity;
import net.minecraft.server.mod_ComputerCraft;

import java.io.File;


public class NetworkedComputerHelper extends NetworkedTerminalHelper implements INetworkedEntity, IComputerEntity, IPeripheral, IComputerEnvironment {
	static final boolean $assertionsDisabled = !NetworkedComputerHelper.class.desiredAssertionStatus();
	private boolean m_firstFrame;
	private final BlockComputerBase m_block;
	private final Computer m_computer;
	private final String m_peripheralType;
	private final NetworkedComputerHelper.ClientData m_clientData;

	public NetworkedComputerHelper(TileEntity tileentity, BlockComputerBase blockcomputerbase, String s, int i, int j) {
		super(tileentity, i, j);
		this.m_block = blockcomputerbase;
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_terminal = new Terminal(i, j);
			this.m_computer = new Computer(this, this.m_terminal);
			this.m_peripheralType = s;
			this.m_clientData = null;
		}
		else {
			this.m_terminal = new Terminal(i, j);
			this.m_computer = null;
			this.m_peripheralType = null;
			this.m_clientData = new NetworkedComputerHelper.ClientData();
		}

		this.m_firstFrame = true;
	}

	@Override
	public void unload() {
		this.destroy();
	}

	@Override
	public void destroy() {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_computer.destroy();
		}
	}

	@Override
	public void update() {
		super.update();
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			if (this.m_firstFrame) {
				this.m_block.refreshInput(this.m_owner.world, this.m_owner.x, this.m_owner.y, this.m_owner.z);
				this.m_firstFrame = false;
			}

			double d = 0.05;
			this.m_computer.advance(d);
			if (this.m_computer.pollChanged()) {
				mod_ComputerCraft.notifyBlockChange(this.m_owner.world, this.m_owner.x, this.m_owner.y, this.m_owner.z, this.m_block.id);
				if (mod_ComputerCraft.isMultiplayerServer()) {
					ComputerCraftPacket computercraftpacket = this.createOutputChangedPacket();
					mod_ComputerCraft.sendToAllPlayers(computercraftpacket);
				}
			}
		}
	}

	public void writeToNBT(NBTTagCompound nbttagcompound) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_computer.writeToNBT(nbttagcompound);
		}
	}

	public void readFromNBT(NBTTagCompound nbttagcompound) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_computer.readFromNBT(nbttagcompound);
		}
	}

	@Override
	public void pressKey(char c, int i) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_computer.pressKey(c, i);
		}
		else {
			ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
			computercraftpacket.packetType = 2;
			computercraftpacket.dataInt = new int[] {this.m_owner.x, this.m_owner.y, this.m_owner.z, i};
			computercraftpacket.dataString = new String[] {"" + c};
			mod_ComputerCraft.sendToServer(computercraftpacket);
		}
	}

	@Override
	public void typeString(String s) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			for (int i = 0; i < s.length(); i++) {
				this.m_computer.pressKey(s.charAt(i), -1);
			}
		}
		else {
			ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
			computercraftpacket.packetType = 18;
			computercraftpacket.dataInt = new int[] {this.m_owner.x, this.m_owner.y, this.m_owner.z};
			computercraftpacket.dataString = new String[] {s};
			mod_ComputerCraft.sendToServer(computercraftpacket);
		}
	}

	@Override
	public void turnOn() {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_computer.turnOn();
		}
	}

	@Override
	public boolean isOn() {
		return !mod_ComputerCraft.isMultiplayerClient() ? this.m_computer.isOn() : this.m_clientData.on;
	}

	@Override
	public void shutdown() {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_computer.turnOff();
		}
		else {
			ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
			computercraftpacket.packetType = 12;
			computercraftpacket.dataInt = new int[] {this.m_owner.x, this.m_owner.y, this.m_owner.z};
			mod_ComputerCraft.sendToServer(computercraftpacket);
		}
	}

	@Override
	public void reboot() {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_computer.reboot();
		}
		else {
			ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
			computercraftpacket.packetType = 9;
			computercraftpacket.dataInt = new int[] {this.m_owner.x, this.m_owner.y, this.m_owner.z};
			mod_ComputerCraft.sendToServer(computercraftpacket);
		}
	}

	@Override
	public void terminate() {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_computer.terminate();
		}
		else {
			ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
			computercraftpacket.packetType = 6;
			computercraftpacket.dataInt = new int[] {this.m_owner.x, this.m_owner.y, this.m_owner.z};
			mod_ComputerCraft.sendToServer(computercraftpacket);
		}
	}

	public boolean isCursorVisible() {
		return !mod_ComputerCraft.isMultiplayerClient() ? this.m_computer.isBlinking() : this.m_clientData.on && this.m_clientData.blinking;
	}

	@Override
	public int getComputerID() {
		return !mod_ComputerCraft.isMultiplayerClient() ? this.m_computer.getID() : -1;
	}

	@Override
	public void setComputerID(int i) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_computer.setID(i);
		}
	}

	@Override
	public boolean getPowerOutput(int i) {
		return !mod_ComputerCraft.isMultiplayerClient() ? this.m_computer.getOutput(i) : this.m_clientData.output[i];
	}

	@Override
	public void setPowerInput(int i, boolean flag) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_computer.setInput(i, flag);
		}
	}

	@Override
	public int getBundledPowerOutput(int i) {
		return !mod_ComputerCraft.isMultiplayerClient() ? this.m_computer.getBundledOutput(i) : this.m_clientData.bundledOutput[i];
	}

	@Override
	public void setBundledPowerInput(int i, int j) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_computer.setBundledInput(i, j);
		}
	}

	@Override
	public void setPeripheral(int i, IPeripheral iperipheral) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_computer.setPeripheral(i, iperipheral);
		}
	}

	public void addPeripheralAsAPI(IPeripheral iperipheral) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_computer.addPeripheralAsAPI(iperipheral);
		}
	}

	@Override
	public String getType() {
		return this.m_peripheralType;
	}

	@Override
	public String[] getMethodNames() {
		return new String[] {"turnOn", "shutdown", "reboot", "getID"};
	}

	@Override
	public Object[] callMethod(IComputerAccess icomputeraccess, int i, Object[] aobj) throws Exception {
		switch (i) {
			case 0:
				this.turnOn();
				return null;
			case 1:
				this.shutdown();
				return null;
			case 2:
				this.reboot();
				return null;
			case 3:
				if (this.isOn()) {
					return new Object[] {this.getComputerID()};
				}

				return null;
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
	public void attach(IComputerAccess computerAccess, String s) {
	}

	@Override
	public void detach(IComputerAccess computerAccess) {
	}

	@Override
	public File getStaticDir() {
		return mod_ComputerCraft.getBaseDir();
	}

	@Override
	public File getSaveDir() {
		return mod_ComputerCraft.getWorldDir(this.m_owner.world);
	}

	@Override
	public double getTimeOfDay() {
		return (this.m_owner.world.getTime() + 6000L) % 24000L / 1000.0;
	}

	@Override
	public boolean isHTTPEnabled() {
		return mod_ComputerCraft.enableAPI_http > 0;
	}

	private ComputerCraftPacket createOutputChangedPacket() {
		ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
		computercraftpacket.packetType = 4;
		int i = 0;
		if (this.m_computer.isOn()) {
			i++;
		}

		if (this.m_computer.isBlinking()) {
			i += 2;
		}

		for (int j = 0; j < 6; j++) {
			if (this.m_computer.getOutput(j)) {
				i += 1 << j + 2;
			}
		}

		computercraftpacket.dataInt = new int[] {
				this.m_owner.x,
				this.m_owner.y,
				this.m_owner.z,
				i,
				this.m_computer.getBundledOutput(0),
				this.m_computer.getBundledOutput(1),
				this.m_computer.getBundledOutput(2),
				this.m_computer.getBundledOutput(3),
				this.m_computer.getBundledOutput(3),
				this.m_computer.getBundledOutput(5)
		};
		return computercraftpacket;
	}

	@Override
	public void updateClient(EntityHuman entityHuman) {
		super.updateClient(entityHuman);
		if (mod_ComputerCraft.isMultiplayerServer()) {
			ComputerCraftPacket computercraftpacket = this.createOutputChangedPacket();
			mod_ComputerCraft.sendToPlayer(entityHuman, computercraftpacket);
		}
	}

	@Override
	public void handlePacket(ComputerCraftPacket computercraftpacket, EntityHuman entityhuman) {
		super.handlePacket(computercraftpacket, entityhuman);
		if (mod_ComputerCraft.isMultiplayerServer()) {
			switch (computercraftpacket.packetType) {
				case 2:
					int i = computercraftpacket.dataInt[3];
					char c = computercraftpacket.dataString[0].charAt(0);
					this.pressKey(c, i);
					break;
				case 6:
					this.terminate();
					break;
				case 9:
					this.reboot();
					break;
				case 12:
					this.shutdown();
					break;
				case 18:
					String s = computercraftpacket.dataString[0];
					this.typeString(s);
			}
		}
		else {
			switch (computercraftpacket.packetType) {
				case 4:
					int j = computercraftpacket.dataInt[3];
					this.m_clientData.on = (j & 1) > 0;
					this.m_clientData.blinking = (j & 2) > 0;

					for (int k = 0; k < 6; k++) {
						this.m_clientData.output[k] = (j & 1 << k + 2) > 0;
						this.m_clientData.bundledOutput[k] = computercraftpacket.dataInt[4 + k];
					}

					mod_ComputerCraft.notifyBlockChange(this.m_owner.world, this.m_owner.x, this.m_owner.y, this.m_owner.z, this.m_block.id);
			}
		}
	}

	private class ClientData {
		boolean on = false;
		boolean blinking = false;
		boolean[] output = new boolean[6];
		int[] bundledOutput = new int[6];

		ClientData() {
		}
	}
}
