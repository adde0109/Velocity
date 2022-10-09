/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.chat;

import com.velocitypowered.api.proxy.ServerConnection;

import java.util.Set;

public interface SecurityProfile {

    Mode getSelectedMode();

    Set<Mode> getSupportedModes();

    public static enum Mode {
        ENFORCING(true),
        ALLOW_DOWNGRADE(true),
        DISABLED(false);

        private final boolean supportsSigning;
        Mode(boolean supportsSigning) {
            this.supportsSigning = supportsSigning;
        }
        public boolean supportsSigning() {
            return supportsSigning;
        }

    }
}
