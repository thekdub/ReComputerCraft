package dan200.turtle.shared;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;


public class TurtlePeripheral implements IPeripheral, ITurtleListener {
	static final boolean $assertionsDisabled = !TurtlePeripheral.class.desiredAssertionStatus();
	private final ITurtle m_turtle;
	private IComputerAccess m_computer;

	public TurtlePeripheral(ITurtle turtle) {
		this.m_turtle = turtle;
		this.m_turtle.addListener(this);
		this.m_computer = null;
	}

	@Override
	public void commandProcessed(int i, boolean flag) {
		if (this.m_computer != null) {
			this.m_computer.queueEvent("turtle_response", new Object[] {i, flag});
		}
	}

	@Override
	public String getType() {
		return "turtle";
	}

	@Override
	public String[] getMethodNames() {
		return new String[] {
				"forward",
				"back",
				"up",
				"down",
				"turnLeft",
				"turnRight",
				"dig",
				"digUp",
				"digDown",
				"place",
				"placeUp",
				"placeDown",
				"drop",
				"select",
				"getItemCount",
				"getItemSpace",
				"detect",
				"detectUp",
				"detectDown",
				"compare",
				"compareUp",
				"compareDown"
		};
	}

	private Object[] tryCommand(int i) {
		int j = this.m_turtle.issueCommand(i);
		return new Object[] {j};
	}

	private int parseSlotNumber(Object[] aobj) throws Exception {
		if (aobj.length >= 1 && aobj[0] instanceof Double) {
			int i = (int) ((Double) aobj[0]).doubleValue();
			if (i >= 1 && i <= 9) {
				return i;
			}
			else {
				throw new Exception("Slot number " + i + " out of range");
			}
		}
		else {
			throw new Exception("Expected number");
		}
	}

	private int parseCount(Object[] aobj) throws Exception {
		if (aobj.length < 1) {
			return -1;
		}
		else if (!(aobj[0] instanceof Double)) {
			throw new Exception("Expected number");
		}
		else {
			int i = (int) ((Double) aobj[0]).doubleValue();
			if (i >= 0 && i <= 64) {
				return i;
			}
			else {
				throw new Exception("Drop count " + i + " out of range");
			}
		}
	}

	@Override
	public Object[] callMethod(IComputerAccess computerAccess, int i, Object[] aobj) throws Exception {
		switch (i) {
			case 0:
				return this.tryCommand(1);
			case 1:
				return this.tryCommand(2);
			case 2:
				return this.tryCommand(3);
			case 3:
				return this.tryCommand(4);
			case 4:
				return this.tryCommand(5);
			case 5:
				return this.tryCommand(6);
			case 6:
				return this.tryCommand(7);
			case 7:
				return this.tryCommand(8);
			case 8:
				return this.tryCommand(9);
			case 9:
				return this.tryCommand(10);
			case 10:
				return this.tryCommand(11);
			case 11:
				return this.tryCommand(12);
			case 12:
				int j = this.parseCount(aobj);
				if (j >= 0) {
					int j1 = 29 + j;
					return this.tryCommand(j1);
				}

				return this.tryCommand(13);
			case 13:
				int k = this.parseSlotNumber(aobj);
				int k1 = 14 + (k - 1);
				return this.tryCommand(k1);
			case 14:
				int l = this.parseSlotNumber(aobj);
				return new Object[] {this.m_turtle.getSlotItemCount(l - 1)};
			case 15:
				int i1 = this.parseSlotNumber(aobj);
				return new Object[] {this.m_turtle.getSlotSpaceRemaining(i1 - 1)};
			case 16:
				return this.tryCommand(23);
			case 17:
				return this.tryCommand(24);
			case 18:
				return this.tryCommand(25);
			case 19:
				return this.tryCommand(26);
			case 20:
				return this.tryCommand(27);
			case 21:
				return this.tryCommand(28);
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
	public void attach(IComputerAccess computerAccess, String s) {
		if (!$assertionsDisabled && this.m_computer != null) {
			throw new AssertionError();
		}
		else {
			this.m_computer = computerAccess;
		}
	}

	@Override
	public void detach(IComputerAccess computerAccess) {
		if (!$assertionsDisabled && this.m_computer == null) {
			throw new AssertionError();
		}
		else {
			this.m_computer = null;
		}
	}
}
