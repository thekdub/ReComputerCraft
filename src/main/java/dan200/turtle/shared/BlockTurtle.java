package dan200.turtle.shared;

import dan200.computer.shared.BlockComputerBase;
import net.minecraft.server.*;

import java.lang.reflect.Constructor;
import java.util.ArrayList;


public class BlockTurtle extends BlockComputerBase {
	public int blockRenderID = -1;

	public BlockTurtle(int i) {
		super(i, Material.ORE);
	}

	public void addCreativeItems(ArrayList arraylist) {
		arraylist.add(new ItemStack(this, 1, 0));
		arraylist.add(new ItemStack(this, 1, 1));
		arraylist.add(new ItemStack(this, 1, 2));
		arraylist.add(new ItemStack(this, 1, 3));
	}

	public int c() {
		return this.blockRenderID;
	}

	public boolean b() {
		return false;
	}

	public boolean a() {
		return false;
	}

	public void updateShape(IBlockAccess iblockaccess, int i, int j, int k) {
		TileEntity tileEntity = iblockaccess.getTileEntity(i, j, k);
		if (tileEntity instanceof TileEntityTurtle) {
			TileEntityTurtle turtle = (TileEntityTurtle) tileEntity;
			int l = turtle.getOffsetDir();
			float f = turtle.getRenderOffset(1.0F);
			float f1 = f * Facing.b[l];
			float f2 = f * Facing.c[l];
			float f3 = f * Facing.d[l];
			this.a(f1 + 0.125F, f2 + 0.125F, f3 + 0.125F, f1 + 0.875F, f2 + 0.875F, f3 + 0.875F);
		}
	}

	public AxisAlignedBB e(World world, int i, int j, int k) {
		this.updateShape(world, i, j, k);
		return super.e(world, i, j, k);
	}

	private TileEntityTurtle createDefaultTurtle(int i, int j) {
		Class<?> clazz = TileEntityTurtle.getTurtleClass();

		try {
			Constructor<?> constructor = clazz.getConstructor(int.class, int.class);
			return (TileEntityTurtle) constructor.newInstance(j, i);
		} catch (Exception var5) {
			var5.printStackTrace();
			return new TileEntityTurtle(j, i);
		}
	}

	public void postPlace(World world, int i, int j, int k, int l) {
		super.postPlace(world, i, j, k, l);
		if (l == 0 || l == 1) {
			l = 3;
		}

		this.setDirection(world, i, j, k, l);
	}

	public void postPlace(World world, int i, int j, int k, EntityLiving entityliving) {
		super.postPlace(world, i, j, k, entityliving);
		int l = MathHelper.floor(entityliving.yaw * 4.0F / 360.0F + 0.5) & 3;
		byte byte0;
		switch (l) {
			case 1:
				byte0 = 4;
				break;
			case 2:
				byte0 = 2;
				break;
			case 3:
				byte0 = 5;
				break;
			default:
				byte0 = 3;
		}

		this.setDirection(world, i, j, k, byte0);
	}

	public boolean interact(World world, int i, int j, int k, EntityHuman entityhuman) {
		if (mod_ComputerCraft.isMultiplayerClient()) {
			return true;
		}
		else if (!entityhuman.isSneaking()) {
			TileEntity tileEntity = world.getTileEntity(i, j, k);
			if (tileEntity instanceof TileEntityTurtle) {
				TileEntityTurtle tileentityturtle = (TileEntityTurtle) tileEntity;
				mod_CCTurtle.openTurtleGUI(entityhuman, tileentityturtle);
				tileentityturtle.turnOn();
				tileentityturtle.updateClient(entityhuman);
			}

			return true;
		}
		else {
			return false;
		}
	}

	public TileEntity a_() {
		return null;
	}

	private void setDirection(World world, int i, int j, int k, int l) {
		TileEntity tileEntity = world.getTileEntity(i, j, k);
		if (tileEntity instanceof TileEntityTurtle) {
			TileEntityTurtle turtle = (TileEntityTurtle) tileEntity;
			turtle.setDir(l);
			if (!mod_ComputerCraft.isMultiplayerClient()) {
				this.refreshInput(world, i, j, k);
			}
		}
	}

	public TileEntity getBlockEntity(int i) {
		return this.createDefaultTurtle(2 + (i >> 2 & 3), i & 3);
	}

	@Override
	public int getDirection(IBlockAccess iblockaccess, int i, int j, int k) {
		TileEntity tileEntity = iblockaccess.getTileEntity(i, j, k);
		if (tileEntity instanceof TileEntityTurtle) {
			TileEntityTurtle turtle = (TileEntityTurtle) tileEntity;
			return turtle.getDir();
		}
		else {
			return 3;
		}
	}
}
