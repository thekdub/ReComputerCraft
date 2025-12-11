package dan200.computer.shared;

import net.minecraft.server.Vec3D;


public interface IWirelessReceiver {
	int getID();

	Vec3D getWorldPosition();

	void receive(int var1, String var2, double var3);
}
