/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.crypto;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.handler.codec.CorruptedFrameException;
import it.unimi.dsi.fastutil.Pair;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

public enum EncryptionUtils {
  ;

  public static final Pair<String, String> PEM_RSA_PUBLIC_KEY_DESCRIPTOR =
          Pair.of("-----BEGIN RSA PUBLIC KEY-----", "-----END RSA PUBLIC KEY-----");
  public static final Pair<String, String> PEM_RSA_PRIVATE_KEY_DESCRIPTOR =
          Pair.of("-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----");

  public static final String SHA1_WITH_RSA = "SHA1withRSA";
  public static final String SHA256_WITH_RSA = "SHA256withRSA";

  public static final QuietDecoderException INVALID_SIGNATURE
          = new QuietDecoderException("Incorrectly signed chat message");
  public static final QuietDecoderException PREVIEW_SIGNATURE_MISSING
          = new QuietDecoderException("Unsigned chat message requested signed preview");

  private static final QuietDecoderException INVALID_ASN1_PAYLOAD
          = new QuietDecoderException("Invalid key payload");
  public static final byte[] EMPTY = new byte[0];
  private static PublicKey YGGDRASIL_SESSION_KEY;
  private static KeyFactory RSA_KEY_FACTORY;

  private static final Base64.Encoder MIME_SPECIAL_ENCODER
          = Base64.getMimeEncoder(76, "\n".getBytes(StandardCharsets.UTF_8));

