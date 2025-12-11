package dan200.computer.shared;

import net.minecraft.server.Packet;
import net.minecraft.server.Packet250CustomPayload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;


public class ComputerCraftPacket {
	public static final int OpenComputerGUI = 1;
	public static final int KeyTypedByClient = 2;
	public static final int TerminalChanged = 3;
	public static final int OutputChanged = 4;
	public static final int RequestUpdate = 5;
	public static final int TerminatePressedByClient = 6;
	public static final int PlayRecord = 7;
	public static final int SetDiskLight = 8;
	public static final int RebootPressedByClient = 9;
	public static final int DiskLabelChanged = 10;
	public static final int DiskLabelRequest = 11;
	public static final int ShutdownPressedByClient = 12;
	public static final int SetModemLight = 13;
	public static final int ComputerLabelChanged = 14;
	public static final int ComputerLabelRequest = 15;
	public static final int TerminalDeleted = 16;
	public static final int MonitorChanged = 17;
	public static final int StringTypedByClient = 18;
	public static final int TurtleAnimation = 50;
	public int packetType = 0;
	public String[] dataString = null;
	public int[] dataInt = null;

	public static ComputerCraftPacket parse(byte[] byte0) throws IOException {
		DataInputStream datainputstream = new DataInputStream(new ByteArrayInputStream(byte0));
		ComputerCraftPacket computercraftpacket = new ComputerCraftPacket();
		computercraftpacket.readData(datainputstream);
		return computercraftpacket;
	}

	private void writeData(DataOutputStream dataoutputstream) throws IOException {
		dataoutputstream.writeInt(this.packetType);
		if (this.dataString != null) {
			dataoutputstream.writeInt(this.dataString.length);
		}
		else {
			dataoutputstream.writeInt(0);
		}

		if (this.dataInt != null) {
			dataoutputstream.writeInt(this.dataInt.length);
		}
		else {
			dataoutputstream.writeInt(0);
		}

		if (this.dataString != null) {
			for (String s : this.dataString) {
				dataoutputstream.writeUTF(s);
			}
		}

		if (this.dataInt != null) {
			for (int i1 : this.dataInt) {
				dataoutputstream.writeInt(i1);
			}
		}
	}

	private void readData(DataInputStream datainputstream) throws IOException {
		this.packetType = datainputstream.readInt();
		int i = datainputstream.readInt();
		int j = datainputstream.readInt();
		if (i <= 32 && j <= 32 && i >= 0 && j >= 0) {
			if (i == 0) {
				this.dataString = null;
			}
			else {
				this.dataString = new String[i];

				for (int k = 0; k < i; k++) {
					this.dataString[k] = datainputstream.readUTF();
				}
			}

			if (j == 0) {
				this.dataInt = null;
			}
			else {
				this.dataInt = new int[j];

				for (int l = 0; l < j; l++) {
					this.dataInt[l] = datainputstream.readInt();
				}
			}
		}
		else {
			throw new IOException("");
		}
	}

	public Packet toPacket() {
		ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
		DataOutputStream dataoutputstream = new DataOutputStream(bytearrayoutputstream);
		Packet250CustomPayload packet250custompayload = new Packet250CustomPayload();

		try {
			this.writeData(dataoutputstream);
		} catch (IOException var5) {
			var5.printStackTrace();
		}

		packet250custompayload.tag = "mod_ComputerCraf";
		packet250custompayload.data = bytearrayoutputstream.toByteArray();
		packet250custompayload.length = packet250custompayload.data.length;
		return packet250custompayload;
	}
}
