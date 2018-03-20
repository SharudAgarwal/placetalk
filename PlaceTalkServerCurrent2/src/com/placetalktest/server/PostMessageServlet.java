/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.placetalktest.server;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.placetalktest.server.SendAllMessagesServlet;

/**
 * Servlet that registers a device, whose registration id is identified by
 * {@link #PARAMETER_MESSAGE}.
 *
 * <p>
 * The client app should call this servlet everytime it receives a
 * {@code com.google.android.c2dm.intent.REGISTRATION C2DM} intent without an
 * error or {@code unregistered} extra.
 */

@SuppressWarnings("serial")
public class PostMessageServlet extends BaseServlet {

  private static final String PARAMETER_MESSAGE = "message";
  static final String PARAMETER_DEVICE = "device";
  private static final String PARAMETER_GROUP = "group";
  private static final String PARAMETER_TIME = "time";
  private static final String PARAMETER_JSON = "json";
  private static final String PARAMETER_LOCATION = "location";
  private static final String SERVER_URL = "http://ska496.appspot.com";
  private static final int BACKOFF_MILLI_SECONDS = 2000;
  private static final Random random = new Random();
  private static final int MAX_ATTEMPTS = 5;
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException {
	  String location = getParameter(req, PARAMETER_LOCATION);
	  String regId = getParameter(req, PARAMETER_DEVICE);
//	  double location = Double.valueOf(slocation).doubleValue();
	  //Double location = Double.valueOf(slocation);
	  ArrayList<String> groupList = new ArrayList<String>();
	  groupList = Datastore.getGroups(regId, location);
	  Gson gson = new Gson();
	  String json = gson.toJson(groupList);
	  setSuccess(resp);
	  boolean result = groupPostToURL(req, resp, json, regId);
  }
  
  public boolean groupPostToURL(HttpServletRequest req, HttpServletResponse resp, String json, String regId) {
	  String serverUrl = SERVER_URL + "/send";
      long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
      Map<String, String> params = new HashMap<String, String>();
      params.put("json", json);
      params.put("device", regId);
      // Once GCM returns a registration id, we need to register it in the
      // demo server. As the server might be down, we will retry it a couple
      // times.
      for (int i = 1; i <= MAX_ATTEMPTS; i++) {
          try {
              groupPost(serverUrl, params);
              //GCMRegistrar.setRegisteredOnServer(context, true);
              //String message = context.getString(R.string.server_registered);
              //CommonUtilities.displayMessage(context, message);
              return true;
          } catch (IOException e) {
              // Here we are simplifying and retrying on any error; in a real
              // application, it should retry only on unrecoverable errors
              // (like HTTP error code 503).
              if (i == MAX_ATTEMPTS) {
                  break;
              }
              try {
                  Thread.sleep(backoff);
              } catch (InterruptedException e1) {
                  // Activity finished before we complete - exit.
                  Thread.currentThread().interrupt();
                  return false;
              }
              // increase backoff exponentially
              backoff *= 2;
          }
      }
      return false;
	}
  
	
	/**
     * Issue a POST request to the server.
     *
     * @param endpoint POST address.
     * @param params request parameters.
     * @param message 
     *
     * @throws IOException propagated from POST.
     */
    private static void groupPost(String endpoint, Map<String, String> params) throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                    .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();
            // handle the response
            int status = conn.getResponseCode();
            if (status != 200) {
              throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
      }

  
  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException {

	String json = getParameter(req, PARAMETER_JSON);
	 // String json = getParameter(req, "device");
	logger.info("in doPost. json = " + json + "about to do Datastore.sendMessage(json)");
    String serverJson = Datastore.sendMessage(json);
    setSuccess(resp);
    
    boolean result = postToURL(req, resp, serverJson);

  }
  

  
  public boolean postToURL(HttpServletRequest req, HttpServletResponse resp, String json) {
	  String serverUrl = SERVER_URL + "/sendAll";
      long backoff = BACKOFF_MILLI_SECONDS + random.nextInt(1000);
      Map<String, String> params = new HashMap<String, String>();
      params.put("json", json);
      // Once GCM returns a registration id, we need to register it in the
      // demo server. As the server might be down, we will retry it a couple
      // times.
      for (int i = 1; i <= MAX_ATTEMPTS; i++) {
          try {
              post(serverUrl, params);
              //GCMRegistrar.setRegisteredOnServer(context, true);
              //String message = context.getString(R.string.server_registered);
              //CommonUtilities.displayMessage(context, message);
              return true;
          } catch (IOException e) {
              // Here we are simplifying and retrying on any error; in a real
              // application, it should retry only on unrecoverable errors
              // (like HTTP error code 503).
              if (i == MAX_ATTEMPTS) {
                  break;
              }
              try {
                  Thread.sleep(backoff);
              } catch (InterruptedException e1) {
                  // Activity finished before we complete - exit.
                  Thread.currentThread().interrupt();
                  return false;
              }
              // increase backoff exponentially
              backoff *= 2;
          }
      }
      return false;
	}
  
	
	/**
     * Issue a POST request to the server.
     *
     * @param endpoint POST address.
     * @param params request parameters.
     * @param message 
     *
     * @throws IOException propagated from POST.
     */
    private static void post(String endpoint, Map<String, String> params)
            throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        // constructs the POST body using the parameters
        while (iterator.hasNext()) {
            Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                    .append(param.getValue());
            if (iterator.hasNext()) {
                bodyBuilder.append('&');
            }
        }
        String body = bodyBuilder.toString();
        byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();
            // handle the response
            int status = conn.getResponseCode();
            if (status != 200) {
              throw new IOException("Post failed with error code " + status);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
      }
  
}
