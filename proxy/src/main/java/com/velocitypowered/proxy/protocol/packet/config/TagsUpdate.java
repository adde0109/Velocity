/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.registry.DataTag;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.key.Key;

import java.util.List;
import java.util.Map;

public class TagsUpdate implements MinecraftPacket {

  private DataTag tag;

  public TagsUpdate(DataTag tag) {
    this.tag = tag;
  }

  public TagsUpdate() {
    this.tag = new DataTag(ImmutableList.of());
  }

  public DataTag getTag() {
    return tag;
  }

  public void setTag(DataTag tag) {
    this.tag = tag;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ImmutableList.Builder<DataTag.Set> entrySet = ImmutableList.builder();

    int size = ProtocolUtils.readVarInt(buf);
    for (int i = 0; i < size; i++) {
      Key setkey = ProtocolUtils.readKey(buf);

      int innerSize = ProtocolUtils.readVarInt(buf);
      ImmutableList.Builder<DataTag.Entry> innerBuilder = ImmutableList.builder();
      for (int j = 0; j < innerSize; j++) {
        innerBuilder.add(new DataTag.Entry(
            ProtocolUtils.readKey(buf),
            ProtocolUtils.readVarIntArray(buf))
        );
      }

      entrySet.add(new DataTag.Set(setkey, innerBuilder.build()));
    }
    tag = new DataTag(entrySet.build());
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, tag.getEntrySets().size());
    for (DataTag.Set set : tag.getEntrySets()) {
      ProtocolUtils.writeKey(buf, set.key());

      ProtocolUtils.writeVarInt(buf, set.getEntries().size());
      for (DataTag.Entry entry : set.getEntries()) {
        ProtocolUtils.writeKey(buf, entry.key());
        ProtocolUtils.writeVarIntArray(buf, entry.getElements());
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
