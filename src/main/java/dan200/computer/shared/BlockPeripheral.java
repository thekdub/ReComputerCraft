package dan200.computer.shared;

import forge.ITextureProvider;
import net.minecraft.server.*;

import java.util.ArrayList;
import java.util.Random;


public class BlockPeripheral extends BlockContainer implements ITextureProvider {
	public int blockRenderID;
	public boolean useActiveModemTextures = false;
	private World lastWorld;
	private int lastX;
	private int lastY;
	private int lastZ;
	private int lastSubType;

	public BlockPeripheral(int i) {
		super(i, Material.STONE);
		this.blockRenderID = -1;
	}

	public static int getSubtypeFromMetadata(int metadata) {
		if (metadata >= 2 && metadata <= 5) {
			return 0;
		}
		else {
			return metadata > 9 ? 2 : 1;
		}
	}

	public static int getDirectionFromMetadata(int metadata) {
		if ((metadata < 2 || metadata > 5) && metadata != 0) {
			if (metadata > 9) {
				return metadata - 8;
			}
			else {
				return metadata < 2 ? metadata : metadata - 4;
			}
		}
		else {
			return metadata;
		}
	}

	public String getTextureFile() {
		return "/terrain/ccterrain.png";
	}

	public void addCreativeItems(ArrayList arraylist) {
		arraylist.add(new ItemStack(this, 1, 0));
		arraylist.add(new ItemStack(this, 1, 1));
		arraylist.add(new ItemStack(this, 1, 2));
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

	public int getDropType(int i, Random random, int j) {
		return this.id;
	}

	protected int getDropData(int metadata) {
		return getSubtypeFromMetadata(metadata);
	}

	public void updateShape(IBlockAccess iblockaccess, int i, int j, int k) {
		int metadata = iblockaccess.getData(i, j, k);
		int subType = getSubtypeFromMetadata(metadata);
		switch (subType) {
			case 0:
			case 2:
				this.a(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
				break;
			case 1:
				int dir = getDirectionFromMetadata(metadata);
				switch (dir) {
					case 0:
						this.a(0.125F, 0.0F, 0.125F, 0.875F, 0.1875F, 0.875F);
						break;
					case 1:
						this.a(0.125F, 0.8125F, 0.125F, 0.875F, 1.0F, 0.875F);
						break;
					case 2:
						this.a(0.125F, 0.125F, 0.0F, 0.875F, 0.875F, 0.1875F);
						break;
					case 3:
						this.a(0.125F, 0.125F, 0.8125F, 0.875F, 0.875F, 1.0F);
						break;
					case 4:
						this.a(0.0F, 0.125F, 0.125F, 0.1875F, 0.875F, 0.875F);
						break;
					case 5:
						this.a(0.8125F, 0.125F, 0.125F, 1.0F, 0.875F, 0.875F);
				}
		}
	}

	public AxisAlignedBB e(World world, int i, int j, int k) {
		this.updateShape(world, i, j, k);
		return super.e(world, i, j, k);
	}

	public void remove(World world, int i, int j, int k) {
		TileEntity tileEntity = world.getTileEntity(i, j, k);
		if (tileEntity instanceof IDestroyableEntity) {
			IDestroyableEntity drive = (IDestroyableEntity) tileEntity;
			drive.destroy();
		}

		super.remove(world, i, j, k);
	}

	public boolean canPlace(World world, int i, int j, int k, int side) {
		int opp = BlockComputerBase.getOppositeSide(side);
		i += Facing.b[opp];
		j += Facing.c[opp];
		k += Facing.d[opp];
		return world.isBlockSolidOnSide(i, j, k, side);
	}

	public boolean isBlockSolidOnSide(World world, int i, int j, int k, int side) {
		int metadata = world.getData(i, j, k);
		int subType = getSubtypeFromMetadata(metadata);
		switch (subType) {
			case 0:
			case 2:
				return true;
			case 1:
				int dir = getDirectionFromMetadata(metadata);
				return side == dir;
			default:
				return false;
		}
	}

	private int getDirectionFromBlockLookup(World world, int i, int j, int k) {
		int l = world.getTypeId(i, j, k - 1);
		int i1 = world.getTypeId(i, j, k + 1);
		int j1 = world.getTypeId(i - 1, j, k);
		int k1 = world.getTypeId(i + 1, j, k);
		if (!Block.n[l] && Block.n[i1]) {
			return 3;
		}
		else if (!Block.n[i1] && Block.n[l]) {
			return 2;
		}
		else if (!Block.n[j1] && Block.n[k1]) {
			return 5;
		}
		else {
			return Block.n[k1] && !Block.n[j1] ? 4 : 3;
		}
	}

	private int getDirectionFromEntityRot(EntityLiving entityliving) {
		int i = MathHelper.floor(entityliving.yaw * 4.0F / 360.0F + 0.5) & 3;
		switch (i) {
			case 1:
				return 5;
			case 2:
				return 3;
			case 3:
				return 4;
			default:
				return 2;
		}
	}

	public void postPlace(World world, int i, int j, int k, int side) {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			int metadata = world.getData(i, j, k);
			int subType = metadata > 2 ? 0 : metadata;
			this.lastWorld = world;
			this.lastX = i;
			this.lastY = j;
			this.lastZ = k;
			this.lastSubType = subType;
			switch (subType) {
				case 0:
					world.setData(i, j, k, this.getDirectionFromBlockLookup(world, i, j, k));
					break;
				case 1:
					int data = BlockComputerBase.getOppositeSide(side);
					if (data >= 2) {
						data += 4;
					}

					world.setData(i, j, k, data);
					break;
				case 2:
					if (side < 2) {
						side = 2;
					}

					TileEntity tileentity = world.getTileEntity(i, j, k);
					if (tileentity instanceof TileEntityMonitor) {
						TileEntityMonitor tileentitymonitor = (TileEntityMonitor) tileentity;
						tileentitymonitor.setDir(side);
						tileentitymonitor.expand();
					}
			}
		}
	}

	public void postPlace(World world, int i, int j, int k, EntityLiving entityliving) {
		int metadata = world.getData(i, j, k);
		int subType;
		if (world == this.lastWorld && i == this.lastX && j == this.lastY && k == this.lastZ) {
			subType = this.lastSubType;
			this.lastWorld = null;
		}
		else {
			subType = metadata > 2 ? 0 : metadata;
		}

		switch (subType) {
			case 0:
				world.setData(i, j, k, this.getDirectionFromEntityRot(entityliving));
			case 1:
			case 2:
				TileEntity tileentity = world.getTileEntity(i, j, k);
				if (tileentity instanceof TileEntityMonitor) {
					TileEntityMonitor tileentitymonitor = (TileEntityMonitor) tileentity;
					int j1 = this.getDirectionFromEntityRot(entityliving);
					int k1 = tileentitymonitor.getDir();
					world.setData(i, j, k, 10);
					if (j1 != k1) {
						tileentitymonitor.contractNeighbours();
						tileentitymonitor.setDir(j1);
						tileentitymonitor.contract();
						tileentitymonitor.expand();
						mod_ComputerCraft.notifyBlockChange(world, i, j, k, mod_ComputerCraft.peripheral.id);
					}
				}
			default:
				break;
		}
	}

	private int getMonitorFaceTexture(int i, int j, int k, int l) {
		if (k == 1 && l == 1) {
			return 0;
		}
		else if (l == 1) {
			if (i == 0) {
				return 1;
			}
			else {
				return i != k - 1 ? 2 : 3;
			}
		}
		else if (k == 1) {
			if (j == 0) {
				return 6;
			}
			else {
				return j != l - 1 ? 5 : 4;
			}
		}
		else if (j == 0) {
			if (i == 0) {
				return 7;
			}
			else {
				return i != k - 1 ? 8 : 9;
			}
		}
		else if (j == l - 1) {
			if (i == 0) {
				return 13;
			}
			else {
				return i != k - 1 ? 14 : 15;
			}
		}
		else if (i == 0) {
			return 10;
		}
		else {
			return i != k - 1 ? 11 : 12;
		}
	}

	public int getBlockTexture(IBlockAccess iblockaccess, int i, int j, int k, int l) {
		int metadata = iblockaccess.getData(i, j, k);
		int subType = getSubtypeFromMetadata(metadata);
		switch (subType) {
			case 0:
				if (l != 0 && l != 1) {
					int dir = getDirectionFromMetadata(metadata);
					if (l == dir) {
						TileEntity tileEntity = iblockaccess.getTileEntity(i, j, k);
						if (tileEntity instanceof TileEntityDiskDrive) {
							TileEntityDiskDrive drive = (TileEntityDiskDrive) tileEntity;
							if (drive.hasAnything()) {
								return !drive.hasDisk() ? 36 : 34;
							}
						}

						return 32;
					}

					return 33;
				}

				return 35;
			case 1:
				int offset = 0;
				TileEntity tileEntity = iblockaccess.getTileEntity(i, j, k);
				if (tileEntity instanceof TileEntityWirelessModem) {
					TileEntityWirelessModem modem = (TileEntityWirelessModem) tileEntity;
					if (modem.isActive()) {
						offset = 2;
					}
				}

				int dir1 = getDirectionFromMetadata(metadata);
				if (dir1 != 0 && dir1 != 1) {
					if (l == BlockComputerBase.getOppositeSide(dir1)) {
						return 48 + offset;
					}

					if (l != 2 && l != 5) {
						return 48 + offset;
					}

					return 48 + offset + 1;
				}

				return 48 + offset;
			case 2:
				TileEntity tileentity2 = iblockaccess.getTileEntity(i, j, k);
				int l1;
				int i2;
				int k2;
				int l2;
				int i3;
				if (tileentity2 instanceof TileEntityMonitor) {
					TileEntityMonitor tileentitymonitor = (TileEntityMonitor) tileentity2;
					l1 = tileentitymonitor.m_xIndex;
					i2 = tileentitymonitor.m_yIndex;
					k2 = tileentitymonitor.m_width;
					l2 = tileentitymonitor.m_height;
					i3 = tileentitymonitor.getDir();
				}
				else {
					i2 = 0;
					l1 = 0;
					l2 = 1;
					k2 = 1;
					i3 = 2;
				}

				if (l != 1 && l != 0) {
					if (l == i3) {
						return 96 + this.getMonitorFaceTexture(l1, i2, k2, l2);
					}
					else if (l == BlockComputerBase.getOppositeSide(i3)) {
						return 112 + this.getMonitorFaceTexture(k2 - 1 - l1, i2, k2, l2);
					}
					else if (l2 == 1) {
						return 84;
					}
					else if (i2 == 0) {
						return 85;
					}
					else {
						return i2 != l2 - 1 ? 86 : 87;
					}
				}
				else if (k2 == 1) {
					return 80;
				}
				else if (l1 == 0) {
					return 81;
				}
				else {
					return l1 != k2 - 1 ? 82 : 83;
				}
			default:
				return 0;
		}
	}

	public int a(int i, int metadata) {
		switch (metadata) {
			case 0:
				if (i != 1 && i != 0) {
					return i != 3 ? 33 : 32;
				}

				return 35;
			case 1:
				if (i != 2 && i != 5) {
					return 48 + (this.useActiveModemTextures ? 2 : 0);
				}

				return 48 + (this.useActiveModemTextures ? 3 : 1);
			case 2:
				if (i != 1 && i != 0) {
					return i != 3 ? 112 : 96;
				}

				return 80;
			default:
				return 0;
		}
	}

	public boolean interact(World world, int i, int j, int k, EntityHuman entityhuman) {
		if (mod_ComputerCraft.isMultiplayerClient()) {
			return true;
		}
		else {
			if (!entityhuman.isSneaking()) {
				int metadata = world.getData(i, j, k);
				int subType = getSubtypeFromMetadata(metadata);
				if (subType == 0) {
					TileEntity tileEntity = world.getTileEntity(i, j, k);
					if (tileEntity instanceof TileEntityDiskDrive) {
						TileEntityDiskDrive drive = (TileEntityDiskDrive) tileEntity;
						mod_ComputerCraft.openDiskDriveGUI(entityhuman, drive);
					}

					return true;
				}
			}

			return false;
		}
	}

	public void doPhysics(World world, int i, int j, int k, int l) {
		int metadata = world.getData(i, j, k);
		int subType = getSubtypeFromMetadata(metadata);
		if (subType == 1) {
			int dir = getDirectionFromMetadata(metadata);
			if (!this.canPlace(world, i, j, k, BlockComputerBase.getOppositeSide(dir))) {
				this.b(world, i, j, k, metadata, 0);
				world.setTypeId(i, j, k, 0);
			}
		}
	}

	public TileEntity a_() {
		return null;
	}

	public TileEntity getBlockEntity(int metadata) {
		int subType = metadata > 2 ? getSubtypeFromMetadata(metadata) : metadata;
		switch (subType) {
			case 0:
				return new TileEntityDiskDrive();
			case 1:
				return new TileEntityWirelessModem();
			case 2:
				return new TileEntityMonitor();
			default:
				return null;
		}
	}
}
