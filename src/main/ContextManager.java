package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import Ice.Current;
import UVSmart.*;

public class ContextManager extends Ice.Application{
	private static int usersLoggedIn = -1;
	private static int shutdown = 0;
	private HashMap<String, String> userTemp = new HashMap<String, String>(); //To store user's current temperature
	private HashMap<String, String> userLocation = new HashMap<String, String>(); //To store user's current location
	private HashMap<String, String> userUVReading = new HashMap<String, String>(); //To store user's current UV Index
	private HashMap<String, String> userLocationStatus = new HashMap<String, String>(); //To store user's current location status
	private HashMap<String, String> userTimer = new HashMap<String, String>(); //Outdoor timer for users
	private HashMap<String, String> userSkinType = new HashMap<String, String>();// user skin type
	private HashMap<String, String> userTemperatureThreshold = new HashMap<String, String>();//user temperature threshold
	private HashMap<String, locationDetails> cityLocations = new HashMap<String, locationDetails>(); // city locations
	private PreferenceMonitorPrx prefPrx;
	LocationIndoorStatusPrx locationPrx;
	private HashMap<String, String> userTempThresholdNotReached = new HashMap<String, String>();
	private HashMap<String, String> userUVThresholdNotReached = new HashMap<String, String>();

	public ContextManager(String fileName)
	{
		Scanner sc2 = null;

		String location = "";
		String name = "";
		String information = "";
		String services = "";
		try {
			sc2 = new Scanner(new File(fileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();  
		}
		while (sc2.hasNextLine()) {
			Scanner s2 = new Scanner(sc2.nextLine());
			while (s2.hasNext()) {
				String s = s2.next();
				if (s.equals("name:"))
				{
					name="";
					while(s2.hasNext())
					{
						name += s2.next();
						name += " ";
					}
				}
				else if(s.equals("location:"))
				{
					location="";
					while(s2.hasNext())
					{
						location += s2.next();
					}
				}
				else if(s.equals("information:"))
				{
					information="";
					while(s2.hasNext())
					{
						information += s2.next();
						information += " ";
					}
				}
				else if(s.equals("services:"))
				{
					services="";
					while(s2.hasNext())
					{
						services += s2.next();
					}
					cityLocations.put(location, new locationDetails(name,information,services));
				}
			}
			s2.close();
		}

	}

	//Implementing location server subscription method
	@SuppressWarnings("serial")
	class LocationServerLocationStatusI extends _LocationServerLocationStatusDisp
	{

		@Override
		public void getCurrentLocationStatus(String userName, String currentLocationStatus,
				String locationSensorReading, Current __current) {
			// TODO Auto-generated method stub
			if(shutdown!=1)
			{
				userLocationStatus.put(userName, currentLocationStatus);
				String oldLocation = userLocation.put(userName, locationSensorReading);
				if(currentLocationStatus.equals("Outdoor"))
				{
					if(!userTimer.containsKey(userName))
					{
						userUVThresholdNotReached.put(userName, Integer.toString(1));
						userTimer.put(userName, Integer.toString(1));
					}
					else{
						if(Integer.parseInt(userUVThresholdNotReached.get(userName))!=1 && !oldLocation.equals(userLocation.get(userName)))
						{
							//						System.out.println("Inside First if");
							userTimer.put(userName, Integer.toString(1));
							userUVThresholdNotReached.put(userName, Integer.toString(1));
						}
						else if(Integer.parseInt(userUVThresholdNotReached.get(userName))==1 && !oldLocation.equals(userLocation.get(userName)))
						{
							//						System.out.println("Inside else if");
							userTimer.put(userName, Integer.toString(1));
							userUVThresholdNotReached.put(userName, Integer.toString(1));
						}
						else if(Integer.parseInt(userUVThresholdNotReached.get(userName))!=1 && oldLocation.equals(userLocation.get(userName)))
						{
							userTimer.put(userName, Integer.toString(0));
							userUVThresholdNotReached.put(userName, Integer.toString(0));

						}
						else if(Integer.parseInt(userUVThresholdNotReached.get(userName))==1 && oldLocation.equals(userLocation.get(userName)))
						{
							//						System.out.println("Inside else");
							int t = Integer.parseInt(userTimer.get(userName));
							userTimer.put(userName, Integer.toString(t+1));
							//System.out.println(userUVThresholdNotReached.get(userName));
							//System.out.println(oldLocation);
							//System.out.println(userTimer.get(userName));
						}
					}
				}
				else
				{
					//System.out.println("Indoor");
					//System.out.println(userLocation.get(userName));
					userTimer.put(userName, Integer.toString(0));
					userUVThresholdNotReached.put(userName, Integer.toString(1));
				}
			}
		}

	}


	//Implementing temperature sensor subscription method
	@SuppressWarnings("serial")
	class TemperatureSensorI extends _TemperatureSensorDisp
	{

		@Override
		public void getTemperature(String userName, String sensorType, int temperatureReading, Current __current) {
			// TODO Auto-generated method stub
			if(shutdown!=1)
			{
				if(!userTemp.containsKey(userName))
				{
					//System.out.println("Inside contains check");
					userTemp.put(userName, Integer.toString(temperatureReading));
					userTempThresholdNotReached.put(userName, Integer.toString(1));
				}
				else
				{
					if(Integer.parseInt(userTempThresholdNotReached.get(userName))==0 && Integer.parseInt(userTemp.get(userName))!=temperatureReading)
					{
						userTemp.put(userName, Integer.toString(temperatureReading));
						userTempThresholdNotReached.put(userName, Integer.toString(1));
					}
					else
					{
						//System.out.println("Inside else");
						userTemp.put(userName, Integer.toString(temperatureReading));
					}
				}
			}
		}

	}

	//Implementing UV Sensor subscription method
	@SuppressWarnings("serial")
	class UVSensorI extends _UVSensorDisp
	{

		@Override
		public void getUVIndex(String userName, String sensorType, int uvIndexReading, Current __current) {
			// TODO Auto-generated method stub
			if(shutdown!=1)
				userUVReading.put(userName, Integer.toString(uvIndexReading));
		}

	}


	//Implementing Context Manager RMI
	@SuppressWarnings("serial")
	class ContextManagerRMII extends _ContextManagerRMIDisp
	{
		private PreferenceMonitorPrx pmPrx;
		public ContextManagerRMII(PreferenceMonitorPrx pmPrx) {
			super();
			this.pmPrx = pmPrx;
		}

		//Returns response to queries
		@Override
		public String getRequestedDetails(String userName, int requestType, String requestData, Current __current) {
			// TODO Auto-generated method stub
			if(requestType == 1)
			{
				System.out.println(requestData);
				for(Map.Entry<String, locationDetails> entry : cityLocations.entrySet()) {
					String locationKey = entry.getKey();
					locationDetails locDet = entry.getValue();
					if(locDet.name.contains(requestData))
						return locDet.information;
				}
				return "No Information found";
			}
			else if(requestType == 2)
			{
				String curLoc = userLocation.get(userName);
				return cityLocations.get(curLoc).name;
			}
			else
				return "No information found";
		}

		//Performs login and gets the skintype and temperature threshold from preference monitor
		@Override
		public int login(String userName, Current __current) {
			// TODO Auto-generated method stub
			//System.out.println("EnteredLogin");
			if(usersLoggedIn == -1)
			{
				userDetails uDetails = this.pmPrx.getUserDetails(userName);
				if(uDetails.skinType==0)
				{
					return 0;

				}
				else
				{
					userSkinType.put(userName, Integer.toString(uDetails.skinType));
					userTemperatureThreshold.put(userName, Integer.toString(uDetails.tempThreshold));
					userTempThresholdNotReached.put(userName, Integer.toString(1));
					userUVThresholdNotReached.put(userName, Integer.toString(1));
					usersLoggedIn = 1;
					return 1;
				}
			}
			else
			{
				userDetails uDetails = this.pmPrx.getUserDetails(userName);
				if(uDetails.skinType==0)
				{
					return 0;
				}
				else
				{
					userSkinType.put(userName, Integer.toString(uDetails.skinType));
					userTemperatureThreshold.put(userName, Integer.toString(uDetails.tempThreshold));
					userTempThresholdNotReached.put(userName, Integer.toString(1));
					userUVThresholdNotReached.put(userName, Integer.toString(1));
					++usersLoggedIn;
					return 1;
				}
			}
		}

		//Performs user logout
		@Override
		public void logout(String userName, Current __current) {
			// TODO Auto-generated method stub
			userSkinType.remove(userName);
			--usersLoggedIn;
		}

	}

	@SuppressWarnings("static-access")
	@Override
	public int run(String[] args) {
		// TODO Auto-generated method stub
		String topicName1 = "locationStatus";
		String topicName2 = "temperatureSensor";
		String topicName3 = "UVSensor";
		String topicName4 = "ContextManagerNotification";
		String topicName5 = "Shutdown";
		String id1 = null;        

		IceStorm.TopicManagerPrx manager1 = IceStorm.TopicManagerPrxHelper.checkedCast(
				communicator().propertyToProxy("TopicManager.Proxy"));
		if(manager1 == null)
		{
			System.err.println("invalid proxy");
			return 1;
		}


		IceStorm.TopicPrx topic1;
		IceStorm.TopicPrx topic2;
		IceStorm.TopicPrx topic3;
		IceStorm.TopicPrx topic4;
		IceStorm.TopicPrx topic5;
		try
		{
			topic4 = manager1.retrieve(topicName4);
			topic5 = manager1.retrieve(topicName5);
		}
		catch(IceStorm.NoSuchTopic e)
		{
			try
			{
				topic4 = manager1.create(topicName4);
				topic5 = manager1.create(topicName5);
			}
			catch(IceStorm.TopicExists ex)
			{
				System.err.println(appName() + ": temporary failure, try again.");
				return 1;
			}
		}

		try
		{
			topic1 = manager1.retrieve(topicName1);
			topic2 = manager1.retrieve(topicName2);
			topic3 = manager1.retrieve(topicName3);
		}
		catch(IceStorm.NoSuchTopic e)
		{
			try
			{
				topic1 = manager1.create(topicName1);
				topic2 = manager1.create(topicName2);
				topic3 = manager1.create(topicName3);
			}
			catch(IceStorm.TopicExists ex)
			{
				System.err.println(appName() + ": temporary failure, try again.");
				return 1;
			}
		}

		Ice.ObjectAdapter adapter1 = communicator().createObjectAdapter("ContextManagerLoc.Subscriber");
		Ice.ObjectAdapter adapter2 = communicator().createObjectAdapter("ContextManagerTemp.Subscriber");
		Ice.ObjectAdapter adapter3 = communicator().createObjectAdapter("ContextManagerUV.Subscriber");
		Ice.Identity subId1 = new Ice.Identity(id1, "");
		Ice.Identity subId2 = new Ice.Identity(id1, "");
		Ice.Identity subId3 = new Ice.Identity(id1, "");
		if(subId1.name == null)
		{
			subId1.name = java.util.UUID.randomUUID().toString();
			subId2.name = java.util.UUID.randomUUID().toString();
			subId3.name = java.util.UUID.randomUUID().toString();
		}
		Ice.ObjectPrx subscriber1 = adapter1.add(new LocationServerLocationStatusI(), subId1);
		Ice.ObjectPrx subscriber2 = adapter2.add(new TemperatureSensorI(), subId2);
		Ice.ObjectPrx subscriber3 = adapter3.add(new UVSensorI(), subId3);

		//
		// Activate the object adapter before subscribing.
		//
		adapter1.activate();
		adapter2.activate();
		adapter3.activate();

		//
		// Set up the proxy.
		//
		java.util.Map<String, String> qos = new java.util.HashMap<String, String>();
		subscriber1 = subscriber1.ice_oneway();

		try
		{
			topic1.subscribeAndGetPublisher(qos, subscriber1);
			topic2.subscribeAndGetPublisher(qos, subscriber2);
			topic3.subscribeAndGetPublisher(qos, subscriber3);
		}
		catch(IceStorm.AlreadySubscribed e)
		{
			// If we're manually setting the subscriber id ignore.

		}
		catch(IceStorm.BadQoS e)
		{
			e.printStackTrace();
			return 1;
		}

		Ice.ObjectPrx obj = communicator().stringToProxy("PreferenceMonitor: tcp -h localhost -p 20110");
		PreferenceMonitorPrx pmPrx = PreferenceMonitorPrxHelper.uncheckedCast(obj);
		ContextManager.this.prefPrx = pmPrx;

		Ice.ObjectPrx obj1 = communicator().stringToProxy("LocationServerRMI: tcp -h localhost -p 20310");
		LocationIndoorStatusPrx lsPrx = LocationIndoorStatusPrxHelper.uncheckedCast(obj1);
		ContextManager.this.locationPrx = lsPrx;

		Ice.ObjectAdapter adapter4 = communicator().createObjectAdapterWithEndpoints("ContextManagerRMI", "tcp -h localhost -p 20210");
		adapter4.add(new ContextManagerRMII(pmPrx), communicator().stringToIdentity("ContextManagerRMI"));
		adapter4.activate();

		Ice.ObjectPrx publisher = topic4.getPublisher();
		publisher = publisher.ice_oneway();
		Ice.ObjectPrx publisher1 = topic5.getPublisher();
		publisher1 = publisher1.ice_oneway();

		ContextManagerNotificationPrx notification = ContextManagerNotificationPrxHelper.uncheckedCast(publisher);
		CloseAllPrx close = CloseAllPrxHelper.uncheckedCast(publisher1);

		try
		{
			while(shutdown!=1)
			{
				if(usersLoggedIn!=0)
				{
					for(Map.Entry<String, String> entry : userTimer.entrySet()) {
						String userName = entry.getKey();
						String timer = entry.getValue();
						//System.out.println("Timer : " + timer);
						if(timer!=null && userSkinType.containsKey(userName))
						{   
							String uvindex = userUVReading.get(userName);
							String temperature = userTemp.get(userName);
							//String location = userLocation.get(userName);
							String skinType = userSkinType.get(userName);
							//String locationStatus = userLocationStatus.get(userName);
							int allowedTime = 0;
							switch(Integer.parseInt(uvindex))
							{
							case 1:
							case 2:
								allowedTime = 15; 
								break;
							case 3:
							case 4:
							case 5:
							case 6:
							case 7:
								allowedTime = 10;
								break;
							case 8:
							case 9:
							case 10:
							case 11:
								allowedTime = 5;
							}
							//            	    	
							//            	    	System.out.println("User temp reached value : " + userTempThresholdNotReached.get(userName));
							//            	    	System.out.println("User UV reached value : " + userUVThresholdNotReached.get(userName));

							//System.out.println(Integer.parseInt(timer));
							if(Integer.parseInt(timer) * Integer.parseInt(userUVThresholdNotReached.get(userName)) >= Integer.parseInt(skinType) * allowedTime)
							{
								//System.out.println(timer);
								String suggestion = ContextManager.this.prefPrx.getSuggestion(userName, "UVO", 0);
								String suggestionString = "";
								for(Map.Entry<String, locationDetails> entry1 : cityLocations.entrySet()) {
									String locationKey = entry1.getKey();
									locationDetails locDetails = entry1.getValue();
									if(locDetails.services.contains(suggestion))
									{
										if(ContextManager.this.locationPrx.getIndoorStatus(userName, locationKey) == 1)
										{
											suggestionString += locDetails.name;
											suggestionString += " ";
										}
									}
								}
								//System.out.println("UV Threshold Reached");
								userUVThresholdNotReached.put(userName, Integer.toString(0));
								userTimer.put(userName, Integer.toString(0));
								notification.getNotification(userName, "UVO Threshold Reached", suggestionString);
							}
							else if(Integer.parseInt(temperature) * Integer.parseInt(userTempThresholdNotReached.get(userName)) >= Integer.parseInt(userTemperatureThreshold.get(userName)))
							{
								//System.out.println("Current Temperature : " + temperature);
								//System.out.println("Threshold reached value " + userTempThresholdNotReached.get(userName));
								//System.out.println("User Temperature threshold value " + Integer.parseInt(userTemperatureThreshold.get(userName)));
								String suggestion = ContextManager.this.prefPrx.getSuggestion(userName, "", Integer.parseInt(temperature));
								String suggestionString = "";
								for(Map.Entry<String, locationDetails> entry1 : cityLocations.entrySet()) {
									String locationKey = entry1.getKey();
									locationDetails locDetails = entry1.getValue();
									if(locDetails.services.contains(suggestion))
									{
										if(ContextManager.this.locationPrx.getIndoorStatus(userName, locationKey) == 1)
										{
											suggestionString += locDetails.name;
											suggestionString += ",";
										}
									}
								}
								//System.out.println("Temperature threshold reached for : " + temperature);
								notification.getNotification(userName, "Temperature Threshold Reached", suggestionString);
								userTempThresholdNotReached.put(userName, Integer.toString(0));
							}

						}
					}

					try
					{
						Thread.currentThread().sleep(1000);
					}
					catch(java.lang.InterruptedException e)
					{
					}
				}
				else
				{
					System.out.println("Publishing Close All");
					close.shutdownAll(1);
					shutdown = 1;
					try
					{
						Thread.currentThread().sleep(1000);
						communicator().shutdown();
					}
					catch(java.lang.InterruptedException e)
					{
					}
				}
			}
		}
		catch(Ice.CommunicatorDestroyedException ex)
		{
			// Ignore
		}

		shutdownOnInterrupt();
		communicator().waitForShutdown();

		topic1.unsubscribe(subscriber1);
		topic2.unsubscribe(subscriber2);
		topic3.unsubscribe(subscriber3);

		return 0;
	}
	public static void main(String[] args)
	{
		ContextManager cm = new ContextManager("src\\Repository\\"+args[0]+".txt");
		int status = cm.main("ContextManager",args,"ContextManagerLoc.sub");
		System.exit(status);
	}
}


class locationDetails
{
	String name;
	String information;
	String services;
	public locationDetails(String name,String information,String services)
	{
		this.name = name;
		this.information = information;
		this.services = services;
	}
}