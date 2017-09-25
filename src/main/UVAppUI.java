package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import Ice.Current;
import UVSmart.*;

public class UVAppUI extends Ice.Application{

	private String userName = ""; // To store the UserName
	private ContextManagerRMIPrx cmprx; // For Context Manager RMI Calls
	BufferedReader reader = new BufferedReader(new InputStreamReader(System.in)); // To read user Input


	// Subscription for Context Manager notification method implementation
	// Prints the warning messages with suggestions
	@SuppressWarnings("serial")
	class ContextManagerNotificationI extends _ContextManagerNotificationDisp
	{

		@Override
		public void getNotification(String userName, String notification, String suggestion, Current __current) {
			Thread thread1 = new Thread ("MenuThread") {
				public void run () {
					try {
						UVAppUI.this.appMenu();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			if(UVAppUI.this.userName.equals(""))
			{

			}
			else if(UVAppUI.this.userName.equals(userName))
			{
				System.out.println("Warning !!! " + notification);
				System.out.println("Suggestions : " + suggestion);
				if(thread1.isAlive())
				{
					System.out.println("In Notification");
					thread1.interrupt();
				}
				System.out.println("Starting In Notification");
				thread1.start();
			}
		}

	}

	@Override
	public int run(String[] args) {
		// TODO Auto-generated method stub
		String topicName1 = "ContextManagerNotification"; //Topic for Context Manager notification
		String id1 = null;
		IceStorm.TopicManagerPrx manager1 = IceStorm.TopicManagerPrxHelper.checkedCast(
				communicator().propertyToProxy("TopicManager.Proxy"));
		if(manager1 == null)
		{
			System.err.println("invalid proxy");
			return 1;
		}


		IceStorm.TopicPrx topic1;
		try
		{
			topic1 = manager1.retrieve(topicName1);
		}
		catch(IceStorm.NoSuchTopic e)
		{
			try
			{
				topic1 = manager1.create(topicName1);
			}
			catch(IceStorm.TopicExists ex)
			{
				System.err.println(appName() + ": temporary failure, try again.");
				return 1;
			}
		}

		Ice.ObjectAdapter adapter1 = communicator().createObjectAdapter("UVAppUI.Subscriber");
		Ice.Identity subId1 = new Ice.Identity(id1, "");
		if(subId1.name == null)
		{
			subId1.name = java.util.UUID.randomUUID().toString();
		}
		Ice.ObjectPrx subscriber1 = adapter1.add(new ContextManagerNotificationI(), subId1);

		//
		// Activate the object adapter before subscribing.
		//
		adapter1.activate();

		//
		// Set up the proxy.
		//
		java.util.Map<String, String> qos = new java.util.HashMap<String, String>();
		subscriber1 = subscriber1.ice_oneway();

		try
		{
			topic1.subscribeAndGetPublisher(qos, subscriber1);
		}
		catch(IceStorm.AlreadySubscribed e)
		{

		}
		catch(IceStorm.BadQoS e)
		{
			e.printStackTrace();
			return 1;
		}


		Ice.ObjectPrx obj = communicator().stringToProxy("ContextManagerRMI: tcp -h localhost -p 20210");
		UVAppUI.this.cmprx = ContextManagerRMIPrxHelper.checkedCast(obj);

		shutdownOnInterrupt();

		try{
			appFirstDisp();
		}
		catch(IOException ie)
		{
			System.out.println("IO Exception : ");
		}
		communicator().waitForShutdown();
		topic1.unsubscribe(subscriber1);

		return 0;
	}


	public static void main(String[] args) throws IOException
	{
		UVAppUI ui = new UVAppUI();
		int status = ui.main("UVAppUI", args, "UVAppUI.sub");
		System.exit(status);
	}

	// Function to display welcome message and login
	public void appFirstDisp() throws IOException
	{
		System.out.println("Welcome to UVSmart");
		System.out.println("Please Enter your user name:");
		this.userName = reader.readLine().trim();
		System.out.println(this.userName);
		if(UVAppUI.this.cmprx.login(userName) == 0)
		{
			System.out.println("User Does Not Exist ! Please contact the administrator");
			System.exit(1);
		}
		Thread thread1 = new Thread ("MenuThread") {
			public void run () {
				try {
					UVAppUI.this.appMenu();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		if(thread1.isAlive())
		{
			thread1.interrupt();
		}
		thread1.start();
	}

	// Function to display and process menu
	public void appMenu() throws IOException
	{
		Thread thread1 = new Thread ("MenuThread") {
			public void run () {
				try {
					UVAppUI.this.appMenu();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		System.out.println("Context-aware UV Smart Application Main Menu");
		System.out.println("Please select an option : ");
		System.out.println("1. Search for information on a specific item of interest ");
		System.out.println("2. Search for items of interest in current location ");
		System.out.println("E. Exit ");
		System.out.flush();
		String selection = reader.readLine();
		System.out.println("Your selection : " + selection);
		int option = 10;
		if(selection.equals("E") || selection.equals("e"))
			option = 3;
		else if(selection.equals(Integer.toString(1)))
			option = 1;
		else if(selection.equals(Integer.toString(2)))
			option = 2;
		else
			option = 10;
		switch(option)
		{
		case 1:
			System.out.println("Please enter name of item of interest : ");
			BufferedReader reader1 = new BufferedReader(new InputStreamReader(System.in));
			String requestData = reader1.readLine();
			System.out.println(UVAppUI.this.cmprx.getRequestedDetails(this.userName, 1, requestData));
			if(thread1.isAlive())
			{
				thread1.interrupt();
			}
			thread1.start();
			break;
		case 2:
			System.out.println(UVAppUI.this.cmprx.getRequestedDetails(this.userName, 2, ""));
			if(thread1.isAlive())
			{
				thread1.interrupt();
			}
			thread1.start();
			break;
		case 3:
			UVAppUI.this.cmprx.logout(userName);
			System.exit(1);
			break;
		default:
			System.out.println("Invalid Selection ! Please choose correct option");
			if(thread1.isAlive())
			{
				thread1.interrupt();
			}
			thread1.start();
			break;
		}
	}
}
