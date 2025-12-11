package dan200.computer.core;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;


public class FileSystem {
	private final FileSystem.Mount m_rootMount;
	private final Map<String, FileSystem.Mount> m_mounts;

	public FileSystem(File file, boolean flag) throws FileSystemException {
		if (file.exists() && file.isDirectory()) {
			this.m_rootMount = new FileSystem.Mount("", file, flag);
			this.m_mounts = new HashMap<>();
		}
		else {
			throw new FileSystemException("No such directory");
		}
	}

	public synchronized void mount(String s, File file, boolean flag) throws FileSystemException {
		if (file.exists()) {
			s = this.sanitizePath(s);
			if (s.contains("..")) {
				throw new FileSystemException("Cannot mount below the root");
			}
			else {
				this.m_mounts.remove(s);

				this.m_mounts.put(s, new FileSystem.Mount(s, file, flag));
			}
		}
		else {
			throw new FileSystemException("No such path");
		}
	}

	public synchronized void unmount(String s) {
		s = this.sanitizePath(s);
		this.m_mounts.remove(s);
	}

	public synchronized String combine(String s, String s1) {
		s = this.sanitizePath(s);
		s1 = this.sanitizePath(s1);
		if (s.isEmpty()) {
			return s1;
		}
		else {
			return s1.isEmpty() ? s : this.sanitizePath(s + '/' + s1);
		}
	}

	public synchronized boolean contains(String s, String s1) {
		return this._contains(this.sanitizePath(s), this.sanitizePath(s1));
	}

	public synchronized String getName(String s) {
		s = this.sanitizePath(s);
		if (s.isEmpty()) {
			return "root";
		}
		else {
			int i = s.lastIndexOf(47);
			return i >= 0 ? s.substring(i + 1) : s;
		}
	}

	public synchronized long getSize(String s) throws FileSystemException {
		File file = this.getRealPath(this.sanitizePath(s));
		if (file.exists() && !file.isDirectory()) {
			return file.length();
		}
		else {
			throw new FileSystemException("No such file");
		}
	}

	public synchronized String[] list(String s) throws FileSystemException {
		s = this.sanitizePath(s);
		File file = this.getRealPath(s);
		if (file.exists() && file.isDirectory()) {
			String[] as = file.list();
			assert as != null;
			ArrayList<String> arraylist = new ArrayList<>(as.length);

			for (String a : as) {
				File file1 = new File(file, a);
				if (file1.exists()) {
					arraylist.add(a);
				}
			}

			for (FileSystem.Mount mount1 : this.m_mounts.values()) {
				if (mount1.parentLocation.equals(s)) {
					arraylist.add(mount1.name);
				}
			}

			return arraylist.toArray(new String[0]);
		}
		else {
			throw new FileSystemException("Not a directory");
		}
	}

	public synchronized boolean exists(String s) throws FileSystemException {
		File file = this.getRealPath(this.sanitizePath(s));
		return file.exists();
	}

	public synchronized boolean isDir(String s) throws FileSystemException {
		File file = this.getRealPath(this.sanitizePath(s));
		return file.exists() && file.isDirectory();
	}

	public synchronized boolean isReadOnly(String s) throws FileSystemException {
		FileSystem.Mount mount1 = this.getMount(this.sanitizePath(s));
		if (mount1 == null) {
			throw new FileSystemException("Invalid path");
		}
		else {
			return s.equals(mount1.location) || mount1.readOnly;
		}
	}

	public synchronized void makeDir(String s) throws FileSystemException {
		if (this.isReadOnly(s)) {
			throw new FileSystemException("Access denied");
		}
		else {
			File file = this.getRealPath(this.sanitizePath(s));
			if (file.exists()) {
				if (!file.isDirectory()) {
					throw new FileSystemException("File exists");
				}
			}
			else {
				boolean flag = file.mkdirs();
				if (!flag) {
					throw new FileSystemException("Access denied");
				}
			}
		}
	}

