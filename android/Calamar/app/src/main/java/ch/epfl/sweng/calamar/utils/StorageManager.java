package ch.epfl.sweng.calamar.utils;

import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import ch.epfl.sweng.calamar.CalamarApplication;
import ch.epfl.sweng.calamar.R;
import ch.epfl.sweng.calamar.SQLiteDatabaseHandler;
import ch.epfl.sweng.calamar.item.FileItem;
import ch.epfl.sweng.calamar.item.ImageItem;
import ch.epfl.sweng.calamar.item.Item;

/**
 * A Singleton managing storing, retrieving and deleting items on local storage.
 */
//TODO Runs dbHandler.get in AsyncTask !
public final class StorageManager {

    private static final Set<WritingTask> currentWritingTasks = new CopyOnWriteArraySet<>();

    //Will be used to requery server if writing of a file has failed and the file is no longer available.
    private static final Set<Integer> currentFilesID = new CopyOnWriteArraySet<>();


    private static final String ROOT_FOLDER_NAME = "Calamar/";
    private static final String IMAGE_FOLDER_NAME = ROOT_FOLDER_NAME + "Calamar Images/";
    private static final String FILE_FOLDER_NAME = ROOT_FOLDER_NAME + "Calamar Others/";
    private static final String FILENAME = "FILE_";
    private static final String IMAGENAME = "IMG_";
    private static final String NAME_SUFFIX = "_CAL";
    private static final String IMAGE_EXT = ".png";

    private static final int RETRY_TIME = 10000;
    private static final int MAX_ITER = 20;

    private static final int OPERATION_READ = 1;
    private static final int OPERATION_DELETE_WITHOUT_DB = 2;
    private static final int OPERATION_DELETE_WITH_DB = 3;

    private static StorageManager instance;
    private final SQLiteDatabaseHandler dbHandler;
    private final Handler handler;
    private final CalamarApplication app;
    private final Calendar calendar;

    /**
     * Returns the only instance of this class
     *
     * @return the singleton
     */
    public static StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    /**
     * Private constructor
     */
    private StorageManager() {
        app = CalamarApplication.getInstance();
        dbHandler = app.getDatabaseHandler();
        handler = new Handler();
        calendar = Calendar.getInstance();
    }

    /**
     * Stops all writing tasks (to free up memory mainly)
     */
    public void cancelWritingTasks(int level) {
        for (WritingTask w : currentWritingTasks) {
            if (w.getIteration() >= level) {
                w.cancel(true);
            }
        }
    }

    /**
     * Retry failed FileItem writings once more memory is available
     * TODO does nothing at the moment
     */
    public void retryFailedWriting() {
        //Ask server
    }

    /**
     * Adds the item to the database and possibly store it if it is a FileItem
     * Also updates the item with his new path
     *
     * @param i The item to be stored
     */
    public void storeItem(Item i, StorageCallbacks caller) {
        switch (i.getType()) {
            case SIMPLETEXTITEM:
                dbHandler.addItem(i);
                break;
            case IMAGEITEM:
                if (!i.getFrom().equals(app.getCurrentUser())) {
                    //if (!i.isLocked()) { While metadatas are not implemented...
                    ImageItem repathedImage = (ImageItem) rePath((ImageItem) i);
                    if (caller != null) {
                        //Gives item updated with new path
                        caller.onItemRetrieved(repathedImage);
                    }
                    ImageItem compressedImage = (ImageItem) Compresser.compressDataForDatabase(repathedImage);
                    app.increaseImageCount();
                    dbHandler.addItem(compressedImage);
                    storeFile(repathedImage);
                    // } else {
                    //Assuming a locked item has no data
                    //    dbHandler.addItem(i);
                    //}
                } else {
                    dbHandler.addItem(Compresser.compressDataForDatabase((ImageItem) i));
                }
                break;
            case FILEITEM:
                if (!i.getFrom().equals(app.getCurrentUser())) {
                    //if (i.getCondition().getValue()) {
                    FileItem repathedFile = rePath((FileItem) i);
                    if (caller != null) {
                        //Gives item updated with new path
                        caller.onItemRetrieved(repathedFile);
                    }
                    FileItem compressedFile = Compresser.compressDataForDatabase(repathedFile);
                    app.increaseFileCount();
                    dbHandler.addItem(compressedFile);
                    storeFile(repathedFile);
                    //} else {
                    //Assuming a locked item has no data
                    //    dbHandler.addItem(i);
                    //}
                } else {
                    dbHandler.addItem(Compresser.compressDataForDatabase((FileItem) i));
                }
                break;
            default:
                throw new IllegalArgumentException(app.getString(R.string.unexpected_item_type, i.getType().name()));
        }

    }

