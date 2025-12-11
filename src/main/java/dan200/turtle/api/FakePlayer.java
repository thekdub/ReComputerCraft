package dan200.turtle.api;

import net.minecraft.server.EntityPlayer;
import net.minecraft.server.ItemInWorldManager;
import net.minecraft.server.ModLoader;
import net.minecraft.server.World;
import net.minecraft.server.mod_CCTurtle;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import java.util.HashMap;


public class FakePlayer {
	public static String name = "[ComputerCraft]";
	private static FakePlayer.Method method = null;
	private static final HashMap<World, HashMap<String, EntityPlayer>> fakePlayersPerturtle = new HashMap<>();
	private static final HashMap<World, EntityPlayer> fakePlayersOne = new HashMap<>();

	public static void setMethod(String value) {
		FakePlayer.Method method = null;

		try {
			method = FakePlayer.Method.valueOf(value.trim().toUpperCase());
		} catch (Exception var3) {
			System.err.println("Invalid turtle FakePlayer mode: " + value + "! Using NULL mode!");
		}

		if (method == null) {
			method = FakePlayer.Method.NULL;
		}

		setMethod(method);
	}

	public static void setMethod(FakePlayer.Method newMethod) {
		if (newMethod == null) {
			newMethod = FakePlayer.Method.NULL;
		}

		method = newMethod;
	}

	private static EntityPlayer makeFakePlayer(World world, String name) {
		EntityPlayer fakePlayer = new EntityPlayer(ModLoader.getMinecraftServerInstance(), world, name, new ItemInWorldManager(world));
		if (mod_CCTurtle.turtle_fakeplayer_dologin) {
			PlayerLoginEvent ple = new PlayerLoginEvent(fakePlayer.getBukkitEntity());
			world.getServer().getPluginManager().callEvent(ple);
			if (ple.getResult() != Result.ALLOWED) {
				System.err.println(name + " Warning: FakePlayer login event was disallowed. Ignoring, but this may cause confused plugins.");
			}

			PlayerJoinEvent pje = new PlayerJoinEvent(fakePlayer.getBukkitEntity(), "");
			world.getServer().getPluginManager().callEvent(pje);
		}

		return fakePlayer;
	}

	public static EntityPlayer get(World world, String turtleName) {
		if (method == null) {
			setMethod(mod_CCTurtle.turtle_fakeplayer_method);
		}

		switch (method) {
			case PERTURTLE:
				HashMap<String, EntityPlayer> fakePlayersInWorld;
				if (fakePlayersPerturtle.containsKey(world)) {
					fakePlayersInWorld = fakePlayersPerturtle.get(world);
				}
				else {
					fakePlayersInWorld = new HashMap<>();
					fakePlayersPerturtle.put(world, fakePlayersInWorld);
				}

				if (fakePlayersInWorld.containsKey(turtleName)) {
					return fakePlayersInWorld.get(turtleName);
				}

				EntityPlayer newFakePlayer = makeFakePlayer(world, turtleName);
				fakePlayersInWorld.put(turtleName, newFakePlayer);
				return newFakePlayer;
			case ONE:
				if (fakePlayersOne.containsKey(world)) {
					return fakePlayersOne.get(world);
				}

				EntityPlayer newFP = makeFakePlayer(world, name);
				fakePlayersOne.put(world, newFP);
				return newFP;
			default:
				return null;
		}
	}

	public static EntityPlayer get(World world) {
		return get(world, name);
	}

	public static CraftPlayer getBukkitEntity(World world, String name) {
		EntityPlayer player = get(world, name);
		return player != null ? player.getBukkitEntity() : null;
	}

	enum Method {
		NULL,
		PERTURTLE,
		ONE
	}
}
