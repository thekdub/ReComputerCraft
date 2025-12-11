package dan200.computer.shared;

import dan200.computer.core.Computer;
import dan200.computer.core.ComputerThread;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.World;
import net.minecraft.server.mod_ComputerCraft;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.WeakHashMap;


public class LabelStore {
	private static final Map<World, LabelStore> s_diskStores = new WeakHashMap<>();
	private static final Map<World, LabelStore> s_computerStores = new WeakHashMap<>();
	private final World m_world;
	private final String m_path;
	private final int m_packetType;
	private final HashMap<Integer, String> m_labels;
	private final HashMap<Integer, Boolean> m_serverLabelRequests;
	private boolean m_labelsChanged;

	private LabelStore(World world, String s, int i) {
		this.m_world = world;
		this.m_path = s;
		this.m_packetType = i;
		this.m_labels = new HashMap<>();
		this.m_serverLabelRequests = new HashMap<>();
		this.m_labelsChanged = false;
	}

	private static LabelStore get(World world, Map<World, LabelStore> map, String s, int i) {
		if (mod_ComputerCraft.isMultiplayerServer()) {
			return new LabelStore(null, s, i);
		}
		else if (world != null) {
			LabelStore labelstore = map.get(world);
			if (labelstore == null) {
				labelstore = new LabelStore(world, s, i);
				map.put(world, labelstore);
			}

			return labelstore;
		}
		else {
			return null;
		}
	}

	public static LabelStore getForDisks(World world) {
		return get(world, s_diskStores, "/computer/disk/labels.txt", 10);
	}

	public static LabelStore getForComputers(World world) {
		return get(world, s_computerStores, "/computer/labels.txt", 14);
	}

	public World getWorld() {
		return this.m_world;
	}

	public String getLabel(int i) {
		if (i >= 0) {
			synchronized (this) {
				String s = this.m_labels.get(i);
				if (s != null) {
					return s;
				}

				if (mod_ComputerCraft.isMultiplayerClient() && !this.m_serverLabelRequests.containsKey(i)) {
					this.m_serverLabelRequests.put(i, true);
					ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
					computercraftpacket.packetType = this.m_packetType + 1;
					computercraftpacket.dataInt = new int[] {i};
					mod_ComputerCraft.sendToServer(computercraftpacket);
				}
			}
		}

		return null;
	}

	public void setLabel(int i, String s) {
		if (i >= 0) {
			synchronized (this) {
				if (s != null && s.isEmpty()) {
					s = null;
				}

				boolean flag = false;
				String s1 = this.m_labels.get(i);
				if (s != null) {
					s = s.trim().replaceAll("[\r\n\t]+", "");
					if (s.length() > 25) {
						s = s.substring(0, 25);
					}

					if (!s.equals(s1)) {
						this.m_labels.put(i, s);
						flag = true;
					}
				}
				else if (s1 != null) {
					this.m_labels.remove(i);
					flag = true;
				}

				if (flag) {
					if (!mod_ComputerCraft.isMultiplayerClient() && !this.m_labelsChanged) {
						this.m_labelsChanged = true;
						ComputerThread.start();
						ComputerThread.queueTask(new ComputerThread.Task() {
							@Override
							public Computer getOwner() {
								return null;
							}

							@Override
							public void execute() {
								LabelStore.this.save();
							}
						});
					}

					if (mod_ComputerCraft.isMultiplayerServer()) {
						ComputerCraftPacket computercraftpacket = this.buildDiskLabelPacket(i, s);
						mod_ComputerCraft.sendToAllPlayers(computercraftpacket);
					}
				}
			}
		}
	}

	public void reload() {
		synchronized (this) {
			if (!mod_ComputerCraft.isMultiplayerClient()) {
				ComputerThread.start();
				ComputerThread.queueTask(new ComputerThread.Task() {
					@Override
					public Computer getOwner() {
						return null;
					}

					@Override
					public void execute() {
						LabelStore.this.load();
					}
				});
			}
		}
	}

	public void sendLabelToPlayer(int i, EntityHuman entityhuman) {
		String s = this.getLabel(i);
		if (s != null) {
			ComputerCraftPacket computercraftpacket = this.buildDiskLabelPacket(i, s);
			mod_ComputerCraft.sendToPlayer(entityhuman, computercraftpacket);
		}
	}

	private void load() {
		if (!mod_ComputerCraft.isMultiplayerClient()) {
			synchronized (this) {
				this.m_labels.clear();
				this.m_serverLabelRequests.clear();
				this.m_labelsChanged = false;
				BufferedReader bufferedreader = null;

				try {
					File file = new File(mod_ComputerCraft.getWorldDir(this.m_world), this.m_path);
					if (file.exists()) {
						bufferedreader = new BufferedReader(new FileReader(file));

						String s;
						while ((s = bufferedreader.readLine()) != null) {
							int i = s.indexOf(32);
							if (i > 0) {
								int j;
								try {
									j = Integer.parseInt(s.substring(0, i));
								} catch (NumberFormatException var9) {
									continue;
								}

								String s1 = s.substring(i + 1).trim();
								this.m_labels.put(j, s1);
							}
						}

						bufferedreader.close();
					}
				} catch (IOException var10) {
					System.out.println("ComputerCraft: Failed to read from labels file");

					try {
						if (bufferedreader != null) {
							bufferedreader.close();
						}
					} catch (IOException ignored) {
					}
				}
			}
		}
	}

	private void save() {
		synchronized (this) {
			if (this.m_labelsChanged) {
				BufferedWriter bufferedwriter = null;

				try {
					File file = new File(mod_ComputerCraft.getWorldDir(this.m_world), this.m_path);
					bufferedwriter = new BufferedWriter(new FileWriter(file));

					for (Entry<?, ?> entry : this.m_labels.entrySet()) {
						bufferedwriter.write(entry.getKey() + " " + entry.getValue());
						bufferedwriter.newLine();
					}

					bufferedwriter.close();
				} catch (IOException var13) {
					System.out.println("ComputerCraft: Failed to write to labels file");

					try {
						if (bufferedwriter != null) {
							bufferedwriter.close();
						}
					} catch (IOException ignored) {
					}
				} finally {
					this.m_labelsChanged = false;
				}
			}
		}
	}

	private ComputerCraftPacket buildDiskLabelPacket(int i, String s) {
		ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
		computercraftpacket.packetType = this.m_packetType;
		computercraftpacket.dataInt = new int[] {i};
		computercraftpacket.dataString = new String[] {s == null ? "" : s};
		return computercraftpacket;
	}
}