    /**
     * Adds the items to the database and possibly store them
     *
     * @param items The items to be stored
     */
    public void storeItems(List<Item> items, StorageCallbacks caller) {
        for (Item i : items) {
            storeItem(i, caller);
        }
    }

    /**
     * Deletes the item given as argument (deletes also in the database)
     *
     * @param item The item to delete
     */
    public void deleteItemWithDatabase(Item item) {
        switch (item.getType()) {
            case SIMPLETEXTITEM:
                dbHandler.deleteItem(item);
                break;
            case FILEITEM:
                new DeleteTask((FileItem) item).execute();
                dbHandler.deleteItem(item);
                break;
            case IMAGEITEM:
                new DeleteTask((ImageItem) item).execute();
                dbHandler.deleteItem(item);
                break;
            default:
                throw new IllegalArgumentException(app.getString(R.string.unexpected_item_type, item.getType().name()));
        }
    }

    /**
     * Deletes an item without deleting it in the database
     *
     * @param item the item to delete
     */
    public void deleteItemWithoutDatabase(Item item) {
        switch (item.getType()) {
            case SIMPLETEXTITEM:
                break;
            case FILEITEM:
                new DeleteTask((FileItem) item).execute();
                break;
            case IMAGEITEM:
                new DeleteTask((ImageItem) item).execute();
                break;
            default:
                throw new IllegalArgumentException(app.getString(R.string.unexpected_item_type, item.getType().name()));
        }
    }

    /**
     * Deletes the item whose ID is given (deletes also in the database)
     *
     * @param ID the id of the item to delete
     */
    public void deleteItemWithDatabase(int ID) {
        Integer[] tempArr = {ID};
        new GetItemFromIDTask(null, OPERATION_DELETE_WITH_DB).execute(tempArr);
    }

    /**
     * Deletes the item whose ID is given, without deleting it in the database
     *
     * @param ID The id of the item to delete
     */
    public void deleteItemWithoutDatabase(int ID) {
        Integer[] tempArr = {ID};
        new GetItemFromIDTask(null, OPERATION_DELETE_WITHOUT_DB).execute(tempArr);
    }

    /**
     * Deletes all stored items (even in database)
     * Deprecated, must be improved
     */
    @Deprecated
    public void deleteAllItemsWithDatabase() {
        List<Item> items = dbHandler.getAllItems();
        for (Item i : items) {
            deleteItemWithDatabase(i);
        }
        dbHandler.deleteAllItems();
    }

    /**
     * Deletes all stored items (without deleting anything in database)
     * Deprecated, must be improved
     */
    @Deprecated
    public void deleteAllItemsWithoutDatabase() {
        List<Item> items = dbHandler.getAllItems();
        for (Item i : items) {
            deleteItemWithoutDatabase(i);
        }
    }

    /**
     * Deletes all items whose id is in the list (deletes also in the database)
     *
     * @param ids the ids of the items to delete
     */
    public void deleteItemsForIdsWithDatabase(List<Integer> ids) {
        new GetItemFromIDTask(null, OPERATION_DELETE_WITH_DB).execute(ids.toArray(new Integer[ids.size()]));
    }

    /**
     * Deletes all items given in the list (deletes also in the database)
     *
     * @param items the items to delete
     */
    public void deleteItemsWithDatabase(List<Item> items) {
        for (Item i : items) {
            deleteItemWithDatabase(i);
        }
    }

    /**
     * Deletes all items whose id is in the list without deleting them in the database
     *
     * @param ids the ids of the items to delete
     */
    public void deleteItemsForIdsWithoutDatabase(List<Integer> ids) {
        new GetItemFromIDTask(null, OPERATION_DELETE_WITHOUT_DB).execute(ids.toArray(new Integer[ids.size()]));
    }

    /**
     * Deletes all items given in the list without deleting them in the database
     *
     * @param items the items to delete
     */
    public void deleteItemsWithoutDatabase(List<Item> items) {
        for (Item i : items) {
            deleteItemWithoutDatabase(i);
        }
    }

