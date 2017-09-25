package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import Ice.Current;
import UVSmart.*;

public class LocationServer extends Ice.Application{
	private HashMap<String, String> userLocations = new HashMap<String, String>(); // for user's current location
	private List<String> indoorLocations = new ArrayList<String>(); //for location status
	private static int shutdown = 0;


	// Implementing shutdown subscription method to subscribe to context manager
	@SuppressWarnings("serial")
	class CloseAllI extends _CloseAllDisp
	{

		@Override
		public void shutdownAll(int shutdownYes, Current __current) {
			// TODO Auto-generated method stub
			System.out.println("Exit Status : " + shutdownYes);
			shutdown = 1;
			communicator().shutdown();
		}

	}

	// Implementing location server RMI method
	@SuppressWarnings("serial")
	class LocationIndoorStatusI extends _LocationIndoorStatusDisp
	{

		@Override
		public int getIndoorStatus(String userName, String location, Current __current) {
			// TODO Auto-generated method stub
			if(indoorLocations.contains(location))
				return 1;
			else
				return 0;
		}

	}

	public LocationServer(String fileName)
	{
		Scanner sc2 = null;
		try {
			sc2 = new Scanner(new File("src\\Repository\\"+fileName+".txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();  
		}
		while (sc2.hasNextLine()) {
			Scanner s2 = new Scanner(sc2.nextLine());
			while (s2.hasNext()) {
				String s = s2.next();
				if (s.equals("Indoor"))
				{
					s = s2.next();
					s = s2.next();
					for (String ind: s.split(",")) {
						indoorLocations.add(ind);
					}
					break;
				}
			}
		}

	}


	//Implementing location sensor subscription
	@SuppressWarnings("serial")
	class LocationSensorI extends _LocationSensorDisp
	{

		@Override
		public void getLocationSensorReading(String userName, String sensorType, String locationSensorReading,
				Current __current) {
			// TODO Auto-generated method stub
			userLocations.put(userName, locationSensorReading);
			//System.out.println("userName : " + userName + " Location " + userLocations.get(userName));
		}

	}

	@SuppressWarnings("static-access")
	@Override
	public int run(String[] args) {
		// TODO Auto-generated method stub
		String topicName = "locationSensor";
		String topicName1 = "locationStatus";
		String topicName5 = "Shutdown";
		String id = null;
		IceStorm.TopicManagerPrx manager = IceStorm.TopicManagerPrxHelper.checkedCast(
				communicator().propertyToProxy("TopicManager.Proxy"));
		if(manager == null)
		{
			System.err.println("invalid proxy");
			return 1;
		}


		IceStorm.TopicPrx topic;
		IceStorm.TopicPrx topic1;
		IceStorm.TopicPrx topic5;
		try
		{
			topic = manager.retrieve(topicName);
		}
		catch(IceStorm.NoSuchTopic e)
		{
			try
			{
				topic = manager.create(topicName);
			}
			catch(IceStorm.TopicExists ex)
			{
				System.err.println(appName() + ": temporary failure, try again.");
				return 1;
			}
			try
			{
				topic = manager.create(topicName);
			}
			catch(IceStorm.TopicExists ex)
			{
				System.err.println(appName() + ": temporary failure, try again.");
				return 1;
			}

		}

		try
		{
			topic1 = manager.retrieve(topicName1);
			topic5 = manager.retrieve(topicName5);
		}
		catch(IceStorm.NoSuchTopic e)
		{
			try
			{
				topic1 = manager.create(topicName1);
				topic5 = manager.create(topicName5);
			}
			catch(IceStorm.TopicExists ex)
			{
				System.err.println(appName() + ": temporary failure, try again.");
				return 1;
			}

		}


		Ice.ObjectAdapter adapter = communicator().createObjectAdapter("LocationServer.Subscriber");
		Ice.Identity subId = new Ice.Identity(id, "");
		Ice.ObjectAdapter adapter1 = communicator().createObjectAdapter("LocationServerShutdown.Subscriber");
		Ice.Identity subId1 = new Ice.Identity(id, "");
		if(subId.name == null)
		{
			subId.name = java.util.UUID.randomUUID().toString();
			subId1.name = java.util.UUID.randomUUID().toString();
		}
		Ice.ObjectPrx subscriber = adapter.add(new LocationSensorI(), subId);
		Ice.ObjectPrx subscriber1 = adapter1.add(new CloseAllI(), subId);

		//
		// Activate the object adapter before subscribing.
		//
		adapter.activate();
		adapter1.activate();


		//
		// Set up the proxy.
		//
		java.util.Map<String, String> qos = new java.util.HashMap<String, String>();
		subscriber = subscriber.ice_oneway();
		// Subscribe then publish
		try
		{
			topic.subscribeAndGetPublisher(qos, subscriber);
			topic5.subscribeAndGetPublisher(qos, subscriber1);
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


		Ice.ObjectPrx publisher = topic1.getPublisher();
		publisher = publisher.ice_oneway();

		LocationServerLocationStatusPrx locationData = LocationServerLocationStatusPrxHelper.uncheckedCast(publisher);

		Ice.ObjectAdapter adapter3 = communicator().createObjectAdapterWithEndpoints("LocationServerRMI", "tcp -h localhost -p 20310");
		adapter3.add(new LocationIndoorStatusI(), communicator().stringToIdentity("LocationServerRMI"));
		adapter3.activate();



		try
		{
			while(shutdown!=1)
			{
				for(Map.Entry<String, String> entry : userLocations.entrySet()) {
					String userName = entry.getKey();
					String currentLocation = entry.getValue();
					if(indoorLocations.contains(currentLocation))
						locationData.getCurrentLocationStatus(userName, "Indoor", currentLocation);
					else
						locationData.getCurrentLocationStatus(userName, "Outdoor", currentLocation);
					// do what you have to do here
					// In your case, an other loop.
				}

				try
				{
					Thread.currentThread().sleep(1000);
				}
				catch(java.lang.InterruptedException e)
				{
				}
			}
		}
		catch(Ice.CommunicatorDestroyedException ex)
		{
			// Ignore
		}

		shutdownOnInterrupt();
		communicator().waitForShutdown();

		topic.unsubscribe(subscriber);

		return 0;
	}

	public static void main(String[] args)
	{
		LocationServer ls = new LocationServer(args[0]);
		int status = ls.main("LocationServer", args,"LocationServer.sub");
		System.exit(status);
	}

}
