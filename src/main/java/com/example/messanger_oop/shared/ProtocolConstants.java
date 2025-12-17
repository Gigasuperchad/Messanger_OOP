package com.example.messanger_oop.shared;

public class ProtocolConstants {
    // Команды от клиента к серверу
    public static final String CMD_LOGIN = "LOGIN";
    public static final String CMD_REGISTER = "REGISTER";
    public static final String CMD_LOGOUT = "LOGOUT";
    public static final String CMD_GET_CHATS = "GET_CHATS";
    public static final String CMD_GET_CHAT_INFO = "GET_CHAT_INFO";
    public static final String CMD_SEND_MESSAGE = "SEND_MESSAGE";
    public static final String CMD_CREATE_CHAT = "CREATE_CHAT";
    public static final String CMD_DELETE_CHAT = "DELETE_CHAT";
    public static final String CMD_EDIT_MESSAGE = "EDIT_MESSAGE";
    public static final String CMD_UPDATE_STATUS = "UPDATE_STATUS";
    public static final String CMD_GET_ONLINE_USERS = "GET_ONLINE_USERS";
    public static final String CMD_QUIT = "QUIT";

    // Ответы от сервера к клиенту
    public static final String RESP_OK = "OK";
    public static final String RESP_ERROR = "ERROR";
    public static final String RESP_AUTH_SUCCESS = "AUTH_SUCCESS";
    public static final String RESP_AUTH_FAILED = "AUTH_FAILED";
    public static final String RESP_CHAT_LIST = "CHAT_LIST";
    public static final String RESP_CHAT_INFO = "CHAT_INFO";
    public static final String RESP_NEW_MESSAGE = "NEW_MESSAGE";
    public static final String RESP_MESSAGE_EDITED = "MESSAGE_EDITED";
    public static final String RESP_CHAT_CREATED = "CHAT_CREATED";
    public static final String RESP_CHAT_DELETED = "CHAT_DELETED";
    public static final String RESP_STATUS_UPDATE = "STATUS_UPDATE";
    public static final String RESP_ONLINE_USERS = "ONLINE_USERS";
    public static final String RESP_NOTIFICATION = "NOTIFICATION";

    // Разделители
    public static final String DELIMITER = "|";
    public static final String LIST_DELIMITER = ";";
    public static final String FIELD_DELIMITER = ":";

    // Статусы сообщений
    public static final String MSG_STATUS_SENDING = "SENDING";
    public static final String MSG_STATUS_SENT = "SENT";
    public static final String MSG_STATUS_DELIVERED = "DELIVERED";
    public static final String MSG_STATUS_READ = "READ";
    public static final String MSG_STATUS_FAILED = "FAILED";

    // Статусы пользователей
    public static final String USER_STATUS_ONLINE = "ONLINE";
    public static final String USER_STATUS_OFFLINE = "OFFLINE";
    public static final String USER_STATUS_AWAY = "AWAY";
    public static final String USER_STATUS_DND = "DND";
    public static final String USER_STATUS_INVISIBLE = "INVISIBLE";

    // Форматы дат
    public static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
}