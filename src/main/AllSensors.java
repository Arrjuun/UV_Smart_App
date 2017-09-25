package main;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import Ice.Current;
import UVSmart.*;

public class AllSensors extends Ice.Application{
	private String userName;
	private static int shutdown = 0;
	public AllSensors(String uName)
	{
		this.userName = uName;
	}

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

	@SuppressWarnings("static-access")
	@Override
	public int run(String[] args) {
		// TODO Auto-generated method stub
		//
		String topicName = "temperatureSensor"; // Temperature sensor topic
		String topicName1 = "UVSensor"; // UV sensor topic
		String topicName2 = "locationSensor"; // Location sensor topic
		String topicName3 = "Shutdown"; // Shutdown sensor topic
		String id1 = null;
		IceStorm.TopicManagerPrx manager = IceStorm.TopicManagerPrxHelper.checkedCast(
				communicator().propertyToProxy("TopicManager.Proxy"));
		if(manager == null)
		{
			System.err.println("invalid proxy");
			return 1;
		}
		IceStorm.TopicPrx topic;
		IceStorm.TopicPrx topic1;
		IceStorm.TopicPrx topic2;
		IceStorm.TopicPrx topic3;
		try
		{
			topic = manager.retrieve(topicName);
			topic1 = manager.retrieve(topicName1);
			topic2 = manager.retrieve(topicName2);
			topic3 = manager.retrieve(topicName3);
		}
		catch(IceStorm.NoSuchTopic e)
		{
			try
			{
				topic = manager.create(topicName);
				topic1 = manager.create(topicName1);
				topic2 = manager.create(topicName2);
				topic3 = manager.create(topicName3);
			}
			catch(IceStorm.TopicExists ex)
			{
				System.err.println(appName() + ": temporary failure, try again.");
				return 1;
			}
		}


		Ice.ObjectAdapter adapter1 = communicator().createObjectAdapter("AllSensors.Subscriber");
		Ice.Identity subId1 = new Ice.Identity(id1, "");
		if(subId1.name == null)
		{
			subId1.name = java.util.UUID.randomUUID().toString();
		}
		Ice.ObjectPrx subscriber1 = adapter1.add(new CloseAllI(), subId1);
		adapter1.activate();

		//
		// Set up the proxy.
		//
		java.util.Map<String, String> qos = new java.util.HashMap<String, String>();
		subscriber1 = subscriber1.ice_oneway();

		try
		{
			topic3.subscribeAndGetPublisher(qos, subscriber1);
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

		//
		// Get the topic's publisher object, and create a Clock proxy with
		// the mode specified as an argument of this application.
		//
		Ice.ObjectPrx publisher = topic.getPublisher();
		publisher = publisher.ice_oneway();
		Ice.ObjectPrx publisher1 = topic1.getPublisher();
		publisher1 = publisher1.ice_oneway();
		Ice.ObjectPrx publisher2 = topic2.getPublisher();
		publisher2 = publisher2.ice_oneway();

		TemperatureSensorPrx temperatureData = TemperatureSensorPrxHelper.uncheckedCast(publisher);
		LocationSensorPrx locationData = LocationSensorPrxHelper.uncheckedCast(publisher2);
		UVSensorPrx uvDataData = UVSensorPrxHelper.uncheckedCast(publisher1);

		System.out.println("Publishing SensorData for User : " + this.userName);
		try
		{
			int time=0;
			int[] tempArray = new int[2000];
			int[] uvArray = new int[2000];
			String[] locArray = new String[2000];
			int numUVReadings = 0;
			int numLocReadings = 0;
			int numTempReadings = 0;
			try(BufferedReader br = new BufferedReader(new FileReader("src\\Repository\\"+this.userName+"Temperature.txt"))) {
				for(String line; (line = br.readLine()) != null;) {
					String[] ar=line.split(",");
					for(int c=0;c < Integer.parseInt(ar[1]);c++)
					{
						tempArray[numTempReadings]=Integer.parseInt(ar[0]);
						numTempReadings++;
					}
				}
				// line is not visible here.
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				System.out.println("File not found Exception");
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				System.out.println("IO Exception");
				e1.printStackTrace();
			}
			try(BufferedReader br = new BufferedReader(new FileReader("src\\Repository\\"+this.userName+"Location.txt"))) {
				for(String line; (line = br.readLine()) != null;) {
					String[] ar=line.split(",");
					for(int c=0;c < Integer.parseInt(ar[1]);c++)
					{
						locArray[numLocReadings]=ar[0];
						numLocReadings++;
					}
				}
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				System.out.println("File not found Exception");
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				System.out.println("IO Exception");
				e1.printStackTrace();
			}
			try(BufferedReader br = new BufferedReader(new FileReader("src\\Repository\\"+this.userName+"UVR.txt"))) {
				for(String line; (line = br.readLine()) != null;) {
					String[] ar=line.split(",");
					for(int c=0;c < Integer.parseInt(ar[1]);c++)
					{
						uvArray[numUVReadings]=Integer.parseInt(ar[0]);
						numUVReadings++;
					}
				}
				// line is not visible here.
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				System.out.println("File not found Exception");
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				System.out.println("IO Exception");
				e1.printStackTrace();
			}
			while(shutdown!=1)
			{
				locationData.getLocationSensorReading(this.userName, "LocationSensor", locArray[time%numLocReadings]);
				temperatureData.getTemperature(this.userName, "Temperature", tempArray[time%numTempReadings]);
				uvDataData.getUVIndex(this.userName, "UV", uvArray[time%numUVReadings]);
				time++;
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

		topic3.unsubscribe(subscriber1);

		return 0;
	}
	public static void main(String[] args)
	{
		AllSensors as = new AllSensors(args[0]);
		int status = as.main("AllSensors", args, "AllSensors.sub");
		System.exit(status);
	}
}
