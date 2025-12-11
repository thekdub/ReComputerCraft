package dan200.turtle.shared;

import dan200.computer.shared.BlockComputerBase;
import dan200.computer.shared.RedPowerInterop;
import eloraam.core.IRedPowerConnectable;
import net.minecraft.server.mod_CCTurtle;


public class RedPowerTileEntityTurtle extends TileEntityTurtle implements IRedPowerConnectable {
	public RedPowerTileEntityTurtle() {
		RedPowerInterop.addComputerConnectMappings();
	}

	public RedPowerTileEntityTurtle(int i) {
		super(i);
		RedPowerInterop.addComputerConnectMappings();
	}

	public RedPowerTileEntityTurtle(int i, int j) {
		super(i, j);
		RedPowerInterop.addComputerConnectMappings();
	}

	@Override
	public int getConnectableMask() {
		if (this.hasModem()) {
			int i = 0;
			int j = mod_CCTurtle.turtle.getDirection(this.world, this.x, this.y, this.z);

			for (int k = 0; k < 6; k++) {
				int l = BlockComputerBase.getLocalSide(k, j);
				if (l != 5) {
					i |= RedPowerInterop.getConDirMask(k);
				}
			}

			return i;
		}
		else {
			return -1;
		}
	}

	@Override
	public int getConnectClass(int i) {
		return RedPowerInterop.getComputerConnectClass();
	}

	@Override
	public int getCornerPowerMode() {
		return 0;
	}

	@Override
	public int getPoweringMask(int i) {
		int j = mod_CCTurtle.turtle.getDirection(this.world, this.x, this.y, this.z);
		return RedPowerInterop.getComputerPoweringMask(i, this, j);
	}
}
