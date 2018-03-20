package com.placetalktest.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Group {
	private ArrayList<String> messages;
	private HashSet<String> devices;
	private String groupName;
//	private long locationID;
	private String locationID;
//	private double location;
	private String location;
	private Integer range;
	private boolean inGroup;
	
	public Group() {
		messages = new ArrayList<String>();
		devices = new HashSet<String>();
	}
	
	public Group(String groupName, String location) {
		this.groupName = groupName;
		messages = new ArrayList<String>();
		devices = new HashSet<String>();
		this.location = location;
//		this.locationID = (long) location;
		this.locationID = location;
		this.range = 50;
		this.inGroup = true;
	}
	
	public Group(String groupName, String location, Integer range) {
		this.groupName = groupName;
		messages = new ArrayList<String>();
		devices = new HashSet<String>();
		this.location = location;
//		this.locationID = (long) location;
		this.locationID = location;
		this.range = range;
		this.inGroup = true;
	}
	
	public HashSet<String> getDevices() {
		return devices;
	}
	
	/*
	 * removes device from group message when user leaves group
	 * For future implementation
	 * If the user is the last device left in the group, function returns false
	 * Go to Datastore and remove entity
	 */
	public void leaveGroupInServer(String regId) {
		
		while (devices.contains(regId)) {
			devices.remove(regId);
		} 

	}
	
	
	public void leaveGroupInApp() {
		this.inGroup = false;
	}
	
	public void joinGroup(String regId) {
		devices.add(regId);
		this.inGroup = true;
	}
	
	public void addMessage(String message) {
		messages.add(message);
	}
	
	public String getLatestMessage() {
		return messages.get(messages.size()-1);
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	@Override
	public String toString() {
		return groupName;
	}
	
	public String getLocation() {
		return location;
	}
	
	public String getLocationID() {
		return locationID;
	}
	
	public Integer getRange() {
		return range;
	}
	
	public void setDevices(HashSet<String> devices) {
		this.devices = devices;
	}
	
	public void addDevice(String regId) {
		devices.add(regId);
	}
	
	public boolean getInGroup() {
		return this.inGroup;
	}
	
}