  static {
    try {
      RSA_KEY_FACTORY = KeyFactory.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    try {
      byte[] bytes = ByteStreams.toByteArray(
              EncryptionUtils.class.getClassLoader().getResourceAsStream("yggdrasil_session_pubkey.der"));
      YGGDRASIL_SESSION_KEY =  parseRsaPublicKey(bytes, 550);
    } catch (IOException | NullPointerException err) {
      throw new RuntimeException(err);
    }
  }

  public static PublicKey getYggdrasilSessionKey() {
    return YGGDRASIL_SESSION_KEY;
  }

  /**
   * Verifies a key signature.
   *
   * @param algorithm the signature algorithm
   * @param base the public key to verify with
   * @param signature the signature to verify against
   * @param toVerify the byte array(s) of data to verify
   * @return validity of the signature
   */
  public static boolean verifySignature(String algorithm, PublicKey base, byte[] signature, byte[]... toVerify) {
    Preconditions.checkArgument(toVerify.length > 0);
    try {
      Signature construct = Signature.getInstance(algorithm);
      construct.initVerify(base);
      for (byte[] bytes : toVerify) {
        construct.update(bytes);
      }
      return construct.verify(signature);
    } catch (GeneralSecurityException e) {
      throw new IllegalArgumentException("Invalid signature parameters");
    }
  }

  /**
   * Generates a signature for input data.
   *
   * @param algorithm the signature algorithm
   * @param base the private key to sign with
   * @param toSign the byte array(s) of data to sign
   * @return the generated signature
   */
  public static byte[] generateSignature(String algorithm, PrivateKey base, byte[]... toSign) {
    Preconditions.checkArgument(toSign.length > 0);
    try {
      Signature construct = Signature.getInstance(algorithm);
      construct.initSign(base);
      for (byte[] bytes : toSign) {
        construct.update(bytes);
      }
      return construct.sign();
    } catch (GeneralSecurityException e) {
      throw new IllegalArgumentException("Invalid signature parameters");
    }
  }

  /**
   * Encodes a long array as Big-endian byte array.
   *
   * @param bits the long (array) of numbers to encode
   * @return the encoded bytes
   */
  public static byte[] longToBigEndianByteArray(long... bits) {
    ByteBuffer ret = ByteBuffer.allocate(8 * bits.length).order(ByteOrder.BIG_ENDIAN);
    for (long put : bits) {
      ret.putLong(put);
    }
    return ret.array();
  }

  public static String encodeUrlEncoded(byte[] data) {
    return MIME_SPECIAL_ENCODER.encodeToString(data);
  }

  public static byte[] decodeUrlEncoded(String toParse) {
    return Base64.getMimeDecoder().decode(toParse);
  }

  /**
   * Parse a cer-encoded RSA key into its key bytes.
   *
   * @param toParse the cer-encoded key String
   * @param descriptors the type of key
   * @return the parsed key bytes
   */
  public static byte[] parsePemEncoded(String toParse, Pair<String, String> descriptors) {
    int startIdx = toParse.indexOf(descriptors.first());
    Preconditions.checkArgument(startIdx >= 0);
    int firstLen = descriptors.first().length();
    int endIdx = toParse.indexOf(descriptors.second(), firstLen + startIdx) + 1;
    Preconditions.checkArgument(endIdx > 0);
    return decodeUrlEncoded(toParse.substring(startIdx + firstLen, endIdx));
  }

  /**
   * Encodes an RSA key as String cer format.
   *
   * @param toEncode the private or public RSA key
   * @return the encoded key cer
   */
  public static String pemEncodeRsaKey(Key toEncode) {
    Preconditions.checkNotNull(toEncode);
    Pair<String, String> encoder;
    if (toEncode instanceof PublicKey) {
      encoder = PEM_RSA_PUBLIC_KEY_DESCRIPTOR;
    } else if (toEncode instanceof PrivateKey) {
      encoder = PEM_RSA_PRIVATE_KEY_DESCRIPTOR;
    } else {
      throw new IllegalArgumentException("Invalid key type");
    }

    return encoder.first() + "\n"
            + encodeUrlEncoded(toEncode.getEncoded()) + "\n"
            + encoder.second() + "\n";
  }

  /**
   * Parse an RSA public key from key bytes.
   *
   * @param keyValue the key bytes
   * @return the generated key
   */
  public static PublicKey parseRsaPublicKey(byte[] keyValue, int maxLen) {
    validateAsn1PayloadLength(keyValue, maxLen);
    try {
      return RSA_KEY_FACTORY.generatePublic(new X509EncodedKeySpec(keyValue));
    } catch (InvalidKeySpecException e) {
      throw new IllegalArgumentException("Invalid key bytes");
    }
  }

  private static int unsignByte(byte b) {
    return b & 0xff;
  }

  private static int byteChunked(int num) {
    int r = 0;
    while (num != 0) {
      num >>= 8;
      r++;
    }
    return r;
  }

  private static void validateAsn1PayloadLength(byte[] data, int maxLen) {
    if (data.length < 2) {
      throw MinecraftDecoder.DEBUG ? new CorruptedFrameException("ASN1 payload missing") : INVALID_ASN1_PAYLOAD;
    }
    int keyTag = data[0];
    if (keyTag != 0x30) {
      throw MinecraftDecoder.DEBUG ? new CorruptedFrameException("Not an ASN1 payload") : INVALID_ASN1_PAYLOAD;
    }

    int sizeIndicator = unsignByte(data[1]);

    if (sizeIndicator == 0x80) {
      throw MinecraftDecoder.DEBUG ? new CorruptedFrameException("ASN1 size limit was infinite") : INVALID_ASN1_PAYLOAD;
    }

    // If set, the highest bit is indicator for multipart
    if ((sizeIndicator & 0x080) != 0x00) {
      if (data.length < 3) {
        throw MinecraftDecoder.DEBUG ? new CorruptedFrameException("ASN1 payload too small") : INVALID_ASN1_PAYLOAD;
      }

      // Bytes below 0x80 make up the byte counter
      int byteCounter = (sizeIndicator & 0x07f);

      if (byteCounter >= 4 || byteCounter > byteChunked(maxLen)) {
        throw MinecraftDecoder.DEBUG ? new CorruptedFrameException("ASN1 payload bits too big") : INVALID_ASN1_PAYLOAD;
      }

      int msb = unsignByte(data[2]);
      if (msb == 0 || (msb << (8 * (byteCounter - 1))) > maxLen) {
        throw MinecraftDecoder.DEBUG ? new CorruptedFrameException("Invalid ASN1 msb size") : INVALID_ASN1_PAYLOAD;
      }
    } else {
      if ((sizeIndicator + 2) > maxLen) {
        throw MinecraftDecoder.DEBUG ? new CorruptedFrameException("ASN1 payload too large") : INVALID_ASN1_PAYLOAD;
      }
    }
  }

  /**
   * Generates an RSA key pair.
   *
   * @param keysize the key size (in bits) for the RSA key pair
   * @return the generated key pair
   */
  public static KeyPair createRsaKeyPair(final int keysize) {
    try {
      final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(keysize);
      return generator.generateKeyPair();
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException("Unable to generate RSA keypair", e);
    }
  }

  /**
   * Generates a hex digest in two's complement form for use with the Mojang joinedServer endpoint.
   *
   * @param digest the bytes to digest
   * @return the hex digest
   */
  public static String twosComplementHexdigest(byte[] digest) {
    return new BigInteger(digest).toString(16);
  }

  /**
   * Decrypts an RSA message.
   *
   * @param keyPair the key pair to use
   * @param bytes the bytes of the encrypted message
   * @return the decrypted message
   * @throws GeneralSecurityException if the message couldn't be decoded
   */
  public static byte[] decryptRsa(KeyPair keyPair, byte[] bytes) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
    return cipher.doFinal(bytes);
  }

  /**
   * Generates the server ID for the hasJoined endpoint.
   *
   * @param sharedSecret the shared secret between the client and the proxy
   * @param key the RSA public key
   * @return the server ID
   */
  public static String generateServerId(byte[] sharedSecret, PublicKey key) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      digest.update(sharedSecret);
      digest.update(key.getEncoded());
      return twosComplementHexdigest(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }
}
