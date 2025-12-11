package dan200.computer.shared;

import net.minecraft.server.EntityHuman;


public interface INetworkedEntity {
	void handlePacket(ComputerCraftPacket var1, EntityHuman var2);
}
