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

import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.SignedMessage;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.crypto.SignedChatMessage;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Chat implements MinecraftPacket {

  public static final byte CHAT_TYPE = (byte) 0;
  public static final byte SYSTEM_TYPE = (byte) 1;
  public static final byte GAME_INFO_TYPE = (byte) 2;

  private static final long EMPTY_SALT = 0L;
  private static final byte[] EMPTY_SALT_TYPE = Longs.toByteArray(EMPTY_SALT);
  private static final byte[] EMPTY_SIGNATURE = new byte[0];

  private static final Logger logger = LogManager.getLogger(Chat.class);

  public static final int MAX_SERVERBOUND_MESSAGE_LENGTH = 256;
  public static final UUID EMPTY_SENDER = new UUID(0, 0);

  protected @Nullable String message;
  protected byte type;
  protected @Nullable UUID sender;

  public Chat() {
  }

  /**
   * Creates a generic pre-1.19 chat message.
   */
  public Chat(String message, byte type, UUID sender) {
    this.message = message;
    this.type = type;
    this.sender = sender;
  }

  /**
   * Returns the chat message.
   */
  public String getMessage() {
    if (message == null) {
      throw new IllegalStateException("Message is not specified");
    }
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public byte getType() {
    return type;
  }

  public void setType(byte type) {
    this.type = type;
  }

  public UUID getSenderUuid() {
    return sender;
  }

  public void setSenderUuid(UUID sender) {
    this.sender = sender;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    message = ProtocolUtils.readString(buf);

    if (direction == ProtocolUtils.Direction.CLIENTBOUND && version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      type = buf.readByte();
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
        sender = ProtocolUtils.readUuid(buf);
      }
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (message == null) {
      throw new IllegalStateException("Message is not specified");
    }
    ProtocolUtils.writeString(buf, message);

    if (direction == ProtocolUtils.Direction.CLIENTBOUND && version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      buf.writeByte(type);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_16) >= 0) {
        ProtocolUtils.writeUuid(buf, sender == null ? EMPTY_SENDER : sender);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }


  /**
   * Creates a generic chat packet.
   *
   * @param direction the destination
   * @param version the connection version
   * @param message the component, string or signed message
   * @param sender the identifiable sender/origin of the message
   * @param name the component display-name of the player
   * @param forceDowngrade force fallback to system chat or unsigned messages
   * @return a generic Chat packet
   */
  public static Chat createUniversal(ProtocolUtils.Direction direction, ProtocolVersion version,
                                     Object message, byte type, UUID sender,
                                     net.kyori.adventure.text.@Nullable Component name, boolean forceDowngrade) {
    String preparedMessage = null;
    if (message instanceof net.kyori.adventure.text.Component) {
      preparedMessage = ProtocolUtils.getJsonChatSerializer(version).serialize((Component) message);
    } else if ((message instanceof SignedMessage)) {
      preparedMessage = ((SignedMessage) message).getMessage();
    } else {
      preparedMessage = (String) message;
    }

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      if (direction == ProtocolUtils.Direction.CLIENTBOUND) {
        if (message instanceof SignedMessage
                && !Arrays.equals(((SignedChatMessage) message).getSalt(), EMPTY_SALT_TYPE)) {
          SignedChatMessage sm = (SignedChatMessage) message;

          return new PlayerChat(sm.getMessage(), type, sm.getSignerUuid(), name, sm.getExpiryTemporal(),
                  Longs.fromByteArray(sm.getSalt()), sm.getSignature());

        } else {
          return new SystemChat(preparedMessage, type);
        }
      } else {
        if (message instanceof SignedMessage && !forceDowngrade) {
          SignedChatMessage sm = (SignedChatMessage) message;
          return new PlayerChat(sm.getMessage(), type, sm.getSignerUuid(), name, sm.getExpiryTemporal(),
                  Longs.fromByteArray(sm.getSalt()), sm.getSignature());
        } else {
          return new PlayerChat(preparedMessage, type, sender, name,
                  Instant.now().plus(SignedChatMessage.EXPIRY_TIME),
                  EMPTY_SALT, EMPTY_SIGNATURE);
        }
      }
    } else {
      return new Chat(preparedMessage, type, sender);
    }
  }

}
