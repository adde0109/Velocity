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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.chat.SecurityProfile;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Set;

public class VelocitySecurityProfile implements SecurityProfile {

    private Mode selectedMode;
    private final Mode highestSupported;
    private final Mode lowestSupported;
    private final Set<Mode> supportedModes;


    // TODO: Input sanity
    public VelocitySecurityProfile(Mode selectedMode, Mode highestSupported, Mode lowestSupported, Mode ... supportedModes) {
        this.selectedMode = Preconditions.checkNotNull(selectedMode);
        this.highestSupported = Preconditions.checkNotNull(highestSupported);
        this.lowestSupported = lowestSupported;
        this.supportedModes = new ImmutableSet.Builder<Mode>()
                .add(selectedMode)
                .add(lowestSupported)
                .add(highestSupported)
                .add(supportedModes).build();
    }

    public void setSelectedMode(Mode selectedMode) {
        Preconditions.checkArgument(supportedModes.contains(selectedMode));
        Preconditions.checkArgument(highestSupported.compare(selectedMode) <= 0);
        Preconditions.checkArgument(lowestSupported.compare(selectedMode) >= 0);
        this.selectedMode = selectedMode;
    }

    @Override
    public Mode getSelectedMode() {
        return selectedMode;
    }

    @Override
    public Mode getHighestSupported() {
        return highestSupported;
    }

    @Override
    public Mode getLowestSupported() {
        return lowestSupported;
    }

    @Override
    public Set<Mode> getSupportedModes() {
        return null;
    }

    public static enum SyncProtocol {
        EXTENDED_SYNC_PROTOCOL,
        STANDARD_SYNC,
        NONE,
    }

    public static class Verdict {

        private @Nullable final Component disconnectReason;
        private final Action action;
        private final boolean messageTracked;


        public Verdict(@Nullable Component disconnectReason, Action action, boolean messageTracked) {
            this.disconnectReason = disconnectReason;
            this.action = action;
            this.messageTracked = messageTracked;
        }

        public Action getAction() {
            return action;
        }

        public Component getDisconnectReason() {
            return disconnectReason;
        }

        public boolean isMessageTracked() {
            return messageTracked;
        }
    }

    public static enum Action {
        PASS,
        DOWNGRADE,
        CONFLICT;

    }


}