    /**
     * Task which deletes the FileItem given to the constructor. Tries only once.
     */
    private class DeleteTask extends AsyncTask<Void, Void, Void> {

        private final FileItem f;

        public DeleteTask(FileItem f) {
            this.f = f;
        }

        @Override
        protected Void doInBackground(Void... params) {
            if (isExternalStorageWritable()) {
                File file = new File(f.getPath());
                if (file.exists()) {
                    if (!file.delete()) {
                        showToast(app.getString(R.string.error_file_deletion, f.getPath()));
                    }
                }
            } else {
                showStorageStateToast();
            }
            return null;
        }
    }

    /**
     * Returns the complete Item (with complete data) for a given Item
     *
     * @param i      The item
     * @param caller The Activity who called this method
     */
    public void getCompleteItem(Item i, StorageCallbacks caller) {
        if (i != null) {
            switch (i.getType()) {
                case SIMPLETEXTITEM:
                    caller.onItemRetrieved(i);
                    break;
                case FILEITEM:
                    new ReadTask((FileItem) i, ((FileItem) i).getPath(), caller).execute();
                    break;
                case IMAGEITEM:
                    new ReadTask((ImageItem) i, ((ImageItem) i).getPath(), caller).execute();
                    break;
                default:
                    throw new IllegalArgumentException(app.getString(R.string.unexpected_item_type, i.getType().name()));
            }
        }
    }

    /**
     * Returns the complete Item (with complete Data) for a given ID
     *
     * @param ID     The id of the item
     * @param caller The Activity who called this method
     */
    public void getCompleteItem(int ID, StorageCallbacks caller) {
        new GetItemFromIDTask(caller, OPERATION_READ).execute(ID);
    }

    /**
     * Returns the data of the file given by the path
     *
     * @param path   The path of the file
     * @param caller The Activity who called this method
     */
    public void getData(String path, StorageCallbacks caller) {
        new ReadTask(null, path, caller);
    }

    /**
     * Called when ReadTask has finished
     *
     * @param i      The FileItem (if null, callbacks OnDataRetrieved, else OnItemRetrieved)
     * @param data   The data retrieved
     * @param caller The Activity who asked for reading
     */
    private void onCompleteItemRetrieved(FileItem i, byte[] data, StorageCallbacks caller) {
        if (data != null && data.length != 0) {
            if (i == null) {
                caller.onDataRetrieved(data);
            } else {
                switch (i.getType()) {
                    case FILEITEM:
                        caller.onItemRetrieved(new FileItem(i.getID(), i.getFrom(), i.getTo(), i.getDate(), i.getCondition(), data, i.getPath()));
                        break;
                    case IMAGEITEM:
                        caller.onItemRetrieved(new ImageItem(i.getID(), i.getFrom(), i.getTo(), i.getDate(), i.getCondition(), data, i.getPath()));
                        break;
                    default:
                        throw new IllegalArgumentException(app.getString(R.string.expected_fileitem));
                }
            }
        }
    }

    private void onItemRetrieved(List<Item> items, StorageCallbacks caller, int operation) {
        switch (operation) {
            case OPERATION_DELETE_WITHOUT_DB:
                deleteItemsWithoutDatabase(items);
                break;
            case OPERATION_DELETE_WITH_DB:
                deleteItemsWithDatabase(items);
                break;
            case OPERATION_READ:
                for (Item i : items) {
                    getCompleteItem(i, caller);
                }
                break;
            default:
                throw new IllegalArgumentException(app.getString(R.string.storage_unknown_operation));
        }
    }

    /**
     * AsyncTask for searching an item in the database given its ID
     */
    private class GetItemFromIDTask extends AsyncTask<Integer, Void, Item[]> {

        private final StorageCallbacks caller;
        private final int operation;

        private GetItemFromIDTask(StorageCallbacks caller, int operation) {
            this.caller = caller;
            this.operation = operation;
        }

        @Override
        protected Item[] doInBackground(Integer... params) {
            List<Integer> toGet = Arrays.asList(params);
            List<Item> retrieved = dbHandler.getItems(toGet);
            return retrieved.toArray(new Item[retrieved.size()]);
        }

        @Override
        protected void onPostExecute(Item... params) {
            onItemRetrieved(Arrays.asList(params), caller, operation);
        }
    }

