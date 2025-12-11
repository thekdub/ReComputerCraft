package dan200.turtle.shared;

import dan200.computer.shared.ItemComputer;
import net.minecraft.server.ItemStack;


public class ItemTurtle extends ItemComputer {
	public ItemTurtle(int i) {
		super(i);
	}

	@Override
	public int filterData(int i) {
		return i;
	}

	@Override
	protected int getComputerIDFromDamage(int i) {
		return (i >> 2) - 1;
	}

	@Override
	protected int getDamageFromComputerIDAndMetadata(int i, int j) {
		return i >= 0 ? (j & 3) + (i + 1 << 2) : j & 3;
	}

	public String a(ItemStack itemstack) {
		int damage = itemstack.getData();
		int j = damage & 3;
		switch (j) {
			case 0:
			case 1:
				return "item.miningturtle";
			case 2:
				return "item.wirelessturtle";
			case 3:
				return "item.wirelessminingturtle";
			default:
				return "item.turtle";
		}
	}
}
