package dan200.computer.shared;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;
import dan200.computer.core.Terminal;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.Facing;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.TileEntity;
import net.minecraft.server.mod_ComputerCraft;


public class TileEntityMonitor extends TileEntity implements IDestroyableEntity, INetworkedEntity, ITerminalEntity, IPeripheral {
	public static final int MaxWidth = 8;
	public static final int MaxHeight = 6;
	public int m_width = 1;
	public int m_height = 1;
	public int m_xIndex = 0;
	public int m_yIndex = 0;
	public int m_displayList = -1;
	private boolean m_destroyed = false;
	private boolean m_ignoreMe = false;
	private final NetworkedTerminalHelper m_terminal = new NetworkedTerminalHelper(this);
	private Terminal m_originTerminal = null;
	private int m_connections = 0;
	private int m_totalConnections = 0;
	private int m_textScale = 1;
	private boolean m_changed = false;
	private int m_dir = 2;

	@Override
	public void destroy() {
		if (!this.m_destroyed) {
			this.m_destroyed = true;
			if (!mod_ComputerCraft.isMultiplayerClient()) {
				this.contractNeighbours();
			}
		}
	}

	public void m() {
		super.m();
		this.m_terminal.requestUpdate();
	}

	@Override
	public boolean isDestroyed() {
		return this.m_destroyed;
	}

	public void b(NBTTagCompound nbttagcompound) {
		super.b(nbttagcompound);
		nbttagcompound.setInt("xIndex", this.m_xIndex);
		nbttagcompound.setInt("yIndex", this.m_yIndex);
		nbttagcompound.setInt("width", this.m_width);
		nbttagcompound.setInt("height", this.m_height);
		nbttagcompound.setInt("dir", this.m_dir);
	}

	public void a(NBTTagCompound nbttagcompound) {
		super.a(nbttagcompound);
		this.m_xIndex = nbttagcompound.getInt("xIndex");
		this.m_yIndex = nbttagcompound.getInt("yIndex");
		this.m_width = nbttagcompound.getInt("width");
		this.m_height = nbttagcompound.getInt("height");
		this.m_dir = nbttagcompound.getInt("dir");
	}

	public void q_() {
		this.m_terminal.update();
		if (mod_ComputerCraft.isMultiplayerServer() && this.pollChanged()) {
			ComputerCraftPacket computercraftpacket = this.createMonitorChangedPacket();
			mod_ComputerCraft.sendToAllPlayers(computercraftpacket);
		}
	}

