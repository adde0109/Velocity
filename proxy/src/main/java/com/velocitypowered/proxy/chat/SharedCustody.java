package com.velocitypowered.proxy.chat;

import io.netty.buffer.ByteBuf;

public enum SharedCustody {
    FILTER_MAPS //Brrrrrrrr ew
    ;


    // if enforce secure chat
    //       : allow downgrade          : boolean
    //         (Allow insecure clients? Insecure clients must be flagged!)
    //       : support preview          : boolean
    //         (Server does secure previews)
    //       : extended control         : boolean
    //         (Without this Velocity has no control over chat)
    //       if extended control
    //              : String sync channel      : String
    //                (Identifier for chat exchange plugin messages)
    //              : proxy can cancel chat    : boolean
    //                (Allow the proxy to cancel chat)
    //              if proxy can cancel chat
    //                     : server-authoritative     : boolean
    //                       (true:  proxy relays message with verdict)
    //                       (false: proxy cancels message and notifies server)
    //              : proxy can preview chat   : boolean
    //                (Allow the proxy to send chat previews)
    //              if proxy can preview chat
    //                     : server-authoritative     : boolean
    //                       (true:  proxy sends preview request to server to decide)
    //                       (false: proxy waits for server preview if enabled, then last say and notifies the server)
    //              : proxy can apply filter maps     : boolean
    //                (proxy relays message with suggested filter)
    //



    //         (Valid flags are:
    public void write(ByteBuf to) {}
}
