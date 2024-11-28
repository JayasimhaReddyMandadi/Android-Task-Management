package com.example.myapp;

public class Constants {
    // Base URL
    public static final String BASE_URL = "http://172.16.20.92:8000/api";

    // SharedPreferences
    public static final String SHARED_PREFS_NAME = "MyAppPrefs";
    public static final String ACCESS_TOKEN_KEY = "access_token";
    public static final String REFRESH_TOKEN_KEY = "refresh_token";  // Added this line

    // API Endpoints
    public static final String ENDPOINT_LOGIN = "/login/";
    public static final String ENDPOINT_REGISTER = "/register/";
    public static final String ENDPOINT_PROFILE = "/profile/";
    public static final String ENDPOINT_UPDATE_PHOTO = "/profile/update-photo/";
    public static final String ENDPOINT_UPDATE_USERNAME = "/profile/update-username/";
    public static final String ENDPOINT_UPDATE_PASSWORD = "/profile/update-password/";
    public static final String ENDPOINT_TASKS = "/tasks/list/";
    public static final String ENDPOINT_CREATE_TASK = "/tasks/create/";
    public static final String ENDPOINT_UPDATE_TASK = "/tasks/update/";
    public static final String ENDPOINT_DELETE_TASK = "/tasks/delete/";

    // Request codes
    public static final int PICK_IMAGE_REQUEST = 100;

    // Headers
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    // Date format
    public static final String DATE_FORMAT = "yyyy-MM-dd";
}