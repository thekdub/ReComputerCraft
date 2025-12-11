package dan200.computer.shared;

import forge.ITextureProvider;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.Item;
import net.minecraft.server.ItemStack;
import net.minecraft.server.World;
import net.minecraft.server.mod_ComputerCraft;

import java.util.List;


public class ItemDisk extends Item implements ITextureProvider {
	private static LabelStore s_labelStore = null;

	public ItemDisk(int i) {
		super(i);
		this.e(1);
		this.a(true);
		this.textureId = 64;
	}

	public static int getDiskID(ItemStack itemstack) {
		if (itemstack.id == mod_ComputerCraft.disk.id) {
			int i = itemstack.getData();
			if (i > 0) {
				return i;
			}
		}

		return -1;
	}

	public static void setDiskID(ItemStack itemstack, int i) {
		if (itemstack.id == mod_ComputerCraft.disk.id) {
			if (i > 0) {
				itemstack.setData(i);
			}
			else {
				itemstack.setData(0);
			}
		}
	}

	public static void loadLabels() {
		s_labelStore = LabelStore.getForDisks(null);
		s_labelStore.reload();
	}

	public static void loadLabels(World world) {
		World world1 = null;
		if (s_labelStore != null) {
			world1 = s_labelStore.getWorld();
		}

		if (world != world1) {
			if (world != null) {
				s_labelStore = LabelStore.getForDisks(world);
				s_labelStore.reload();
			}
			else {
				s_labelStore = null;
			}
		}
	}

	public static void sendDiskLabelToPlayer(int i, EntityHuman entityhuman) {
		if (s_labelStore != null) {
			s_labelStore.sendLabelToPlayer(i, entityhuman);
		}
	}

	public static String getDiskLabel(int i) {
		return s_labelStore != null ? s_labelStore.getLabel(i) : null;
	}

	public static String getDiskLabel(ItemStack itemstack) {
		int i = getDiskID(itemstack);
		return getDiskLabel(i);
	}

	public static void setDiskLabel(int i, String s) {
		if (s_labelStore != null) {
			s_labelStore.setLabel(i, s);
		}
	}

	public String getTextureFile() {
		return "/terrain/ccterrain.png";
	}

	public void addInformation(ItemStack itemstack, List<String> list) {
		String s = getDiskLabel(itemstack);
		if (s != null && !s.isEmpty()) {
			list.add(s);
		}
	}
}
