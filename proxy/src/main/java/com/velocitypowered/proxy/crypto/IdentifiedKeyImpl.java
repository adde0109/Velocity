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

import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.util.except.QuietDecoderException;

import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

public class IdentifiedKeyImpl implements IdentifiedKey {

  private final PublicKey publicKey;
  private final byte[] signature;
  private final Instant expiryTemporal;
  private final boolean isSignatureValid;

  /**
   * Constructs an unsigned instance.
   */
  public IdentifiedKeyImpl(PublicKey publicKey) {
    this.publicKey = publicKey;
    this.expiryTemporal = null;
    this.signature = null;
    this.isSignatureValid = false;
  }

  private static final QuietDecoderException DATA_PARSE_ERROR = new QuietDecoderException("Couldn't parse key data");

  /**
   * Create a players Identified key from data.
   */
  public static IdentifiedKeyImpl parsePlayerPublicKey(String key, String expiry, String signature) {
    try {
      Instant expiryTemporal = Instant.parse(expiry);
      byte[] signatureBytes = EncryptionUtils.decodeUrlEncoded(signature);

      if (signatureBytes.length != 512) {
        throw DATA_PARSE_ERROR;
      }

      byte[] keyBytes = EncryptionUtils.parsePemEncoded(key, EncryptionUtils.PEM_RSA_PUBLIC_KEY_DESCRIPTOR);
      if (keyBytes.length < 221 || keyBytes.length > 294) {
        throw DATA_PARSE_ERROR;
      }
      PublicKey publicKey = EncryptionUtils.parseRsaPublicKey(keyBytes);
      boolean isValid = EncryptionUtils.verifySignature(
              EncryptionUtils.SHA1_WITH_RSA, EncryptionUtils.getYggdrasilSessionKey(), signatureBytes,
              (expiryTemporal.toEpochMilli() + key).getBytes(StandardCharsets.UTF_8));

      return new IdentifiedKeyImpl(publicKey, expiryTemporal, signatureBytes, isValid);

    } catch (IllegalArgumentException | DateTimeParseException e) {
      throw DATA_PARSE_ERROR;
    }
  }

  private IdentifiedKeyImpl(PublicKey publicKey, Instant expiryTemporal,
                              byte[] signature, boolean isSignatureValid) {
    this.publicKey = publicKey;
    this.expiryTemporal = expiryTemporal;
    this.signature = signature;
    this.isSignatureValid = isSignatureValid;
  }

  @Override
  public PublicKey getSignedPublicKey() {
    return publicKey;
  }

  @Override
  public PublicKey getSigner() {
    return EncryptionUtils.getYggdrasilSessionKey();
  }

  @Override
  public Instant getExpiryTemporal() {
    return null;
  }

  @Override
  public byte[] getSignature() {
    return signature;
  }

  @Override
  public boolean isSignatureValid() {
    return isSignatureValid;
  }

  @Override
  public boolean verifyDataSignature(byte[] signature, byte[]... toVerify) {
    try {
      return EncryptionUtils.verifySignature(EncryptionUtils.SHA1_WITH_RSA, publicKey, signature, toVerify);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  @Override
  public String toString() {
    return "IdentifiedKeyImpl{"
        + "publicKey=" + publicKey
        + ", signature=" + Arrays.toString(signature)
        + ", expiryTemporal=" + expiryTemporal
        + ", isSignatureValid=" + isSignatureValid
        + '}';
  }
}
