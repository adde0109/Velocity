/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.chat.SecurityProfile;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired before the player connects to a server. Velocity will wait on this event to
 * finish firing before the connection continues.
 * Denying this event ignores the result and disables the security protocol.
 */
@AwaitingEvent
public final class ServerSecuritySetupEvent implements
    ResultedEvent<ServerSecuritySetupEvent.SecurityProfileResult> {

  private final ServerConnection connection;
  private final SecurityProfile offer;
  private SecurityProfileResult result;

  public ServerSecuritySetupEvent(ServerConnection connection, SecurityProfile offer, ) {
    this.connection = connection;
    this.offer = offer;
    this.result = new SecurityProfileResult(offer, offer);
  }

  public SecurityProfile getOffer() {
    return offer;
  }

  @Override
  public SecurityProfileResult getResult() {
    return result;
  }

  @Override
  public void setResult(SecurityProfileResult result) {
    Preconditions.checkArgument(result.originalProfile == this.offer, "Profile must be from original offer");
    this.result = Preconditions.checkNotNull(result, "result");
  }

  /**
   * Represents the result of the {@link ServerSecuritySetupEvent}.
   */
  public static class SecurityProfileResult implements Result {
    private final @Nullable SecurityProfile securityProfile;
    private final boolean isAllowed;

    public final static SecurityProfileResult DENIED = new SecurityProfileResult(null, false);

    public SecurityProfileResult(@Nullable SecurityProfile securityProfile, boolean isAllowed) {
      this.securityProfile = securityProfile;
      this.isAllowed = isAllowed;
    }

    @Override
    public boolean isAllowed() {
      return isAllowed;
    }

    public @Nullable SecurityProfile getSecurityProfile() {
      return securityProfile;
    }
  }
}
