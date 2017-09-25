package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import Ice.Current;
import UVSmart.*;

public class PreferenceMonitor extends Ice.Application{
	String fileName;
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

	public PreferenceMonitor(String fileName)
	{
		this.fileName = fileName;
	}

	//Implementing Preference Monitor RMI methods
	@SuppressWarnings("serial")
	class PreferenceMonitorI extends _PreferenceMonitorDisp
	{

		//Returns the skin type and temperature threshold
		@Override
		public userDetails getUserDetails(String userName, Current __current) {
			// TODO Auto-generated method stub
			userDetails thisUser = new userDetails();
			Scanner sc2 = null;
			try {
				sc2 = new Scanner(new File("src\\Repository\\"+PreferenceMonitor.this.fileName+".txt"));
			} catch (FileNotFoundException e) {
				e.printStackTrace();  
			}

			while (sc2.hasNextLine()) {
				Scanner s2 = new Scanner(sc2.nextLine());
				while (s2.hasNext()) {
					String s = s2.next();
					if (s.equals(userName))
					{
						if (sc2.hasNextLine()) {
							s2 = new Scanner(sc2.nextLine());
							while (s2.hasNext()) {
								s = s2.next();
								if (s.equals("Type:"))
								{
									String sType = s2.next();
									thisUser.skinType = Integer.parseInt(sType);
								}
							}
						}
						while (sc2.hasNextLine()) {
							s2 = new Scanner(sc2.nextLine());
							while (s2.hasNext()) {
								s = s2.next();
								if (s.equals("when"))
								{
									s = s2.next();
									if(!s.equals("UVO"))
										thisUser.tempThreshold = Integer.parseInt(s);
									while (sc2.hasNextLine()) {
										s2 = new Scanner(sc2.nextLine());
									}
								}
							}
						}

					}
				}

			}
			return thisUser;
		}


		//Test Method 
		@Override
		public String getQueryResult(String userName, String currentLocation, int queryType, String data,
				Current __current) {
			// TODO Auto-generated method stub
			System.out.println(userName);
			if(queryType == 1)
			{
				return "That's a nice place";
			}
			else
			{

				return "Swimming, Table Tennis";
			}
		}


		//Returns suggestion based on preference file entries
		@Override
		public String getSuggestion(String userName, String UVthreshold, int temperature, Current __current) {
			// TODO Auto-generated method stub
			if(UVthreshold.equalsIgnoreCase("UVO") && temperature == 0)
			{
				Scanner sc2 = null;
				try {
					sc2 = new Scanner(new File("src\\Repository\\"+PreferenceMonitor.this.fileName+".txt"));
				} catch (FileNotFoundException e) {
					e.printStackTrace();  
				}

				while (sc2.hasNextLine()) {
					Scanner s2 = new Scanner(sc2.nextLine());
					while (s2.hasNext()) {
						String s = s2.next();
						if (s.equals(userName))
						{
							if (sc2.hasNextLine()) {
								s2 = new Scanner(sc2.nextLine());
								while (s2.hasNext()) {
									s = s2.next();
									if (s.equals("UVO"))
									{
										String sType = s2.next();
										return s2.next();
									}
								}
							}
						}
					}

				}
			}
			else if(UVthreshold.equalsIgnoreCase("") && temperature != 0)
			{
				Scanner sc2 = null;
				try {
					sc2 = new Scanner(new File("src\\Repository\\"+PreferenceMonitor.this.fileName+".txt"));
				} catch (FileNotFoundException e) {
					e.printStackTrace();  
				}

				while (sc2.hasNextLine()) {
					Scanner s2 = new Scanner(sc2.nextLine());
					while (s2.hasNext()) {
						String s = s2.next();
						if (s.equals(userName))
						{
							if (sc2.hasNextLine()) {
								if (sc2.hasNextLine()) {
									s2 = new Scanner(sc2.nextLine());
									s2 = new Scanner(sc2.nextLine());
									s = s2.next();
									s = s2.next();
									s = s2.next();
									s = s2.next();
									return s2.next();
								}
							}
						}
					}

				}
			}
			else
			{
				return "";
			}

			return "";
		}
	}

	@Override
	public int run(String[] args) {
		// TODO Auto-generated method stub
		String topicName5 = "Shutdown";
		String id1 = null;
		IceStorm.TopicManagerPrx manager1 = IceStorm.TopicManagerPrxHelper.checkedCast(
				communicator().propertyToProxy("TopicManager.Proxy"));
		if(manager1 == null)
		{
			System.err.println("invalid proxy");
			return 1;
		}

		IceStorm.TopicPrx topic5;
		try
		{
			topic5 = manager1.retrieve(topicName5);
		}
		catch(IceStorm.NoSuchTopic e)
		{
			try
			{
				topic5 = manager1.create(topicName5);
			}
			catch(IceStorm.TopicExists ex)
			{
				System.err.println(appName() + ": temporary failure, try again.");
				return 1;
			}
		}

		Ice.ObjectAdapter adapter1 = communicator().createObjectAdapter("PreferenceMonitorSub.Subscriber");
		Ice.Identity subId1 = new Ice.Identity(id1, "");

		if(subId1.name == null)
		{
			subId1.name = java.util.UUID.randomUUID().toString();
		}
		Ice.ObjectPrx subscriber1 = adapter1.add(new CloseAllI(), subId1);

		adapter1.activate();

		java.util.Map<String, String> qos = new java.util.HashMap<String, String>();
		subscriber1 = subscriber1.ice_oneway();



		try
		{
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

		Ice.ObjectAdapter adapter = communicator().createObjectAdapterWithEndpoints("PreferenceMonitor", "tcp -h localhost -p 20110");
		adapter.add(new PreferenceMonitorI(), communicator().stringToIdentity("PreferenceMonitor"));
		adapter.activate();


		communicator().waitForShutdown();
		topic5.unsubscribe(subscriber1);
		return 0;
	}



	public static void main(String[] args)
	{
		PreferenceMonitor pr = new PreferenceMonitor(args[0]);
		int status = pr.main("PreferenceMonitor", args, "PreferenceMonitor.sub");
		System.exit(status);
	}
}
