package se.aceone.housenews.config;

import java.util.Random;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

@Singleton
public class MqttConfig {

	@Produces
	public IMqttClient mqttClient(@ConfigProperty(name = "mqtt-broker.url") String url,
			@ConfigProperty(name = "mqtt-broker.id") String id,
			@ConfigProperty(name = "mqtt-broker.persist-dir") String persistDir) throws MqttException {
		MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(persistDir);
		return new MqttClient(url, id + "-" + generatingRandomAlphanumericString(), dataStore);
	}

	@Produces
	public MqttConnectOptions mqttConnectOptions() {
		MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(true);
		options.setCleanSession(true);
		options.setConnectionTimeout(10);
		options.setKeepAliveInterval(60);
		return options;
	}

	public String generatingRandomAlphanumericString() {
		int leftLimit = 48; // numeral '0'
		int rightLimit = 122; // letter 'z'
		int targetStringLength = 10;
		Random random = new Random();

		return random.ints(leftLimit, rightLimit + 1).filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
				.limit(targetStringLength)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
	}
}
