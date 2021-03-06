package com.unwheeze.database;

public class DbScheme {

    public final static int _VERSION = 1;

    public final static String DB = "Unwheeze";

    public final static String _AIRDATA = "airData";
    public final static String AIRDATA_ID = "id";
    public final static String AIRDATA_PM2 = "pm25";
    public final static String AIRDATA_PM10 = "pm10";
    public final static String AIRDATA_PM1 = "pm1";
    public final static String AIRDATA_LOCATION = "location";
    public final static String AIRDATA_USERID = "userID";
    public final static String AIRDATA_DATETIME = "datetime";
    public final static String AIRDATA_GEOLOCATION = "geolocation";

    public final static String _USERS = "users";
    public final static String USERS_ID ="id";
    public final static String USERS_FIRSTNAME = "firstName";
    public final static String USERS_LASTNAME = "lastName";
    public final static String USERS_IMG = "imgUrl";
    public final static String USERS_EMAIL = "email";
    public final static String USERS_SCOPE = "scope";
    public final static String USERS_LOCATION = "location";
    public final static String USERS_CITY = "city";
    public final static String USERS_PWD = "pwd";
    public final static String USERS_SALT = "salt";

    public final static String _WSAUTH = "wsauth";
    public final static String WSAUTH_ID = "id";
    public final static String WSAUTH_KEY = "key";
}
