package se.aceone.housenews.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import se.aceone.housenews.connection.Connection;
import se.aceone.housenews.connection.JSerialCommConnection;
import se.aceone.housenews.connection.Pi4JSerialConnection;
import se.aceone.housenews.connection.RXTXSerialConnection;

@Configuration
public class SerialConnectionConfig {

	@Bean
	public Connection jSerialCommConnection(@Value("${serial.port}") String serialPort,
			@Value("${serial.baud-rate}") int baudRate) {
		return new JSerialCommConnection(serialPort, baudRate);
	}

//	@Bean
	public Connection pi4JSerialConnection(@Value("${serial.port}") String serialPort,
			@Value("${serial.baud-rate}") int baudRate) {
		return new Pi4JSerialConnection(serialPort, baudRate);
	}

//	@Bean
	public Connection rxtxSerialConnection(@Value("${serial.port}") String serialPort,
			@Value("${serial.baud-rate}") int baudRate) {
		return new RXTXSerialConnection(serialPort, baudRate);
	}

}
