package com.barofarm.user.auth.application.port;

import com.barofarm.user.auth.application.event.HotlistEventMessage;

public interface HotlistEventPublisher {
    void publish(HotlistEventMessage message);
}
