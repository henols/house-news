package se.aceone.housenews.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import se.aceone.housenews.connection.Connection;

@Service
public class SensorPublisher {

	private static final String MAIN = "Main";
	private static final String HEATPUMP = "Heatpump";

	private static final byte METER_1 = 0;
	private static final byte METER_2 = 1;
	private static final int[] METERS = { METER_1, METER_2 };

	private static final byte[] READ_METER_1 = { '4', '0' };
	private static final byte[] READ_METER_2 = { '4', '1' };
	private static final byte[][] READ_METER = { READ_METER_1, READ_METER_2 };

	private static final byte[] CONFIRM_METER_1 = { 'c', '0' };
	private static final byte[] CONFIRM_METER_2 = { 'c', '1' };
	private static final byte[][] CONFIRM_METER = { CONFIRM_METER_1, CONFIRM_METER_2 };

	private static final byte[] TEMPERATURE = { 't', 't' };

	private static final boolean CLEAR_COUNT = true;

	private static final long POWER_PING_TIME = 20;
	private static final long ENERGY_PING_TIME = 300;
	private static final long TEMPERATURE_PING_TIME = 60;

	private static final String ENERGY = "kwh%s";
	private static final String POWER = "power%s";

	private final static String POWER_TOPIC = "%s/powermeter/" + POWER;
	private final static String ENERGY_TOPIC = "%s/powermeter/" + ENERGY;
	private final static String TEMPERATURE_TOPIC = "%s/temperature/%s";

	private static final Logger log = LoggerFactory.getLogger(SensorPublisher.class);

	private int oldKWh[] = { Integer.MIN_VALUE, Integer.MIN_VALUE };

	private int powerPublishCount[] = { 0, 0 };

	private Object lock = new Object();
	private final IMqttClient client;
	private final Connection connection;
	private final MqttConnectOptions options;
	private final String location;

	private boolean registerTemp = true;
	private boolean registerPower = true;

	@Autowired
	public SensorPublisher(@Value("${sensor.location}") String location, IMqttClient client, Connection connection,
			MqttConnectOptions options) throws Exception {
		this.location = location;
		this.client = client;
		this.connection = connection;
		this.options = options;

		log.info("Using location name: " + location);

	}

//	@PostConstruct
	public void init() throws Exception {
		log.info("Connecting to serial port: " + connection.getName());
		connection.open();
		log.info("Serial port is connected: " + connection.isOpen());

		log.info("Connecting client: " + client.getClientId() + " to MQTT server: " + client.getServerURI());
		client.connect(options);
		log.info("Connected to MQTT server: " + client.isConnected());
	}

	@Scheduled(initialDelay = 2000, fixedRate = TEMPERATURE_PING_TIME * 1000)
	private void readTemperature() throws Exception {
		log.debug("readTemperature");
		synchronized (lock) {
			if (checkConnections()) {
				publishTemperature();
			}
		}
	}

	@Scheduled(initialDelay = 5000, fixedRate = POWER_PING_TIME * 1000)
	private void readPowerMeter() throws Exception {
		log.debug("readPowerMeter");
		synchronized (lock) {
			if (checkConnections()) {
				if (registerPower) {
					buildDiscovery(Arrays.stream(METERS).mapToObj(String::valueOf).collect(Collectors.toList()), POWER,
							POWER_TOPIC, "power", "W");
					buildDiscovery(Arrays.stream(METERS).mapToObj(String::valueOf).collect(Collectors.toList()), ENERGY,
							ENERGY_TOPIC, "energy", "kWh");
					registerPower = false;
				}
				long timestamp = System.currentTimeMillis();
				publishPower(METER_1, timestamp);
				publishPower(METER_2, timestamp);
			}
		}
	}

