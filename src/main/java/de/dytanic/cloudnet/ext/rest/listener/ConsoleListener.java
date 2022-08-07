package de.dytanic.cloudnet.ext.rest.listener;

import de.dytanic.cloudnet.driver.event.EventListener;
import de.dytanic.cloudnet.driver.network.http.websocket.WebSocketFrameType;
import de.dytanic.cloudnet.event.service.CloudServiceConsoleLogReceiveEntryEvent;
import de.dytanic.cloudnet.ext.rest.CloudNetRestModule;

public class ConsoleListener {

    private final CloudNetRestModule cloudNetRestModule;

    public ConsoleListener(CloudNetRestModule cloudNetRestModule) {
        this.cloudNetRestModule = cloudNetRestModule;
    }

    @EventListener
    public void handleTaskAdd(CloudServiceConsoleLogReceiveEntryEvent event) {
        this.cloudNetRestModule.getChannels().forEach((iWebSocketChannel, s) -> {
            if (event.getServiceInfoSnapshot().getServiceId().getUniqueId().toString().equalsIgnoreCase(s)) {
                iWebSocketChannel.sendWebSocketFrame(WebSocketFrameType.TEXT, event.getMessage());
            }
        });
    }

}
