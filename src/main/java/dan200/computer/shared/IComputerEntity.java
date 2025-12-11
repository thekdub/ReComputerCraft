package dan200.computer.shared;

import dan200.computer.api.IPeripheral;


public interface IComputerEntity extends ITerminalEntity, IDestroyableEntity {
	void turnOn();

	void shutdown();

	void reboot();

	void terminate();

	boolean isOn();

	void pressKey(char var1, int var2);

	void typeString(String var1);

	int getComputerID();

	void setComputerID(int var1);

	boolean getPowerOutput(int var1);

	void setPowerInput(int var1, boolean var2);

	int getBundledPowerOutput(int var1);

	void setBundledPowerInput(int var1, int var2);

	void setPeripheral(int var1, IPeripheral var2);

	void unload();
}
