package se.aceone.housenews.config;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import se.aceone.housenews.connection.Connection;
import se.aceone.housenews.connection.JSerialCommConnection;
import se.aceone.housenews.connection.Pi4JSerialConnection;
import se.aceone.housenews.connection.RXTXSerialConnection;

@ApplicationScoped
public class SerialConnectionConfig {
	@Produces
	public Connection jSerialCommConnection(@ConfigProperty(name = "serial.port") String serialPort,
			@ConfigProperty(name = "serial.baud-rate") int baudRate) {
		return new JSerialCommConnection(serialPort, baudRate);
	}

//	@Produces
	public Connection pi4JSerialConnection(@ConfigProperty(name = "serial.port") String serialPort,
			@ConfigProperty(name = "serial.baud-rate") int baudRate) {
		return new Pi4JSerialConnection(serialPort, baudRate);
	}

//	@Produces
	public Connection rxtxSerialConnection(@ConfigProperty(name = "serial.port") String serialPort,
			@ConfigProperty(name = "serial.baud-rate") int baudRate) {
		return new RXTXSerialConnection(serialPort, baudRate);
	}

}
