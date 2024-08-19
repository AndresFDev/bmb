package com.example.bmb.utils;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.bmb.data.models.ChatMessageModel;

import java.util.ArrayList;
import java.util.List;

public class ChatDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "chat.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_CHATS = "chats";
    private static final String COLUMN_CHAT_ID = "chat_id";
    private static final String TABLE_MESSAGES = "messages";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_SENDER_ID = "sender_id";
    private static final String COLUMN_RECEIVER_ID = "receiver_id";
    private static final String COLUMN_MESSAGE = "message";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    public ChatDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createChatsTable = "CREATE TABLE " + TABLE_CHATS + " (" +
                COLUMN_CHAT_ID + " TEXT PRIMARY KEY)";
        db.execSQL(createChatsTable);

        String createMessagesTable = "CREATE TABLE " + TABLE_MESSAGES + " (" +
                COLUMN_ID + " TEXT PRIMARY KEY, " +
                COLUMN_CHAT_ID + " TEXT NOT NULL, " +
                COLUMN_SENDER_ID + " TEXT, " +
                COLUMN_RECEIVER_ID + " TEXT, " +
                COLUMN_MESSAGE + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER, " +
                "FOREIGN KEY(" + COLUMN_CHAT_ID + ") REFERENCES " + TABLE_CHATS + "(" + COLUMN_CHAT_ID + "))";
        db.execSQL(createMessagesTable);
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MESSAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CHATS);
        onCreate(db);
    }

    public void addChat(String chatId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CHAT_ID, chatId);

        db.insertWithOnConflict(TABLE_CHATS, null, values, SQLiteDatabase.CONFLICT_IGNORE);
        db.close();
    }

    public void addMessage(String chatId,ChatMessageModel message) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_ID, message.getId());
        values.put(COLUMN_CHAT_ID, chatId);
        values.put(COLUMN_SENDER_ID, message.getSenderId());
        values.put(COLUMN_RECEIVER_ID, message.getReceiverId());
        values.put(COLUMN_MESSAGE, message.getMessage());
        values.put(COLUMN_TIMESTAMP, message.getTimestamp());

        db.insertWithOnConflict(TABLE_MESSAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        db.close();
    }

    public List<ChatMessageModel> getMessagesByChatId(String chatId) {
        List<ChatMessageModel> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT * FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_CHAT_ID + " = ? ORDER BY " + COLUMN_TIMESTAMP + " ASC";
        Cursor cursor = db.rawQuery(query, new String[]{chatId});

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") ChatMessageModel message = new ChatMessageModel(
                        cursor.getString(cursor.getColumnIndex(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_SENDER_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_RECEIVER_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_MESSAGE)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                );
                messages.add(message);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return messages;
    }

    @SuppressLint("Range")
    public List<String> getChatIdsByUserId(String userId) {
        List<String> chatIds = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String query = "SELECT DISTINCT " + COLUMN_CHAT_ID + " FROM " + TABLE_MESSAGES + " WHERE " + COLUMN_SENDER_ID + " = ? OR " + COLUMN_RECEIVER_ID + " = ?";
        Cursor cursor = db.rawQuery(query, new String[]{userId, userId});

        if (cursor.moveToFirst()) {
            do {
                chatIds.add(cursor.getString(cursor.getColumnIndex(COLUMN_CHAT_ID)));
            } while (cursor.moveToNext());
        } else {
            Log.d("ChatDatabaseHelper", "No se encontraron mensajes para el usuario " + userId);
        }

        cursor.close();
        db.close();

        return chatIds;
    }


    public List<ChatMessageModel> getMessagesByUsers(String senderId, String receiverId) {
        List<ChatMessageModel> messages = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        if (senderId == null || receiverId == null) {
            Log.e("ChatDatabaseHelper", "senderId o receiverId es null. No se puede ejecutar la consulta.");
            return messages; // Retornar lista vacía o manejar según sea necesario
        }

        String selection = "(sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)";
        String[] selectionArgs = { senderId, receiverId, receiverId, senderId };

        Cursor cursor = db.query(TABLE_MESSAGES, null, selection, selectionArgs, null, null, COLUMN_TIMESTAMP);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") ChatMessageModel message = new ChatMessageModel(
                        cursor.getString(cursor.getColumnIndex(COLUMN_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_SENDER_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_RECEIVER_ID)),
                        cursor.getString(cursor.getColumnIndex(COLUMN_MESSAGE)),
                        cursor.getLong(cursor.getColumnIndex(COLUMN_TIMESTAMP))
                );
                messages.add(message);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return messages;
    }


    public void deleteMessage(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MESSAGES, COLUMN_ID + " =?", new String[]{id});
        db.close();
    }

}