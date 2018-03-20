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

import com.google.android.gcm.server.Constants;
import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet that sends a message to a device.
 * <p>
 * This servlet is invoked by AppEngine's Push Queue mechanism.
 */
@SuppressWarnings("serial")
public class SendGroupsServlet extends BaseServlet {

  private static final String HEADER_QUEUE_COUNT = "X-AppEngine-TaskRetryCount";
  private static final String HEADER_QUEUE_NAME = "X-AppEngine-QueueName";
  private static final int MAX_RETRY = 3;

  static final String PARAMETER_DEVICE = "device";
  static final String PARAMETER_MULTICAST = "multicastKey";
  static final String PARAMETER_GROUP = "group";
  static final String PARAMETER_MESSAGE = "message";
  static final String PARAMETER_JSON = "json";

  private Sender sender;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    sender = newSender(config);
  }

  /**
   * Creates the {@link Sender} based on the servlet settings.
   */
  protected Sender newSender(ServletConfig config) {
    String key = (String) config.getServletContext()
        .getAttribute(ApiKeyInitializer.ATTRIBUTE_ACCESS_KEY);
    return new Sender(key);
  }

  /**
   * Indicates to App Engine that this task should be retried.
   */
  private void retryTask(HttpServletResponse resp) {
    resp.setStatus(500);
  }

  /**
   * Indicates to App Engine that this task is done.
   */
  private void taskDone(HttpServletResponse resp) {
    resp.setStatus(200);
  }
  
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

	    String regId = req.getParameter(PARAMETER_DEVICE);
	    if (regId != null) {
	      sendGroupList(regId, req, resp);
	      return;
	    }
	    logger.severe("Invalid request!");
	    taskDone(resp);
	    return;
	  }
  
  private void sendGroupList(String regId, HttpServletRequest req, HttpServletResponse resp) {
	    logger.info("Sending message to device " + regId);
	    Message message = new Message.Builder().addData("jsonList", req.getParameter("json")).build();
	    Result result;
	    try {
	      result = sender.sendNoRetry(message, regId);
	    } catch (IOException e) {
	      logger.log(Level.SEVERE, "Exception posting " + message, e);
	      taskDone(resp);
	      return;
	    }
	    if (result == null) {
	      retryTask(resp);
	      return;
	    }
	    if (result.getMessageId() != null) {
	      logger.info("Succesfully sent message to device " + regId);
	      String canonicalRegId = result.getCanonicalRegistrationId();
	      if (canonicalRegId != null) {
	        // same device has more than on registration id: update it
	        logger.finest("canonicalRegId " + canonicalRegId);
	        Datastore.updateRegistration(regId, canonicalRegId);
	      }
	    } else {
	      String error = result.getErrorCodeName();
	      if (error.equals(Constants.ERROR_NOT_REGISTERED)) {
	        // application has been removed from device - unregister it
	        Datastore.unregister(regId);
	      } else {
	        logger.severe("Error sending message to device " + regId
	            + ": " + error);
	      }
	    }
	  }
}
