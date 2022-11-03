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

import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.connection.VelocityConstants;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class ModernForwardingPacket extends LoginPluginResponse {

    private final int id;
    private final int requestedVersion;
    private final byte[] forwardingSecret;
    private final @Nullable IdentifiedKey playerKey;
    private final String address;
    private final GameProfile profile;

    public ModernForwardingPacket(int id, int requestedVersion, byte[] forwardingSecret,
                                  @Nullable IdentifiedKey playerKey, String address, GameProfile profile) {
        this.id = id;
        this.requestedVersion = requestedVersion;
        this.forwardingSecret = forwardingSecret;
        this.playerKey = playerKey;
        this.address = address;
        this.profile = profile;
    }


    @Override
    public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
        ProtocolUtils.writeVarInt(buf, id);
        buf.writeBoolean(true);

        int writeMarker = buf.writerIndex();
        int readerMarker = buf.readerIndex();

        try {
            int actualVersion = findForwardingVersion();

            ProtocolUtils.writeVarInt(buf, actualVersion);
            ProtocolUtils.writeString(buf, address);
            ProtocolUtils.writeUuid(buf, profile.getId());
            ProtocolUtils.writeString(buf, profile.getName());
            ProtocolUtils.writeProperties(buf, profile.getProperties());

            // This serves as additional redundancy. The key normally is stored in the
            // login start to the server, but some setups require this.
            if (actualVersion >= VelocityConstants.MODERN_FORWARDING_WITH_KEY) {
                assert playerKey != null;
                ProtocolUtils.writePlayerKey(buf, playerKey);

                // Provide the signer UUID since the UUID may differ from the
                // assigned UUID. Doing that breaks the signatures anyway but the server
                // should be able to verify the key independently.
                if (actualVersion >= VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2) {
                    if (playerKey.getSignatureHolder() != null) {
                        buf.writeBoolean(true);
                        ProtocolUtils.writeUuid(buf, playerKey.getSignatureHolder());
                    } else {
                        // Should only not be provided if the player was connected
                        // as offline-mode and the signer UUID was not backfilled
                        buf.writeBoolean(false);
                    }
                }
            }

            // Extract forwarding data and reset
            buf.readerIndex(writeMarker);
            int dataLen = buf.readableBytes() - writeMarker;
            byte[] data = new byte[dataLen];
            buf.readBytes(data);
            buf.readerIndex(readerMarker);

            SecretKey key = new SecretKeySpec(forwardingSecret, "HmacSHA256");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);

            mac.update(data, 0, dataLen);
            byte[] sig = mac.doFinal();

            // Write back to packet
            buf.writerIndex(writeMarker);
            buf.writeBytes(sig);
            buf.writeBytes(data);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Unable to authenticate data", e);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean handle(MinecraftSessionHandler handler) {
        throw new IllegalArgumentException("Packet can never be decoded");
    }

    private int findForwardingVersion() {
        // Ensure we are in range
        int requested = Math.min(requestedVersion, VelocityConstants.MODERN_FORWARDING_MAX_VERSION);
        if (requested > VelocityConstants.MODERN_FORWARDING_DEFAULT) {
            if (playerKey != null) {
                // No enhanced switch on java 11
                switch (playerKey.getKeyRevision()) {
                    case GENERIC_V1:
                        return VelocityConstants.MODERN_FORWARDING_WITH_KEY;
                    // Since V2 is not backwards compatible we have to throw the key if v2 and requested is v1
                    case LINKED_V2:
                        return requested >= VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2
                                ? VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2 : VelocityConstants.MODERN_FORWARDING_DEFAULT;
                    default:
                        return VelocityConstants.MODERN_FORWARDING_DEFAULT;
                }
            } else {
                return VelocityConstants.MODERN_FORWARDING_DEFAULT;
            }
        }
        return VelocityConstants.MODERN_FORWARDING_DEFAULT;
    }
}

