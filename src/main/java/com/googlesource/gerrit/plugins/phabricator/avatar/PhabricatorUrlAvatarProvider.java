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

import com.google.gerrit.common.Nullable;
import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.restapi.Url;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.avatar.AvatarProvider;
import com.google.gerrit.server.config.CanonicalWebUrl;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.googlesource.gerrit.plugins.phabricator.avatar.results.UserLdap;
import com.googlesource.gerrit.plugins.phabricator.avatar.results.UserLdapQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Listen
@Singleton
public class PhabricatorUrlAvatarProvider implements AvatarProvider {

  private static final String USER_PLACEHOLDER = "${user}";
  private static final String EMAIL_PLACEHOLDER = "${email}";

  private final String pluginName;
  private final boolean ssl;
  private String url;
  private String avatarChangeUrl;
  private String sizeParameter;
  private String token;
  private final PhabConduitConnection phabConduitConnection;
  private final Gson gson;

  @Inject
  PhabricatorUrlAvatarProvider(PluginConfigFactory cfgFactory,
      @PluginName String pluginName,
      @CanonicalWebUrl @Nullable String canonicalUrl) {
    this.pluginName = pluginName;
    PluginConfig cfg = cfgFactory.getFromGerritConfig(pluginName);
    url = cfg.getString("url");
    avatarChangeUrl = cfg.getString("changeUrl");
    sizeParameter = cfg.getString("sizeParameter");
    ssl = canonicalUrl != null && canonicalUrl.startsWith("https://");
    token = cfg.getString("token");

    this.phabConduitConnection = new PhabConduitConnection(url);
  }

  @Override
  public String getUrl(IdentifiedUser forUser, int imageSize) {
    if (url == null) {
      Logger log = LoggerFactory.getLogger(PhabricatorUrlAvatarProvider.class);
      log.warn("Phabricator URL is not configured. Please configure plugin."
          + pluginName + ".url in etc/gerrit.config");
      return null;
    }

    try {
      return getUserProfileImage(forUser).toString();
    } catch (PhabConduitException e) {
      // TODO: return default image
      return "";
    }
    /*avatarUrl.append(replaceInUrl(EMAIL_PLACEHOLDER, userReplacedAvatarURL,
        forUser.getAccount().getPreferredEmail()));
    if (imageSize > 0 && sizeParameter != null) {
      if (avatarUrl.indexOf("?") < 0) {
        avatarUrl.append("?");
      } else {
        avatarUrl.append("&");
      }
      avatarUrl.append(sizeParameter.replaceAll("\\$\\{size\\}",
          Integer.toString(imageSize)));
    }
    //return avatarUrl.toString();*/
  }

  @Override
  public String getChangeAvatarUrl(IdentifiedUser forUser) {
    String userReplacedAvatarChangeURL = replaceInUrl(USER_PLACEHOLDER,
        avatarChangeUrl, forUser.getUserName());
    return replaceInUrl(EMAIL_PLACEHOLDER, userReplacedAvatarChangeURL,
        forUser.getAccount().getPreferredEmail());
  }

  /**
   * Takes #{replacement} and substitutes the marker #{placeholder} in #{url}
   * after it has been URL encoded
   * @param placeholder The placeholder to be replaced
   * @param url The URL, usually containing #{placeholder}
   * @param replacement String to be put inside
   * @return new URL
   */
  private String replaceInUrl(String placeholder, String url,
      String replacement) {
    if (url == null || replacement == null
        || !url.contains(placeholder)) {
      return url;
    }

    // as we can't assume anything of 'replacement', we're URL encoding it
    return url.replace(placeholder, Url.encode(replacement));
  }

  private UserLdap getUserProfileImage(IdentifiedUser forUser) throws PhabConduitException {
    Map<String, Object> params = new HashMap<>();
    params.put("ldapnames", Arrays.asList(forUser.getUserName()));

    JsonElement callResult = phabConduitConnection.call("user.ldapquery", params, token);
    UserLdapQuery queryResult = gson.fromJson(callResult, UserLdapQuery.class);
    JsonObject queryResultData = queryResult.getResultZero().getAsJsonObject();

    UserLdap result = null;
    JsonElement queryResultEntryValue = queryResultData;
    UserLdap queryResultUserLdap = gson.fromJson(queryResultEntryValue, UserLdap.class);
    if (queryResultUserLdap.getLdapUserName().equals(forUser.getUserName())) {
      result = queryResultUserLdap.getProfileIamge();
    }

    /*
    for (Entry<String, JsonElement> queryResultEntry : queryResultData.entrySet()) {
      JsonElement queryResultEntryValue = queryResultEntry.getValue();
      UserLdap queryResultUserLdap = gson.fromJson(queryResultEntryValue, UserLdap.class);
      if (queryResultUserLdap.getLdapName().equals(forUser.getUserName())) {
        result = queryResultUserLdap.getProfileIamge();
      }
    }*/

    return result;
  }
}
