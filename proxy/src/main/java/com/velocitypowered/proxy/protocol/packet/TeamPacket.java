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

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.text.Component;

import java.util.Arrays;
import java.util.List;

public class TeamPacket implements MinecraftPacket {

  public enum Action {
    CREATE_TEAM,
    REMOVE_TEAM,
    UPDATE_INFO,
    ADD_ENTITY,
    REMOVE_ENTITY;
  }

  private String teamName;
  private Action action;
  private Component displayName;
  private Component prefix, suffix;
  private byte friendlyFire;
  private String nameTagVisibility;
  private String collisionRule;
  private int color;
  private List<String> entites; // Can either be a player name or an UUID

  public String getTeamName() {
    return teamName;
  }

  public void setTeamName(String teamName) {
    this.teamName = teamName;
  }

  public Action getAction() {
    return action;
  }

  public void setAction(Action action) {
    this.action = action;
  }

  public Component getDisplayName() {
    return displayName;
  }

  public void setDisplayName(Component displayName) {
    this.displayName = displayName;
  }

  public Component getPrefix() {
    return prefix;
  }

  public void setPrefix(Component prefix) {
    this.prefix = prefix;
  }

  public Component getSuffix() {
    return suffix;
  }

  public void setSuffix(Component suffix) {
    this.suffix = suffix;
  }

  public byte getFriendlyFire() {
    return friendlyFire;
  }

  public void setFriendlyFire(byte friendlyFire) {
    this.friendlyFire = friendlyFire;
  }

  public String getNameTagVisibility() {
    return nameTagVisibility;
  }

  public void setNameTagVisibility(String nameTagVisibility) {
    this.nameTagVisibility = nameTagVisibility;
  }

  public String getCollisionRule() {
    return collisionRule;
  }

  public void setCollisionRule(String collisionRule) {
    this.collisionRule = collisionRule;
  }

  public int getColor() {
    return color;
  }

  public void setColor(int color) {
    this.color = color;
  }

  public List<String> getEntites() {
    return ImmutableList.copyOf(entites);
  }

  public void setEntites(List<String> entites) {
    this.entites = ImmutableList.copyOf(entites);
  }

  @Override
  public String toString() {
    return "TeamPacket{" +
        "teamName='" + teamName + '\'' +
        ", action=" + action +
        ", displayName=" + displayName +
        ", prefix=" + prefix +
        ", suffix=" + suffix +
        ", friendlyFire=" + friendlyFire +
        ", nameTagVisibility='" + nameTagVisibility + '\'' +
        ", collisionRule='" + collisionRule + '\'' +
        ", color=" + color +
        ", entites=" + entites.toString() +
        '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    teamName = ProtocolUtils.readString(buf, 16);
    action = Action.values()[buf.readByte()];
	if (action == Action.CREATE_TEAM || action == Action.UPDATE_INFO) {
      displayName = ProtocolUtils.readScoreboardComponent(buf, version);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
        prefix = ProtocolUtils.readScoreboardComponent(buf, version, 16);
        suffix = ProtocolUtils.readScoreboardComponent(buf, version, 16);
      }
      friendlyFire = buf.readByte();
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
        nameTagVisibility = ProtocolUtils.readString(buf, 40);
        if (version.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
          collisionRule = ProtocolUtils.readString(buf, 40);
        }
        if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
          color = ProtocolUtils.readVarInt(buf);
          prefix = ProtocolUtils.readScoreboardComponent(buf, version);
          suffix = ProtocolUtils.readScoreboardComponent(buf, version);
        } else {
          color = buf.readByte();
        }
      }
    }
    if (action == Action.CREATE_TEAM || action == Action.ADD_ENTITY
			|| action == Action.REMOVE_ENTITY) {
      entites = Arrays.asList(version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0
              ? ProtocolUtils.readStringArray(buf) : ProtocolUtils.readStringArray17(buf));
	  }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    ProtocolUtils.writeString(buf, teamName);
    buf.writeByte(action.ordinal());
    if (action == Action.CREATE_TEAM || action == Action.UPDATE_INFO) {
      ProtocolUtils.writeScoreboardComponent(buf, displayName, version);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
        ProtocolUtils.writeScoreboardComponent(buf, prefix, version, 16);
        ProtocolUtils.writeScoreboardComponent(buf, suffix, version, 16);
      }
      buf.writeByte(friendlyFire);
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
        ProtocolUtils.writeString(buf, nameTagVisibility);
        if (version.compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
          ProtocolUtils.writeString(buf, collisionRule);
        }
        if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
          ProtocolUtils.writeVarInt(buf, color);
          ProtocolUtils.writeScoreboardComponent(buf, prefix, version);
          ProtocolUtils.writeScoreboardComponent(buf, suffix, version);
        } else {
          buf.writeByte(color);
        }
      }
    }
    if (action == Action.CREATE_TEAM || action == Action.ADD_ENTITY
			|| action == Action.REMOVE_ENTITY) {
      if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
        ProtocolUtils.writeStringArray(buf, entites.toArray(new String[entites.size()]));
      } else {
        ProtocolUtils.writeStringArray17(buf, entites.toArray(new String[entites.size()]));
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
