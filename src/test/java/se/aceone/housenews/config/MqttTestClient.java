package se.aceone.housenews.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MqttTestClient {
	@Bean
	@Primary
	public MqttConnectOptions mqttTestConnectOptions() {
		MqttConnectOptions options = new MqttConnectOptions();
		options.setAutomaticReconnect(false);
		options.setCleanSession(true);
		options.setConnectionTimeout(10);
		options.setKeepAliveInterval(60);
		return options;
	}
	
	
}

