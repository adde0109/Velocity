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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ScoreboardScore implements MinecraftPacket {

  public enum Method {
    CHANGE,
    REMOVE
  }

  private String owner;
  private @Nullable String objectiveName;
  private int score;
  private Method method;

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public @Nullable String getObjectiveName() {
    return objectiveName;
  }

  public void setObjectiveName(String objectiveName) {
    this.objectiveName = objectiveName;
  }

  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    this.score = score;
  }

  public Method getMethod() {
    return method;
  }

  public void setMethod(Method method) {
    this.method = method;
  }

  @Override
  public String toString() {
    return "ScoreboardScore{" +
        "owner='" + owner + '\'' +
        ", objectiveName='" + objectiveName + '\'' +
        ", score=" + score +
        ", method=" + method +
        '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    owner = ProtocolUtils.readString(buf, 40);
    method = Method.values()[buf.readByte()];
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      String read = ProtocolUtils.readString(buf, 16);
      objectiveName = read.isEmpty() ? null : read;
    }

    if (method != Method.REMOVE) {
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
        score = ProtocolUtils.readVarInt(buf);
      } else {
        String read = ProtocolUtils.readString(buf, 16);
        objectiveName = read.isEmpty() ? null : read;
        score = buf.readInt();
      }
    } else {
      score = 0;
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    ProtocolUtils.writeString(buf, owner, 40);
    buf.writeByte(method.ordinal());
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      ProtocolUtils.writeString(buf, objectiveName == null ? "" : objectiveName, 16);
    }

    if (method != Method.REMOVE) {
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
        ProtocolUtils.writeVarInt(buf, score);
      } else {
        ProtocolUtils.writeString(buf, objectiveName == null ? "" : objectiveName, 16);
        buf.writeInt(score);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