	private synchronized void recurseDelete(File file) throws FileSystemException {
		if (file.isDirectory()) {
			String[] as = file.list();

			assert as != null;
			for (String a : as) {
				this.recurseDelete(new File(file, a));
			}
		}

		boolean flag = file.delete();
		if (!flag) {
			throw new FileSystemException("Access denied");
		}
	}

	public synchronized void delete(String s) throws FileSystemException {
		s = this.sanitizePath(s);
		if (this.isReadOnly(s)) {
			throw new FileSystemException("Access denied");
		}
		else {
			File file = this.getRealPath(s);
			if (file.exists()) {
				this.recurseDelete(file);
			}
		}
	}

	public synchronized void move(String s, String s1) throws FileSystemException {
		s = this.sanitizePath(s);
		s1 = this.sanitizePath(s1);
		if (!this.isReadOnly(s) && !this.isReadOnly(s1)) {
			File file = this.getRealPath(s);
			File file1 = this.getRealPath(s1);
			if (!file.exists()) {
				throw new FileSystemException("No such file");
			}
			else if (file1.exists()) {
				throw new FileSystemException("File exists");
			}
			else if (this._contains(s, s1)) {
				throw new FileSystemException("Can't move a directory inside itself");
			}
			else {
				boolean flag = file.renameTo(file1);
				if (!flag) {
					throw new FileSystemException("Access denied");
				}
			}
		}
		else {
			throw new FileSystemException("Access denied");
		}
	}

	private synchronized void recurseCopy(File file, File file1) throws FileSystemException {
		if (!file.exists()) {
			throw new AssertionError();
		}
		else {
			if (file.isDirectory()) {
				boolean flag = file1.mkdirs();
				if (!flag) {
					throw new FileSystemException("Access denied");
				}

				String[] as = file.list();

				assert as != null;
				for (String a : as) {
					this.recurseCopy(new File(file, a), new File(file1, a));
				}
			}
			else {
				try (FileInputStream fis = new FileInputStream(file); FileOutputStream fos = new FileOutputStream(file1)) {
					FileChannel fisChannel = fis.getChannel();
					FileChannel fosChannel = fos.getChannel();
					fosChannel.transferFrom(fisChannel, 0L, fisChannel.size());
				}
				catch (IOException var13) {
					throw new FileSystemException("Access denied");
				}
			}
		}
	}

	public synchronized void copy(String s, String s1) throws FileSystemException {
		s = this.sanitizePath(s);
		s1 = this.sanitizePath(s1);
		if (this.isReadOnly(s1)) {
			throw new FileSystemException("Access denied");
		}
		else {
			File file = this.getRealPath(s);
			File file1 = this.getRealPath(s1);
			if (!file.exists()) {
				throw new FileSystemException("No such file");
			}
			else if (file1.exists()) {
				throw new FileSystemException("File exists");
			}
			else if (this._contains(s, s1)) {
				throw new FileSystemException("Can't copy a directory inside itself");
			}
			else {
				this.recurseCopy(file, file1);
			}
		}
	}

	public synchronized BufferedReader openForRead(String s) throws FileSystemException {
		File file = this.getRealPath(this.sanitizePath(s));
		if (!file.exists()) {
			throw new FileSystemException("File not found");
		}
		else if (file.isDirectory()) {
			throw new FileSystemException("Cannot read from directory");
		}
		else {
			try {
				return new BufferedReader(new FileReader(file));
			} catch (IOException var4) {
				throw new FileSystemException("Access denied");
			}
		}
	}

	public synchronized BufferedWriter openForWrite(String s, boolean flag) throws FileSystemException {
		s = this.sanitizePath(s);
		if (this.isReadOnly(s)) {
			throw new FileSystemException("Access denied");
		}
		else {
			File file = this.getRealPath(s);
			if (file.exists() && file.isDirectory()) {
				throw new FileSystemException("Cannot write to directory");
			}
			else {
				try {
					return new BufferedWriter(new FileWriter(file.toString(), flag));
				} catch (IOException var5) {
					throw new FileSystemException("Access denied");
				}
			}
		}
	}

