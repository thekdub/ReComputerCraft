package dan200.computer.shared;

import net.minecraft.server.Vec3D;
import net.minecraft.server.World;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;


public class WirelessNetwork {
	private static final Map<World, WirelessNetwork> s_networks = new WeakHashMap<>();
	private final Map<Integer, IWirelessReceiver> m_receivers = new HashMap<>();

	private WirelessNetwork() {
	}

	public static WirelessNetwork get(World world) {
		if (world != null) {
			WirelessNetwork wirelessnetwork = s_networks.get(world);
			if (wirelessnetwork == null) {
				wirelessnetwork = new WirelessNetwork();
				s_networks.put(world, wirelessnetwork);
			}

			return wirelessnetwork;
		}
		else {
			return null;
		}
	}

	public synchronized void addReceiver(IWirelessReceiver wirelessReceiver) {
		this.m_receivers.put(wirelessReceiver.getID(), wirelessReceiver);
	}

	public synchronized void removeReceiver(IWirelessReceiver wirelessReceiver) {
		this.m_receivers.remove(wirelessReceiver.getID());
	}

	public synchronized void broadcast(int i, String s, double d, double d1, double d2, double d3) {
		for (IWirelessReceiver wirelessReceiver : this.m_receivers.values()) {
			this.tryTransmit(wirelessReceiver, i, s, d, d1, d2, d3);
		}
	}

	public synchronized void transmit(int i, int j, String s, double d, double d1, double d2, double d3) {
		IWirelessReceiver wirelessReceiver = this.m_receivers.get(j);
		if (wirelessReceiver != null) {
			this.tryTransmit(wirelessReceiver, i, s, d, d1, d2, d3);
		}
	}

	private void tryTransmit(IWirelessReceiver wirelessReceiver, int i, String s, double d, double d1, double d2, double d3) {
		if (wirelessReceiver.getID() != i) {
			Vec3D vec3d = wirelessReceiver.getWorldPosition();
			double d4 = vec3d.d(d1, d2, d3);
			if (d4 <= d * d) {
				wirelessReceiver.receive(i, s, Math.sqrt(d4));
			}
		}
	}
}
