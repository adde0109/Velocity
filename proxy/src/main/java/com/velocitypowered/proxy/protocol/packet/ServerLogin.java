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

package com.velocitypowered.proxy.protocol.packet;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.crypto.EncryptionUtils;
import com.velocitypowered.proxy.crypto.IdentifiedKeyImpl;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ServerLogin implements MinecraftPacket {

  private static final QuietDecoderException EMPTY_USERNAME = new QuietDecoderException("Empty username!");
  private static final QuietDecoderException INVALID_KEY = new QuietDecoderException("Invalid key!");

  private @Nullable String username;
  private @Nullable IdentifiedKey key; // Added in 1.19
  private static final BinaryTagIO.Reader KEY_READER = BinaryTagIO.reader(4 * 2 * 512);

  public ServerLogin() {
  }

  public ServerLogin(String username) {
    this.username = Preconditions.checkNotNull(username, "username");
  }

  public ServerLogin(String username, IdentifiedKey key) {
    this.username = Preconditions.checkNotNull(username, "username");
    this.key = key;
  }

  public String getUsername() {
    if (username == null) {
      throw new IllegalStateException("No username found!");
    }
    return username;
  }

  public IdentifiedKey getKey() {
    return key;
  }

  @Override
  public String toString() {
    return "ServerLogin{"
        + "username='" + username + '\''
        + ", key='" + key + '\''
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    username = ProtocolUtils.readString(buf, 16);
    if (username.isEmpty()) {
      throw EMPTY_USERNAME;
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      if (buf.readBoolean()) {
        CompoundBinaryTag keyData = ProtocolUtils.readCompoundTag(buf, KEY_READER);
        if (keyData.keySet().size() != 3) {
          throw INVALID_KEY;
        }

        String keyCer = keyData.getString("key");
        String signature = keyData.getString("signature");
        String expiry = keyData.getString("expires_at");

        key = IdentifiedKeyImpl.parsePlayerPublicKey(keyCer, expiry, signature);
        if (!key.isSignatureValid()) {
          throw INVALID_KEY;
        }
      }
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (username == null) {
      throw new IllegalStateException("No username found!");
    }
    ProtocolUtils.writeString(buf, username);

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      if (key != null) {
        buf.writeBoolean(true);
        String keyCer = EncryptionUtils.encodeRsaKey(key.getSignedPublicKey());
        String signature = EncryptionUtils.encodeUrlEncoded(key.getSignature());
        String expiry = key.getExpiryTemporal().toString(); // Defaults to UTC zero

        CompoundBinaryTag keyData = CompoundBinaryTag.empty();
        keyData.putString("key", keyCer);
        keyData.putString("signature", signature);
        keyData.putString("expires_at", expiry);

        ProtocolUtils.writeCompoundTag(buf, keyData);
      } else {
        buf.writeBoolean(false);
      }
    }
  }

  @Override
  public int expectedMaxLength(ByteBuf buf, Direction direction, ProtocolVersion version) {
    // Accommodate the rare (but likely malicious) use of UTF-8 usernames, since it is technically
    // legal on the protocol level.
    int base = 1 + (16 * 4);
    return version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0 ? base + 1 + 4 * 2 * 512 : base;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
