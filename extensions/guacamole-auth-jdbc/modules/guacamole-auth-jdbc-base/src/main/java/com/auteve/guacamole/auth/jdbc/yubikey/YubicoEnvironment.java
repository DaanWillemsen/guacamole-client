package com.auteve.guacamole.auth.jdbc.yubikey;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.LocalEnvironment;

public class YubicoEnvironment extends LocalEnvironment {
	
	public YubicoEnvironment() throws GuacamoleException {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
     * The default yubico client id.
     */
    public static final int DEFAULT_CLIENT_ID = 0;
    
    /**
     * The default yubico api key.
     */
	public static final String DEFAULT_API_KEY = "";
	
	/**
	 * Returns the yubico client id as stated in the guacamole.properties.
	 * 
	 * @return
	 * @throws GuacamoleException
	 */
	public int getYubicoClientId() throws GuacamoleException {
		return getProperty(YubicoGuacamoleProperties.YUBICO_CLIENT_ID, DEFAULT_CLIENT_ID);
	}
	
	/**
	 * Returns the yubico client id as stated in the guacamole.properties.
	 * 
	 * @return
	 * @throws GuacamoleException
	 */
	public String getYubicoApiKey() throws GuacamoleException {
		return getProperty(YubicoGuacamoleProperties.YUBICO_API_KEY, DEFAULT_API_KEY);
	}
}
