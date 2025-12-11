package dan200.computer.shared;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.Packet;
import net.minecraft.server.TileEntity;
import net.minecraft.server.mod_ComputerCraft;

public class TileEntityWirelessModem extends TileEntity implements IPeripheral, INetworkedEntity, IDestroyableEntity {
   private WirelessModemPeripheral m_modem = new WirelessModemPeripheral(this);
   private boolean m_light = false;

   public void m() {
      super.m();
      if (mod_ComputerCraft.isMultiplayerClient()) {
         ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
         computercraftpacket.packetType = 5;
         computercraftpacket.dataInt = new int[]{this.x, this.y, this.z};
         mod_ComputerCraft.sendToServer(computercraftpacket);
      }
   }

   @Override
   public synchronized void destroy() {
      this.m_modem.destroy();
      this.m_modem = null;
   }

   public Packet d() {
      return null;
   }

   public void a(NBTTagCompound nbttagcompound) {
      super.a(nbttagcompound);
   }

   public void b(NBTTagCompound nbttagcompound) {
      super.b(nbttagcompound);
   }

   public void q_() {
      if (!mod_ComputerCraft.isMultiplayerClient() && this.m_modem.pollChanged()) {
         this.m_light = this.m_modem.isActive();
         if (mod_ComputerCraft.isMultiplayerServer()) {
            ComputerCraftPacket computercraftpacket = this.createModemLightPacket();
            mod_ComputerCraft.sendToAllPlayers(computercraftpacket);
         }

         mod_ComputerCraft.notifyBlockChange(this.world, this.x, this.y, this.z, mod_ComputerCraft.peripheral.id);
      }
   }

   public synchronized boolean isActive() {
      return this.m_light;
   }

   @Override
   public String getType() {
      return this.m_modem.getType();
   }

   @Override
   public String[] getMethodNames() {
      return this.m_modem.getMethodNames();
   }

   @Override
   public Object[] callMethod(IComputerAccess icomputeraccess, int i, Object[] aobj) throws Exception {
      return this.m_modem.callMethod(icomputeraccess, i, aobj);
   }

   @Override
   public boolean canAttachToSide(int i) {
      if (this.m_modem.canAttachToSide(i)) {
         int j = this.world.getData(this.x, this.y, this.z);
         int k = BlockPeripheral.getDirectionFromMetadata(j);
         return k == i;
      } else {
         return false;
      }
   }

   @Override
   public synchronized void attach(IComputerAccess computerAccess, String s) {
      this.m_modem.attach(computerAccess, s);
   }

   @Override
   public synchronized void detach(IComputerAccess computerAccess) {
      if (this.m_modem != null) {
         this.m_modem.detach(computerAccess);
      }
   }

   public void updateClient(EntityHuman entityhuman) {
      if (mod_ComputerCraft.isMultiplayerServer()) {
         ComputerCraftPacket computercraftpacket = this.createModemLightPacket();
         mod_ComputerCraft.sendToPlayer(entityhuman, computercraftpacket);
      }
   }

   @Override
   public void handlePacket(ComputerCraftPacket computercraftpacket, EntityHuman entityhuman) {
      if (mod_ComputerCraft.isMultiplayerServer()) {
         switch (computercraftpacket.packetType) {
            case 5:
               this.updateClient(entityhuman);
         }
      } else {
         switch (computercraftpacket.packetType) {
            case 13:
               this.m_light = computercraftpacket.dataInt[3] > 0;
               mod_ComputerCraft.notifyBlockChange(this.world, this.x, this.y, this.z, mod_ComputerCraft.peripheral.id);
         }
      }
   }

   private ComputerCraftPacket createModemLightPacket() {
      ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
      computercraftpacket.packetType = 13;
      synchronized (this) {
         computercraftpacket.dataInt = new int[]{this.x, this.y, this.z, this.m_light ? 1 : 0};
         return computercraftpacket;
      }
   }
}
