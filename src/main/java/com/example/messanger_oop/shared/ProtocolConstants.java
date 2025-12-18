package com.example.messanger_oop.shared;

public class ProtocolConstants {
    // Разделители
    public static final char DELIMITER = '|';
    public static final char LIST_DELIMITER = ';';
    public static final char FIELD_DELIMITER = ':';

    // Команды клиента
    public static final String CMD_LOGIN = "LOGIN";
    public static final String CMD_REGISTER = "REGISTER";
    public static final String CMD_GET_CHATS = "GET_CHATS";
    public static final String CMD_SEND_MESSAGE = "SEND_MSG";
    public static final String CMD_CREATE_CHAT = "CREATE_CHAT";
    public static final String CMD_UPDATE_STATUS = "UPDATE_STATUS";
    public static final String CMD_GET_ONLINE_USERS = "GET_ONLINE_USERS";
    public static final String CMD_DELETE_CHAT = "DELETE_CHAT";
    public static final String CMD_LOGOUT = "LOGOUT";
    public static final String CMD_QUIT = "QUIT";

    // Ответы сервера
    public static final String RESP_AUTH_SUCCESS = "AUTH_OK";
    public static final String RESP_AUTH_FAILED = "AUTH_FAIL";
    public static final String RESP_OK = "OK";
    public static final String RESP_ERROR = "ERROR";
    public static final String RESP_CHAT_LIST = "CHAT_LIST";
    public static final String RESP_NEW_MESSAGE = "NEW_MSG";
    public static final String RESP_CHAT_CREATED = "CHAT_CREATED";
    public static final String RESP_CHAT_DELETED = "CHAT_DELETED";
    public static final String RESP_STATUS_UPDATE = "STATUS_UPDATE";
    public static final String RESP_ONLINE_USERS = "ONLINE_USERS";

    // Статусы пользователей
    public static final String USER_STATUS_ONLINE = "ONLINE";
    public static final String USER_STATUS_OFFLINE = "OFFLINE";
    public static final String USER_STATUS_AWAY = "AWAY";
    public static final String USER_STATUS_DND = "DND";
    public static final String USER_STATUS_INVISIBLE = "INVISIBLE";
}