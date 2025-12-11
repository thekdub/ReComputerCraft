package dan200.computer.shared;

import net.minecraft.server.EntityHuman;
import net.minecraft.server.Facing;
import net.minecraft.server.ItemBlock;
import net.minecraft.server.ItemStack;
import net.minecraft.server.TileEntity;
import net.minecraft.server.World;

import java.util.List;


public class ItemComputer extends ItemBlock {
	private static LabelStore s_labelStore = null;

	public ItemComputer(int i) {
		super(i);
		this.e(64);
		this.a(true);
	}

	public static void loadLabels() {
		s_labelStore = LabelStore.getForComputers(null);
		s_labelStore.reload();
	}

	public static void loadLabels(World world) {
		World world1 = null;
		if (s_labelStore != null) {
			world1 = s_labelStore.getWorld();
		}

		if (world != world1) {
			if (world != null) {
				s_labelStore = LabelStore.getForComputers(world);
				s_labelStore.reload();
			}
			else {
				s_labelStore = null;
			}
		}
	}

	public static void sendComputerLabelToPlayer(int i, EntityHuman entityhuman) {
		if (s_labelStore != null) {
			s_labelStore.sendLabelToPlayer(i, entityhuman);
		}
	}

	public static String getComputerLabel(int i) {
		return s_labelStore != null ? s_labelStore.getLabel(i) : null;
	}

	public static void setComputerLabel(int i, String s) {
		if (s_labelStore != null) {
			s_labelStore.setLabel(i, s);
		}
	}

	public int filterData(int i) {
		return i;
	}

	protected int getComputerIDFromDamage(int i) {
		return i - 1;
	}

	protected int getDamageFromComputerIDAndMetadata(int i, int j) {
		return i >= 0 ? i + 1 : 0;
	}

	public ItemStack createFromComputer(IComputerEntity computerentity, int i) {
		int j = computerentity.getComputerID();
		String s = getComputerLabel(j);
		return s != null
				? new ItemStack(this.a(), 1, this.getDamageFromComputerIDAndMetadata(j, i))
				: new ItemStack(this.a(), 1, this.getDamageFromComputerIDAndMetadata(-1, i));
	}

	public void setupComputerAfterPlacement(ItemStack itemstack, World world, int i, int j, int k) {
		int l = itemstack.getData();
		int i1 = this.getComputerIDFromDamage(l);
		if (i1 >= 0) {
			TileEntity tileentity = world.getTileEntity(i, j, k);
			if (tileentity instanceof IComputerEntity) {
				IComputerEntity computerentity = (IComputerEntity) tileentity;
				computerentity.setComputerID(i1);
			}
		}
	}

	public boolean interactWith(ItemStack itemstack, EntityHuman entityhuman, World world, int i, int j, int k, int l) {
		if (super.interactWith(itemstack, entityhuman, world, i, j, k, l)) {
			i += Facing.b[l];
			j += Facing.c[l];
			k += Facing.d[l];
			this.setupComputerAfterPlacement(itemstack, world, i, j, k);
			return true;
		}
		else {
			return false;
		}
	}

	public String getComputerLabel(ItemStack itemstack) {
		int i = this.getComputerIDFromDamage(itemstack.getData());
		return getComputerLabel(i);
	}

	public void addInformation(ItemStack itemstack, List<String> list) {
		String s = this.getComputerLabel(itemstack);
		if (s != null && !s.isEmpty()) {
			list.add(s);
		}
	}
}
