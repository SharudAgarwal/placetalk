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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Text;
import com.google.appengine.api.datastore.Transaction;
import com.google.gson.Gson;

/**
 * Simple implementation of a data store using standard Java collections.
 * <p>
 * This class is neither persistent (it will lost the data when the app is
 * restarted) nor thread safe.
 */
public final class Datastore {

  static final int MULTICAST_SIZE = 1000;
  private static final String DEVICE_TYPE = "Device";
  private static final String DEVICE_REG_ID_PROPERTY = "regId";
  private static final String MULTICAST_TYPE = "Multicast";
  private static final String MULTICAST_REG_IDS_PROPERTY = "regIds";

  private static final FetchOptions DEFAULT_FETCH_OPTIONS = FetchOptions.Builder
      .withPrefetchSize(MULTICAST_SIZE).chunkSize(MULTICAST_SIZE);

  private static final Logger logger =
      Logger.getLogger(Datastore.class.getName());
  private static final DatastoreService datastore =
      DatastoreServiceFactory.getDatastoreService();
  private static double GROUP_RANGE;

  private Datastore() {
    throw new UnsupportedOperationException();
  }

  /**
   * Registers a device.
   *
   * @param regId device's registration id.
   */
  public static void register(String regId) {
    logger.info("Registering " + regId);
    Transaction txn = datastore.beginTransaction();
    try {
      Entity entity = findDeviceByRegId(regId);
      if (entity != null) {
        logger.fine(regId + " is already registered; ignoring.");
        return;
      }
      entity = new Entity(DEVICE_TYPE);
      entity.setProperty(DEVICE_REG_ID_PROPERTY, regId);
      datastore.put(entity);
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }

  /**
   * Unregisters a device.
   *
   * @param regId device's registration id.
   */
  public static void unregister(String regId) {
    logger.info("Unregistering " + regId);
    Transaction txn = datastore.beginTransaction();
    try {
      Entity entity = findDeviceByRegId(regId);
      if (entity == null) {
        logger.warning("Device " + regId + " already unregistered");
      } else {
        Key key = entity.getKey();
        datastore.delete(key);
      }
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }

  /**
   * Updates the registration id of a device.
   */
  public static void updateRegistration(String oldId, String newId) {
    logger.info("Updating " + oldId + " to " + newId);
    Transaction txn = datastore.beginTransaction();
    try {
      Entity entity = findDeviceByRegId(oldId);
      if (entity == null) {
        logger.warning("No device for registration id " + oldId);
        return;
      }
      entity.setProperty(DEVICE_REG_ID_PROPERTY, newId);
      datastore.put(entity);
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }

  /**
   * Gets all registered devices.
   */
  public static List<String> getDevices() {
    List<String> devices;
    Transaction txn = datastore.beginTransaction();
    try {
      Query query = new Query(DEVICE_TYPE);
      Iterable<Entity> entities =
          datastore.prepare(query).asIterable(DEFAULT_FETCH_OPTIONS);
      devices = new ArrayList<String>();
      for (Entity entity : entities) {
        String device = (String) entity.getProperty(DEVICE_REG_ID_PROPERTY);
        devices.add(device);
      }
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
    return devices;
  }

  /**
   * Gets the number of total devices.
   */
  public static int getTotalDevices() {
    Transaction txn = datastore.beginTransaction();
    try {
      Query query = new Query(DEVICE_TYPE).setKeysOnly();
      List<Entity> allKeys =
          datastore.prepare(query).asList(DEFAULT_FETCH_OPTIONS);
      int total = allKeys.size();
      logger.fine("Total number of devices: " + total);
      txn.commit();
      return total;
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }

  private static Entity findDeviceByRegId(String regId) {
	Filter filter = new FilterPredicate(DEVICE_REG_ID_PROPERTY, FilterOperator.EQUAL, regId);
    Query query = new Query(DEVICE_TYPE)
        //.addFilter(DEVICE_REG_ID_PROPERTY, FilterOperator.EQUAL, regId);
    	.setFilter(filter);
    PreparedQuery preparedQuery = datastore.prepare(query);
    List<Entity> entities = preparedQuery.asList(DEFAULT_FETCH_OPTIONS);
    Entity entity = null;
    if (!entities.isEmpty()) {
      entity = entities.get(0);
    }
    int size = entities.size();
    if (size > 0) {
      logger.severe(
          "Found " + size + " entities for regId " + regId + ": " + entities);
    }
    return entity;
  }

  /**
   * Creates a persistent record with the devices to be notified using a
   * multicast message.
   *
   * @param devices registration ids of the devices.
   * @return encoded key for the persistent record.
   */
  public static String createMulticast(List<String> devices) {
    logger.info("Storing multicast for " + devices.size() + " devices");
    String encodedKey;
    Transaction txn = datastore.beginTransaction();
    try {
      Entity entity = new Entity(MULTICAST_TYPE);
      entity.setProperty(MULTICAST_REG_IDS_PROPERTY, devices);
      datastore.put(entity);
      Key key = entity.getKey();
      encodedKey = KeyFactory.keyToString(key);
      logger.fine("multicast key: " + encodedKey);
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
    return encodedKey;
  }

  /**
   * Gets a persistent record with the devices to be notified using a
   * multicast message.
   *
   * @param encodedKey encoded key for the persistent record.
   */
  public static List<String> getMulticast(String encodedKey) {
    Key key = KeyFactory.stringToKey(encodedKey);
    Entity entity;
    Transaction txn = datastore.beginTransaction();
    try {
      entity = datastore.get(key);
      @SuppressWarnings("unchecked")
      List<String> devices =
          (List<String>) entity.getProperty(MULTICAST_REG_IDS_PROPERTY);
      txn.commit();
      return devices;
    } catch (EntityNotFoundException e) {
      logger.severe("No entity for key " + key);
      return Collections.emptyList();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }

  /**
   * Updates a persistent record with the devices to be notified using a
   * multicast message.
   *
   * @param encodedKey encoded key for the persistent record.
   * @param devices new list of registration ids of the devices.
   */
  public static void updateMulticast(String encodedKey, List<String> devices) {
    Key key = KeyFactory.stringToKey(encodedKey);
    Entity entity;
    Transaction txn = datastore.beginTransaction();
    try {
      try {
        entity = datastore.get(key);
      } catch (EntityNotFoundException e) {
        logger.severe("No entity for key " + key);
        return;
      }
      entity.setProperty(MULTICAST_REG_IDS_PROPERTY, devices);
      datastore.put(entity);
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }

  /**
   * Deletes a persistent record with the devices to be notified using a
   * multicast message.
   *
   * @param encodedKey encoded key for the persistent record.
   */
  public static void deleteMulticast(String encodedKey) {
    Transaction txn = datastore.beginTransaction();
    try {
      Key key = KeyFactory.stringToKey(encodedKey);
      datastore.delete(key);
      txn.commit();
    } finally {
      if (txn.isActive()) {
        txn.rollback();
      }
    }
  }
  
  public static void deleteGroup(Key key) {
	  logger.info("deleteGroup");
	    Transaction txn = datastore.beginTransaction();
	    try {
	      datastore.delete(key);
	      txn.commit();
	    } finally {
	      if (txn.isActive()) {
	        txn.rollback();
	      }
	    }
	  }
  
  
  public static String sendMessage(String clientJson) {
	    logger.info("Posting " + clientJson);
	    Transaction txn = datastore.beginTransaction();
	    Gson gson = new Gson();
	    Group clientGroup = gson.fromJson(clientJson, Group.class);
	    String locationID = clientGroup.getLocationID();
	    String location = clientGroup.getLocation();
	    String range = clientGroup.getRange().toString();
	    HashSet<String> devices = clientGroup.getDevices();
//	    logger.info("inGroup = " + serverGroup.getInGroup());
//        String[] deviceArray = (String[]) serverGroup.getDevices().toArray();
//        String device = deviceArray[0];
	    String key = clientGroup.getGroupName() + ":" + locationID;
	    Entity clientEntity = new Entity("groups", key);
	    Key clientKey = clientEntity.getKey();
//	    if (!serverGroup.getInGroup()) {
//	    	logger.severe("Deleting group " + myKey.toString());
//	    	Datastore.deleteGroup(myKey);
//	    } else {
		try {
			String[] deviceArray = devices.toArray(new String[0]);
			String device = deviceArray[0];
//			logger.severe("device in device array is " + device);
			Entity serverEntity = datastore.get(clientKey);
			Text serverText = (Text) serverEntity.getProperty("json");
		    String serverJson = serverText.getValue();
		    Group serverGroup = gson.fromJson(serverJson, Group.class);
		    if (!clientGroup.getInGroup()) {
//		    	logger.severe("device left group");
		    	serverGroup.leaveGroupInServer(device);
		    } else {
//		    	logger.severe("adding device: " + device + " to server");
	    		serverGroup.addDevice(device);
		    }
	    	if (serverGroup.getDevices().isEmpty()) {
//	    		logger.severe("Deleting group " + clientKey.toString());
		    	Datastore.deleteGroup(clientKey);
		    } else {
//		    	logger.severe("adding message to group");
		    	serverGroup.addMessage(clientGroup.getLatestMessage());
			    serverJson = gson.toJson(serverGroup);
		    	Text newText = new Text(serverJson);
			    serverEntity.setProperty("json", newText);
			    serverEntity.setProperty("location", location);
			    serverEntity.setProperty("range", range);
			    datastore.put(serverEntity);
			    txn.commit();
			    if (txn.isActive()) {
			        txn.rollback();
			    }
			    return serverJson;
		    }
		    
//		    } else {
//		    	logger.severe("adding device: " + device + " to server");
//		    	oldGroup.addDevice(device);
//		    	oldGroup.addMessage(serverGroup.getLatestMessage());
//			    json = gson.toJson(oldGroup);
//		    	Text myText = new Text(json);
//			    entity.setProperty("json", myText);
//			    entity.setProperty("location", location);
//			    datastore.put(entity);
//			    txn.commit();
//		    }
		} catch (EntityNotFoundException e) {
			logger.info("Entity not found");
			Text newText = new Text(clientJson);
		    clientEntity.setProperty("json", newText);
		    clientEntity.setProperty("location", location);
		    clientEntity.setProperty("range", range);
		    datastore.put(clientEntity);
		    txn.commit();
		}
	      
//	      Text myText = new Text(json);
//	      entity.setProperty("json", myText);
//	      entity.setProperty("location", location);
//	      datastore.put(entity);
//	      txn.commit();
//	    } finally {
	      if (txn.isActive()) {
	        txn.rollback();
	      }
//	    }
//	    }
	    return clientJson;
	  }

	public static ArrayList<String> getGroups(String regId, String myLocation) {
		
	    Query query = new Query("groups");
	    PreparedQuery preparedQuery = datastore.prepare(query);
	    List<Entity> entities = preparedQuery.asList(DEFAULT_FETCH_OPTIONS);
	    ArrayList<String> groups = new ArrayList<String>();
	    for(int x = 0; x < entities.size(); x++){
	    	String groupLocation = (String) entities.get(x).getProperty("location");
	    	String groupRange = (String) entities.get(x).getProperty("range");
	    	GROUP_RANGE = Integer.parseInt(groupRange);
	    	int index = groupLocation.indexOf(",");
	    	String slat = groupLocation.substring(0, index);
	    	double groupLat = Double.parseDouble(slat);
	    	String slon = groupLocation.substring(index+1, groupLocation.length());
	    	double groupLon = Double.parseDouble(slon);
	    	logger.info("about to look for groups within my location of: " + myLocation);
	    	logger.info("the group location is: " + groupLocation);
//	    	}
	    	index = myLocation.indexOf(",");
	    	slat = myLocation.substring(0, index);
	    	double myLat = Double.parseDouble(slat);
	    	slon = myLocation.substring(index+1, myLocation.length());
	    	double myLon = Double.parseDouble(slon);
	    	long radius = 6371; // km
	    	double dLat = ((myLat - groupLat) * Math.PI);
	    	double dLon = ((myLon- groupLon) * Math.PI);
	    	double lat1 = myLat * Math.PI;
	    	double lat2 = groupLat * Math.PI;

	    	double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
	    	        Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2); 
	    	double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a)); 
	    	double dist = radius * c;		//dist is in meters
	    	
	    	if (dist < GROUP_RANGE) { 	//if myLocation is within 100 m of groupLocation
	    		Text myText = (Text) entities.get(x).getProperty("json");
	    		String json = myText.getValue();
	    		//groups.add((String) entities.get(x).getProperty("json"));
	    		groups.add(json);
	    		logger.info("GOT GROUP: " + myText);
	    	}
	    }
	    logger.info("In Datastore.getGroups(). The return variable groups is: " + groups);
		return groups;
	}
	

}
