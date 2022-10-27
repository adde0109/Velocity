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

/**
 * This event is fired before the player connects to a server. Velocity will wait on this event to
 * finish firing before the connection continues.
 */
@AwaitingEvent
public final class ServerSecuritySetupEvent implements
    ResultedEvent<ServerSecuritySetupEvent.SecurityProfileResult> {

  private final ServerConnection connection;
  private final SecurityProfile offer;
  private SecurityProfileResult result;

  public ServerSecuritySetupEvent(ServerConnection connection, SecurityProfile offer) {
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

  public SecurityProfileResult createResult(SecurityProfile toSet) {
    return new SecurityProfileResult(toSet, offer);
  }

  /**
   * Represents the result of the {@link ServerSecuritySetupEvent}.
   */
  public static class SecurityProfileResult implements Result {
    private final SecurityProfile securityProfile;
    private final SecurityProfile originalProfile;

    private SecurityProfileResult(SecurityProfile securityProfile, SecurityProfile originalProfile) {
      this.securityProfile = securityProfile;
      this.originalProfile = originalProfile;
    }

    @Override
    public boolean isAllowed() {
      return securityProfile.compatibleWith(originalProfile);
    }

    public SecurityProfile getSecurityProfile() {
      return securityProfile;
    }
  }
}
