package dan200.computer.shared;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;


public class ComputerPeripheral implements IPeripheral {
	static final boolean $assertionsDisabled = !ComputerPeripheral.class.desiredAssertionStatus();
	private IComputerEntity m_computer;
	private final String m_type;

	public ComputerPeripheral(IComputerEntity computerEntity, String s) {
		this.m_computer = computerEntity;
		this.m_type = s;
	}

	public void setComputer(IComputerEntity computerEntity) {
		this.m_computer = computerEntity;
	}

	@Override
	public String getType() {
		return this.m_type;
	}

	@Override
	public String[] getMethodNames() {
		return new String[] {"turnOn", "shutdown", "reboot", "getID", "isOn"};
	}

	@Override
	public Object[] callMethod(IComputerAccess computerAccess, int i, Object[] obj) throws Exception {
		switch (i) {
			case 0:
				this.m_computer.turnOn();
				return null;
			case 1:
				this.m_computer.shutdown();
				return null;
			case 2:
				this.m_computer.reboot();
				return null;
			case 3:
				if (this.m_computer.isOn()) {
					return new Object[] {this.m_computer.getComputerID()};
				}

				return null;
			case 4:
				return new Object[] {this.m_computer.isOn()};
			default:
				if (!$assertionsDisabled) {
					throw new AssertionError();
				}
				else {
					return null;
				}
		}
	}

	@Override
	public boolean canAttachToSide(int i) {
		return true;
	}

	@Override
	public synchronized void attach(IComputerAccess computerAccess, String s) {
	}

	@Override
	public synchronized void detach(IComputerAccess computerAccess) {
	}
}
