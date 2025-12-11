package dan200.computer.shared;

import net.minecraft.server.Container;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.IInventory;
import net.minecraft.server.ItemStack;
import net.minecraft.server.PlayerInventory;
import net.minecraft.server.Slot;


public class ContainerDiskDrive extends Container {
	private final TileEntityDiskDrive diskDrive;
	private final PlayerInventory playerInventory;

	public ContainerDiskDrive(IInventory inventory, TileEntityDiskDrive tileEntity) {
		this.playerInventory = (PlayerInventory) inventory;
		this.diskDrive = tileEntity;
		this.a(new Slot(this.diskDrive, 0, 80, 35));

		for (int i = 0; i < 3; i++) {
			for (int k = 0; k < 9; k++) {
				this.a(new Slot(inventory, k + i * 9 + 9, 8 + k * 18, 84 + i * 18));
			}
		}

		for (int j = 0; j < 9; j++) {
			this.a(new Slot(inventory, j, 8 + j * 18, 142));
		}
	}

	public boolean b(EntityHuman entityhuman) {
		return this.diskDrive.a(entityhuman);
	}

	public ItemStack a(int i) {
		ItemStack itemstack = null;
		Slot slot = (Slot) this.e.get(i);
		if (slot != null && slot.c()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.cloneItemStack();
			if (i == 0) {
				if (!this.a(itemstack1, 1, 37, true)) {
					return null;
				}
			}
			else if (!this.a(itemstack1, 0, 1, false)) {
				return null;
			}

			if (itemstack1.count == 0) {
				slot.c(null);
			}
			else {
				slot.d();
			}

			if (itemstack1.count == itemstack.count) {
				return null;
			}

			slot.set(itemstack1);
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
