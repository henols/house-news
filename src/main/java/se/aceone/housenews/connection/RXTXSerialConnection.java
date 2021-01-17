package se.aceone.housenews.connection;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.SerialPort;


public class RXTXSerialConnection implements Connection {
	private static final Logger log = LoggerFactory.getLogger(RXTXSerialConnection .class);

	private String portName;
	private final int baudRate;

	protected InputStream is = null;
	protected OutputStream os = null;
	private CommPort comPort;

	public RXTXSerialConnection(String portName, int baudRate) {
		this.portName = portName;
		this.baudRate = baudRate;
	}

	@Override
	public void open() throws Exception {
		listPorts();
		CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(portName);
		if (portIdentifier.isCurrentlyOwned()) {
			log.error("Error: Port is currently in use");
		} else {
			 comPort = portIdentifier.open(this.getClass().getName(), 2000);

			if (comPort instanceof SerialPort) {
				SerialPort serialPort = (SerialPort) comPort;
				serialPort.setSerialPortParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1,
						SerialPort.PARITY_NONE);

				is = serialPort.getInputStream();
				os = serialPort.getOutputStream();

			} else {
				log.error("Not an serial port");
			}
		}
	}

	static void listPorts() {
		@SuppressWarnings("unchecked")
		Enumeration<CommPortIdentifier> portEnum = CommPortIdentifier.getPortIdentifiers();
		while (portEnum.hasMoreElements()) {
			CommPortIdentifier portIdentifier = portEnum.nextElement();
			log.info(portIdentifier.getName() + " - " + getPortTypeName(portIdentifier.getPortType()));
		}
	}

	static String getPortTypeName(int portType) {
		switch (portType) {
		case CommPortIdentifier.PORT_I2C:
			return "I2C";
		case CommPortIdentifier.PORT_PARALLEL:
			return "Parallel";
		case CommPortIdentifier.PORT_RAW:
			return "Raw";
		case CommPortIdentifier.PORT_RS485:
			return "RS485";
		case CommPortIdentifier.PORT_SERIAL:
			return "Serial";
		default:
			return "unknown type";
		}
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
		if(comPort!=null){
			comPort.close();
			comPort = null;
		}
	}
	
	@Override
	public String getName() {
		return portName;
	}

	@Override
	public boolean isOpen() {
		return comPort!=null;
	}

}
