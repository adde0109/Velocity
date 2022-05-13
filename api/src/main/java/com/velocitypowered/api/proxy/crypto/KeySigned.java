/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.crypto;

import java.security.PublicKey;
import java.time.Instant;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface KeySigned {

  /**
   * Returns the key used to sign the object.
   *
   * @return the key
   */
  PublicKey getSigner();

  /**
   * Returns the expiry time point of the key.
   * Note: this limit is arbitrary. RSA keys don't expire,
   * but the signature of this key as provided by the session
   * server will expire.
   *
   * @return the expiry time point
   */
  Instant getExpiryTemporal();


  /**
   * Check if the signature has expired.
   *
   * @return true if proxy time is after expiry time
   */
  default boolean hasExpired() {
    return Instant.now().isAfter(getExpiryTemporal());
  }

  /**
   * Retrieves the signature of the signed object.
   *
   * @return an RSA signature
   */
  @Nullable
  byte[] getSignature();

  /**
   * Validates the signature, expiry temporal and key against the
   * signer public key. Note: This will **not** check for
   * expiry. You can check for expiry with {@link KeySigned#hasExpired()}.
   *
   * @return validity of the signature
   */
  boolean isSignatureValid();

  /**
   * Returns the signature salt or null if not salted.
   *
   * @return signature salt or null
   */
  default byte[] getSalt() {
    return null;
  }

}