	@Scheduled(cron = "0 0 0 * * *")
	private void readDailyConsumtion() throws Exception {
		log.debug("readDailyConsumtion");
		synchronized (lock) {
			if (checkConnections()) {
				long timestamp = System.currentTimeMillis();
				publishDailyConsumtion(METER_1, timestamp);
				publishDailyConsumtion(METER_2, timestamp);
			}
		}
	}

	private boolean publishTemperature() throws IOException, MqttException {
		log.debug("Read and publish temperature");
		String result = readProtocol(TEMPERATURE);
		if (result == null) {
			return false;
		}

		if (registerTemp) {
			buildDiscovery(Arrays.stream(result.split(",")).map(s -> s.substring(0, s.indexOf(':')))
					.collect(Collectors.toList()), "%s", TEMPERATURE_TOPIC, "temperature", "°C");
			registerTemp = false;
		}

		long timestamp = System.currentTimeMillis();

		String[] strings = result.split(",");
		for (String string : strings) {
			int indexOf = string.indexOf(':');
			if (indexOf < 0) {
				continue;
			}
			MqttTopic topic = client.getTopic(String.format(TEMPERATURE_TOPIC, location, string.substring(0, indexOf)));
			String payload = buildJson(string.substring(indexOf + 1), timestamp, string.substring(0, indexOf), null);

			log.debug("Publishing to broker: " + topic + " : " + payload);
			topic.publish(payload.getBytes(), 1, true);
		}
		return true;
	}

	private boolean publishPower(byte meter, long timestamp) throws IOException, MqttException {

		powerPublishCount[meter]++;

		String result = readProtocol(READ_METER[meter]);
		if (result == null) {
			return false;
		}

		String[] r;
		try {
			r = splitPowerResult(result);
		} catch (NoSuchElementException e) {
			log.error("Got some crapp from reader: " + result, e);
			return false;
		}

		String power = r[2];
		if (Integer.parseInt(power) < 0) {
			log.error("We seem to have a negative power:" + power);
		} else {
			String payload = buildJson(power, timestamp, String.format(POWER, meter), meter == 0 ? MAIN : HEATPUMP);
			MqttTopic topic = client.getTopic(String.format(POWER_TOPIC, location, meter));
			log.debug("Publishing to broker: " + topic + " : " + payload);
			topic.publish(payload.getBytes(), 1, true);
		}

		String pulses = r[1];
		int energy;
		try {
			energy = (int) Double.parseDouble(pulses);
			if (oldKWh[meter] < 0) {
				oldKWh[meter] = energy;
			}
		} catch (NumberFormatException e) {
			log.error("Failed to read energy pulses for meter: " + meter, e);
			return false;
		}
		if (powerPublishCount[meter] % (ENERGY_PING_TIME / POWER_PING_TIME) == 0) {
			if (energy < 0) {
				log.error("We seem to have a negative pulses:" + energy);
				return false;
			}

			String energyNow = String.valueOf(energy - oldKWh[meter]);
			String payload = buildJson(energyNow, timestamp, String.format(ENERGY, meter),
					meter == 0 ? MAIN : HEATPUMP);
			MqttTopic topic = client.getTopic(String.format(ENERGY_TOPIC, location, meter));
			log.debug("Publishing to broker: " + topic + " : " + payload);
			topic.publish(payload.getBytes(), 1, true);

			oldKWh[meter] = energy;
		}
		return true;
	}

	private String buildJson(String value, long timestamp, String name, String alias) {

		StringBuffer sb = new StringBuffer();
		sb.append("{\"value\":");
		sb.append(value);
		sb.append(",\"timestamp\":");
		sb.append(timestamp);
		if (name != null) {
			sb.append(",\"name\":\"");
			sb.append(name);
			sb.append("\"");
		}
		if (alias != null) {
			sb.append(",\"alias\":\"");
			sb.append(alias);
			sb.append("\"");
		}
		sb.append("}");
		return sb.toString();
	}

