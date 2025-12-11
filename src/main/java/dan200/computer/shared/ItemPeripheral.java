package dan200.computer.shared;

import net.minecraft.server.ItemBlock;
import net.minecraft.server.ItemStack;


public class ItemPeripheral extends ItemBlock {
	public ItemPeripheral(int i) {
		super(i);
		this.e(64);
		this.a(true);
	}

	public int filterData(int damage) {
		return damage;
	}

	public String a(ItemStack itemstack) {
		int damage = itemstack.getData();
		switch (damage) {
			case 0:
				return "item.diskdrive";
			case 1:
				return "item.wirelessmodem";
			case 2:
				return "item.monitor";
			default:
				return "";
		}
	}
}
