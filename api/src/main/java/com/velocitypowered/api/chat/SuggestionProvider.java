/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.chat;

import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface SuggestionProvider {

    @Nullable Component getLastSuggestion();

    @Nullable Component provideSuggestion(String currentMessage);
}