	private boolean publishDailyConsumtion(byte meter, long timestamp) throws Exception {
		log.debug("Read power counter.");
		String result = readProtocol(READ_METER[meter]);
		if (result == null) {
			return false;
		}
		String[] r = splitPowerResult(result.toString());
		@SuppressWarnings("unused")
		String counter = r[0];
		String energy = r[1];
		@SuppressWarnings("unused")
		String power = r[2];
		// oldWh = 0;
		oldKWh[meter] = 0;

		MqttTopic topic = client.getTopic(String.format(ENERGY_TOPIC + "/dailyconsumption", location, meter));

		String payload = buildJson(energy, timestamp, String.format(ENERGY, meter), meter == 0 ? MAIN : HEATPUMP);

		try {
			topic.publish(payload.getBytes(), 1, true);
		} catch (MqttException e) {
			log.error("Failed to publish: " + payload, e);
		}
		if (CLEAR_COUNT) {
			connection.getOutputStream().write(CONFIRM_METER[meter]);
			for (int i = 0; i < energy.length(); i++) {
				byte charAt = (byte) energy.charAt(i);
				connection.getOutputStream().write(charAt);
			}
			connection.getOutputStream().write('\n');
		}
		return true;

	}

	private String[] splitPowerResult(String result) throws NoSuchElementException {
		String[] r = new String[3];
		StringTokenizer st = new StringTokenizer(result, ",");
		r[0] = st.nextToken();
		String tmp = st.nextToken();
		r[1] = tmp.substring(tmp.indexOf(":") + 1);
		tmp = st.nextToken();
		r[2] = tmp.substring(tmp.indexOf(":") + 1).trim();
		return r;
	}

	private String readProtocol(byte[] protocol) throws IOException {
		StringBuilder sb = new StringBuilder();
		log.debug("Read protocol:" + new String(protocol));
		try {
			connection.getOutputStream().write(protocol);
			connection.getOutputStream().flush();
			char c;
			while (true) {
				int sleeps = 0;
				while (connection.getInputStream().available() <= 0) {
					sleeps++;
					if (sleeps > 20) {
						log.error("Slept to long.");
						return null;
					}
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}
				}
				if ((c = (char) connection.getInputStream().read()) == '\n') {
					break;
				}
				sb.append(c);
				if (sb.length() > 135) {
					String message = "To mutch to read... '" + sb + "'";

					log.error(message);
					return null;
				}
			}
		} catch (IOException e) {
			log.warn("Failed to read from serial connection: " + e.getMessage());
			connection.close();
			throw e;
		}

		String result = sb.toString().trim();
		log.debug("Response: " + result);
		return result;
	}

	private void buildDiscovery(List<String> sensors, String nameTemplate, String topicTemplate, String type,
			String unit) throws MqttPersistenceException, MqttException {
		String payload = "[" + sensors.stream().map(i -> i + "")
				.map(s -> "{ \"ids\": \"" + String.format(nameTemplate, s) + "\"" //
						+ ", \"topic\": \"" + String.format(topicTemplate, location, s) + "\"" //
						+ ", \"type\": \"" + type + "\"" //
						+ ", \"unit\": \"" + unit + "\"}")
				.collect(Collectors.joining(",")) + "]";
		log.debug("Discovery: " + payload);

		MqttTopic topic = client.getTopic("discovery/" + type);
		topic.publish(payload.getBytes(), 1, true);
	}

	private boolean checkConnections() throws Exception, MqttSecurityException, MqttException {
		if (!connection.isOpen()) {
			log.info("Reconnecting serial port: " + connection.getName());
			connection.open();
			log.info("Serial port is connected: " + connection.isOpen());
		}
		if (!client.isConnected()) {
			log.info("Reconnecting client: " + client.getClientId() + " to MQTT server: " + client.getServerURI());
			client.connect(options);
			log.info("Connected to MQTT server: " + client.isConnected());
		}
		return connection.isOpen() && client.isConnected();
	}

}
