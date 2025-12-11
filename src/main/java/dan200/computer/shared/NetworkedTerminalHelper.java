package dan200.computer.shared;

import dan200.computer.core.Terminal;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.TileEntity;
import net.minecraft.server.mod_ComputerCraft;


public class NetworkedTerminalHelper implements INetworkedEntity, ITerminalEntity {
	protected TileEntity m_owner;
	protected Terminal m_terminal;
	private boolean m_terminalReady;
	private boolean m_terminalChanged;

	public NetworkedTerminalHelper(TileEntity tileentity) {
		this.m_owner = tileentity;
		this.m_terminal = null;
		this.m_terminalReady = false;
		this.m_terminalChanged = false;
	}

	public NetworkedTerminalHelper(TileEntity tileentity, int i, int j) {
		this.m_owner = tileentity;
		this.m_terminal = new Terminal(i, j);
		this.m_terminalReady = !mod_ComputerCraft.isMultiplayerClient();
		this.m_terminalChanged = false;
	}

	public void copyFrom(Terminal terminal) {
		this.m_terminal = terminal;
		this.m_terminalChanged = true;
	}

	public void resize(int i, int j) {
		if (this.m_terminal == null) {
			this.m_terminal = new Terminal(i, j);
			this.m_terminalChanged = true;
		}
		else {
			this.m_terminal.resize(i, j);
		}
	}

	public void delete() {
		if (this.m_terminal != null) {
			this.m_terminal = null;
			this.m_terminalChanged = true;
		}
	}

	public void setOwner(TileEntity tileentity) {
		this.m_owner = tileentity;
	}

	public void update() {
		if (mod_ComputerCraft.isMultiplayerServer()) {
			ComputerCraftPacket computercraftpacket = null;
			if (this.m_terminal == null) {
				if (this.m_terminalChanged) {
					computercraftpacket = this.createTerminalChangedPacket(true);
				}

				this.m_terminalChanged = false;
			}
			else {
				synchronized (this.m_terminal) {
					if (this.m_terminalChanged || this.m_terminal != null && this.m_terminal.getChanged()) {
						computercraftpacket = this.createTerminalChangedPacket(this.m_terminalChanged);
						if (this.m_terminal != null) {
							this.m_terminal.clearChanged();
						}

						this.m_terminalChanged = false;
					}
				}
			}

			if (computercraftpacket != null) {
				mod_ComputerCraft.sendToAllPlayers(computercraftpacket);
			}
		}
	}

	@Override
	public boolean isDestroyed() {
		return false;
	}

	@Override
	public Terminal getTerminal() {
		return this.m_terminal;
	}

	public boolean isTerminalReady() {
		return this.m_terminalReady;
	}

	private ComputerCraftPacket createTerminalChangedPacket(boolean flag) {
		if (this.m_terminal == null) {
			ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
			computercraftpacket.packetType = 16;
			computercraftpacket.dataInt = new int[] {this.m_owner.x, this.m_owner.y, this.m_owner.z};
			return computercraftpacket;
		}
		else {
			synchronized (this.m_terminal) {
				int i = this.m_terminal.getWidth();
				int j = this.m_terminal.getHeight();
				boolean[] aflag = this.m_terminal.getLinesChanged();
				int k = this.m_terminal.getCursorBlink() ? 1 : 0;
				int l = 0;
				int i1 = 0;

				for (int j1 = 0; j1 < j; j1++) {
					if (aflag[j1] || flag) {
						if (j1 < 30) {
							k += 1 << j1 + 1;
						}
						else {
							l += 1 << j1 - 30;
						}

						i1++;
					}
				}

				ComputerCraftPacket computercraftpacket1 = new ComputerCraftPacket();
				computercraftpacket1.packetType = 3;
				if (l != 0) {
					computercraftpacket1.dataInt = new int[] {
							this.m_owner.x, this.m_owner.y, this.m_owner.z, i, j, this.m_terminal.getCursorX(), this.m_terminal.getCursorY(), k, l
					};
				}
				else {
					computercraftpacket1.dataInt = new int[] {
							this.m_owner.x, this.m_owner.y, this.m_owner.z, i, j, this.m_terminal.getCursorX(), this.m_terminal.getCursorY(), k
					};
				}

				computercraftpacket1.dataString = new String[i1];
				int k1 = 0;

				for (int l1 = 0; l1 < this.m_terminal.getHeight(); l1++) {
					if (aflag[l1] || flag) {
						computercraftpacket1.dataString[k1++] = this.m_terminal.getLine(l1).replaceAll(" +$", "");
					}
				}

				return computercraftpacket1;
			}
		}
	}

	public void requestUpdate() {
		if (mod_ComputerCraft.isMultiplayerClient()) {
			ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
			computercraftpacket.packetType = 5;
			computercraftpacket.dataInt = new int[] {this.m_owner.x, this.m_owner.y, this.m_owner.z};
			mod_ComputerCraft.sendToServer(computercraftpacket);
		}
	}

	public void updateClient(EntityHuman entityHuman) {
		if (mod_ComputerCraft.isMultiplayerServer()) {
			ComputerCraftPacket computercraftpacket = this.createTerminalChangedPacket(true);
			mod_ComputerCraft.sendToPlayer(entityHuman, computercraftpacket);
		}
	}

	@Override
	public void handlePacket(ComputerCraftPacket computercraftpacket, EntityHuman entityHuman) {
		if (mod_ComputerCraft.isMultiplayerServer()) {
			switch (computercraftpacket.packetType) {
				case 5:
					this.updateClient(entityHuman);
			}
		}
		else {
			switch (computercraftpacket.packetType) {
				case 3:
					int i = computercraftpacket.dataInt[3];
					int j = computercraftpacket.dataInt[4];
					this.resize(i, j);
					synchronized (this.m_terminal) {
						int k = 0;
						int l = computercraftpacket.dataInt[7];
						int i1 = 0;
						if (computercraftpacket.dataInt.length >= 9) {
							i1 = computercraftpacket.dataInt[8];
						}

						for (int j1 = 0; j1 < j; j1++) {
							boolean flag = false;
							if (j1 < 30) {
								flag = (l & 1 << j1 + 1) > 0;
							}
							else {
								flag = (i1 & 1 << j1 - 30) > 0;
							}

							if (flag) {
								this.m_terminal.setCursorPos(0, j1);
								this.m_terminal.clearLine();
								this.m_terminal.write(computercraftpacket.dataString[k++]);
							}
						}

						this.m_terminal.setCursorPos(computercraftpacket.dataInt[5], computercraftpacket.dataInt[6]);
						this.m_terminal.setCursorBlink((l & 1) > 0);
						this.m_terminalReady = true;
						break;
					}
				case 16:
					this.delete();
			}
		}
	}
}
