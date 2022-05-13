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

package com.velocitypowered.proxy.protocol.packet.chat;

import com.google.common.primitives.Longs;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.SignedMessage;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.crypto.SignedChatMessage;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public class PlayerChat extends Chat {

  private static final byte[] EMPTY_SIGNATURE = new byte[0];
  private static final long EMPTY_SALT = 0L;

  public PlayerChat() {
    super();
  }

  /**
   * Create a new PlayerChat instance.
   */
  public PlayerChat(String message, byte type, UUID sender, Component name,
                    Instant timestamp, long salt, byte[] signature) {
    super(message, type, sender);
    this.name = name;
    this.timestamp = timestamp;
    this.salt = salt;
    this.signature = signature;
  }

  private Component name;
  private Instant timestamp;
  private long salt;
  private byte[] signature;

  @Override
  public String toString() {
    return "PlayerChat{"
        + "message='" + message + '\''
        + ", type=" + type
        + ", sender=" + sender
        + ", name='" + name + '\''
        + ", salt=" + salt
        + ", timestamp='" + timestamp + '\''
        + ", signature='" + Arrays.toString(signature) + '\''
        + '}';
  }

  public byte[] getSignature() {
    return signature;
  }

  public void setSignature(byte[] signature) {
    this.signature = signature;
  }

  public Component getName() {
    return name;
  }

  public void setName(Component name) {
    this.name = name;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public long getSalt() {
    return salt;
  }

  public void setSalt(long salt) {
    this.salt = salt;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (direction == ProtocolUtils.Direction.SERVERBOUND) {
      timestamp = Instant.ofEpochSecond(buf.readLong());
    }

    super.decode(buf, direction, version);

    if (direction == ProtocolUtils.Direction.CLIENTBOUND) {
      name = GsonComponentSerializer.gson().deserialize(ProtocolUtils.readString(buf));
      timestamp = Instant.ofEpochSecond(buf.readLong());
    }

    salt = buf.readLong();
    signature = ProtocolUtils.readByteArray(buf);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (direction == ProtocolUtils.Direction.SERVERBOUND) {
      buf.writeLong(timestamp.getEpochSecond());
    }

    super.encode(buf, direction, version);

    if (direction == ProtocolUtils.Direction.CLIENTBOUND) {
      ProtocolUtils.writeString(buf, GsonComponentSerializer.gson().serialize(name));
      buf.writeLong(timestamp.getEpochSecond());
    }

    buf.writeLong(salt);
    ProtocolUtils.writeByteArray(buf, signature);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

}
