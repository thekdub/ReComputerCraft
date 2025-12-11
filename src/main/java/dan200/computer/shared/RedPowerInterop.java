package dan200.computer.shared;

import net.minecraft.server.IBlockAccess;

import java.lang.reflect.Method;


public class RedPowerInterop {
	private static boolean redPowerSearched = false;
	private static Class<?> redPowerLib = null;
	private static Method redPowerLib_isSearching = null;
	private static Method redPowerLib_isPoweringTo = null;
	private static Method redPowerLib_getPowerState = null;
	private static Method redPowerLib_getConDirMask = null;
	private static Method redPowerLib_addCompatibleMapping = null;
	private static final int computerConnectClass = 1337;
	private static boolean computerConnectMappingsAdded = false;

	private static Method findRedPowerMethod(String s, Class<?>[] clazz) {
		try {
			return redPowerLib.getMethod(s, clazz);
		} catch (NoSuchMethodException var3) {
			System.out.println("ComputerCraft: RedPowerLib method " + s + " not found.");
			return null;
		}
	}

	private static void findRedPower() {
		if (!redPowerSearched) {
			try {
				System.out.println("ComputerCraft: Searching for RedPowerLib...");
				redPowerLib = Class.forName("eloraam.core.RedPowerLib");
				redPowerLib_isSearching = findRedPowerMethod("isSearching", new Class[0]);
				redPowerLib_isPoweringTo = findRedPowerMethod("isPoweringTo", new Class[] {IBlockAccess.class, int.class, int.class, int.class, int.class});
				redPowerLib_getPowerState = findRedPowerMethod(
						"getPowerState", new Class[] {IBlockAccess.class, int.class, int.class, int.class, int.class, int.class}
				);
				redPowerLib_getConDirMask = findRedPowerMethod("getConDirMask", new Class[] {int.class});
				redPowerLib_addCompatibleMapping = findRedPowerMethod("addCompatibleMapping", new Class[] {int.class, int.class});
				System.out.println("ComputerCraft: RedPowerLib and methods located.");
			} catch (ClassNotFoundException var4) {
				System.out.println("ComputerCraft: RedPowerLib not found.");
			} finally {
				redPowerSearched = true;
			}
		}
	}

	public static boolean isRedPowerInstalled() {
		findRedPower();
		return redPowerLib != null;
	}

	public static boolean isSearching() {
		findRedPower();
		if (redPowerLib_isSearching != null) {
			try {
				Object obj = redPowerLib_isSearching.invoke(null);
				return (Boolean) obj;
			} catch (Exception var1) {
				return false;
			}
		}
		else {
			return false;
		}
	}

	public static boolean isPoweringTo(IBlockAccess iblockaccess, int i, int j, int k, int l) {
		findRedPower();
		if (redPowerLib_isPoweringTo != null) {
			try {
				Object obj = redPowerLib_isPoweringTo.invoke(null, iblockaccess, i, j, k, l);
				return (Boolean) obj;
			} catch (Exception var6) {
				return false;
			}
		}
		else {
			return false;
		}
	}

	public static int getPowerState(IBlockAccess iblockaccess, int i, int j, int k, int l, int i1) {
		findRedPower();
		if (redPowerLib_getPowerState != null) {
			try {
				Object obj = redPowerLib_getPowerState.invoke(null, iblockaccess, i, j, k, l, i1);
				return (Integer) obj;
			} catch (Exception var7) {
				return 0;
			}
		}
		else {
			return 0;
		}
	}

	public static int getConDirMask(int i) {
		findRedPower();
		if (redPowerLib_getConDirMask != null) {
			try {
				Object obj = redPowerLib_getConDirMask.invoke(null, i);
				return (Integer) obj;
			} catch (Exception var2) {
				return 0;
			}
		}
		else {
			return 0;
		}
	}

	public static void addCompatibleMapping(int i, int j) {
		findRedPower();
		if (redPowerLib_addCompatibleMapping != null) {
			try {
				redPowerLib_addCompatibleMapping.invoke(null, i, j);
			} catch (Exception ignored) {
			}
		}
	}

	public static int getComputerConnectClass() {
		return computerConnectClass;
	}

	public static void addComputerConnectMappings() {
		findRedPower();
		if (redPowerLib_addCompatibleMapping != null && !computerConnectMappingsAdded) {
			addCompatibleMapping(0, computerConnectClass);
			addCompatibleMapping(18, computerConnectClass);

			for (int i = 0; i < 16; i++) {
				addCompatibleMapping(1 + i, computerConnectClass);
				addCompatibleMapping(19 + i, computerConnectClass);
			}

			computerConnectMappingsAdded = true;
		}
	}

	public static int getComputerPoweringMask(int i, IComputerEntity icomputerentity, int j) {
		findRedPower();
		if (isRedPowerInstalled()) {
			if (i == 0) {
				int k = 0;

				for (int i1 = 0; i1 < 6; i1++) {
					int k1 = BlockComputer.getOppositeSide(BlockComputerBase.getLocalSide(i1, j));
					if (icomputerentity.getPowerOutput(k1)) {
						k |= getConDirMask(i1);
					}
				}

				return k;
			}
			else {
				int l = 0;

				for (int j1 = 0; j1 < 6; j1++) {
					int l1 = BlockComputer.getOppositeSide(BlockComputerBase.getLocalSide(j1, j));
					int i2 = icomputerentity.getBundledPowerOutput(l1);
					if ((i2 & 1 << i - 1) > 0) {
						l |= getConDirMask(j1);
					}
				}

				return l;
			}
		}
		else {
			return 0;
		}
	}
}
