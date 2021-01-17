package se.aceone.housenews.connection;

import java.io.InputStream;
import java.io.OutputStream;

import com.fazecast.jSerialComm.SerialPort;

public class JSerialCommConnection implements Connection {

	private final String comPortName;
	private final int baudRate;

	private SerialPort serialPort;

	public JSerialCommConnection(String comPortName, int baudRate) {
		this.comPortName = comPortName;
		this.baudRate = baudRate;
	}

	@Override
	public void open() throws Exception {
		serialPort = SerialPort.getCommPort(comPortName);
		serialPort.setBaudRate(baudRate);
		serialPort.openPort();
		serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
	}

	@Override
	public InputStream getInputStream() {
		return serialPort.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() {
		return serialPort.getOutputStream();
	}

	@Override
	public void close() {
		if (serialPort != null && serialPort.isOpen()) {
			serialPort.closePort();
			serialPort = null;
		}
	}

	@Override
	public String getName() {
		return comPortName;
	}

	@Override
	public boolean isOpen() {
		return serialPort != null && serialPort.isOpen();
	}

}
