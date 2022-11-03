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

package com.velocitypowered.proxy.protocol.packet.custom;

import com.velocitypowered.api.chat.SecurityProfile;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.chat.SharedCustody;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Locale;
import java.util.Set;

public class ChatSyncResponsePacket extends LoginPluginResponse {

    private final int id;
    private final int version;
    private final SecurityProfile selectedProfile;
    private final @Nullable Set<SharedCustody> syncFeatures;


    public ChatSyncResponsePacket(int id, int version, SecurityProfile selectedProfile, @Nullable Set<SharedCustody> syncFeatures) {
        this.id = id;
        this.version = version;
        this.selectedProfile = selectedProfile;
        this.syncFeatures = syncFeatures;
    }

    @Override
    public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
        ProtocolUtils.writeVarInt(buf, id);
        buf.writeBoolean(true);

        // Version
        ProtocolUtils.writeVarInt(buf, version);
        // 0: Disabled, 1: Allow downgrade, 2: Enforce
        ProtocolUtils.writeVarInt(buf, selectedProfile.getSelectedMode().ordinal());

        if (syncFeatures != null && syncFeatures.size() > 0) {
            buf.writeBoolean(true);
            ProtocolUtils.writeVarInt(buf, syncFeatures.size());
            for (SharedCustody c : syncFeatures) {
                c.write(buf);
            }
        } else {
            buf.writeBoolean(false);
        }
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        throw new IllegalArgumentException("Packet can never be decoded");
    }

}