	public boolean pollChanged() {
		if (this.m_changed) {
			this.m_changed = false;
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public String getType() {
		return "monitor";
	}

	@Override
	public String[] getMethodNames() {
		return new String[] {"write", "scroll", "setCursorPos", "setCursorBlink", "getCursorPos", "getSize", "clear", "clearLine", "setTextScale"};
	}

	@Override
	public Object[] callMethod(IComputerAccess computerAccess, int i, Object[] aobj) throws Exception {
		TileEntityMonitor monitor = this.origin();
		Terminal terminal = this.m_originTerminal;
		if (monitor != null && terminal != null) {
			switch (i) {
				case 0:
					String s = null;
					if (aobj.length > 0) {
						s = aobj[0].toString();
					}
					else {
						s = "";
					}

					synchronized (terminal) {
						terminal.write(s);
						terminal.setCursorPos(terminal.getCursorX() + s.length(), terminal.getCursorY());
						return null;
					}
				case 1:
					if (aobj.length >= 1 && aobj[0] instanceof Double) {
						synchronized (terminal) {
							terminal.scroll((int) ((Double) aobj[0]).doubleValue());
							return null;
						}
					}

					throw new Exception("Expected number");
				case 2:
					if (aobj.length >= 2 && aobj[0] instanceof Double && aobj[1] instanceof Double) {
						synchronized (terminal) {
							int k = (int) ((Double) aobj[0]).doubleValue() - 1;
							int l = (int) ((Double) aobj[1]).doubleValue() - 1;
							terminal.setCursorPos(k, l);
							return null;
						}
					}

					throw new Exception("Expected number, number");
				case 3:
					if (aobj.length >= 1 && aobj[0] instanceof Boolean) {
						synchronized (terminal) {
							terminal.setCursorBlink((Boolean) aobj[0]);
							return null;
						}
					}

					throw new Exception("Expected boolean");
				case 4:
					synchronized (terminal) {
						return new Object[] {terminal.getCursorX() + 1, terminal.getCursorY() + 1};
					}
				case 5:
					synchronized (terminal) {
						return new Object[] {terminal.getWidth(), terminal.getHeight()};
					}
				case 6:
					synchronized (terminal) {
						terminal.clear();
						return null;
					}
				case 7:
					synchronized (terminal) {
						terminal.clearLine();
						return null;
					}
				case 8:
					if (aobj.length >= 1 && aobj[0] instanceof Double) {
						int j = (int) ((Double) aobj[0]).doubleValue();
						if (j >= 1 && j <= 5) {
							synchronized (terminal) {
								if (monitor.m_textScale != j) {
									monitor.m_textScale = j;
									monitor.rebuildTerminal(null);
									monitor.m_changed = true;
								}

								return null;
							}
						}

						throw new Exception("Expected number in range 1-5");
					}

					throw new Exception("Expected number");
				default:
					return null;
			}
		}
		else {
			throw new Exception("Invalid monitor setup");
		}
	}

	@Override
	public boolean canAttachToSide(int i) {
		return true;
	}

	@Override
	public void attach(IComputerAccess computerAccess, String s) {
		this.m_connections++;
		TileEntityMonitor monitor = this.origin();
		if (monitor != null) {
			monitor.m_totalConnections++;
			if (monitor.m_totalConnections == 1) {
				monitor.m_textScale = 1;
				monitor.rebuildTerminal(null);
				monitor.m_changed = true;
			}
		}
	}

	@Override
	public void detach(IComputerAccess computerAccess) {
		this.m_connections--;
		TileEntityMonitor monitor = this.origin();
		if (monitor != null) {
			monitor.m_totalConnections--;
			if (monitor.m_totalConnections == 0) {
				monitor.destroyTerminal();
			}
		}
	}

	@Override
	public void handlePacket(ComputerCraftPacket computercraftpacket, EntityHuman entityhuman) {
		this.m_terminal.handlePacket(computercraftpacket, entityhuman);
		if (mod_ComputerCraft.isMultiplayerServer()) {
			if (computercraftpacket.packetType == 5) {
				ComputerCraftPacket computercraftpacket1 = this.createMonitorChangedPacket();
				mod_ComputerCraft.sendToPlayer(entityhuman, computercraftpacket1);
			}
		}
		else {
			if (computercraftpacket.packetType == 17) {
				this.m_xIndex = computercraftpacket.dataInt[3];
				this.m_yIndex = computercraftpacket.dataInt[4];
				this.m_width = computercraftpacket.dataInt[5];
				this.m_height = computercraftpacket.dataInt[6];
				this.m_textScale = computercraftpacket.dataInt[7];
				this.m_dir = computercraftpacket.dataInt[8];
				mod_ComputerCraft.notifyBlockChange(this.world, this.x, this.y, this.z, mod_ComputerCraft.peripheral.id);
			}
		}
	}

	private ComputerCraftPacket createMonitorChangedPacket() {
		ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
		computercraftpacket.packetType = 17;
		computercraftpacket.dataInt = new int[] {this.x, this.y, this.z, this.m_xIndex, this.m_yIndex, this.m_width, this.m_height, this.m_textScale, this.m_dir};
		return computercraftpacket;
	}

	@Override
	public Terminal getTerminal() {
		return this.m_terminal != null ? this.m_terminal.getTerminal() : null;
	}

	public float getTextScale() {
		return this.m_textScale * 2.0F;
	}

	private void rebuildTerminal(Terminal terminal) {
		float f = this.getTextScale();
		int i = (int) ((13.34F * (this.m_height - 1) + 10.0F) / f);
		int j = (int) ((20.0F * this.m_width - 5.0F) / f);
		if (terminal != null) {
			this.m_terminal.copyFrom(terminal);
		}

		this.m_terminal.resize(j, i);
		this.propagateTerminal();
	}

	private void destroyTerminal() {
		this.m_terminal.delete();
		this.propagateTerminal();
	}

	private void propagateTerminal() {
		Terminal terminal = this.getTerminal();

		for (int i = 0; i < this.m_height; i++) {
			for (int j = 0; j < this.m_width; j++) {
				TileEntityMonitor monitor = this.getNeighbour(j, i);
				if (monitor != null) {
					if (j != 0 || i != 0) {
						monitor.m_terminal.delete();
					}

					monitor.m_originTerminal = terminal;
					monitor.m_textScale = this.m_textScale;
				}
			}
		}
	}

	public int getDir() {
		return this.m_dir;
	}

	public void setDir(int i) {
		this.m_dir = i;
		this.m_changed = true;
	}

	private int getRight() {
		int i = this.getDir();
		switch (i) {
			case 2:
				return 4;
			case 3:
				return 5;
			case 4:
				return 3;
			case 5:
				return 2;
			default:
				return i;
		}
	}

	private TileEntityMonitor getSimilarMonitorAt(int i, int j, int k) {
		if (j >= 0 && j < this.world.getHeight()) {
			TileEntity tileEntity = this.world.getTileEntity(i, j, k);
			if (tileEntity instanceof TileEntityMonitor) {
				TileEntityMonitor monitor = (TileEntityMonitor) tileEntity;
				if (monitor.getDir() == this.getDir() && !monitor.m_destroyed && !monitor.m_ignoreMe) {
					return monitor;
				}
			}
		}

		return null;
	}

	private TileEntityMonitor getNeighbour(int i, int j) {
		int k = this.getRight();
		int l = -this.m_xIndex + i;
		return this.getSimilarMonitorAt(this.x + Facing.b[k] * l, this.y - this.m_yIndex + j, this.z + Facing.d[k] * l);
	}

	private TileEntityMonitor origin() {
		return this.getNeighbour(0, 0);
	}

	private void resize(int i, int j) {
		this.resize(i, j, false);
	}

	private void resize(int i, int j, boolean flag) {
		int k = this.getRight();
		int l = Facing.b[k];
		int i1 = Facing.d[k];
		int j1 = 0;
		int k1 = 0;
		Terminal terminal = null;
		int l1 = 1;

		for (int i2 = 0; i2 < j; i2++) {
			for (int j2 = 0; j2 < i; j2++) {
				TileEntityMonitor monitor = this.getSimilarMonitorAt(this.x + l * j2, this.y + i2, this.z + i1 * j2);
				if (monitor != null) {
					j1 += monitor.m_connections;
					if (!flag && monitor.m_connections > k1) {
						terminal = monitor.m_originTerminal;
						l1 = monitor.m_textScale;
						k1 = monitor.m_connections;
					}

					monitor.m_totalConnections = 0;
					monitor.m_xIndex = j2;
					monitor.m_yIndex = i2;
					monitor.m_width = i;
					monitor.m_height = j;
					monitor.m_changed = true;
				}
			}
		}

		this.m_totalConnections = j1;
		if (j1 > 0) {
			this.m_textScale = terminal == null ? 1 : l1;
			this.rebuildTerminal(terminal);
			this.m_changed = true;
		}
		else {
			this.m_textScale = 1;
			this.destroyTerminal();
		}

		this.world.b(this.x, this.y, this.z, this.x + l * i, this.y + j, this.z + i1 * i);
	}

	private boolean mergeLeft() {
		TileEntityMonitor monitor = this.getNeighbour(-1, 0);
		if (monitor != null && monitor.m_yIndex == 0 && monitor.m_height == this.m_height) {
			int i = monitor.m_width + this.m_width;
			if (i <= 8) {
				monitor.origin().resize(i, this.m_height);
				monitor.expand();
				return true;
			}
		}

		return false;
	}

	private boolean mergeRight() {
		TileEntityMonitor monitor = this.getNeighbour(this.m_width, 0);
		if (monitor != null && monitor.m_yIndex == 0 && monitor.m_height == this.m_height) {
			int i = this.m_width + monitor.m_width;
			if (i <= 8) {
				this.origin().resize(i, this.m_height);
				this.expand();
				return true;
			}
		}

		return false;
	}

	private boolean mergeUp() {
		TileEntityMonitor monitor = this.getNeighbour(0, this.m_height);
		if (monitor != null && monitor.m_xIndex == 0 && monitor.m_width == this.m_width) {
			int i = monitor.m_height + this.m_height;
			if (i <= 6) {
				this.origin().resize(this.m_width, i);
				this.expand();
				return true;
			}
		}

		return false;
	}

	private boolean mergeDown() {
		TileEntityMonitor monitor = this.getNeighbour(0, -1);
		if (monitor != null && monitor.m_xIndex == 0 && monitor.m_width == this.m_width) {
			int i = this.m_height + monitor.m_height;
			if (i <= 6) {
				monitor.origin().resize(this.m_width, i);
				monitor.expand();
				return true;
			}
		}

		return false;
	}

	public void expand() {
		while (this.mergeLeft() || this.mergeRight() || this.mergeUp() || this.mergeDown()) {
			continue;
			// Do nothing because these are conditions and actions!
		}
	}

	public void contractNeighbours() {
		this.m_ignoreMe = true;
		if (this.m_xIndex > 0) {
			TileEntityMonitor monitor = this.getNeighbour(this.m_xIndex - 1, this.m_yIndex);
			if (monitor != null) {
				monitor.contract();
			}
		}

		if (this.m_xIndex + 1 < this.m_width) {
			TileEntityMonitor monitor1 = this.getNeighbour(this.m_xIndex + 1, this.m_yIndex);
			if (monitor1 != null) {
				monitor1.contract();
			}
		}

		if (this.m_yIndex > 0) {
			TileEntityMonitor monitor2 = this.getNeighbour(this.m_xIndex, this.m_yIndex - 1);
			if (monitor2 != null) {
				monitor2.contract();
			}
		}

		if (this.m_yIndex + 1 < this.m_height) {
			TileEntityMonitor monitor3 = this.getNeighbour(this.m_xIndex, this.m_yIndex + 1);
			if (monitor3 != null) {
				monitor3.contract();
			}
		}

		this.m_ignoreMe = false;
	}

	public void contract() {
		int i = this.m_height;
		int j = this.m_width;
		TileEntityMonitor monitor = this.origin();
		if (monitor == null) {
			TileEntityMonitor monitor1 = null;
			TileEntityMonitor monitor2 = null;
			if (j > 1) {
				monitor1 = this.getNeighbour(1, 0);
			}

			if (i > 1) {
				monitor2 = this.getNeighbour(0, 1);
			}

			Terminal terminal = null;
			if (monitor1 != null) {
				monitor1.resize(j - 1, 1);
				terminal = monitor1.getTerminal();
			}

			if (monitor2 != null) {
				monitor2.resize(j, i - 1, terminal != null);
			}

			if (monitor1 != null) {
				monitor1.expand();
			}

			if (monitor2 != null) {
				monitor2.expand();
			}
		}
		else {
			for (int k = 0; k < i; k++) {
				for (int l = 0; l < j; l++) {
					TileEntityMonitor tileentitymonitor3 = monitor.getNeighbour(l, k);
					if (tileentitymonitor3 == null) {
						TileEntityMonitor monitor4 = null;
						TileEntityMonitor monitor5 = null;
						TileEntityMonitor monitor6 = null;
						TileEntityMonitor monitor7 = null;
						Terminal terminal1 = null;
						if (k > 0) {
							monitor4 = monitor;
							monitor.resize(j, k);
							terminal1 = monitor.getTerminal();
						}

						if (l > 0) {
							monitor5 = monitor.getNeighbour(0, k);
							monitor5.resize(l, 1, terminal1 != null);
							if (terminal1 == null) {
								terminal1 = monitor5.getTerminal();
							}
						}

						if (l + 1 < j) {
							monitor6 = monitor.getNeighbour(l + 1, k);
							monitor6.resize(j - (l + 1), 1, terminal1 != null);
							if (terminal1 == null) {
								terminal1 = monitor6.getTerminal();
							}
						}

						if (k + 1 < i) {
							monitor7 = monitor.getNeighbour(0, k + 1);
							monitor7.resize(j, i - (k + 1), terminal1 != null);
						}

						if (monitor4 != null) {
							monitor4.expand();
						}

						if (monitor5 != null) {
							monitor5.expand();
						}

						if (monitor6 != null) {
							monitor6.expand();
						}

						if (monitor7 != null) {
							monitor7.expand();
						}

						return;
					}
				}
			}
		}
	}
}
