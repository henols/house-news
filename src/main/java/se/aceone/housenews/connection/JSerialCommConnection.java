package se.aceone.housenews.connection;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;

import se.aceone.housenews.service.SensorPublisher;

public class JSerialCommConnection implements Connection {
	private static final Logger log = LoggerFactory.getLogger(JSerialCommConnection.class);

	private final String comPortName;
	private final int baudRate;

	private SerialPort serialPort;

	public JSerialCommConnection(String comPortName, int baudRate) {
		this.comPortName = comPortName;
		this.baudRate = baudRate;
		Arrays.stream(SerialPort.getCommPorts()).forEach(port -> log.debug("Found port: {}", port.getSystemPortName()));
	}

	@Override
	public void open() throws Exception {
		serialPort = SerialPort.getCommPort(comPortName);
		log.debug("Serial port: " + serialPort);
		
		boolean baudRateOk = serialPort.setBaudRate(baudRate);
		log.debug("Serial baud rate: " +baudRate + " set: "+ baudRateOk);
		boolean timeouts = serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
		log.debug("Serial timeout read semi blocking: " + timeouts);
		boolean openPort = serialPort.openPort();
		log.debug("Serial port open: " + openPort);
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
