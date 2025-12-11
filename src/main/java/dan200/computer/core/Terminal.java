package dan200.computer.core;

public class Terminal {
	static final boolean $assertionsDisabled = !Terminal.class.desiredAssertionStatus();
	private int m_cursorX;
	private int m_cursorY;
	private boolean m_cursorBlink;
	private int m_width;
	private int m_height;
	private String m_emptyLine;
	private String[] m_lines;
	private boolean m_changed;
	private boolean[] m_lineChanged;

	public Terminal(int i, int j) {
		this.m_width = i;
		this.m_height = j;
		StringBuilder stringbuilder = new StringBuilder();

		for (int k = 0; k < this.m_width; k++) {
			stringbuilder.append(' ');
		}

		this.m_emptyLine = stringbuilder.toString();
		this.m_lines = new String[this.m_height];

		for (int l = 0; l < this.m_height; l++) {
			this.m_lines[l] = this.m_emptyLine;
		}

		this.m_cursorX = 0;
		this.m_cursorY = 0;
		this.m_cursorBlink = false;
		this.m_changed = false;
		this.m_lineChanged = new boolean[this.m_height];
	}

	public int getWidth() {
		return this.m_width;
	}

	public int getHeight() {
		return this.m_height;
	}

	public void resize(int i, int j) {
		if (i != this.m_width || j != this.m_height) {
			int k = this.m_height;
			String[] as = this.m_lines;
			this.m_width = i;
			this.m_height = j;
			StringBuilder stringbuilder = new StringBuilder();

			for (int l = 0; l < this.m_width; l++) {
				stringbuilder.append(' ');
			}

			this.m_emptyLine = stringbuilder.toString();
			this.m_lines = new String[this.m_height];

			for (int i1 = 0; i1 < this.m_height; i1++) {
				if (i1 < k) {
					String s = as[i1];
					if (s.length() >= this.m_width) {
						this.m_lines[i1] = s.substring(0, this.m_width);
					}
					else {
						this.m_lines[i1] = s + this.m_emptyLine.substring(s.length(), this.m_width);
					}
				}
				else {
					this.m_lines[i1] = this.m_emptyLine;
				}
			}

			this.m_changed = true;
			this.m_lineChanged = new boolean[this.m_height];
		}
	}

	public void setCursorPos(int i, int j) {
		if (this.m_cursorX != i || this.m_cursorY != j) {
			this.m_cursorX = i;
			this.m_cursorY = j;
			this.m_changed = true;
		}
	}

	public int getCursorX() {
		return this.m_cursorX;
	}

	public int getCursorY() {
		return this.m_cursorY;
	}

	public boolean getCursorBlink() {
		return this.m_cursorBlink;
	}

	public void setCursorBlink(boolean flag) {
		if (this.m_cursorBlink != flag) {
			this.m_cursorBlink = flag;
			this.m_changed = true;
		}
	}

	public void write(String s) {
		if (this.m_cursorY >= 0 && this.m_cursorY < this.m_height) {
			s = s.replace('\t', ' ');
			s = s.replace('\r', ' ');
			int i = this.m_cursorX;
			int j = this.m_width - this.m_cursorX;
			if (j > this.m_width + s.length()) {
				return;
			}

			if (j > this.m_width) {
				i = 0;
				s = s.substring(j - this.m_width);
				j = this.m_width;
			}

			if (j > 0) {
				String s1 = this.m_lines[this.m_cursorY];
				StringBuilder stringbuilder = new StringBuilder();
				stringbuilder.append(s1, 0, i);
				if (s.length() < j) {
					stringbuilder.append(s);
					stringbuilder.append(s1.substring(i + s.length()));
				}
				else {
					stringbuilder.append(s, 0, j);
				}

				this.m_lines[this.m_cursorY] = stringbuilder.toString();
				if (!stringbuilder.toString().equals(s1)) {
					this.m_changed = true;
					this.m_lineChanged[this.m_cursorY] = true;
				}
			}
		}
	}

	public void scroll(int i) {
		String[] as = new String[this.m_height];

		for (int j = 0; j < this.m_height; j++) {
			int k = j + i;
			if (k >= 0 && k < this.m_height) {
				as[j] = this.m_lines[k];
			}
			else {
				as[j] = this.m_emptyLine;
			}

			if (!as[j].equals(this.m_lines[j])) {
				this.m_changed = true;
				this.m_lineChanged[j] = true;
			}
		}

		this.m_lines = as;
	}

	public void clear() {
		for (int i = 0; i < this.m_height; i++) {
			if (!this.m_lines[i].equals(this.m_emptyLine)) {
				this.m_lines[i] = this.m_emptyLine;
				this.m_lineChanged[i] = true;
				this.m_changed = true;
			}
		}
	}

	public void clearLine() {
		if (this.m_cursorY >= 0 && this.m_cursorY < this.m_height && !this.m_lines[this.m_cursorY].equals(this.m_emptyLine)) {
			this.m_lines[this.m_cursorY] = this.m_emptyLine;
			this.m_lineChanged[this.m_cursorY] = true;
			this.m_changed = true;
		}
	}

	public String getLine(int i) {
		if ($assertionsDisabled || i >= 0 && i < this.m_height) {
			return this.m_lines[i];
		}
		else {
			throw new AssertionError();
		}
	}

	public boolean getChanged() {
		return this.m_changed;
	}

	public boolean[] getLinesChanged() {
		return this.m_lineChanged;
	}

	public void clearChanged() {
		if (this.m_changed) {
			this.m_changed = false;

			for (int i = 0; i < this.m_height; i++) {
				this.m_lineChanged[i] = false;
			}
		}
	}
}