    /**
     * The task whose job is to retrieve data from storage and return it
     */
    protected class ReadTask extends AsyncTask<Void, Void, byte[]> {

        private final FileItem f;
        private final String path;
        private final StorageCallbacks caller;

        public ReadTask(FileItem f, String path, StorageCallbacks caller) {
            this.f = f;
            this.path = path;
            this.caller = caller;
        }

        @Override
        protected byte[] doInBackground(Void... params) {
            return getData(path);
        }

        @Override
        protected void onPostExecute(byte[] data) {
            onCompleteItemRetrieved(f, data, caller);
        }
    }

    /**
     * Returns a byte array representing the data of the FileItem
     *
     * @param f the FileItem whose data needs to be retrieved from storage
     * @return The data
     */
    private byte[] getData(FileItem f) {
        switch (f.getType()) {
            case FILEITEM:
                return getData(f.getPath());
            case IMAGEITEM:
                return getData(f.getPath());
            default:
                throw new IllegalArgumentException(app.getString(R.string.expected_fileitem));
        }
    }


    private byte[] getData(String path) {
        if (isExternalStorageReadable()) {
            File file = new File(path);
            if (file.exists()) {
                try {
                    return FileUtils.toByteArray(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    showStorageStateToast();
                }
            });
        }
        return null;
    }

    /**
     * Store a FileItem in storage
     *
     * @param f the FileItem to store
     */
    private void storeFile(FileItem f) {
        if (f.getData().length != 0) {
            WritingTask task = new WritingTask(f, 0);
            currentWritingTasks.add(task);
            currentFilesID.add(f.getID());
            task.execute();
        }
    }

    /**
     * Shows different error toast depending on the state of the storage
     */
    private void showStorageStateToast() {
        switch (Environment.getExternalStorageState()) {
            case Environment.MEDIA_UNMOUNTED:
                Toast.makeText(app, R.string.error_media_unmounted, Toast.LENGTH_LONG).show();
            case Environment.MEDIA_SHARED:
                Toast.makeText(app, R.string.error_media_shared, Toast.LENGTH_LONG).show();
                break;
            case Environment.MEDIA_UNMOUNTABLE:
                Toast.makeText(app, R.string.error_media_unmountable, Toast.LENGTH_LONG).show();
                break;
            default:
                Toast.makeText(app, R.string.error_media_generic, Toast.LENGTH_LONG).show();
                break;
        }
    }

    /* Checks if external storage is available for read and write */
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    private boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

