package com.auteve.guacamole.auth.jdbc.yubikey;

import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;

public class YubicoGuacamoleProperties {

    /**
     * Property that holds the yubico client id.
     */
    public static final IntegerGuacamoleProperty YUBICO_CLIENT_ID = new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "yubico-client-id"; }

    };
    
    /**
     * Property that holds the yubico api key.
     */
    public static final StringGuacamoleProperty YUBICO_API_KEY = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "yubico-api-key"; }

    };
}
