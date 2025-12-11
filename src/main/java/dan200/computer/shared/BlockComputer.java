package dan200.computer.shared;

import net.minecraft.server.*;

import java.util.ArrayList;


public class BlockComputer extends BlockComputerBase {
	public static final int blinkTexture = 4;

	public BlockComputer(int i) {
		super(i, Material.STONE);
	}

	public void addCreativeItems(ArrayList arraylist) {
		arraylist.add(new ItemStack(this));
	}

	public boolean b() {
		return false;
	}

	public boolean a() {
		return false;
	}

	public boolean isBlockSolidOnSide(World world, int i, int j, int k, int l) {
		return true;
	}

	@Override
	public void onPlace(World world, int i, int j, int k) {
		super.onPlace(world, i, j, k);
		this.setDefaultDirection(world, i, j, k);
		this.refreshInput(world, i, j, k);
	}

	private void setDefaultDirection(World world, int i, int j, int k) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			world.setData(i, j, k, 3);
		}
	}

	public void postPlace(World world, int i, int j, int k, EntityLiving entityliving) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			int l = MathHelper.floor(entityliving.yaw * 4.0F / 360.0F + 0.5) & 3;
			if (l == 0) {
				world.setData(i, j, k, 2);
			}

			if (l == 1) {
				world.setData(i, j, k, 5);
			}

			if (l == 2) {
				world.setData(i, j, k, 3);
			}

			if (l == 3) {
				world.setData(i, j, k, 4);
			}

			this.refreshInput(world, i, j, k);
		}
	}

	public int getBlockTexture(IBlockAccess iblockaccess, int i, int j, int k, int l) {
		if (l != 1 && l != 0) {
			int i1 = iblockaccess.getData(i, j, k);
			if (l == i1) {
				TileEntity tileentity = iblockaccess.getTileEntity(i, j, k);
				if (tileentity instanceof TileEntityComputer) {
					TileEntityComputer tileentitycomputer = (TileEntityComputer) tileentity;
					if (tileentitycomputer.isOn()) {
						return !tileentitycomputer.isCursorVisible() ? 17 : 4;
					}
				}

				return 16;
			}
			else {
				return 19;
			}
		}
		else {
			return 18;
		}
	}

	public int a(int i) {
		if (i != 1 && i != 0) {
			return i != 3 ? 19 : 4;
		}
		else {
			return 18;
		}
	}

	public boolean interact(World world, int i, int j, int k, EntityHuman entityhuman) {
		if (mod_ComputerCraft.isMultiplayerClient()) {
			return true;
		}
		else if (!entityhuman.isSneaking()) {
			TileEntity tileentity = world.getTileEntity(i, j, k);
			if (tileentity instanceof TileEntityComputer) {
				TileEntityComputer tileentitycomputer = (TileEntityComputer) tileentity;
				mod_ComputerCraft.openComputerGUI(entityhuman, tileentitycomputer);
				tileentitycomputer.turnOn();
				tileentitycomputer.updateClient(entityhuman);
			}

			return true;
		}
		else {
			return false;
		}
	}

	public TileEntity a_() {
		Class<?> clazz = TileEntityComputer.getComputerClass();

		try {
			return (TileEntity) clazz.newInstance();
		} catch (Exception var3) {
			return new TileEntityComputer();
		}
	}

	@Override
	public int getDirection(IBlockAccess iblockaccess, int i, int j, int k) {
		return iblockaccess.getData(i, j, k);
	}
}
