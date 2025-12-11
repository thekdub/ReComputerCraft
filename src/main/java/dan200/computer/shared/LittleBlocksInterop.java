package dan200.computer.shared;

import net.minecraft.server.World;


public class LittleBlocksInterop {
	private static boolean littleBlocksSearched = false;
	private static Class<?> littleWorld = null;

	private static void findLittleBlocks() {
		if (!littleBlocksSearched) {
			try {
				littleWorld = Class.forName("net.minecraft.littleblocks.LittleWorld");
				System.out.println("ComputerCraft: LittleBlocks located.");
			} catch (ClassNotFoundException ignored) {
			} finally {
				littleBlocksSearched = true;
			}
		}
	}

	public static boolean isLittleBlocksInstalled() {
		findLittleBlocks();
		return littleWorld != null;
	}

	public static boolean isLittleWorld(World world) {
		findLittleBlocks();
		return littleWorld != null && littleWorld.isInstance(world);
	}
}
