package com.example.beatacomapp.constants;

public class StatusMessages {
    private StatusMessages() {
    }

    public static final String ADD_ITEM_SUCCESS = "Item created successfull.";
    public static final String ADD_ITEM_FAILURE = "You have not provided an authentication token, the one provided has expired, was revoked or is not authentic.";
    public static final String GET_ITEMS_FAILURE = "You have not provided an authentication token, the one provided has expired, was revoked or is not authentic.";
    public static final String LOGIN_SUCCESS = "Authenticate with the platform.";
    public static final String LOGIN_FAILURE = "Wrong login or password! Please try again.";
    public static final String REGISTER_SUCCESS = "Register to the platform.";
    public static final String REGISTER_FAILURE = "Account with given username already exist! Please try again. ";
}
