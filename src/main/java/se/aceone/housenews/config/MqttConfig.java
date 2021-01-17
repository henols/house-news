package se.aceone.housenews.config;

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
	public IMqttClient mqttClient(@Value("${mqtt-broker.url}") String url, @Value("${mqtt-broker.id}") String id)
			throws MqttException {
//		java.io.tmpdir
		MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence();

		return new MqttClient(url, id, dataStore);
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

}
