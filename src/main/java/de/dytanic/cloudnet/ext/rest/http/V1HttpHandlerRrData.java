/*
 * Copyright 2019-2021 CloudNetService team & contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.dytanic.cloudnet.ext.rest.http;

import de.dytanic.cloudnet.common.document.gson.JsonDocument;
import de.dytanic.cloudnet.driver.network.http.HttpResponseCode;
import de.dytanic.cloudnet.driver.network.http.IHttpContext;
import de.dytanic.cloudnet.ext.rest.CloudNetRestModule;
import de.dytanic.cloudnet.ext.rest.HttpHandler;

import java.util.Map;
import java.util.stream.Collectors;

public final class V1HttpHandlerRrData extends HttpHandler {

  public V1HttpHandlerRrData(String permission) {
    super(permission);
  }

  @Override
  public void handleGet(String path, IHttpContext context) throws Exception {
    context
            .response()
            .statusCode(HttpResponseCode.HTTP_OK)
            .header("Content-Type", "application/json")
            .body(GSON.toJson(
                    new JsonDocument()
                            .append("cpu", CloudNetRestModule.getCloudNetRestModule().getRrDataCpu())
                            .append("memory", CloudNetRestModule.getCloudNetRestModule().getRrDataMemory())
            ))
            .context()
            .closeAfter(true)
            .cancelNext();
    super.handleGet(path, context);
  }
}
