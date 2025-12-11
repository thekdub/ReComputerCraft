package dan200.computer.shared;

import dan200.computer.api.IPeripheral;
import forge.ITextureProvider;
import net.minecraft.server.*;

import java.util.ArrayList;
import java.util.Random;


public abstract class BlockComputerBase extends BlockContainer implements ITextureProvider {
	private static final int[] oppositeSide = new int[] {1, 0, 3, 2, 5, 4};

	public BlockComputerBase(int i, Material material) {
		super(i, material);
	}

	public static IPeripheral getPeripheralAt(World world, int i, int j, int k, int l) {
		if (j >= 0 && j < world.getHeight()) {
			TileEntity tileentity = world.getTileEntity(i, j, k);
			if (tileentity instanceof IPeripheral) {
				IPeripheral iperipheral = (IPeripheral) tileentity;
				if (iperipheral.canAttachToSide(l)) {
					return iperipheral;
				}
			}
		}

		return null;
	}

	public static int getOppositeSide(int i) {
		return oppositeSide[i];
	}

	public static int getLocalSide(int i, int j) {
		byte byte0;
		byte byte1;
		byte byte2;
		switch (j) {
			case 2:
				byte0 = 3;
				byte1 = 4;
				byte2 = 5;
				break;
			case 3:
				byte0 = 2;
				byte1 = 5;
				byte2 = 4;
				break;
			case 4:
				byte0 = 5;
				byte1 = 3;
				byte2 = 2;
				break;
			case 5:
				byte0 = 4;
				byte1 = 2;
				byte2 = 3;
				break;
			default:
				return i;
		}

		if (i == j) {
			return 3;
		}
		else if (i == byte0) {
			return 2;
		}
		else if (i == byte1) {
			return 4;
		}
		else {
			return i == byte2 ? 5 : i;
		}
	}

	public String getTextureFile() {
		return "/terrain/ccterrain.png";
	}

	public abstract int getDirection(IBlockAccess var1, int var2, int var3, int var4);

	public int getDropType(int i, Random random, int j) {
		return this.id;
	}

	protected int getDropData(int i) {
		return 0;
	}

	public void dropBlockAsItemWithChance(World world, int i, int j, int k, int l, float f) {
	}

	public ArrayList<ItemStack> getBlockDropped(World world, int i, int j, int k, int l, int i1) {
		ArrayList<ItemStack> list = new ArrayList<>();
		TileEntity tileentity = world.getTileEntity(i, j, k);
		if (tileentity instanceof IComputerEntity) {
			IComputerEntity icomputerentity = (IComputerEntity) tileentity;
			ItemComputer itemcomputer = (ItemComputer) Item.byId[this.id];
			list.add(itemcomputer.createFromComputer(icomputerentity, l));
		}

		return list;
	}

	public void onPlace(World world, int i, int j, int k) {
		super.onPlace(world, i, j, k);
		this.refreshInput(world, i, j, k);
	}

	public void remove(World world, int i, int j, int k) {
		TileEntity tileentity = world.getTileEntity(i, j, k);
		if (tileentity instanceof IComputerEntity) {
			IComputerEntity icomputerentity = (IComputerEntity) tileentity;
			icomputerentity.destroy();
		}

		super.remove(world, i, j, k);
	}

	public boolean removeBlockByPlayer(World world, EntityHuman entityhuman, int i, int j, int k) {
		if (mod_ComputerCraft.isMultiplayerClient()) {
			return false;
		}
		else {
			int l = world.getData(i, j, k);

			for (ItemStack itemstack : this.getBlockDropped(world, i, j, k, l, 0)) {
				this.a(world, i, j, k, itemstack);
			}

			return super.removeBlockByPlayer(world, entityhuman, i, j, k);
		}
	}

	private boolean isBlockProvidingPower(World world, int i, int j, int k, int l) {
		return j >= 0 && j < world.getHeight() && (world.isBlockFacePowered(i, j, k, l)
				|| world.getTypeId(i, j, k) == Block.REDSTONE_WIRE.id && world.getData(i, j, k) > 0
				|| RedPowerInterop.isPoweringTo(world, i, j, k, l));
	}

