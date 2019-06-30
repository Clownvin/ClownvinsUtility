package com.clownvin.security;

import java.security.SecureRandom;
import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class Passwords {
	private static final Random RANDOM = new SecureRandom();
	private static final int ITERATIONS = 11237;
	private static final int KEY_LENGTH = 256;
	
	private Passwords() {
		
	}
	
	public static byte[] getNextSalt() {
		byte[] salt = new byte[16];
		RANDOM.nextBytes(salt);
		return salt;
	}
	
	public static byte[] hash(char[] password, byte[] salt) {
		byte[] pass = new byte[password.length];
		for (int i = 0; i < password.length; i++) {
			pass[i] = (byte) password[i];
		}
		PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
		try {
			SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			byte[] hash = skf.generateSecret(spec).getEncoded();
			return hash;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			spec.clearPassword();
		}
	}
	
	public static boolean matches(char[] password, byte[] salt, byte[] expected) {
		byte[] pass = new byte[password.length];
		for (int i = 0; i < password.length; i++) {
			pass[i] = (byte) password[i];
		}
		byte[] hash = hash(password, salt);
		if (hash.length != expected.length)
			return false;
		for (int i = 0; i < hash.length; i++)
			if (hash[i] != expected[i])
				return false;
		return true;
	}
}
