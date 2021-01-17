package se.aceone.housenews.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialFactory;

public class Pi4JSerialConnection implements Connection {

	private final String comPortName;
	private final int baudRate;

	private OutputStream os;
	private InputStream is;
	private Serial serial;

	public Pi4JSerialConnection(String comPortName, int baudRate) {
		this.comPortName = comPortName;
		this.baudRate = baudRate;
	}

	@Override
	public void open() throws Exception {
		serial = SerialFactory.createInstance();
		serial.open(comPortName, baudRate);
		os = serial.getOutputStream();
		is = serial.getInputStream();
	}

	@Override
	public InputStream getInputStream() {
		return is;
	}

	@Override
	public OutputStream getOutputStream() {
		return os;
	}

	@Override
	public void close() {
		if (serial != null) {
			try {
				serial.close();
			} catch (IOException e) {
			}
			serial = null;
		}
	}

	@Override
	public String getName() {
		return comPortName;
	}

	@Override
	public boolean isOpen() {
		return serial.isOpen();
	}

}
