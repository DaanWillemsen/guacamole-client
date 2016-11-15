package com.auteve.guacamole.auth.jdbc.yubikey;

import javax.annotation.Nonnull;

import org.apache.guacamole.GuacamoleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yubico.client.v2.VerificationResponse;
import com.yubico.client.v2.YubicoClient;
import com.yubico.client.v2.exceptions.YubicoValidationFailure;
import com.yubico.client.v2.exceptions.YubicoVerificationException;

public class YubikeyValidator {

	private static final Logger logger = LoggerFactory.getLogger(YubikeyValidator.class);

	/**
	 * Validate the yubikey of the user.
	 * 
	 * @param yubikey
	 *            the yubikey string as inputted by the user.
	 * @param storedYubikey
	 *            the stored public yubikey string (first 12 characters).
	 * 
	 * @return the validation result, true if validation attempt is successful.
	 */
	public static boolean validate(@Nonnull String yubikey, String storedYubikey) {
		return validateStaticYubikey(yubikey, storedYubikey) && validateYubikeyWithYubico(yubikey);
	}

	/**
	 * Validate the public yubikey String. This is the first 12 characters
	 * specific to the user.
	 * 
	 * @param yubikey
	 *            the otp. Cannot be {@code null}
	 * @param storedYubikey
	 *            the public yubikey String as stored in the db.
	 * 
	 * @return the validation result, true if the public yubikey part matches
	 *         with the one stored in the db.
	 */
	private static boolean validateStaticYubikey(@Nonnull String yubikey, String storedYubikey) {
		if (yubikey.length() >= 12)
			if (YubicoClient.getPublicId(yubikey).equals(storedYubikey))
				return true;

		logger.warn("Authentication fails because the static yubikey does not match with the stored one.");
		return false;
	}

	/**
	 * Validate the yubikey String (otp) against yubico.
	 * 
	 * @param yubikey the otp. Cannot be {@code null}
	 * 
	 * @return the validation result, true if the validation of the otp against yubico is successful.
	 */
	private static boolean validateYubikeyWithYubico(@Nonnull String yubikey) {
		
		YubicoClient client = YubicoClient.getClient(getYubicoClientId() , getYubicoApiKey());
		VerificationResponse response;
		try {
			response = client.verify(yubikey);
			if(response.isOk()) {
				logger.info("OTP is validated against yubico");
				return true;
			}
		} catch (YubicoVerificationException e) {
			logger.warn("Verification failed at Yubico server", e.getMessage());
		} catch (YubicoValidationFailure e) {
			logger.warn("Validation of yubikey failed at Yubico server: " + e.getMessage());
		}
		
		logger.warn("Failed to validate otp (yubikey) against yubico");
		return false;
	}

	private static int getYubicoClientId() {
		try {
			YubicoEnvironment yubicoEnvironment = new YubicoEnvironment();
			return yubicoEnvironment.getYubicoClientId();
		} catch (GuacamoleException guae) {
			logger.warn("Unable to retrieve yubico environment", guae);
			// return the default client id
			return YubicoEnvironment.DEFAULT_CLIENT_ID;
		}
	}
	
	private static String getYubicoApiKey() {
		try {
			YubicoEnvironment yubicoEnvironment = new YubicoEnvironment();
			return yubicoEnvironment.getYubicoApiKey();
		} catch (GuacamoleException guae) {
			logger.warn("Unable to retrieve yubico environment", guae);
			// return the default client id
			return YubicoEnvironment.DEFAULT_API_KEY;
		}
	}

}