    /**
     * Writes a FileItem in the local storage
     *
     * @param f The FileItem to store
     * @throws IOException If there is a problem writing it
     */
    private void writeFile(FileItem f) throws IOException {
        if (f.getData() != null) {
            OutputStream stream = null;
            try {
                stream = new BufferedOutputStream(new FileOutputStream(f.getPath()));
                byte[] toWrite = Compresser.decompress(f.getData());
                if (toWrite != null) {
                    stream.write(toWrite);
                }
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        }
    }

    /**
     * Rename and changes the path of a FileItem which will be stored.
     *
     * @param f The FileItem to "repath"
     * @return The repathed FileItem
     */
    private FileItem rePath(FileItem f) {
        switch (f.getType()) {
            case FILEITEM:
                File filePath = Environment.getExternalStoragePublicDirectory(FILE_FOLDER_NAME);
                return new FileItem(f.getID(), f.getFrom(), f.getTo(), f.getDate(), f.getCondition(), f.getData(), filePath.getAbsolutePath() + '/' + FILENAME + app.getString(R.string.empty_string) + formatDate() + NAME_SUFFIX + app.getTodayFileCount());
            case IMAGEITEM:
                File imagePath = Environment.getExternalStoragePublicDirectory(IMAGE_FOLDER_NAME);
                return new ImageItem(f.getID(), f.getFrom(), f.getTo(), f.getDate(), f.getCondition(), f.getData(), imagePath.getAbsolutePath() + '/' + IMAGENAME + app.getString(R.string.empty_string) + formatDate() + NAME_SUFFIX + app.getTodayImageCount() + IMAGE_EXT);
            default:
                throw new IllegalArgumentException(app.getString(R.string.expected_fileitem));
        }
    }

    /**
     * Creates a String using the current Date
     *
     * @return the String, used in rePath
     */
    private String formatDate() {
        return calendar.get(Calendar.DAY_OF_MONTH) + app.getString(R.string.empty_string) + (calendar.get(Calendar.MONTH) + 1) + app.getString(R.string.empty_string) + calendar.get(Calendar.YEAR);
    }

    /**
     * An AsyncTask whose task is to write a FileItem to the storage. It will retry for ~30 min (20 times) before giving up
     */
    protected class WritingTask extends AsyncTask<Void, Void, Boolean> {

        private final int iterCount;
        private final FileItem f;

        protected WritingTask(FileItem f, int iterCount) {
            this.iterCount = iterCount;
            this.f = f;
        }

        public int getIteration() {
            return iterCount;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            if (isExternalStorageWritable() && !isCancelled()) {
                switch (f.getType()) {
                    case FILEITEM:
                        File filePath = Environment.getExternalStoragePublicDirectory(FILE_FOLDER_NAME);
                        if (!filePath.exists()) {
                            Log.i(app.getString(R.string.storage), app.getString(R.string.creating_dir, app.getString(R.string.others_dir)));
                            if (filePath.mkdirs()) {
                                Log.i(app.getString(R.string.storage), app.getString(R.string.directory_creation_success));
                                try {
                                    writeFile(f);
                                    return true;
                                } catch (IOException e) {
                                    showToast(app.getString(R.string.error_file_creation, f.getName()));
                                    return false;
                                }
                            } else {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(app, app.getString(R.string.error_directory_creation), Toast.LENGTH_SHORT).show();
                                    }
                                });
                                return false;
                            }
                        } else {
                            try {
                                writeFile(f);
                                return true;
                            } catch (IOException e) {
                                showToast(app.getString(R.string.error_file_creation, f.getName()));
                                return false;
                            }
                        }
                    case IMAGEITEM:
                        File imagePath = Environment.getExternalStoragePublicDirectory(IMAGE_FOLDER_NAME);
                        if (!imagePath.exists()) {
                            Log.i(app.getString(R.string.storage), app.getString(R.string.creating_dir, app.getString(R.string.images_dir)));
                            if (imagePath.mkdirs()) {
                                Log.i(app.getString(R.string.storage), app.getString(R.string.directory_creation_success));
                                try {
                                    writeFile(f);
                                    return true;
                                } catch (IOException e) {
                                    showToast(app.getString(R.string.error_image_creation, f.getName()));
                                    return false;
                                }
                            } else {
                                showToast(app.getString(R.string.error_directory_creation));
                                return false;
                            }
                        } else {
                            try {
                                writeFile(f);
                                return true;
                            } catch (IOException e) {
                                showToast(app.getString(R.string.error_image_creation, f.getName()));
                                return false;
                            }
                        }

                    default:
                        throw new IllegalArgumentException(app.getString(R.string.expected_fileitem));
                }
            }
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean b) {
            currentWritingTasks.remove(this);
            if (!b) {
                Log.i(app.getString(R.string.storage), app.getString(R.string.writing_failed, f.getPath()));
                if (!isCancelled()) {
                    if (iterCount < MAX_ITER) {
                        Log.i(app.getString(R.string.storage), app.getString(R.string.retrying_write, f.getPath()));
                        final WritingTask task = new WritingTask(f, iterCount + 1);
                        currentWritingTasks.add(task);
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                //TODO seems that task is executed twice in tests so it fails
                                //I dont understand why.
                                if (!(task.getStatus().equals(Status.FINISHED)) && !(task.getStatus().equals(Status.RUNNING))) {
                                    task.execute();
                                }
                            }
                        }, RETRY_TIME * (iterCount + 1));
                    } else {
                        showStorageStateToast();
                    }
                }
            } else {
                Log.i(app.getString(R.string.storage), app.getString(R.string.file_stored, f.getPath()));
                currentFilesID.remove(f.getID());
            }
        }

        @Override
        protected void onCancelled() {
            currentWritingTasks.remove(this);
        }
    }

    protected Set<WritingTask> getCurrentWritingTasks() {
        return new HashSet<>(currentWritingTasks);
    }

    protected Set<Integer> getCurrentFilesID() {
        return new HashSet<>(currentFilesID);
    }

    /**
     * Shows toast on the UI thread
     *
     * @param str The string to print
     */
    private void showToast(final String str) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(app, str, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
