package dan200.computer.shared;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;
import dan200.computer.core.Terminal;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.Packet;
import net.minecraft.server.TileEntity;
import net.minecraft.server.mod_ComputerCraft;


public class TileEntityComputer extends TileEntity implements INetworkedEntity, IComputerEntity, IPeripheral {
	private final NetworkedComputerHelper m_computer;
	private boolean m_destroyed;

	public TileEntityComputer() {
		boolean flag = mod_ComputerCraft.isMultiplayerClient();
		this.m_computer = new NetworkedComputerHelper(this, mod_ComputerCraft.computer, "computer", 50, 18);
	}

	public static Class getComputerClass() {
		if (RedPowerInterop.isRedPowerInstalled()) {
			try {
				return Class.forName("dan200.computer.shared.RedPowerTileEntityComputer");
			} catch (ClassNotFoundException var1) {
				System.out.println("ComputerCraft: Exception loading dan200.computer.shared.RedPowerTileEntityComputer");
				System.out.println("ComputerCraft: Computers will not have RedPower support");
			}
		}

		return TileEntityComputer.class;
	}

	public void m() {
		super.m();
		this.m_computer.requestUpdate();
	}

	public void j() {
		super.j();
	}

	public Packet d() {
		return null;
	}

	@Override
	public void unload() {
		this.destroy();
	}

	@Override
	public void destroy() {
		if (!this.m_destroyed) {
			this.m_computer.destroy();
			this.m_destroyed = true;
		}
	}

	public void q_() {
		this.m_computer.update();
	}

	public void b(NBTTagCompound nbttagcompound) {
		super.b(nbttagcompound);
		this.m_computer.writeToNBT(nbttagcompound);
	}

	public void a(NBTTagCompound nbttagcompound) {
		super.a(nbttagcompound);
		this.m_computer.readFromNBT(nbttagcompound);
	}

	@Override
	public boolean isDestroyed() {
		return this.m_destroyed;
	}

	@Override
	public Terminal getTerminal() {
		return this.m_computer.getTerminal();
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

	public boolean isCursorVisible() {
		return this.m_computer.isCursorVisible();
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
		return this.m_computer.getPowerOutput(i);
	}

	@Override
	public void setPowerInput(int i, boolean flag) {
		this.m_computer.setPowerInput(i, flag);
	}

	@Override
	public int getBundledPowerOutput(int i) {
		return this.m_computer.getBundledPowerOutput(i);
	}

	@Override
	public void setBundledPowerInput(int i, int j) {
		this.m_computer.setBundledPowerInput(i, j);
	}

	@Override
	public void setPeripheral(int i, IPeripheral iperipheral) {
		this.m_computer.setPeripheral(i, iperipheral);
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
	public Object[] callMethod(IComputerAccess icomputeraccess, int i, Object[] aobj) throws Exception {
		return this.m_computer.callMethod(icomputeraccess, i, aobj);
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

	public void updateClient(EntityHuman entityhuman) {
		this.m_computer.updateClient(entityhuman);
	}

	@Override
	public void handlePacket(ComputerCraftPacket computercraftpacket, EntityHuman entityhuman) {
		this.m_computer.handlePacket(computercraftpacket, entityhuman);
	}
}
