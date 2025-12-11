package dan200.turtle.shared;

import dan200.computer.shared.IComputerEntity;
import net.minecraft.server.*;


public class ContainerTurtle extends Container {
	private final TileEntityTurtle turtle;
	private final PlayerInventory playerInventory;
	private boolean m_initialStateTransmitted;
	private int m_lastTerminalX;
	private int m_lastTerminalY;
	private int m_lastTerminalZ;
	private int m_lastSelectedSlot;

	public ContainerTurtle(IInventory inventory, TileEntityTurtle tileentityturtle) {
		this.playerInventory = (PlayerInventory) inventory;
		this.turtle = tileentityturtle;
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			this.m_lastTerminalX = this.turtle.x;
			this.m_lastTerminalY = this.turtle.z;
			this.m_lastTerminalZ = this.turtle.y;
			this.m_lastSelectedSlot = this.turtle.getSelectedSlot();
			this.m_initialStateTransmitted = false;
		}
		else {
			this.m_lastTerminalX = 0;
			this.m_lastTerminalY = 0;
			this.m_lastTerminalZ = 0;
			this.m_lastSelectedSlot = -1;
		}

		for (int i = 0; i < 3; i++) {
			for (int l = 0; l < 3; l++) {
				this.a(new Slot(this.turtle, l + i * 3, 176 + l * 18, 126 + i * 18));
			}
		}

		for (int j = 0; j < 3; j++) {
			for (int i1 = 0; i1 < 9; i1++) {
				this.a(new Slot(inventory, i1 + j * 9 + 9, 8 + i1 * 18, 126 + j * 18));
			}
		}

		for (int k = 0; k < 9; k++) {
			this.a(new Slot(inventory, k, 8 + k * 18, 184));
		}
	}

	public int getSelectedSlot() {
		return this.m_lastSelectedSlot;
	}

	public IComputerEntity getTerminal(World world) {
		TileEntity tileentity = world.getTileEntity(this.m_lastTerminalX, this.m_lastTerminalY, this.m_lastTerminalZ);
		if (tileentity instanceof TileEntityTurtle) {
			TileEntityTurtle tileentityturtle = (TileEntityTurtle) tileentity;
			if (tileentityturtle.isTerminalReady()) {
				return tileentityturtle;
			}
		}

		return null;
	}

	public void a() {
		super.a();
		if (mod_ComputerCraft.isMultiplayerServer()) {
			int i = this.turtle.getSelectedSlot();
			TileEntityTurtle tileentityturtle = this.turtle.getRealSelf();
			if (tileentityturtle != null) {
				int j = tileentityturtle.x;
				int k = tileentityturtle.y;
				int l = tileentityturtle.z;

				for (Object listener : this.listeners) {
					ICrafting icrafting = (ICrafting) listener;
					if (j != this.m_lastTerminalX || !this.m_initialStateTransmitted) {
						icrafting.setContainerData(this, 0, j);
					}

					if (k != this.m_lastTerminalY || !this.m_initialStateTransmitted) {
						icrafting.setContainerData(this, 1, k);
					}

					if (l != this.m_lastTerminalZ || !this.m_initialStateTransmitted) {
						icrafting.setContainerData(this, 2, l);
					}

					if (this.m_lastSelectedSlot != i || !this.m_initialStateTransmitted) {
						icrafting.setContainerData(this, 3, i);
					}
				}

				this.m_lastSelectedSlot = i;
				this.m_lastTerminalX = j;
				this.m_lastTerminalY = k;
				this.m_lastTerminalZ = l;
				this.m_initialStateTransmitted = true;
			}
		}
	}

	public void updateProgressBar(int i, int j) {
		if (mod_ComputerCraft.isMultiplayerClient()) {
			switch (i) {
				case 0:
					this.m_lastTerminalX = j;
					break;
				case 1:
					this.m_lastTerminalY = j;
					break;
				case 2:
					this.m_lastTerminalZ = j;
					break;
				case 3:
					this.m_lastSelectedSlot = j;
			}
		}
	}

	public boolean b(EntityHuman entityhuman) {
		return this.turtle.a(entityhuman);
	}

	public ItemStack a(int i) {
		ItemStack itemstack = null;
		Slot slot = (Slot) this.e.get(i);
		if (slot != null && slot.c()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.cloneItemStack();
			if (i < 9) {
				if (!this.a(itemstack1, 9, 45, true)) {
					return null;
				}
			}
			else if (!this.a(itemstack1, 0, 9, false)) {
				return null;
			}

			if (itemstack1.count == 0) {
				slot.set(null);
			}
			else {
				slot.d();
			}

			if (itemstack1.count == itemstack.count) {
				return null;
			}

			slot.c(itemstack1);
		}

		return itemstack;
	}

	public EntityHuman getPlayer() {
		return this.playerInventory.player;
	}

	public IInventory getInventory() {
		return this.playerInventory;
	}
}
