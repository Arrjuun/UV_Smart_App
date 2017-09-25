module UVSmart
{
	interface TemperatureSensor
	{
		void getTemperature(string userName, string sensorType, int temperatureReading);
	};
	interface UVSensor
	{
		void getUVIndex(string userName, string sensorType, int uvIndexReading);
	};
	interface LocationSensor
	{
		void getLocationSensorReading(string userName, string sensorType, string locationSensorReading);
	};
	interface LocationServerLocationStatus
	{
		void getCurrentLocationStatus(string userName, string currentLocationStatus, string locationSensorReading);
	};
	interface LocationServerIndoorLocations
	{
		string getIndoorLocations(); 
	};
	struct userDetails
	{
		int skinType;
		int tempThreshold;
	};
	interface PreferenceMonitor
	{
		userDetails getUserDetails(string userName);
		string getQueryResult(string userName, string currentLocation, int queryType, string data);
		string getSuggestion(string userName, string UVthreshold, int temperature);
	};
	interface ContextManagerNotification
	{
		void getNotification(string userName, string notification, string suggestion);
	};
	interface ContextManagerRMI
	{
		string getRequestedDetails(string userName, int requestType, string requestData);
		int login(string userName);
		void logout(string userName);
	};
	interface LocationIndoorStatus
	{
		int getIndoorStatus(string userName, string location);
	};
	interface CloseAll
	{
		void shutdownAll(int shutdownYes);
	};
};