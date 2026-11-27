package dan200.computer.core;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.IPeripheral;
import dan200.computer.shared.ItemComputer;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.SharedConstants;
import net.minecraft.server.mod_ComputerCraft;

import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaThread;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

import javax.annotation.Nonnull;
import java.io.*;
import java.util.*;


public class Computer {
	static final boolean $assertionsDisabled = !Computer.class.desiredAssertionStatus();
	private static final String[] sides = new String[] {"top", "bottom", "front", "back", "left", "right"};
	private IComputerEnvironment m_environment;
	private int m_id;
	private final Terminal m_terminal;
	private FileSystem m_fileSystem;
	private boolean m_on;
	private boolean m_onDesired;
	private boolean m_persistedOn;
	private boolean m_rebootDesired;
	private boolean m_stopped;
	private boolean m_aborted;
	private boolean m_blinking;
	private final List<Timer> m_timers;
	private final List<Alarm> m_alarms;
	private final List<HTTPRequest> m_httpRequests;
	private LuaValue m_mainFunction;
	private LuaValue m_globals;
	private String m_eventFilter;
	private final boolean[] m_output;
	private final int[] m_bundledOutput;
	private boolean m_outputChanged;
	private final boolean[] m_input;
	private final int[] m_bundledInput;
	private boolean m_inputChanged;
	private final Computer.PeripheralWrapper[] m_peripherals;
	private final List<Computer.PeripheralWrapper> m_peripheralsAsAPIs;
	private double m_clock;
	private double m_time;
	private ThreadGroup threadGroup;

	public Computer(IComputerEnvironment icomputerenvironment, Terminal terminal) {
		this.m_environment = icomputerenvironment;
		ComputerThread.start();
		this.m_id = -1;
		this.m_terminal = terminal;
		this.m_fileSystem = null;
		this.m_on = false;
		this.m_onDesired = false;
		this.m_rebootDesired = false;
		this.m_stopped = false;
		this.m_aborted = false;
		this.m_blinking = false;
		this.m_timers = new ArrayList<>();
		this.m_alarms = new ArrayList<>();
		this.m_httpRequests = new ArrayList<>();
		this.m_mainFunction = null;
		this.m_globals = null;
		this.m_eventFilter = null;
		this.m_output = new boolean[6];
		this.m_bundledOutput = new int[6];
		this.m_outputChanged = false;
		this.m_input = new boolean[6];
		this.m_bundledInput = new int[6];
		this.m_inputChanged = false;
		this.m_peripherals = new Computer.PeripheralWrapper[6];

		for (int i = 0; i < 6; i++) {
			this.m_peripherals[i] = null;
		}

		this.m_peripheralsAsAPIs = new ArrayList<>();
		this.m_clock = 0.0;
		this.m_time = 0.0;
	}

	public synchronized ThreadGroup getThreadGroup() {
		if (threadGroup == null) {
			threadGroup = new ThreadGroup("ReCC-Computer-" + m_id);
			threadGroup.setMaxPriority(Thread.MIN_PRIORITY);
		}
		return threadGroup;
	}