	private int getBundledPowerOutput(World world, int i, int j, int k, int l) {
		if (j >= 0 && j < world.getHeight()) {
			int i1 = RedPowerInterop.getConDirMask(l);
			int j1 = 0;

			for (int k1 = 0; k1 < 16; k1++) {
				if (RedPowerInterop.getPowerState(world, i, j, k, i1, k1 + 1) > 0) {
					j1 |= 1 << k1;
				}
			}

			return j1;
		}
		else {
			return 0;
		}
	}

	public void doPhysics(World world, int i, int j, int k, int l) {
		this.refreshInput(world, i, j, k);
	}

	public void refreshInput(World world, int i, int j, int k) {
		TileEntity tileentity = world.getTileEntity(i, j, k);
		if (tileentity instanceof IComputerEntity) {
			IComputerEntity icomputerentity = (IComputerEntity) tileentity;
			int l = this.getDirection(world, i, j, k);
			icomputerentity.setPowerInput(getLocalSide(0, l), this.isBlockProvidingPower(world, i, j + 1, k, 1));
			icomputerentity.setPowerInput(getLocalSide(1, l), this.isBlockProvidingPower(world, i, j - 1, k, 0));
			icomputerentity.setPowerInput(getLocalSide(2, l), this.isBlockProvidingPower(world, i, j, k + 1, 3));
			icomputerentity.setPowerInput(getLocalSide(3, l), this.isBlockProvidingPower(world, i, j, k - 1, 2));
			icomputerentity.setPowerInput(getLocalSide(4, l), this.isBlockProvidingPower(world, i + 1, j, k, 5));
			icomputerentity.setPowerInput(getLocalSide(5, l), this.isBlockProvidingPower(world, i - 1, j, k, 4));
			if (RedPowerInterop.isRedPowerInstalled()) {
				icomputerentity.setBundledPowerInput(getLocalSide(0, l), this.getBundledPowerOutput(world, i, j, k, 1));
				icomputerentity.setBundledPowerInput(getLocalSide(1, l), this.getBundledPowerOutput(world, i, j, k, 0));
				icomputerentity.setBundledPowerInput(getLocalSide(2, l), this.getBundledPowerOutput(world, i, j, k, 3));
				icomputerentity.setBundledPowerInput(getLocalSide(3, l), this.getBundledPowerOutput(world, i, j, k, 2));
				icomputerentity.setBundledPowerInput(getLocalSide(4, l), this.getBundledPowerOutput(world, i, j, k, 5));
				icomputerentity.setBundledPowerInput(getLocalSide(5, l), this.getBundledPowerOutput(world, i, j, k, 4));
			}

			icomputerentity.setPeripheral(getLocalSide(0, l), getPeripheralAt(world, i, j + 1, k, 0));
			icomputerentity.setPeripheral(getLocalSide(1, l), getPeripheralAt(world, i, j - 1, k, 1));
			icomputerentity.setPeripheral(getLocalSide(2, l), getPeripheralAt(world, i, j, k + 1, 2));
			icomputerentity.setPeripheral(getLocalSide(3, l), getPeripheralAt(world, i, j, k - 1, 3));
			icomputerentity.setPeripheral(getLocalSide(4, l), getPeripheralAt(world, i + 1, j, k, 4));
			icomputerentity.setPeripheral(getLocalSide(5, l), getPeripheralAt(world, i - 1, j, k, 5));
		}
	}

	public boolean isPowerSource() {
		return true;
	}

	public boolean a(IBlockAccess iblockaccess, int i, int j, int k, int l) {
		TileEntity tileentity = iblockaccess.getTileEntity(i, j, k);
		if (tileentity instanceof IComputerEntity) {
			IComputerEntity icomputerentity = (IComputerEntity) tileentity;
			int i1 = getLocalSide(l, this.getDirection(iblockaccess, i, j, k));
			return icomputerentity.getPowerOutput(i1);
		}
		else {
			return false;
		}
	}

	public boolean d(World world, int i, int j, int k, int l) {
		return this.a(world, i, j, k, l);
	}
}
