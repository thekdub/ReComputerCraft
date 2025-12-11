package dan200.computer.shared;

import dan200.computer.core.Terminal;


public interface ITerminalEntity {
	boolean isDestroyed();

	Terminal getTerminal();
}
