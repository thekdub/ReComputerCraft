package dan200.computer.shared;

import eloraam.core.IRedPowerConnectable;
import net.minecraft.server.mod_ComputerCraft;

public class RedPowerTileEntityComputer extends TileEntityComputer implements IRedPowerConnectable {
   public RedPowerTileEntityComputer() {
      RedPowerInterop.addComputerConnectMappings();
   }

   @Override
   public int getConnectableMask() {
      return -1;
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
      int j = mod_ComputerCraft.computer.getDirection(this.world, this.x, this.y, this.z);
      return RedPowerInterop.getComputerPoweringMask(i, this, j);
   }
}
