package se.aceone.housenews.config;

import java.util.Random;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttConfig {

	@Bean
	public IMqttClient mqttClient(@Value("${mqtt-broker.url}") String url, @Value("${mqtt-broker.id}") String id,
			@Value("${mqtt-broker.persist-dir}") String persistDir) throws MqttException {
		MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(persistDir);
		return new MqttClient(url, id + "-" + generatingRandomAlphanumericString(), dataStore);
	}

	@Bean
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