	public synchronized void interruptThreads() {
		if (threadGroup != null) {
			threadGroup.interrupt();

			long timeout = System.currentTimeMillis() + 20;
			while (threadGroup.activeCount() > 0 && System.currentTimeMillis() < timeout) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException ignored) {}
			}
			try {
				threadGroup.stop();
				threadGroup.destroy();
			}
			catch (IllegalThreadStateException e) {
				System.out.println("ReCC: Error destroying thread group for computer " + m_id);
				e.printStackTrace();
			}
			threadGroup = null;
		}
	}

	public void setOwner(IComputerEnvironment icomputerenvironment) {
		this.m_environment = icomputerenvironment;
	}

	public void turnOn() {
		synchronized (this) {
			this.m_onDesired = true;
			this.m_rebootDesired = false;
			this.m_persistedOn = true;
		}
	}

	public void turnOff() {
		synchronized (this) {
			this.m_onDesired = false;
			this.m_rebootDesired = false;
			this.m_persistedOn = false;
		}
	}

	public void reboot() {
		synchronized (this) {
			this.m_onDesired = false;
			this.m_rebootDesired = true;
			this.m_persistedOn = true;
		}
	}

	public boolean isOn() {
		return this.m_on;
	}

	public void abort() {
		synchronized (this) {
			if (this.m_on) {
				this.m_aborted = true;
			}
		}
	}

	public void destroy() {
		synchronized (this) {
			this.m_onDesired = false;
			this.m_rebootDesired = false;

			if (this.m_on) {
				this.stopComputer();
			}
		}
	}

	public synchronized void writeToNBT(NBTTagCompound nbttagcompound) {
		nbttagcompound.setBoolean("on", this.isOn() || this.m_persistedOn);
		String s = this.getUserDir();
		if (s != null) {
			nbttagcompound.setString("userDir", s);
		}

		synchronized (this.m_input) {
			synchronized (this.m_bundledInput) {
				int i = 0;

				for (int j = 0; j < 6; j++) {
					if (this.getInput(j)) {
						i += 1 << j;
					}

					int k = this.getBundledInput(j);
					if (k > 0) {
						nbttagcompound.setInt("rp" + sides[j], k);
					}
				}

				if (i > 0) {
					nbttagcompound.setInt("input", i);
				}
			}
		}
	}

	public synchronized void readFromNBT(NBTTagCompound nbttagcompound) {
		boolean flag = nbttagcompound.getBoolean("on");
		if (flag) {
			this.turnOn();
		}
		else {
			this.turnOff();
		}

		String s = nbttagcompound.getString("userDir");
		if (s != null && !s.isEmpty()) {
			this.setUserDir(s);
		}

		synchronized (this.m_input) {
			synchronized (this.m_bundledInput) {
				int i = nbttagcompound.getInt("input");

				for (int j = 0; j < 6; j++) {
					this.m_input[j] = (i & 1 << j) > 0;
					this.m_bundledInput[j] = nbttagcompound.getInt("rp" + sides[j]);
				}
			}
		}
	}

	public int getID() {
		synchronized (this) {
			return this.m_id;
		}
	}

	public void setID(int i) {
		synchronized (this) {
			if (!$assertionsDisabled && this.m_id != -1 && this.m_id != i) {
				throw new AssertionError();
			}
			else {
				this.m_id = i;
			}
		}
	}

	private String getUserDir() {
		int i = this.getID();
		return i >= 0 ? Integer.toString(i) : null;
	}

	private void setUserDir(String s) {
		int i;
		try {
			i = Integer.parseInt(s);
		} catch (NumberFormatException var4) {
			System.out.println("ComputerCraft: Error! Computer has non-numerical userDir; this is not allowed. A new ID will be assigned.");
			return;
		}

		this.setID(i);
	}

	public void pressKey(final char ch, final int key) {
		if (this.m_on) {
			if (key >= 0) {
				this.queueLuaEvent(() -> new LuaValue[] {LuaValue.valueOf("key"), LuaValue.valueOf(key)});
			}

			if (SharedConstants.allowedCharacters.indexOf(ch) >= 0) {
				this.queueLuaEvent(() -> new LuaValue[] {LuaValue.valueOf("char"), LuaValue.valueOf("" + ch)});
			}
		}
	}

	public void terminate() {
		if (this.m_on) {
			this.queueLuaEvent(() -> new LuaValue[] {LuaValue.valueOf("terminate")});
		}
	}

	public void advance(double time) {
		synchronized (this) {
			if (!this.m_on && this.m_rebootDesired) {
				this.turnOn();
			}

			if (!this.m_on && this.m_onDesired) {
				this.startComputer();
			}

			if (this.m_on && !this.m_onDesired) {
				this.stopComputer();
			}
		}

		synchronized (this) {
			if (this.m_on) {
				synchronized (this.m_input) {
					if (this.m_inputChanged) {
						this.queueLuaEvent(() -> new LuaValue[] {LuaValue.valueOf("redstone")});
						this.m_inputChanged = false;
					}
				}

				this.m_clock += time;
				synchronized (this.m_timers) {
					Iterator<Timer> iterator = this.m_timers.iterator();

					while (iterator.hasNext()) {
						Computer.Timer timer = iterator.next();
						timer.timeLeft -= time;
						if (timer.timeLeft <= 0.0) {
							final LuaValue token = timer.token;
							this.queueLuaEvent(() -> new LuaValue[] {LuaValue.valueOf("timer"), token});
							iterator.remove();
						}
					}
				}

				synchronized (this.m_alarms) {
					double computerTime = this.m_time;
					double worldTime = this.m_environment.getTimeOfDay();
					if (worldTime < computerTime) {
						worldTime = worldTime + 24.0;
					}

					ArrayList<Computer.Alarm> finishedAlarms = null;
					Iterator<Computer.Alarm> alarms = this.m_alarms.iterator();

					while (alarms.hasNext()) {
						Computer.Alarm alarm = alarms.next();
						double alarmTime = alarm.time;
						if (alarmTime < computerTime) {
							alarmTime += 24.0;
						}

						if (worldTime >= alarmTime) {
							if (finishedAlarms == null) {
								finishedAlarms = new ArrayList<>();
							}

							finishedAlarms.add(alarm);
							alarms.remove();
						}
					}

					if (finishedAlarms != null) {
						Collections.sort(finishedAlarms);
						alarms = finishedAlarms.iterator();

						while (alarms.hasNext()) {
							final LuaValue token = alarms.next().token;
							this.queueLuaEvent(() -> new LuaValue[] {LuaValue.valueOf("alarm"), token});
						}
					}

					this.m_time = worldTime;
				}

				synchronized (this.m_httpRequests) {
					Iterator<HTTPRequest> httpRequests = this.m_httpRequests.iterator();

					while (httpRequests.hasNext()) {
						HTTPRequest httprequest = httpRequests.next();
						if (httprequest.isComplete()) {
							final String url = httprequest.getURL();
							if (httprequest.wasSuccessful()) {
								final BufferedReader contents = httprequest.getContents();
								this.queueLuaEvent(() -> {
									LuaValue luavalue = Computer.this.wrapBufferedReader(contents);
									return new LuaValue[] {LuaValue.valueOf("http_success"), LuaValue.valueOf(url), luavalue};
								});
							}
							else {
								this.queueLuaEvent(() -> new LuaValue[] {LuaValue.valueOf("http_failure"), LuaValue.valueOf(url)});
							}

							httpRequests.remove();
						}
					}
				}
			}
		}

		synchronized (this.m_terminal) {
			boolean flag = this.m_terminal.getCursorBlink()
					&& this.m_terminal.getCursorX() >= 0
					&& this.m_terminal.getCursorX() < this.m_terminal.getWidth()
					&& this.m_terminal.getCursorY() >= 0
					&& this.m_terminal.getCursorY() < this.m_terminal.getHeight();
			if (flag != this.m_blinking) {
				synchronized (this.m_output) {
					this.m_outputChanged = true;
					this.m_blinking = flag;
				}
			}
		}
	}

	public boolean pollChanged() {
		synchronized (this.m_output) {
			if (this.m_outputChanged) {
				this.m_outputChanged = false;
				return true;
			}
			else {
				return false;
			}
		}
	}

	public boolean isBlinking() {
		synchronized (this.m_terminal) {
			return this.isOn() && this.m_blinking;
		}
	}

	private File getUserDir(boolean flag) {
		File file = new File(this.m_environment.getSaveDir(), "/computer/");
		if (this.m_id < 0) {
			if (!flag) {
				return null;
			}

			this.m_id = 0;

			while (new File(file, Integer.toString(this.m_id)).exists()) {
				this.m_id++;
			}
		}

		File file1 = new File(file, Integer.toString(this.m_id));
		file1.mkdirs();
		return file1;
	}

	private void initFileSystem() {
		File file = new File(this.m_environment.getStaticDir(), "mods/ComputerCraft/lua/rom");
		File file1 = this.getUserDir(true);

		try {
			this.m_fileSystem = new FileSystem(file1, false);
			this.m_fileSystem.mount("rom", file, true);
		} catch (FileSystemException var4) {
			var4.printStackTrace();
		}
	}

	private void setBundledOutput(int i, int j) {
		synchronized (this.m_output) {
			if (this.m_bundledOutput[i] != j) {
				this.m_bundledOutput[i] = j;
				this.m_outputChanged = true;
			}
		}
	}

	public int getBundledOutput(int i) {
		synchronized (this.m_output) {
			return this.isOn() ? this.m_bundledOutput[i] : 0;
		}
	}

	private void setOutput(int i, boolean flag) {
		synchronized (this.m_output) {
			if (this.m_output[i] != flag) {
				this.m_output[i] = flag;
				this.m_outputChanged = true;
			}
		}
	}

	public boolean getOutput(int i) {
		synchronized (this.m_output) {
			return this.isOn() && this.m_output[i];
		}
	}

	public void setBundledInput(int i, int j) {
		synchronized (this.m_input) {
			if (this.m_bundledInput[i] != j) {
				this.m_bundledInput[i] = j;
				this.m_inputChanged = true;
			}
		}
	}

	private int getBundledInput(int i) {
		synchronized (this.m_input) {
			return this.m_bundledInput[i];
		}
	}

	public void setInput(int i, boolean flag) {
		synchronized (this.m_input) {
			if (this.m_input[i] != flag) {
				this.m_input[i] = flag;
				this.m_inputChanged = true;
			}
		}
	}

	private boolean getInput(int i) {
		synchronized (this.m_input) {
			return this.m_input[i];
		}
	}

	public void setPeripheral(final int side, IPeripheral iperipheral) {
		synchronized (this.m_peripherals) {
			IPeripheral peripheral1 = null;
			if (this.m_peripherals[side] != null) {
				peripheral1 = this.m_peripherals[side].getPeripheral();
			}

			if (iperipheral != peripheral1) {
				if (this.m_peripherals[side] != null && this.m_on && !this.m_stopped) {
					final Computer.PeripheralWrapper wrapper = this.m_peripherals[side];
					ComputerThread.queueTask(new ComputerThread.Task() {
						@Override
						public Computer getOwner() {
							return Computer.this;
						}

						@Override
						public void execute() {
							synchronized (this) {
								if (!Computer.this.m_on || Computer.this.m_stopped) {
									return;
								}
							}

							synchronized (Computer.this.m_peripherals) {
								if (wrapper.isAttached()) {
									wrapper.detach();
								}
							}
						}
					});
					this.queueLuaEvent(new Computer.Event() {
						@Override
						public LuaValue[] getArguments() {
							return new LuaValue[] {LuaValue.valueOf("peripheral_detach"), LuaValue.valueOf(Computer.sides[side])};
						}
					});
				}

				if (iperipheral != null) {
					this.m_peripherals[side] = new Computer.PeripheralWrapper(iperipheral);
				}
				else {
					this.m_peripherals[side] = null;
				}

				if (this.m_peripherals[side] != null && this.m_on && !this.m_stopped) {
					final Computer.PeripheralWrapper wrapper = this.m_peripherals[side];
					ComputerThread.queueTask(new ComputerThread.Task() {
						@Override
						public Computer getOwner() {
							return Computer.this;
						}

						@Override
						public void execute() {
							synchronized (this) {
								if (!Computer.this.m_on || Computer.this.m_stopped) {
									return;
								}
							}

							synchronized (Computer.this.m_peripherals) {
								if (!wrapper.isAttached()) {
									wrapper.attach(Computer.sides[side]);
								}
							}
						}
					});
					this.queueLuaEvent(new Computer.Event() {
						@Override
						public LuaValue[] getArguments() {
							return new LuaValue[] {LuaValue.valueOf("peripheral"), LuaValue.valueOf(Computer.sides[side])};
						}
					});
				}
			}
		}
	}

	public void addPeripheralAsAPI(IPeripheral iperipheral) {
		synchronized (this.m_peripheralsAsAPIs) {
			final Computer.PeripheralWrapper wrapper = new Computer.PeripheralWrapper(iperipheral);
			this.m_peripheralsAsAPIs.add(wrapper);
			if (this.m_on && !this.m_stopped) {
				final Computer computer = this;
				ComputerThread.queueTask(new ComputerThread.Task() {
					@Override
					public Computer getOwner() {
						return computer;
					}

					@Override
					public void execute() {
						synchronized (this) {
							if (!Computer.this.m_on || Computer.this.m_stopped) {
								return;
							}
						}

						synchronized (Computer.this.m_peripherals) {
							if (!wrapper.isAttached()) {
								wrapper.attach("api");
								wrapper.installAsAPI(Computer.this.m_globals);
							}
						}
					}
				});
			}
		}
	}

	private void tryAbort() {
		if (this.m_stopped) {
			LuaValue coroutine = this.m_globals.get("coroutine");
			if (coroutine != null) {
				coroutine.get("yield").call();
			}
		}

		if (this.m_aborted) {
			this.m_aborted = false;
			throw new LuaError("Too long without yielding");
		}
	}

	private void initLua() {
		LuaTable globalTable = JsePlatform.debugGlobals();
		LuaValue loadstringValue = globalTable.get("loadstring");

        LuaTable initConfig = new LuaTable();
        initConfig.set("max_modem_range", mod_ComputerCraft.modem_range);
        globalTable.set("base_config", initConfig);

		globalTable.set("collectgarbage", LuaValue.NIL);
		globalTable.set("dofile", LuaValue.NIL);
		globalTable.set("load", LuaValue.NIL);
		globalTable.set("loadfile", LuaValue.NIL);
		globalTable.set("module", LuaValue.NIL);
		globalTable.set("require", LuaValue.NIL);
		globalTable.set("package", LuaValue.NIL);
		globalTable.set("io", LuaValue.NIL);
		globalTable.set("os", LuaValue.NIL);
		globalTable.set("debug", LuaValue.NIL);
		globalTable.set("print", LuaValue.NIL);
		globalTable.set("luajava", LuaValue.NIL);

		LuaTable termTable = new LuaTable();
		termTable.set("write", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue value) {
				Computer.this.tryAbort();
				String s1 = "";
				if (!value.isnil()) {
					s1 = value.toString();
				}

				synchronized (Computer.this.m_terminal) {
					Computer.this.m_terminal.write(s1);
					Computer.this.m_terminal.setCursorPos(Computer.this.m_terminal.getCursorX() + s1.length(), Computer.this.m_terminal.getCursorY());
				}

				return LuaValue.NIL;
			}
		});
		termTable.set("scroll", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue amount) {
				Computer.this.tryAbort();
				int i = amount.checkint();
				synchronized (Computer.this.m_terminal) {
					Computer.this.m_terminal.scroll(i);
				}

				return LuaValue.NIL;
			}
		});
		termTable.set("setCursorPos", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue x, LuaValue y) {
				Computer.this.tryAbort();
				int i = x.checkint() - 1;
				int j = y.checkint() - 1;
				synchronized (Computer.this.m_terminal) {
					Computer.this.m_terminal.setCursorPos(i, j);
				}

				return LuaValue.NIL;
			}
		});
		termTable.set("setCursorBlink", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue blink) {
				Computer.this.tryAbort();
				boolean flag = blink.checkboolean();
				synchronized (Computer.this.m_terminal) {
					Computer.this.m_terminal.setCursorBlink(flag);
				}

				return LuaValue.NIL;
			}
		});
		termTable.set("getCursorPos", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs varargs) {
				Computer.this.tryAbort();
				int i;
				int j;
				synchronized (Computer.this.m_terminal) {
					i = Computer.this.m_terminal.getCursorX();
					j = Computer.this.m_terminal.getCursorY();
				}

				return LuaValue.varargsOf(new LuaValue[] {LuaValue.valueOf(i + 1), LuaValue.valueOf(j + 1)});
			}
		});
		termTable.set("getSize", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs varargs) {
				Computer.this.tryAbort();
				int i;
				int j;
				synchronized (Computer.this.m_terminal) {
					i = Computer.this.m_terminal.getWidth();
					j = Computer.this.m_terminal.getHeight();
				}

				return LuaValue.varargsOf(new LuaValue[] {LuaValue.valueOf(i), LuaValue.valueOf(j)});
			}
		});
		termTable.set("clear", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();
				synchronized (Computer.this.m_terminal) {
					Computer.this.m_terminal.clear();
				}

				return LuaValue.NIL;
			}
		});
		termTable.set("clearLine", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();
				synchronized (Computer.this.m_terminal) {
					Computer.this.m_terminal.clearLine();
				}

				return LuaValue.NIL;
			}
		});
		globalTable.set("term", termTable);
		LuaTable redstoneTable = new LuaTable();
		redstoneTable.set("getSides", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();
				LuaTable luatable7 = new LuaTable();

				for (int i = 0; i < 6; i++) {
					luatable7.set(i + 1, LuaValue.valueOf(Computer.sides[i]));
				}

				return luatable7;
			}
		});
		redstoneTable.set("setOutput", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue side, LuaValue state) {
				Computer.this.tryAbort();
				int i = Computer.this.parseSide(side.checkstring().toString());
				Computer.this.setOutput(i, state.checkboolean());
				return LuaValue.NIL;
			}
		});
		redstoneTable.set("getOutput", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue side) {
				Computer.this.tryAbort();
				int i = Computer.this.parseSide(side.checkstring().toString());
				return LuaValue.valueOf(Computer.this.getOutput(i));
			}
		});
		redstoneTable.set("getInput", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue side) {
				Computer.this.tryAbort();
				int i = Computer.this.parseSide(side.checkstring().toString());
				return LuaValue.valueOf(Computer.this.getInput(i));
			}
		});
		redstoneTable.set("setBundledOutput", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue side, LuaValue color) {
				Computer.this.tryAbort();
				int i = Computer.this.parseSide(side.checkstring().toString());
				Computer.this.setBundledOutput(i, color.checkint());
				return LuaValue.NIL;
			}
		});
		redstoneTable.set("getBundledOutput", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue side) {
				Computer.this.tryAbort();
				int i = Computer.this.parseSide(side.checkstring().toString());
				return LuaValue.valueOf(Computer.this.getBundledOutput(i));
			}
		});
		redstoneTable.set("getBundledInput", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue side) {
				Computer.this.tryAbort();
				int i = Computer.this.parseSide(side.checkstring().toString());
				return LuaValue.valueOf(Computer.this.getBundledInput(i));
			}
		});
		redstoneTable.set("testBundledInput", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue side, LuaValue color) {
				Computer.this.tryAbort();
				int i = Computer.this.parseSide(side.checkstring().toString());
				int j = color.checkint();
				int k = Computer.this.getBundledInput(i);
				return LuaValue.valueOf((j & k) == j);
			}
		});
		globalTable.set("redstone", redstoneTable);
		globalTable.set("rs", redstoneTable);
		LuaTable fsTable = new LuaTable();
		fsTable.set("list", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				Computer.this.tryAbort();
				String s1 = path.checkstring().toString();

				try {
					String[] as = Computer.this.m_fileSystem.list(s1);
					LuaTable fileTable = new LuaTable();

					for (int i = 0; i < as.length; i++) {
						fileTable.set(i + 1, LuaValue.valueOf(as[i]));
					}

					return fileTable;
				} catch (FileSystemException var6) {
					throw new LuaError(var6.getMessage());
				}
			}
		});
		fsTable.set("combine", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue pathA, LuaValue pathB) {
				Computer.this.tryAbort();
				String s1 = pathA.checkstring().toString();
				String s2 = pathB.checkstring().toString();
				return LuaValue.valueOf(Computer.this.m_fileSystem.combine(s1, s2));
			}
		});
		fsTable.set("getName", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				Computer.this.tryAbort();
				String s1 = path.checkstring().toString();
				return LuaValue.valueOf(Computer.this.m_fileSystem.getName(s1));
			}
		});
		fsTable.set("getSize", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				Computer.this.tryAbort();
				String s1 = path.checkstring().toString();

				try {
					return LuaValue.valueOf((double) Computer.this.m_fileSystem.getSize(s1));
				} catch (FileSystemException var4) {
					throw new LuaError(var4.getMessage());
				}
			}
		});
		fsTable.set("exists", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				Computer.this.tryAbort();
				String s1 = path.checkstring().toString();

				try {
					return LuaValue.valueOf(Computer.this.m_fileSystem.exists(s1));
				} catch (FileSystemException var4) {
					return LuaValue.FALSE;
				}
			}
		});
		fsTable.set("isDir", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				Computer.this.tryAbort();
				String s1 = path.checkstring().toString();

				try {
					return LuaValue.valueOf(Computer.this.m_fileSystem.isDir(s1));
				} catch (FileSystemException var4) {
					return LuaValue.FALSE;
				}
			}
		});
		fsTable.set("isReadOnly", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				Computer.this.tryAbort();
				String s1 = path.checkstring().toString();

				try {
					return LuaValue.valueOf(Computer.this.m_fileSystem.isReadOnly(s1));
				} catch (FileSystemException var4) {
					return LuaValue.FALSE;
				}
			}
		});
		fsTable.set("makeDir", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				Computer.this.tryAbort();
				String s1 = path.checkstring().toString();

				try {
					Computer.this.m_fileSystem.makeDir(s1);
					return LuaValue.NIL;
				} catch (FileSystemException var4) {
					throw new LuaError(var4.getMessage());
				}
			}
		});
		fsTable.set("move", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue pathFrom, LuaValue pathTo) {
				Computer.this.tryAbort();
				String s1 = pathFrom.checkstring().toString();
				String s2 = pathTo.checkstring().toString();

				try {
					Computer.this.m_fileSystem.move(s1, s2);
					return LuaValue.NIL;
				} catch (FileSystemException var6) {
					throw new LuaError(var6.getMessage());
				}
			}
		});
		fsTable.set("copy", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue pathFrom, LuaValue pathTo) {
				Computer.this.tryAbort();
				String s1 = pathFrom.checkstring().toString();
				String s2 = pathTo.checkstring().toString();

				try {
					Computer.this.m_fileSystem.copy(s1, s2);
					return LuaValue.NIL;
				} catch (FileSystemException var6) {
					throw new LuaError(var6.getMessage());
				}
			}
		});
		fsTable.set("delete", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				Computer.this.tryAbort();
				String s1 = path.checkstring().toString();

				try {
					Computer.this.m_fileSystem.delete(s1);
					return LuaValue.NIL;
				} catch (FileSystemException var4) {
					throw new LuaError(var4.getMessage());
				}
			}
		});
		fsTable.set("open", new TwoArgFunction() {
			@Override
			public LuaValue call(LuaValue path, LuaValue mode) {
				Computer.this.tryAbort();
				String s1 = path.checkstring().toString();
				String s2 = mode.checkstring().toString();

				try {
					switch (s2) {
						case "r":
							BufferedReader bufferedreader1 = Computer.this.m_fileSystem.openForRead(s1);
							return Computer.this.wrapBufferedReader(bufferedreader1);
						case "w":
							BufferedWriter bufferedwriter = Computer.this.m_fileSystem.openForWrite(s1, false);
							return Computer.this.wrapBufferedWriter(bufferedwriter);
						case "a":
							BufferedWriter bufferedwriter1 = Computer.this.m_fileSystem.openForWrite(s1, true);
							return Computer.this.wrapBufferedWriter(bufferedwriter1);
						case "rb":
							BufferedInputStream bufferedinputstream = Computer.this.m_fileSystem.openForBinaryRead(s1);
							return Computer.this.wrapInputStream(bufferedinputstream);
						case "wb":
							BufferedOutputStream bufferedoutputstream = Computer.this.m_fileSystem.openForBinaryWrite(s1, false);
							return Computer.this.wrapOutputStream(bufferedoutputstream);
						case "ab":
							BufferedOutputStream bufferedoutputstream1 = Computer.this.m_fileSystem.openForBinaryWrite(s1, true);
							return Computer.this.wrapOutputStream(bufferedoutputstream1);
						default:
							throw new LuaError("Unsupported mode");
					}
				} catch (FileSystemException var6) {
					return LuaValue.NIL;
				}
			}
		});
		fsTable.set("getDrive", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue path) {
				Computer.this.tryAbort();
				String s1 = path.checkstring().toString();

				try {
					synchronized (Computer.this.m_fileSystem) {
						if (!Computer.this.m_fileSystem.exists(s1)) {
							return LuaValue.NIL;
						}

						if (Computer.this.m_fileSystem.contains("rom", s1)) {
							return LuaValue.valueOf("rom");
						}
					}

					return LuaValue.valueOf("hdd");
				} catch (FileSystemException var5) {
					throw new LuaError(var5.getMessage());
				}
			}
		});
		globalTable.set("fs", fsTable);
		LuaTable peripheralTable = new LuaTable();
		peripheralTable.set("isPresent", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue side) {
				Computer.this.tryAbort();
				int i = Computer.this.parseSide(side.checkstring().toString());
				synchronized (Computer.this.m_peripherals) {
					Computer.PeripheralWrapper peripheralwrapper1 = Computer.this.m_peripherals[i];
					if (peripheralwrapper1 != null) {
						return LuaValue.TRUE;
					}
				}

				return LuaValue.FALSE;
			}
		});
		peripheralTable.set("getType", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue side) {
				Computer.this.tryAbort();
				int i = Computer.this.parseSide(side.checkstring().toString());
				synchronized (Computer.this.m_peripherals) {
					Computer.PeripheralWrapper peripheralwrapper1 = Computer.this.m_peripherals[i];
					if (peripheralwrapper1 != null) {
						return LuaValue.valueOf(peripheralwrapper1.getType());
					}
				}

				return LuaValue.NIL;
			}
		});
		peripheralTable.set("getMethods", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue side) {
				Computer.this.tryAbort();
				int i = Computer.this.parseSide(side.checkstring().toString());
				synchronized (Computer.this.m_peripherals) {
					Computer.PeripheralWrapper peripheralwrapper1 = Computer.this.m_peripherals[i];
					if (peripheralwrapper1 != null) {
						String[] as = peripheralwrapper1.getMethods();
						LuaTable luatable7 = new LuaTable();

						for (int j = 0; j < as.length; j++) {
							luatable7.set(j + 1, LuaValue.valueOf(as[j]));
						}

						return luatable7;
					}
				}

				return LuaValue.NIL;
			}
		});
		peripheralTable.set("call", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs side) {
				Computer.this.tryAbort();
				int i = Computer.this.parseSide(side.checkstring(1).toString());
				String s1 = side.checkstring(2).toString();
				synchronized (Computer.this.m_peripherals) {
					Computer.PeripheralWrapper peripheralwrapper1 = Computer.this.m_peripherals[i];
					if (peripheralwrapper1 != null) {
						Object[] aobj = Computer.this.toObjects(side, 3);
						Object[] aobj1 = peripheralwrapper1.call(s1, aobj);
						return LuaValue.varargsOf(Computer.this.toValues(aobj1, 0));
					}
				}

				throw new LuaError("No peripheral attached");
			}
		});
		globalTable.set("peripheral", peripheralTable);
		LuaTable osTable = new LuaTable();
		osTable.set("queueEvent", new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs event) {
				Computer.this.tryAbort();
				LuaString luastring = event.checkstring(1);
				final LuaValue[] args = new LuaValue[6];
				args[0] = luastring;

				for (int i = 0; i < 5; i++) {
					args[i + 1] = event.arg(i + 2);
				}

				Computer.this.queueLuaEvent(() -> args);
				return LuaValue.varargsOf(LuaValue.NOVALS);
			}
		});
		osTable.set("startTimer", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue length) {
				Computer.this.tryAbort();
				double d = Math.max(length.checkdouble(), 0.0);
				LuaTable luatable7 = new LuaTable();
				synchronized (Computer.this.m_timers) {
					Computer.this.m_timers.add(new Timer(d, luatable7));
					return luatable7;
				}
			}
		});
		osTable.set("setAlarm", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue length) {
				Computer.this.tryAbort();
				double d = length.checkdouble();
				if (!(d < 0.0) && !(d > 24.0)) {
					LuaTable luatable7 = new LuaTable();
					synchronized (Computer.this.m_alarms) {
						Computer.this.m_alarms.add(Computer.this.new Alarm(d, luatable7));
						return luatable7;
					}
				}
				else {
					throw new LuaError("Out of range");
				}
			}
		});
		osTable.set("shutdown", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();
				Computer.this.turnOff();
				return LuaValue.NIL;
			}
		});
		osTable.set("reboot", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();
				Computer.this.reboot();
				return LuaValue.NIL;
			}
		});
		ZeroArgFunction zeroargfunction = new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();
				return LuaValue.valueOf(Computer.this.m_id);
			}
		};
		osTable.set("computerID", zeroargfunction);
		osTable.set("getComputerID", zeroargfunction);
		osTable.set("setComputerLabel", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue label) {
				Computer.this.tryAbort();
				if (!label.isnil()) {
					String s1 = label.checkstring().toString();
					ItemComputer.setComputerLabel(Computer.this.m_id, s1);
				}
				else {
					ItemComputer.setComputerLabel(Computer.this.m_id, null);
				}

				return LuaValue.NIL;
			}
		});
		ZeroArgFunction getLabel = new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();
				String s1 = ItemComputer.getComputerLabel(Computer.this.m_id);
				return s1 != null ? LuaValue.valueOf(s1) : LuaValue.NIL;
			}
		};
		osTable.set("computerLabel", getLabel);
		osTable.set("getComputerLabel", getLabel);
		osTable.set("clock", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();
				synchronized (this) {
					return LuaValue.valueOf((float) Computer.this.m_clock);
				}
			}
		});
		osTable.set("time", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();
				synchronized (this) {
					return LuaValue.valueOf(Computer.this.m_time);
				}
			}
		});
		globalTable.set("os", osTable);
		if (this.m_environment.isHTTPEnabled()) {
			LuaTable httpTable = new LuaTable();
			httpTable.set("request", new TwoArgFunction() {
				@Override
				public LuaValue call(LuaValue url, LuaValue value) {
					Computer.this.tryAbort();
					String s1 = url.checkstring().toString();
					String s2 = null;
					if (value.isstring()) {
						s2 = value.checkstring().toString();
					}

					try {
						HTTPRequest httprequest = new HTTPRequest(s1, s2);
						synchronized (Computer.this.m_httpRequests) {
							Computer.this.m_httpRequests.add(httprequest);
						}

						return LuaValue.NIL;
					} catch (HTTPRequestException var8) {
						throw new LuaError(var8.getMessage());
					}
				}
			});
			globalTable.set("http", httpTable);
		}

		synchronized (this.m_peripheralsAsAPIs) {
			if (!this.m_peripheralsAsAPIs.isEmpty()) {
				for (Computer.PeripheralWrapper peripheralwrapper : this.m_peripheralsAsAPIs) {
					if (!peripheralwrapper.isAttached()) {
						peripheralwrapper.attach("api");
						peripheralwrapper.installAsAPI(globalTable);
					}
				}
			}
		}

		try {
			LuaValue biosValue;
			File biosFile = new File(this.m_environment.getStaticDir(), "mods/ComputerCraft/lua/bios.lua");

			try {
				BufferedReader bufferedreader = new BufferedReader(new FileReader(biosFile));
				StringBuilder biosBuilder = new StringBuilder();
				String biosCursor = bufferedreader.readLine();

				while (biosCursor != null) {
					biosBuilder.append(biosCursor);
					biosCursor = bufferedreader.readLine();
					if (biosCursor != null) {
						biosBuilder.append("\n");
					}
				}

				bufferedreader.close();
				biosValue = globalTable.get("assert").call(loadstringValue.call(LuaValue.valueOf(biosBuilder.toString()), LuaValue.valueOf("bios")));
			} catch (IOException var16) {
				throw new LuaError("IOException: " + var16);
			}

			LuaValue coroutineValue = globalTable.get("coroutine");
			this.m_mainFunction = coroutineValue.get("create").call(biosValue);
			this.m_globals = globalTable;
			this.m_eventFilter = null;
		} catch (LuaError luaError) {
			synchronized (this.m_terminal) {
				this.m_terminal.write("Failed to load mods/ComputerCraft/lua/bios.lua");
				this.m_terminal.setCursorPos(0, this.m_terminal.getCursorY() + 1);
				this.m_terminal.write("Check you have installed ComputerCraft correctly.");
			}

			luaError.printStackTrace();
			this.m_mainFunction = null;
			this.m_globals = null;
			this.m_eventFilter = null;
		}
	}

	private LuaValue[] toValues(Object[] aobj, int i) {
		if (aobj != null && aobj.length != 0) {
			LuaValue[] aluavalue = new LuaValue[aobj.length + i];

			for (int j = 0; j < aluavalue.length; j++) {
				if (j < i) {
					aluavalue[j] = null;
				}
				else {
					Object obj = aobj[j - i];
					if (obj == null) {
						aluavalue[j] = LuaValue.NIL;
					}
					else if (obj instanceof Boolean) {
						boolean flag = (Boolean) obj;
						aluavalue[j] = LuaValue.valueOf(flag);
					}
					else if (obj instanceof String) {
						String s = (String) obj;
						aluavalue[j] = LuaValue.valueOf(s);
					}
					else if (obj instanceof Integer) {
						int k = (Integer) obj;
						aluavalue[j] = LuaValue.valueOf(k);
					}
					else if (obj instanceof Double) {
						double d = (Double) obj;
						aluavalue[j] = LuaValue.valueOf(d);
					}
					else if (obj instanceof Float) {
						double d1 = ((Float) obj).floatValue();
						aluavalue[j] = LuaValue.valueOf(d1);
					}
					else {
						System.out.println("ComputerCraft: Could not convert object of type " + obj.getClass().getName() + " to LuaValue");
						aluavalue[j] = LuaValue.NIL;
					}
				}
			}

			return aluavalue;
		}
		else {
			return new LuaValue[i];
		}
	}

	private Object[] toObjects(Varargs varargs, int i) {
		int j = varargs.narg();
		Object[] aobj = new Object[j - i + 1];

		for (int k = i; k <= j; k++) {
			int l = k - i;
			LuaValue luavalue = varargs.arg(k);
			switch (luavalue.type()) {
				case -2:
				case 3:
					aobj[l] = luavalue.todouble();
					break;
				case -1:
				case 0:
				case 2:
					aobj[l] = null;
					break;
				case 1:
					aobj[l] = luavalue.toboolean();
					break;
				case 4:
					aobj[l] = luavalue.toString();
					break;
				default:
					aobj[l] = null;
					break;
			}
		}

		return aobj;
	}

	private void startComputer() {
		synchronized (this) {
	label31:
			{
				synchronized (this.m_output) {
					if (!this.m_on) {
						this.m_on = true;
						this.m_outputChanged = true;
						this.m_stopped = false;
						this.m_aborted = false;
						this.m_clock = 0.0;
						break label31;
					}
				}

				return;
			}
		}

		final Computer computer = this;
		ComputerThread.queueTask(new ComputerThread.Task() {
			@Override
			public Computer getOwner() {
				return computer;
			}

			@Override
			public void execute() {
				synchronized (this) {
					synchronized (Computer.this.m_terminal) {
						Computer.this.m_terminal.clear();
						Computer.this.m_terminal.setCursorPos(0, 0);
						Computer.this.m_terminal.setCursorBlink(false);
					}

					Computer.this.initFileSystem();
					Computer.this.initLua();
					synchronized (Computer.this.m_peripherals) {
						for (int i = 0; i < 6; i++) {
							Computer.PeripheralWrapper peripheralwrapper = Computer.this.m_peripherals[i];
							if (peripheralwrapper != null && !peripheralwrapper.isAttached()) {
								peripheralwrapper.attach(Computer.sides[i]);
							}
						}
					}
				}
			}
		});
		this.queueLuaEvent(new Computer.Event() {
			@Override
			public LuaValue[] getArguments() {
				return LuaValue.NOVALS;
			}
		});
	}

	private void stopComputer() {
		synchronized (this) {
			if (this.m_stopped) {
				return;
			}

			this.m_stopped = true;
		}

		final Computer computer = this;
		ComputerThread.queueTask(new ComputerThread.Task() {
			@Override
			public Computer getOwner() {
				return computer;
			}

			@Override
			public void execute() {
				synchronized (this) {
					synchronized (Computer.this.m_peripherals) {
						for (int i = 0; i < 6; i++) {
							Computer.PeripheralWrapper peripheralwrapper = Computer.this.m_peripherals[i];
							if (peripheralwrapper != null && peripheralwrapper.isAttached()) {
								peripheralwrapper.detach();
							}
						}
					}

					synchronized (Computer.this.m_peripheralsAsAPIs) {
						if (!Computer.this.m_peripheralsAsAPIs.isEmpty()) {
							for (Computer.PeripheralWrapper peripheralwrapper1 : Computer.this.m_peripheralsAsAPIs) {
								if (peripheralwrapper1.isAttached()) {
									peripheralwrapper1.detach();
								}
							}
						}
					}

					synchronized (Computer.this.m_terminal) {
						Computer.this.m_terminal.clear();
						Computer.this.m_terminal.setCursorPos(0, 0);
						Computer.this.m_terminal.setCursorBlink(false);
					}

					Computer.this.m_fileSystem = null;
					synchronized (Computer.this.m_timers) {
						Computer.this.m_timers.clear();
					}

					synchronized (Computer.this.m_alarms) {
						Computer.this.m_alarms.clear();
					}

					synchronized (Computer.this.m_httpRequests) {
						for (HTTPRequest httprequest : Computer.this.m_httpRequests) {
							httprequest.cancel();
						}

						Computer.this.m_httpRequests.clear();
					}

					synchronized (Computer.this.m_output) {
						for (int j = 0; j < 6; j++) {
							Computer.this.m_output[j] = false;
							Computer.this.m_bundledOutput[j] = 0;
						}

						Computer.this.m_outputChanged = true;
					}

					synchronized (this) {
						Computer.this.m_on = false;
						Computer.this.m_stopped = false;
						Computer.this.m_globals = null;
						if (Computer.this.m_mainFunction != null) {
							((LuaThread) Computer.this.m_mainFunction).abandon();
							Computer.this.m_mainFunction = null;
						}

						Computer.this.m_eventFilter = null;
					}

					System.gc();
				}
			}
		});
	}

	private void queueLuaEvent(final Computer.Event _event) {
		synchronized (this) {
			if (!this.m_on || this.m_stopped) {
				return;
			}
		}

		final Computer computer = this;
		ComputerThread.queueTask(
				new ComputerThread.Task() {
					@Override
					public Computer getOwner() {
						return computer;
					}

					@Override
					public void execute() {
						synchronized (this) {
							if (!Computer.this.m_on || Computer.this.m_stopped) {
								return;
							}
						}

						try {
							LuaValue[] aluavalue = _event.getArguments();
							if (Computer.this.m_mainFunction != null
									&& (
									Computer.this.m_eventFilter == null
											|| aluavalue.length == 0
											|| aluavalue[0].toString().equals("terminate")
											|| aluavalue[0].toString().equals(Computer.this.m_eventFilter)
							)) {
								LuaThread luathread = Computer.this.m_mainFunction.checkthread();
								Varargs varargs = luathread.resume(LuaValue.varargsOf(aluavalue));
								if (Computer.this.m_aborted) {
									Computer.this.m_aborted = false;
								}

								if (!varargs.arg1().checkboolean()) {
									throw new LuaError(varargs.arg(2).checkstring().toString());
								}

								LuaValue luavalue = varargs.arg(2);
								if (luavalue.isstring()) {
									Computer.this.m_eventFilter = luavalue.toString();
								}
								else {
									Computer.this.m_eventFilter = null;
								}

								if (luathread.getStatus().equals("dead")) {
									Computer.this.m_mainFunction = null;
									Computer.this.m_globals = null;
									Computer.this.m_eventFilter = null;
									Computer.this.turnOff();
								}
							}
						} catch (LuaError luaError) {
							Computer.this.m_mainFunction = null;
							Computer.this.m_globals = null;
							Computer.this.m_eventFilter = null;
							synchronized (Computer.this.m_terminal) {
								Computer.this.m_terminal.write(luaError.getMessage());
								Computer.this.m_terminal.setCursorBlink(false);
								Computer.this.m_terminal.setCursorPos(0, Computer.this.m_terminal.getCursorY() + 1);
								if (Computer.this.m_terminal.getCursorY() >= Computer.this.m_terminal.getHeight()) {
									Computer.this.m_terminal.scroll(1);
									Computer.this.m_terminal.setCursorPos(0, Computer.this.m_terminal.getHeight() - 1);
								}
							}

							luaError.printStackTrace();
						}
					}
				}
		);
	}

	private String findFreeLocation(String s) {
		try {
			synchronized (this.m_fileSystem) {
				if (!this.m_fileSystem.exists(s)) {
					return s;
				}
				else {
					int i = 2;

					while (this.m_fileSystem.exists(s + i)) {
						i++;
					}

					return s + i;
				}
			}
		} catch (FileSystemException var5) {
			return null;
		}
	}

	private File getRealUserPath(String s, int i) {
		File file = new File(this.m_environment.getSaveDir(), s);
		File file1 = new File(file, Integer.toString(i));
		file1.mkdirs();
		return file1;
	}

	private File getRealFixedPath(String s) {
		File file = new File(this.m_environment.getStaticDir(), s);
		file.mkdirs();
		return file;
	}

	private int createUserPath(String s) {
		File file = new File(this.m_environment.getSaveDir(), s);
		int i = 1;

		while (new File(file, Integer.toString(i)).exists()) {
			i++;
		}

		File file1 = new File(file, Integer.toString(i));
		file1.mkdirs();
		return i;
	}

	private int parseSide(String s) {
		for (int i = 0; i < 6; i++) {
			if (s.equals(sides[i])) {
				return i;
			}
		}

		throw new LuaError("Invalid side.");
	}

	private LuaValue wrapBufferedReader(final BufferedReader _reader) {
		LuaTable luatable = new LuaTable();
		luatable.set("readLine", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();

				try {
					String s = _reader.readLine();
					return s != null ? LuaValue.valueOf(s) : LuaValue.NIL;
				} catch (IOException var2) {
					return LuaValue.NIL;
				}
			}
		});
		luatable.set("readAll", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();

				try {
					StringBuilder stringbuilder = new StringBuilder();
					String s = _reader.readLine();

					while (s != null) {
						stringbuilder.append(s);
						s = _reader.readLine();
						if (s != null) {
							stringbuilder.append("\n");
						}
					}

					return LuaValue.valueOf(stringbuilder.toString());
				} catch (IOException var3) {
					return LuaValue.NIL;
				}
			}
		});
		luatable.set("close", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();

				try {
					_reader.close();
					return LuaValue.NIL;
				} catch (IOException var2) {
					return LuaValue.NIL;
				}
			}
		});
		return luatable;
	}

	private LuaValue wrapBufferedWriter(final BufferedWriter _writer) {
		LuaTable luatable = new LuaTable();
		luatable.set("write", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue luavalue) {
				Computer.this.tryAbort();

				try {
					String s = "";
					if (!luavalue.isnil()) {
						s = luavalue.toString();
					}

					_writer.write(s, 0, s.length());
					return LuaValue.NIL;
				} catch (IOException var3) {
					return LuaValue.NIL;
				}
			}
		});
		luatable.set("writeLine", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue luavalue) {
				Computer.this.tryAbort();

				try {
					String s = "";
					if (!luavalue.isnil()) {
						s = luavalue.toString();
					}

					_writer.write(s, 0, s.length());
					_writer.newLine();
					return LuaValue.NIL;
				} catch (IOException var3) {
					return LuaValue.NIL;
				}
			}
		});
		luatable.set("close", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();

				try {
					_writer.close();
					return LuaValue.NIL;
				} catch (IOException var2) {
					return LuaValue.NIL;
				}
			}
		});
		return luatable;
	}

	private LuaValue wrapInputStream(final InputStream _reader) {
		LuaTable luatable = new LuaTable();
		luatable.set("read", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();

				try {
					int i = _reader.read();
					return i != -1 ? LuaValue.valueOf(i) : LuaValue.NIL;
				} catch (IOException var2) {
					return LuaValue.NIL;
				}
			}
		});
		luatable.set("close", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();

				try {
					_reader.close();
					return LuaValue.NIL;
				} catch (IOException var2) {
					return LuaValue.NIL;
				}
			}
		});
		return luatable;
	}

	private LuaValue wrapOutputStream(final OutputStream _writer) {
		LuaTable luatable = new LuaTable();
		luatable.set("write", new OneArgFunction() {
			@Override
			public LuaValue call(LuaValue luavalue) {
				Computer.this.tryAbort();

				try {
					int i = luavalue.checkint();
					_writer.write(i);
					return LuaValue.NIL;
				} catch (IOException var3) {
					return LuaValue.NIL;
				}
			}
		});
		luatable.set("close", new ZeroArgFunction() {
			@Override
			public LuaValue call() {
				Computer.this.tryAbort();

				try {
					_writer.close();
					return LuaValue.NIL;
				} catch (IOException var2) {
					return LuaValue.NIL;
				}
			}
		});
		return luatable;
	}

	private interface Event {
		LuaValue[] getArguments();
	}

	private class Alarm implements Comparable<Alarm> {
		double time;
		LuaValue token;

		Alarm(double d, LuaValue luavalue) {
			this.time = d;
			this.token = luavalue;
		}

		public int compareTo(@Nonnull Computer.Alarm alarm) {
			double d = this.time;
			if (d < Computer.this.m_time) {
				d += 24.0;
			}

			double d1 = alarm.time;
			if (d1 < Computer.this.m_time) {
				d1 += 24.0;
			}

			if (this.time < alarm.time) {
				return -1;
			}
			else {
				return this.time <= alarm.time ? 0 : 1;
			}
		}
	}

	private class PeripheralWrapper implements IComputerAccess {
		private final IPeripheral m_peripheral;
		private final String m_type;
		private final String[] m_methods;
		private final HashMap<String, Integer> m_methodMap;
		private boolean m_attached;
		private final HashSet<String> m_mounts;

		public PeripheralWrapper(IPeripheral iperipheral) {
			this.m_peripheral = iperipheral;
			this.m_attached = false;
			this.m_type = iperipheral.getType();
			this.m_methods = iperipheral.getMethodNames();
			if (this.m_type == null) {
				throw new AssertionError();
			}
			else if (this.m_methods == null) {
				throw new AssertionError();
			}
			else {
				this.m_methodMap = new HashMap<>();

				for (int i = 0; i < this.m_methods.length; i++) {
					if (this.m_methods[i] != null) {
						this.m_methodMap.put(this.m_methods[i], i);
					}
				}

				this.m_mounts = new HashSet<>();
			}
		}

		public IPeripheral getPeripheral() {
			return this.m_peripheral;
		}

		public String getType() {
			return this.m_type;
		}

		public String[] getMethods() {
			return this.m_methods;
		}

		public synchronized boolean isAttached() {
			return this.m_attached;
		}

		public synchronized void attach(String s) {
			if (this.m_attached) {
				throw new AssertionError();
			}
			else {
				this.m_attached = true;
				this.m_peripheral.attach(this, s);
			}
		}

		public synchronized void detach() {
			if (!this.m_attached) {
				throw new AssertionError();
			}
			else {
				this.m_peripheral.detach(this);
				this.m_attached = false;

				for (String mMount : this.m_mounts) {
					Computer.this.m_fileSystem.unmount(mMount);
				}

				this.m_mounts.clear();
			}
		}

		public synchronized void installAsAPI(LuaValue luavalue) {
			LuaTable luatable = new LuaTable();

			for (int i = 0; i < this.m_methods.length; i++) {
				if (this.m_methods[i] != null) {
					int finalI = i;
					luatable.set(this.m_methods[i], new VarArgFunction() {
						@Override
						public Varargs invoke(Varargs varargs) {
							Computer.this.tryAbort();
							Object[] obj = Computer.this.toObjects(varargs, 1);
							Object[] obj1 = null;
							synchronized (this) {
								try {
									obj1 = PeripheralWrapper.this.m_peripheral.callMethod(PeripheralWrapper.this, finalI, obj);
								} catch (Exception var6) {
									throw new LuaError(var6.getMessage());
								}
							}

							return LuaValue.varargsOf(Computer.this.toValues(obj1, 0));
						}
					});
				}
			}

			luavalue.set(this.m_type, luatable);
		}

		public synchronized Object[] call(String s, Object[] obj) throws LuaError {
			if (!this.m_attached) {
				throw new AssertionError();
			}
			else if (this.m_methodMap.containsKey(s)) {
				int i = (Integer) this.m_methodMap.get(s);

				try {
					return this.m_peripheral.callMethod(this, i, obj);
				} catch (Exception var5) {
					throw new LuaError(var5.getMessage());
				}
			}
			else {
				throw new LuaError("No such method " + s);
			}
		}

		@Override
		public synchronized int createNewSaveDir(String s) {
			if (!this.m_attached) {
				throw new RuntimeException("You are not attached to this Computer");
			}
			else {
				return Computer.this.createUserPath(s);
			}
		}

		@Override
		public synchronized String mountSaveDir(String s, String s1, int i, boolean flag) {
			if (!this.m_attached) {
				throw new RuntimeException("You are not attached to this Computer");
			}
			else {
				String s2;
				synchronized (Computer.this.m_fileSystem) {
					s2 = Computer.this.findFreeLocation(s);
					File file = Computer.this.getRealUserPath(s1, i);
					if (s2 != null && file != null) {
						try {
							Computer.this.m_fileSystem.mount(s2, file, flag);
						} catch (FileSystemException ignored) {
						}
					}
				}

				if (s2 != null) {
					this.m_mounts.add(s2);
				}

				return s2;
			}
		}

		@Override
		public synchronized String mountFixedDir(String s, String s1, boolean flag) {
			if (!this.m_attached) {
				throw new RuntimeException("You are not attached to this Computer");
			}
			else {
				String s2 = null;
				synchronized (Computer.this.m_fileSystem) {
					s2 = Computer.this.findFreeLocation(s);
					File file = Computer.this.getRealFixedPath(s1);
					if (s2 != null && file != null) {
						try {
							Computer.this.m_fileSystem.mount(s2, file, flag);
						} catch (FileSystemException ignored) {
						}
					}
				}

				if (s2 != null) {
					this.m_mounts.add(s2);
				}

				return s2;
			}
		}

		@Override
		public synchronized void unmount(String s) {
			if (!this.m_attached) {
				throw new RuntimeException("You are not attached to this Computer");
			}
			else if (!this.m_mounts.contains(s)) {
				throw new RuntimeException("You didn't mount this location");
			}
			else {
				Computer.this.m_fileSystem.unmount(s);
				this.m_mounts.remove(s);
			}
		}

		@Override
		public synchronized int getID() {
			if (!this.m_attached) {
				throw new RuntimeException("You are not attached to this Computer");
			}
			else {
				return Computer.this.m_id;
			}
		}

		@Override
		public synchronized void queueEvent(String s) {
			this.queueEvent(s, null);
		}

		@Override
		public synchronized void queueEvent(final String event, final Object[] arguments) {
			if (!this.m_attached) {
				throw new RuntimeException("You are not attached to this Computer");
			}
			else {
				Computer.this.queueLuaEvent(new Computer.Event() {
					@Override
					public LuaValue[] getArguments() {
						LuaValue[] aluavalue = Computer.this.toValues(arguments, 1);
						aluavalue[0] = LuaValue.valueOf(event);
						return aluavalue;
					}
				});
			}
		}
	}

	private static class Timer {
		double timeLeft;
		LuaValue token;

		Timer(double d, LuaValue luavalue) {
			this.timeLeft = d;
			this.token = luavalue;
		}
	}
}
