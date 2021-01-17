package se.aceone.housenews.connection;

import java.io.InputStream;
import java.io.OutputStream;

public interface Connection {

	InputStream getInputStream();

	OutputStream getOutputStream();

	void close();

	void open() throws Exception;

	String getName();

	boolean isOpen();

}