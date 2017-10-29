// Copyright (C) 2017 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.phabricator.avatar;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import com.googlesource.gerrit.plugins.phabricator.avatar.results.PhabCallCapsule;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstracts the connection to Conduit API
 */
class PhabConduitConnection {
  private static final Logger log = LoggerFactory.getLogger(PhabricatorUrlAvatarProvider.class);

  private final String apiUrlBase;
  private final Gson gson;

  private CloseableHttpClient client;

  PhabConduitConnection(final String baseUrl) {
    apiUrlBase = baseUrl.replaceAll("/+$", "") + "/api/";
    gson = new Gson();
    client = null;
  }

  /**
   * Gives a cached HttpClient
   * <p/>
   * If  no cached HttpClient exists, a new one is spawned.
   *
   * @return the cached CloseableHttpClient
   */
  private CloseableHttpClient getClient() {
    if (client == null) {
      log.trace("Creating new client connection");
      client = HttpClients.createDefault();
    }
    return client;
  }

  /**
   * Call the given Conduit method without parameters
   *
   * @param method The name of the method that should get called
   * @return The call's result, if there has been no error
   * @throws ConduitException
   */
  JsonElement call(String method, String token) throws PhabConduitException {
    return call(method, new HashMap<String, Object>(), token);
  }

  /**
   * Calls a conduit method with some parameters
   *
   * @param method The name of the method that should get called
   * @param params A map of parameters to pass to the call
   * @return The call's result, if there has been no error
   * @throws ConduitException
   */
  JsonElement call(String method, Map<String, Object> params, String token) throws PhabConduitException {
    String methodUrl = apiUrlBase + method;

    HttpPost httppost = new HttpPost(methodUrl);


    if (token != null) {
      Map<String, Object> conduitParams = new HashMap<>();
      conduitParams.put("token", token);
      params.put("__conduit__", conduitParams);
    }

    String json = gson.toJson(params);

    log.trace("Calling phabricator method " + method
        + " with the parameters " + json );
    httppost.setEntity(new StringEntity("params=" + json, StandardCharsets.UTF_8));

    CloseableHttpResponse response;
    try {
      response = getClient().execute(httppost);
    } catch (IOException e) {
      throw new PhabConduitException("Could not execute Phabricator API call", e);
    }
    try {
      log.trace("Phabricator HTTP response status: " + response.getStatusLine());
      HttpEntity entity = response.getEntity();
      String entityString;
      try {
        entityString = EntityUtils.toString(entity);
      } catch (IOException e) {
        throw new PhabConduitException("Could not read the API response", e);
      }

      log.trace("Phabricator response " + entityString);
      PhabCallCapsule callCapsule = gson.fromJson(entityString, PhabCallCapsule.class);
      log.trace("callCapsule.result: " + callCapsule.getResult());
      log.trace("callCapsule.error_code: " + callCapsule.getErrorCode());
      log.trace("callCapsule.error_info: " + callCapsule.getErrorInfo());
      if (callCapsule.getErrorCode() != null
          || callCapsule.getErrorInfo() != null) {
        throw new PhabConduitErrorException(method, callCapsule.getErrorCode(),
            callCapsule.getErrorInfo());
      }
      return callCapsule.getResult();
    } finally {
      try {
        response.close();
      } catch (IOException e) {
        throw new PhabConduitException("Could not close API response", e);
      }
    }
  }
}