	public synchronized BufferedInputStream openForBinaryRead(String s) throws FileSystemException {
		File file = this.getRealPath(this.sanitizePath(s));
		if (!file.exists()) {
			throw new FileSystemException("File not found");
		}
		else if (file.isDirectory()) {
			throw new FileSystemException("Cannot read from directory");
		}
		else {
			try {
				return new BufferedInputStream(Files.newInputStream(file.toPath()));
			} catch (IOException var4) {
				throw new FileSystemException("Access denied");
			}
		}
	}

	public synchronized BufferedOutputStream openForBinaryWrite(String s, boolean flag) throws FileSystemException {
		s = this.sanitizePath(s);
		if (this.isReadOnly(s)) {
			throw new FileSystemException("Access denied");
		}
		else {
			File file = this.getRealPath(s);
			if (file.exists() && file.isDirectory()) {
				throw new FileSystemException("Cannot write to directory");
			}
			else {
				try {
					return new BufferedOutputStream(new FileOutputStream(file.toString(), flag));
				} catch (IOException var5) {
					throw new FileSystemException("Access denied");
				}
			}
		}
	}

	private String sanitizePath(String s) {
		s = s.replace('\\', '/');
		String[] as = s.split("/");
		Stack<String> stack = new Stack<>();

		for (String s1 : as) {
			if (!s1.isEmpty() && !s1.equals(".")) {
				if (s1.equals("..")) {
					if (!stack.empty()) {
						String s2 = (String) stack.peek();
						if (!s2.equals("..")) {
							stack.pop();
						}
						else {
							stack.push("..");
						}
					}
					else {
						stack.push("..");
					}
				}
				else {
					stack.push(s1);
				}
			}
		}

		StringBuilder stringbuilder = new StringBuilder();
		Iterator<String> iterator = stack.iterator();

		while (iterator.hasNext()) {
			String s3 = (String) iterator.next();
			stringbuilder.append(s3);
			if (iterator.hasNext()) {
				stringbuilder.append('/');
			}
		}

		return stringbuilder.toString();
	}

	private FileSystem.Mount getMount(String s) {
		Iterator<FileSystem.Mount> iterator = this.m_mounts.values().iterator();
		FileSystem.Mount mount1 = null;
		int i = 99;

		while (iterator.hasNext()) {
			FileSystem.Mount mount2 = iterator.next();
			if (this._contains(mount2.location, s)) {
				int j = mount2.toLocal(s).length();
				if (mount1 == null || j < i) {
					mount1 = mount2;
					i = j;
				}
			}
		}

		if (mount1 != null) {
			return mount1;
		}
		else {
			return this._contains(this.m_rootMount.location, s) ? this.m_rootMount : null;
		}
	}

	private File getRealPath(String s) throws FileSystemException {
		FileSystem.Mount mount1 = this.getMount(s);
		if (mount1 == null) {
			throw new FileSystemException("Invalid path.");
		}
		else {
			return new File(mount1.realPath, mount1.toLocal(s));
		}
	}

	private boolean _contains(String s, String s1) {
		if (s1.contains("..")) {
			return false;
		}
		else if (s1.equals(s)) {
			return true;
		}
		else {
			return s.isEmpty() || s1.startsWith(s + "/");
		}
	}

	private class Mount {
		String name;
		String location;
		String parentLocation;
		File realPath;
		boolean readOnly;

		Mount(String s, File file, boolean flag) {
			this.location = s;
			this.realPath = file;
			this.readOnly = flag;
			int i = this.location.lastIndexOf(47);
			if (i >= 0) {
				this.name = this.location.substring(i + 1);
				this.parentLocation = this.location.substring(0, i);
			}
			else {
				this.name = this.location;
				this.parentLocation = "";
			}
		}

		private String toLocal(String s) {
			if (!FileSystem.this._contains(this.location, s)) {
				throw new AssertionError();
			}
			else {
				String s1 = s.substring(this.location.length());
				return s1.startsWith("/") ? s1.substring(1) : s1;
			}
		}
	}
}
