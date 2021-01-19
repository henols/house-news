package se.aceone.housenews;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import se.aceone.housenews.config.MqttTestClient;

@SpringBootTest(classes = { MqttTestClient.class }, properties = { "mqtt-broker.url=tcp://localhost:1883"})
class PowerCoutnerApplicationTests {

	@Test
	void contextLoads() {
	}

}
