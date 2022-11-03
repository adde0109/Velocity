/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.chat;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.ServerConnection;

import java.util.Set;

public interface SecurityProfile {

    Mode getSelectedMode();

    Mode getHighestSupported();

    Mode getLowestSupported();

    Set<Mode> getSupportedModes();

    boolean compatibleWith(SecurityProfile profile);

    public static enum Mode {
        DISABLED(false),
        ALLOW_DOWNGRADE(true),
        ENFORCING(true);

        private final boolean supportsSigning;
        Mode(boolean supportsSigning) {
            this.supportsSigning = supportsSigning;
        }

        public boolean supportsSigning() {
            return supportsSigning;
        }

        public int compare(Mode toCompare) {
            Preconditions.checkNotNull(toCompare);
            return this.ordinal() - toCompare.ordinal();
        }

    }
}
