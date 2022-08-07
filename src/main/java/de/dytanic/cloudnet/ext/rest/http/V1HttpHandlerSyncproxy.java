package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.CloudNetDriver;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.ext.rest.HttpHandler;
import de.dytanic.cloudnet.ext.rest.utils.gson.SyncproxyPut;
import de.dytanic.cloudnet.ext.syncproxy.AbstractSyncProxyManagement;
import de.dytanic.cloudnet.ext.syncproxy.SyncProxyConstants;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyConfigurationWriterAndReader;
import de.dytanic.cloudnet.ext.syncproxy.configuration.SyncProxyProxyLoginConfiguration;
import de.dytanic.cloudnet.ext.syncproxy.node.CloudNetSyncProxyModule;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class V1HttpHandlerSyncproxy extends HttpHandler {
    public V1HttpHandlerSyncproxy(String permission) {
        super(permission);
    }

    private final SyncProxyConfiguration syncProxyManagement = CloudNetDriver.getInstance().getServicesRegistry()
            .getFirstService(SyncProxyConfiguration.class);

    @Override
    public void handleGet(String path, IHttpContext context) throws Exception {

        context
                .response()
                .header("Content-Type", "application/json")
                .statusCode(HttpResponseCode.HTTP_OK)
                .body(GSON.toJson(CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration()))
                .context()
                .closeAfter(true)
                .cancelNext()
        ;

        super.handleGet(path, context);
    }

    @Override
    public void handlePut(String path, IHttpContext context) throws Exception {
        try {
            if (context.request().body().length > 0) {
                SyncProxyConfiguration syncProxyConfiguration = GSON
                        .fromJson(context.request().bodyAsString(), SyncProxyConfiguration.TYPE);

                if (syncProxyConfiguration != null) {
                    CloudNetSyncProxyModule.getInstance().setSyncProxyConfiguration(syncProxyConfiguration);
                    SyncProxyConfigurationWriterAndReader
                            .write(syncProxyConfiguration, CloudNetSyncProxyModule.getInstance().getConfigurationFilePath());

                    CloudNetDriver.getInstance().getMessenger().sendChannelMessage(
                            SyncProxyConstants.SYNC_PROXY_CHANNEL_NAME,
                            SyncProxyConstants.SYNC_PROXY_UPDATE_CONFIGURATION,
                            new JsonDocument("syncProxyConfiguration",
                                    CloudNetSyncProxyModule.getInstance().getSyncProxyConfiguration())
                    );

                    context
                            .response()
                            .statusCode(HttpResponseCode.HTTP_OK)
                            .header("Content-Type", "application")
                            .body(new JsonDocument("success", true).toByteArray())
                            .context()
                            .closeAfter(true)
                            .cancelNext()
                    ;
                }
            }

        } catch (Exception ex) {

            try (StringWriter writer = new StringWriter();
                 PrintWriter printWriter = new PrintWriter(writer)) {
                ex.printStackTrace(printWriter);
                this.send400Response(context, writer.getBuffer().toString());
            }
        }
    }
}
