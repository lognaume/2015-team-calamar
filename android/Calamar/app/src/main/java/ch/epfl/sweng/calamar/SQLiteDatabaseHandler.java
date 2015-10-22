package ch.epfl.sweng.calamar;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class SQLiteDatabaseHandler extends SQLiteOpenHelper {

    //TODO add support for other items (now assuming only SimpleTextItem)

    private final Context context;

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "CalamarDB";

    private static final String ITEMS_TABLE = "tb_Items";
    private static final String ITEMS_KEY_ID = "id";
    private static final String ITEMS_KEY_TEXT = "text";
    private static final String ITEMS_KEY_FROM = "from";
    private static final String ITEMS_KEY_TO = "to";
    private static final String ITEMS_KEY_TIME = "time";
    private static final String[] ITEMS_COLUMNS = {ITEMS_KEY_ID, ITEMS_KEY_TEXT, ITEMS_KEY_FROM, ITEMS_KEY_TO, ITEMS_KEY_TIME};

    private static final String RECIPIENTS_TABLE = "tb_Recipients";
    private static final String RECIPIENTS_KEY_ID = "id";
    private static final String RECIPIENTS_KEY_NAME = "name";
    private static final String[] RECIPIENTS_COLUMN = {RECIPIENTS_KEY_ID, RECIPIENTS_KEY_NAME};

    /**
     * Create a databasehandler for managing stored informations on the user phone.
     * @param context The context of the application
     */
    public SQLiteDatabaseHandler(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context=context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String createMessagesTable = "CREATE TABLE " + ITEMS_TABLE + " ( "
                + ITEMS_KEY_ID + " INTEGER PRIMARY, " + ITEMS_KEY_TEXT + " TEXT, " + ITEMS_KEY_FROM + " INTEGER, " + ITEMS_KEY_TO + " INTEGER, " + ITEMS_KEY_TIME + " INTEGER )";
        db.execSQL(createMessagesTable);
        final String createRecipientsTable = "CREATE TABLE " + RECIPIENTS_TABLE + " ( " + RECIPIENTS_KEY_ID + " INTEGER PRIMARY, " + RECIPIENTS_KEY_NAME + " TEXT )";
        db.execSQL(createRecipientsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TODO does nothing at the moment
    }

    /**
     * Deletes the database
     */
    public void deleteDatabase(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.rawQuery("DROP TABLE IF EXISTS " + ITEMS_TABLE, null);
        db.rawQuery("DROP TABLE IF EXISTS " + RECIPIENTS_TABLE, null);
        this.close();
        context.deleteDatabase(DATABASE_NAME);
    }

    /**
     * Drops the Recipient table
     */
    public void dropRecipientTable(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.rawQuery("DROP TABLE IF EXISTS " + RECIPIENTS_TABLE, null);
    }

    /**
     * Drops the Item table
     */
    public void dropItemTable(){
        SQLiteDatabase db =this.getWritableDatabase();
        db.rawQuery("DROP TABLE IF EXISTS "+ITEMS_TABLE,null);
    }

    /**
     * Deletes all items in the database
     */
    public void deleteAllItems() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(ITEMS_TABLE, null, null);
    }

    /**
     *
     * Deletes a item given itself
     * @param message the item to delete
     */
    public void deleteItem(Item message) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(ITEMS_TABLE, ITEMS_KEY_ID + " = " + message.getID(), null);
    }

    /**
     *
     * Deletes an item given an id
     * @param id the id of the item
     */
    public void deleteItem(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(ITEMS_TABLE, ITEMS_KEY_ID + " = " + id, null);
    }

    /**
     *
     * Deletes several items
     * @param ids the ids of the items to delete
     */
    public void deleteItems(List<Integer> ids) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (Integer id : ids) {
            db.delete(ITEMS_TABLE, ITEMS_KEY_ID + " = " + id, null);
        }
    }

    /**
     *
     * Adds an item
     * @param item the item to add
     */
    public void addItem(Item item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", item.getID());
        values.put("text", ((SimpleTextItem) item).getMessage());
        values.put("from", item.getFrom().getID());
        values.put("to", item.getTo().getID());
        db.replace(ITEMS_TABLE, null, values);
    }

    /**
     *
     * Adds all items given a list
     * @param items the list of items to add
     */
    public void addItems(List<Item> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (Item item : items) {
            ContentValues values = new ContentValues();
            values.put("id", item.getID());
            values.put("text", ((SimpleTextItem) item).getMessage());
            values.put("from", item.getFrom().getID());
            values.put("to", item.getTo().getID());
            db.replace(ITEMS_TABLE, null, values);
        }
    }


    /**
     * Updates an item
     * @param item the item to update
     */
    public void updateItem(Item item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", item.getID());
        values.put("text", ((SimpleTextItem) item).getMessage());
        values.put("from", item.getFrom().getID());
        values.put("to", item.getTo().getID());
        db.update(ITEMS_TABLE, values, ITEMS_KEY_ID + " = " + item.getID(), null);
    }

    /**
     *
     * Updates several items given a list
     * @param items the list of items to update
     */
    public void updateItems(List<Item> items) {
        SQLiteDatabase db = this.getWritableDatabase();
        for (Item item : items) {
            ContentValues values = new ContentValues();
            values.put("id", item.getID());
            values.put("text", ((SimpleTextItem) item).getMessage());
            values.put("from", item.getFrom().getID());
            values.put("to", item.getTo().getID());
            db.update(ITEMS_TABLE, values, ITEMS_KEY_ID + " = " + item.getID(), null);
        }
    }

    /**
     *
     * Returns an item given an id
     * @param id the id of the item to retrieve
     * @return the item, or null
     */
    public Item getItem(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(ITEMS_TABLE, ITEMS_COLUMNS, ITEMS_KEY_ID + " = " + id, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {

            String text = cursor.getString(1);
            User from = (User) getRecipient(cursor.getInt(2));
            Recipient to = getRecipient(cursor.getInt(3));
            Date time = new Date(cursor.getInt(4));
            cursor.close();
            return new SimpleTextItem(id, from, to, time, text);
        }
        return null;
    }

    /**
     *
     * Returns a list of items given a list of ids
     * @param ids the ids corresponding to the items
     * @return the items
     */
    public List<Item> getMessages(List<Integer> ids){
        SQLiteDatabase db = this.getReadableDatabase();
        List<Item> items = new ArrayList<>();
        for (Integer id : ids){
        Cursor cursor = db.query(ITEMS_TABLE, ITEMS_COLUMNS, ITEMS_KEY_ID + " = " + id, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {

            String text = cursor.getString(1);
            User from = (User) getRecipient(cursor.getInt(2));
            Recipient to = getRecipient(cursor.getInt(3));
            Date time = new Date(cursor.getInt(4));
            cursor.close();
            items.add(new SimpleTextItem(id, from, to, time, text));
        }
        }
        return items;
    }

    /**
     *
     * Returns all items
     * @return the list of Item
     */
    public List<Item> getAllItems() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + ITEMS_TABLE, null);
        boolean hasNext = true;
        List<Item> items = new ArrayList<>();
        if (cursor != null) {
            hasNext = cursor.moveToFirst();
            while (hasNext) {
                int id = cursor.getInt(0);
                String text = cursor.getString(1);
                User from = (User) getRecipient(cursor.getInt(2));
                Recipient to = getRecipient(cursor.getInt(3));
                Date time = new Date(cursor.getInt(4));
                items.add(new SimpleTextItem(id, from, to, time, text));
                hasNext = cursor.moveToNext();
            }
            cursor.close();
        }
        return items;
    }

    /**
     *
     * Adds a Recipient to the database
     * @param recipient the recipient to add
     */
    public void addRecipient(Recipient recipient) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(RECIPIENTS_KEY_ID, recipient.getID());
        values.put(RECIPIENTS_KEY_NAME, recipient.getName());
        db.replace(RECIPIENTS_TABLE, null, values);
    }

    /**
     *
     * Adds several recipients
     * @param recipients the list of recipients to add.
     */
    public void addRecipients(List<Recipient> recipients){
        SQLiteDatabase db = this.getWritableDatabase();
        for (Recipient recipient : recipients){
            ContentValues values = new ContentValues();
            values.put(RECIPIENTS_KEY_ID, recipient.getID());
            values.put(RECIPIENTS_KEY_NAME, recipient.getName());
            db.replace(RECIPIENTS_TABLE, null, values);
        }
    }

    /**
     *
     * Updates a recipient
     * @param recipient the recipient to update
     */
    public void updateRecipient(Recipient recipient) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(RECIPIENTS_KEY_ID, recipient.getID());
        values.put(RECIPIENTS_KEY_NAME, recipient.getName());
        db.update(RECIPIENTS_TABLE, values, RECIPIENTS_KEY_ID + " = " + recipient.getID(), null);
    }

    /**
     *
     * Updates all recipients given in the list
     * @param recipients the list of recipients to update
     */
    public void updateRecipients(List<Recipient> recipients){
        SQLiteDatabase db = this.getWritableDatabase();
        for (Recipient recipient : recipients){
            ContentValues values = new ContentValues();
            values.put(RECIPIENTS_KEY_ID, recipient.getID());
            values.put(RECIPIENTS_KEY_NAME, recipient.getName());
            db.update(RECIPIENTS_TABLE, values, RECIPIENTS_KEY_ID + " = " + recipient.getID(), null);
        }
    }

    /**
     *
     * Deletes the recipient given in argument
     * @param recipient the recipient to delete
     */
    public void deleteRecipient(Recipient recipient) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(RECIPIENTS_TABLE, RECIPIENTS_KEY_ID + " = " + recipient.getID(), null);
    }

    /**
     *
     * Deletes the recipient corresponding to an id
     * @param id the id
     */
    public void deleteRecipient(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(RECIPIENTS_TABLE, RECIPIENTS_KEY_ID + " = " + id, null);
    }

    /**
     *
     * Deletes all recipients given as argument.
     * @param recipients a list of recipients to delete.
     */
    public void deleteRecipients(List<Recipient> recipients){
        SQLiteDatabase db = this.getWritableDatabase();
        for (Recipient recipient : recipients){
            db.delete(RECIPIENTS_TABLE, RECIPIENTS_KEY_ID + " = " + recipient.getID(), null);
        }
    }

    /**
     *
     * Deletes all recipients
     */
    public void deleteAllRecipients(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(RECIPIENTS_TABLE, null ,null);
    }
    /**
     *
     * Returns a recipient corresponding to an id.
     * @param id the id of the recipient
     * @return the recipient
     */
    public Recipient getRecipient(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(RECIPIENTS_TABLE, RECIPIENTS_COLUMN, RECIPIENTS_KEY_ID + " = " + id, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {

            String name = cursor.getString(1);
            cursor.close();
            //TODO returns only user now
            return new User(id, name);
        }
        return null;
    }

    /**
     *
     * Returns the recipients corresponding to the ids
     * @param ids a list of Integers
     * @return a list of Recipients
     */
    public List<Recipient> getRecipients(List<Integer> ids){
        SQLiteDatabase db = this.getReadableDatabase();
        List<Recipient> recipients = new ArrayList<>();
        for (Integer id: ids){
            Cursor cursor = db.query(RECIPIENTS_TABLE, RECIPIENTS_COLUMN, RECIPIENTS_KEY_ID + " = " + id, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {

                String name = cursor.getString(1);
                cursor.close();
                recipients.add(new User(id, name));
            }
        }
        return recipients;
    }

    /**
     *
     * Returns all recipients.
     * @return all recipients as a List
     */
    public List<Recipient> getAllRecipients() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + RECIPIENTS_TABLE, null);
        boolean hasNext = true;
        List<Recipient> recipients = new ArrayList<>();
        if (cursor != null) {
            hasNext = cursor.moveToFirst();
            while (hasNext) {
                int id = cursor.getInt(0);
                String name = cursor.getString(1);
                recipients.add(new User(id, name));
                hasNext = cursor.moveToNext();
            }
            cursor.close();
        }
        return recipients;
    }
}
