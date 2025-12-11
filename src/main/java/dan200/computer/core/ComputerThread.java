package dan200.computer.core;

import java.util.concurrent.LinkedBlockingQueue;


public class ComputerThread {
	private static final Object m_lock = new Object();
	private static Thread m_thread = null;
	private static final LinkedBlockingQueue<Task> m_taskQueue = new LinkedBlockingQueue<>(256); // TODO: Set a configuration value for max task queue size
	private static boolean m_busy = false;
	private static boolean m_running = false;
	private static boolean m_stopped = false;

	public static void start() {
		synchronized (m_lock) {
			if (m_running) {
				m_stopped = false;
			}
			else {
				m_thread = new Thread(() -> {
					while (true) {
						synchronized (ComputerThread.m_lock) {
							if (ComputerThread.m_stopped) {
								ComputerThread.m_running = false;
								ComputerThread.m_thread = null;
								return;
							}
						}

						try {
							final Task task = ComputerThread.m_taskQueue.take();
							ComputerThread.m_busy = true;
							Thread thread = new Thread(() -> {
								try {
									task.execute();
								} catch (Exception var2) {
									System.out.println("ComputerCraft: Error running task.");
									var2.printStackTrace();
								}
							});
							thread.start();
							thread.join(5000L);
							if (thread.isAlive()) {
								Computer computer = task.getOwner();
								if (computer != null) {
									computer.abort();
									thread.join(1250L);
									if (thread.isAlive()) {
										computer.turnOff();
										thread.join(1250L);
									}
								}

								if (thread.isAlive()) {
									System.out.println("ComputerCraft: Warning! Failed to abort a Computer. Dangling lua thread could cause errors.");
									thread.stop();
								}
							}
						} catch (InterruptedException ignored) {
						} finally {
							ComputerThread.m_busy = false;
						}
					}
				}, "CCThread"); // Added thread name
				m_thread.start();
				m_running = true;
			}
		}
	}

	public static void stop() {
		synchronized (m_lock) {
			if (m_running) {
				m_stopped = true;
				m_thread.interrupt();
			}
		}
	}

	public static void queueTask(ComputerThread.Task task) {
		boolean flag = m_taskQueue.offer(task);
		if (!flag) {
			System.out.println("ComputerCraft: Warning! Computer task queue overflowed");
		}
	}

	public static boolean isBusy() {
		return m_taskQueue.peek() != null || m_busy;
	}

	public interface Task {
		Computer getOwner();

		void execute();
	}
}
