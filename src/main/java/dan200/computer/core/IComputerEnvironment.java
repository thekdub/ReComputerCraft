package dan200.computer.core;

import java.io.File;


public interface IComputerEnvironment {
	File getStaticDir();

	File getSaveDir();

	double getTimeOfDay();

	boolean isHTTPEnabled();
}
