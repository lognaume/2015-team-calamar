package ch.epfl.sweng.calamar.item;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Date;

import ch.epfl.sweng.calamar.CalamarApplication;
import ch.epfl.sweng.calamar.R;
import ch.epfl.sweng.calamar.condition.Condition;
import ch.epfl.sweng.calamar.recipient.Recipient;
import ch.epfl.sweng.calamar.recipient.User;
import ch.epfl.sweng.calamar.utils.Compresser;

/**
 * Created by pierre on 11/12/15.
 */
public final class ImageItem extends FileItem {

    protected final static Type ITEM_TYPE = Type.IMAGEITEM;
    private final static int IMAGE_VIEW_SIZE = 1000;

    /**
     * Instantiates a new ImageItem with the following parameters
     *
     * @param ID        the id
     * @param from      the 'from' field of the Item (sender)
     * @param to        the 'to' field of the Item (recipient)
     * @param date      the creation/posting date of the Item
     * @param condition the content (text message)
     * @param data      the image as a byte array
     * @param path      the path of the image
     * @param message   the message of the image
     * @see Item#Item(int, User, Recipient, Date, Condition, String)
     */
    public ImageItem(int ID, User from, Recipient to, Date date, Condition condition, byte[] data, String path, String message) {
        super(ID, from, to, date, condition, data, path, message);
    }

    /**
     * Instantiates a new ImageItem with the following parameters
     *
     * @param ID      the id
     * @param from    the 'from' field of the Item (sender)
     * @param to      the 'to' field of the Item (recipient)
     * @param date    the creation/posting date of the Item
     * @param data    the image as a byte array
     * @param path    the path of the image
     * @param message the message of the image
     * @see Item#Item(int, User, Recipient, Date, Condition, String)
     */
    public ImageItem(int ID, User from, Recipient to, Date date, byte[] data, String path, String message) {
        this(ID, from, to, date, Condition.trueCondition(), data, path, message);
    }

    /**
     * Instantiates a new ImageItem with the following parameters
     *
     * @param ID        the id
     * @param from      the 'from' field of the Item (sender)
     * @param to        the 'to' field of the Item (recipient)
     * @param date      the creation/posting date of the Item
     * @param condition the content (text message)
     * @param data      the image as a byte array
     * @param path      the path of the image
     * @see Item#Item(int, User, Recipient, Date, Condition, String)
     */
    public ImageItem(int ID, User from, Recipient to, Date date, Condition condition, byte[] data, String path) {
        this(ID, from, to, date, condition, data, path, CalamarApplication.getInstance().getString(R.string.empty_string));
    }

    /**
     * Instantiates a new ImageItem with the following parameters
     *
     * @param ID   the id
     * @param from the 'from' field of the Item (sender)
     * @param to   the 'to' field of the Item (recipient)
     * @param date the creation/posting date of the Item
     * @param data the image as a byte array
     * @param path the path of the image
     * @see Item#Item(int, User, Recipient, Date, Condition, String)
     */
    public ImageItem(int ID, User from, Recipient to, Date date, byte[] data, String path) {
        this(ID, from, to, date, Condition.trueCondition(), data, path, CalamarApplication.getInstance().getString(R.string.empty_string));
    }

    /**
     * gets type of this ImageItem (always returns IMAGEITEM)
     *
     * @return IMAGEITEM
     */
    @Override
    public Type getType() {
        return ITEM_TYPE;
    }

    @Override
    public View getItemView(Activity context) {
        //Trying to use xml layout
        /*LayoutInflater inflater = context.getLayoutInflater();
        LinearLayout parent = (LinearLayout) context.findViewById(R.id.ItemDetailsItemPreview);
        LinearLayout root;
        if (parent == null) {
            root = (LinearLayout) inflater.inflate(R.layout.item_details_base_layout, null);
            parent = (LinearLayout) root.findViewById(R.id.ItemDetailsItemPreview);
        }
        ImageView imageView = (ImageView) context.findViewById(R.id.imageitem_view);
        if (imageView == null) {
            imageView = (ImageView) inflater.inflate(R.layout.image_view, parent).findViewById(R.id.imageitem_view);
        }
        */
        ImageView imageView = new ImageView(context);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        imageView.setMaxHeight(IMAGE_VIEW_SIZE);
        imageView.setMaxWidth(IMAGE_VIEW_SIZE);
        imageView.setImageBitmap(getBitmap());
        imageView.setAdjustViewBounds(true);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForFile(ImageItem.this);
            }
        });
        return imageView;
    }

    /**
     * Appends the fields of ImageItem to the given JSONObject. <br><br>
     * Should <b>NOT</b> be used alone
     *
     * @param json the json to which we append data
     * @throws JSONException
     */
    @Override
    protected void compose(JSONObject json) throws JSONException {
        super.compose(json);
        json.put(JSON_TYPE, ITEM_TYPE.name());
    }

    /**
     * Parses a ImageItem from a JSONObject.<br>
     *
     * @param json the well formed {@link JSONObject json} representing the {@link ImageItem item}
     * @return a {@link ImageItem} parsed from the JSONObject
     * @throws JSONException
     * @see Item#fromJSON(JSONObject) Recipient.fromJSON
     */

    public static ImageItem fromJSON(JSONObject json) throws JSONException {
        return new ImageItem.Builder().parse(json).build();
    }

    /**
     * @return a JSONObject representing a {@link ImageItem}
     * @throws JSONException
     */
    @Override
    public JSONObject toJSON() throws JSONException {
        JSONObject ret = new JSONObject();
        this.compose(ret);
        return ret;
    }

    /**
     * Returns the image of the item
     *
     * @return a bitmap
     */
    public Bitmap getBitmap() {
        byte[] tempData = Compresser.decompress(getData());
        if (tempData != null) {
            return BitmapFactory.decodeByteArray(tempData, 0, tempData.length);
        } else {
            return null;
        }
    }


    /**
     * used to transform a string (containing an array of bytes representing the png image) to a bitmap
     *
     * @param str png image as byte[]
     * @return bitmap corresponding to given byte[] input
     */
    public static Bitmap string2Bitmap(String str) {
        byte[] bytes2 = Base64.decode(str, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes2, 0, bytes2.length);
    }

    /**
     * used to transform a bitmap to a string containing an array of bytes representing the png image
     *
     * @param bitmap the bitmap to convert
     * @return string representation as array of byte in png format
     */
    public static String bitmap2String(Bitmap bitmap) {
        ByteArrayOutputStream blob = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, blob);
        return Base64.encodeToString(blob.toByteArray(), Base64.DEFAULT);
    }

    /**
     * A Builder for {@link ImageItem}, currently only used to parse JSON (little overkill..but ..)
     *
     * @see Item.Builder
     */
    public static class Builder extends FileItem.Builder {

        @Override
        public Builder parse(JSONObject json) throws JSONException {
            super.parse(json);
            String type = json.getString(JSON_TYPE);
            if (!type.equals(ImageItem.ITEM_TYPE.name())) {
                throw new IllegalArgumentException(CalamarApplication.getInstance().getString(R.string.expected_but_was, ImageItem.ITEM_TYPE.name(), type));
            }
            return this;
        }

        @Override
        public ImageItem build() {
            return new ImageItem(super.ID, super.from, super.to, super.date, super.condition, super.data, super.path, super.message);
        }
    }
}
