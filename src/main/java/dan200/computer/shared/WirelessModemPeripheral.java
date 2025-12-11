package dan200.computer.shared;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;
import net.minecraft.server.TileEntity;
import net.minecraft.server.Vec3D;
import net.minecraft.server.mod_ComputerCraft;

public class WirelessModemPeripheral implements IPeripheral, IWirelessReceiver {
   private TileEntity m_owner;
   private WirelessNetwork m_network;
   private IComputerAccess m_computer;
   private Vec3D m_position;
   private boolean m_open;
   private boolean m_changed;
   static final boolean $assertionsDisabled = !WirelessModemPeripheral.class.desiredAssertionStatus();

   public WirelessModemPeripheral(TileEntity tileEntity) {
      this.m_owner = tileEntity;
      this.m_network = null;
      this.m_computer = null;
      this.m_open = false;
      this.m_changed = true;
   }

   public void setOwnerAndPos(TileEntity tileEntity, int i, int j, int k) {
      this.m_owner = tileEntity;
      this.m_position = Vec3D.a(i + 0.5, j + 0.5, k + 0.5);
   }

   public synchronized void destroy() {
      if (this.m_network != null) {
         this.m_network.removeReceiver(this);
         this.m_network = null;
      }
   }

   public synchronized boolean pollChanged() {
      if (this.m_changed) {
         this.m_changed = false;
         return true;
      } else {
         return false;
      }
   }

   private double getRange() {
      return this.m_owner.world.x() && this.m_owner.world.w() ? mod_ComputerCraft.modem_rangeDuringStorm : mod_ComputerCraft.modem_range;
   }

   public synchronized boolean isActive() {
      return this.m_computer != null && this.m_open;
   }

   @Override
   public synchronized int getID() {
      return this.m_computer.getID();
   }

   @Override
   public synchronized Vec3D getWorldPosition() {
      return this.m_position;
   }

   @Override
   public synchronized void receive(int i, String s, double d) {
      if (this.m_open) {
         this.m_computer.queueEvent("rednet_message", new Object[]{i, s, d});
      }
   }

   @Override
   public String getType() {
      return "modem";
   }

   @Override
   public String[] getMethodNames() {
      return new String[]{"open", "close", "send", "broadcast"};
   }

   @Override
   public Object[] callMethod(IComputerAccess computerAccess, int i, Object[] aobj) throws Exception {
      switch (i) {
         case 0:
            synchronized (this) {
               if (!this.m_open) {
                  this.m_open = true;
                  this.m_changed = true;
               }

               return null;
            }
         case 1:
            synchronized (this) {
               if (this.m_open) {
                  this.m_open = false;
                  this.m_changed = true;
               }

               return null;
            }
         case 2:
            if (aobj.length >= 2 && aobj[0] instanceof Double && aobj[1] instanceof String) {
               int j = (int)((Double)aobj[0]).doubleValue();
               synchronized (this) {
                  if (!this.m_open) {
                     throw new Exception("Must call open before sending");
                  }

                  this.m_network.transmit(computerAccess.getID(), j, (String)aobj[1], this.getRange(), this.m_position.a, this.m_position.b, this.m_position.c);
                  return null;
               }
            }

            throw new Exception("Expected number, string");
         case 3:
            if (aobj.length >= 1 && aobj[0] instanceof String) {
               synchronized (this) {
                  if (!this.m_open) {
                     throw new Exception("Must call open before sending");
                  }

                  this.m_network.broadcast(computerAccess.getID(), (String)aobj[0], this.getRange(), this.m_position.a, this.m_position.b, this.m_position.c);
                  return null;
               }
            }

            throw new Exception("Expected string");
         default:
            if (!$assertionsDisabled) {
               throw new AssertionError();
            } else {
               return null;
            }
      }
   }

   @Override
   public boolean canAttachToSide(int i) {
      return true;
   }

   @Override
   public synchronized void attach(IComputerAccess computerAccess, String s) {
      this.m_computer = computerAccess;
      this.m_position = Vec3D.a(this.m_owner.x + 0.5, this.m_owner.y + 0.5, this.m_owner.z + 0.5);
      this.m_network = WirelessNetwork.get(this.m_owner.world);
      this.m_network.addReceiver(this);
      this.m_open = false;
   }

   @Override
   public synchronized void detach(IComputerAccess computerAccess) {
      if (this.m_network != null) {
         this.m_network.removeReceiver(this);
         this.m_network = null;
         this.m_computer = null;
         this.m_open = false;
      }
   }

   static Class<?> _mthclass$(String s) {
      try {
         return Class.forName(s);
      } catch (ClassNotFoundException var2) {
         throw new NoClassDefFoundError(var2.getMessage());
      }
   }
}
