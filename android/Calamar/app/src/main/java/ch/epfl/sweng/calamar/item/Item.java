package ch.epfl.sweng.calamar.item;

import android.app.Activity;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import ch.epfl.sweng.calamar.CalamarApplication;
import ch.epfl.sweng.calamar.R;
import ch.epfl.sweng.calamar.condition.Condition;
import ch.epfl.sweng.calamar.recipient.Recipient;
import ch.epfl.sweng.calamar.recipient.User;

/**
 * Models an Item, superclass of all the possibly many kind of 'item' the app manage. <br><br>
 * known subclasses : <li>
 * <ul>{@link SimpleTextItem},</ul>
 * <ul>{@link FileItem},</ul>
 * <ul>{@link ImageItem},</ul>
 * </li>
 * Item is immutable
 */
public abstract class Item {

    public static final int DUMMY_ID = 1;
    protected static final String JSON_TYPE = "type";
    private static final String JSON_ID = "ID";
    private static final String JSON_FROM = "from";
    private static final String JSON_TO = "to";
    private static final String JSON_DATE = "date";
    private static final String JSON_CONDITION = "condition";
    private static final String JSON_MESSAGE = "message";
    private final int ID;
    private final User from;
    private final Recipient to;
    private final Date date; //posix date
    private final Condition condition;
    private final String message;

    /**
     * Type of the item, (SIMPLETEXTITEM, IMAGEITEM, FILEITEM)
     */
    public enum Type {SIMPLETEXTITEM, IMAGEITEM, FILEITEM}

    private final Set<Item.Observer> observers = new HashSet<>();


    protected Item(int ID, User from, Recipient to, Date date, Condition condition, String message) {
        if (null == from || null == to || null == condition || null == date) {
            throw new IllegalArgumentException(CalamarApplication.getInstance().getString(R.string.item_field_null));
        }
        if (message == null) {
            this.message = CalamarApplication.getInstance().getString(R.string.empty_string);
        } else {
            this.message = message;
        }
        this.ID = ID;
        this.from = from; //User is immutable
        this.to = to;     //Recipient is immutable
        this.date = date;
        this.condition = condition; //TODO Condition is not immutable...

        final Condition.Observer conditionObserver = new Condition.Observer() {
            @Override
            public void update(Condition condition) {
                for (Observer o : observers) {
                    o.update(Item.this);
                }
            }
        };
        condition.addObserver(conditionObserver);
    }

    protected Item(int ID, User from, Recipient to, Date date, String message) {
        this(ID, from, to, date, Condition.trueCondition(), message);
    }

    /**
     * @return the type of the item ({@link ch.epfl.sweng.calamar.item.Item.Type})
     */
    public abstract Type getType();

    /**
     * @param context parent activity
     * @return view of the item
     */
    public abstract View getItemView(Activity context);

    /**
     * @return the text content (message) of the Item
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Get the complete view of the item. ( With condition(s) )
     *
     * @param context the context from which this method is called
     * @return the view of the item.
     */
    public View getView(final Activity context) {
        //Inflate a basic layout
        LayoutInflater li = LayoutInflater.from(context);
        View baseView = li.inflate(R.layout.item_details_base_layout, null);

        //FIll the layout
        TextView dateText = (TextView) baseView.findViewById(R.id.ItemDetailsDate);
        dateText.setText(date.toString());

        TextView fromText = (TextView) baseView.findViewById(R.id.ItemDetailsUserFrom);
        fromText.setText(from.toString());

        TextView toText = (TextView) baseView.findViewById(R.id.ItemDetailsUserTo);
        toText.setText(to.toString());

        LinearLayout previewLayout = (LinearLayout) baseView.findViewById(R.id.ItemDetailsItemPreview);
        previewLayout.addView(getPreView(context));

        LinearLayout conditionLayout = (LinearLayout) baseView.findViewById(R.id.ItemDetailsConditionLayout);
        conditionLayout.addView(condition.getView(context));

        return baseView;
    }

    /**
     * Get a simple pre view of the item
     *
     * @param context parent activity
     * @return the preview of the item
     */
    public View getPreView(final Activity context) {
        final LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        int childCount = 0;
        if (condition.getValue()) {
            View itemView = getItemView(context);
            if (itemView != null) {
                if (itemView.getParent() != null) {
                    ViewGroup parent = (ViewGroup) itemView.getParent();
                    parent.removeView(itemView);
                }
                view.addView(itemView, childCount);
                childCount += 1;
            }
            if (!message.equals(CalamarApplication.getInstance().getString(R.string.empty_string))) {
                TextView text = new TextView(context);
                text.setText(message);
                view.addView(text, childCount);
                childCount += 1;
            }
        } else {
            TextView lockMessage = new TextView(context);
            lockMessage.setText(R.string.item_is_locked_getview);
            view.addView(lockMessage, childCount);
            childCount += 1;
        }

        return view;
    }

    /**
     * @return the 'condition' field of the Item
     */
    public final Condition getCondition() {
        return condition;
    }

    /**
     * @return the 'from' field of the Item (sender)
     */
    public final User getFrom() {
        return from;
    }

    /**
     * @return the 'to' field of the Item (recipient)
     */
    public final Recipient getTo() {
        return to;
    }

    /**
     * @return the creation/posting date of the Item
     */
    public final Date getDate() {
        return date;
    }

    public final int getID() {
        return ID;
    }

    /**
     * @return the item's location if {@link #hasLocation()} is true.
     * (simple shortcut for condition.getLocation)
     * @see Condition#getLocation()
     */
    public final Location getLocation() {
        return getCondition().getLocation();
    }

    /**
     * @return true if the item's condition contains at least one location, false otherwise
     * @see Condition#getLocation()
     */
    public final boolean hasLocation() {
        return getCondition().hasLocation();
    }

    /**
     * @return true if the item is locked, i.e. its condition {@link Condition#getValue() value} is false
     */
    public final boolean isLocked() {
        return !getCondition().getValue();
    }

    /**
     * Appends the fields of {@link Item} to a {@link JSONObject} representing the Item.<br>
     * is called by the {@link #compose(JSONObject)} method of the child classes in
     * a chain where each compose method append the field of its class to the object.<br>
     * The chain begins by a call to {@link #toJSON()} in an instantiable child class.<br><br>
     * Should <b>NOT</b> be used alone.
     *
     * @param json the json to which we append (using {@link JSONObject#accumulate(String, Object)} ) data
     * @throws JSONException
     */
    protected void compose(JSONObject json) throws JSONException {
        json.accumulate(JSON_ID, ID);
        json.accumulate(JSON_FROM, from.toJSON());
        json.accumulate(JSON_TO, to.toJSON());
        json.accumulate(JSON_DATE, date.getTime());
        json.accumulate(JSON_CONDITION, condition.toJSON());
        json.accumulate(JSON_MESSAGE, message);
    }

    /**
     * @return a JSONObject representing a {@link Item)
     * @throws JSONException
     */
    //must remains abstract if class not instantiable
    public abstract JSONObject toJSON() throws JSONException;

    /**
     * Parses an Item from a JSONObject.<br>
     * To instantiate the correct Item ({@link SimpleTextItem}, etc ...)
     * the JSON must have a {@link ch.epfl.sweng.calamar.item.Item.Type 'type'} field indicating the type...('simpleText', ...)
     *
     * @param json the well formed {@link JSONObject json} representing the {@link Item item}
     * @return a {@link Item item} parsed from the JSONObject
     * @throws JSONException
     */
    public static Item fromJSON(JSONObject json) throws JSONException, IllegalArgumentException {
        if (null == json || json.isNull(JSON_TYPE)) {
            throw new IllegalArgumentException(CalamarApplication.getInstance().getString(R.string.malformed_json));
        }
        Item item;
        String type = json.getString(JSON_TYPE);
        switch (Type.valueOf(type)) {
            case SIMPLETEXTITEM:
                item = SimpleTextItem.fromJSON(json);
                break;
            case IMAGEITEM:
                item = ImageItem.fromJSON(json);
                break;
            case FILEITEM:
                item = FileItem.fromJSON(json);
                break;
            default:
                throw new IllegalArgumentException(CalamarApplication.getInstance().getString(R.string.unexpected_item_type, type));
        }
        return item;
    }

    /**
     * java equals
     *
     * @param o other Object to compare this with
     * @return true if o is equal in value to this
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof Item)) return false;
        Item that = (Item) o;
        return that.ID == ID && that.from.equals(from) && that.to.equals(to) &&
                that.date.getTime() == date.getTime() &&
                that.message.equals(this.message);
    }

    /**
     * java hash function
     *
     * @return hash of the Object
     */
    @Override
    public int hashCode() {
        return ID + from.hashCode() * 89 + to.hashCode() * 197 + ((int) date.getTime()) * 479 + (message != null ? message.hashCode() : 0) * 701;
    }

    @Override
    public String toString() {
        return "id : " + ID + " , from : (" + from + ") , to : (" + to + ") , at : " + date
                + " message : " + message;
    }

    /**
     * A Builder for {@link Item}, has no build() method since Item isn't instantiable,
     * is used by the child builders (in {@link SimpleTextItem} or...) to build the "Item
     * part of the object". currently only used to parse JSON (little overkill..but ..)
     */
    public abstract static class Builder {
        protected int ID;
        protected User from;
        protected Recipient to;
        protected Date date;
        protected Condition condition = Condition.trueCondition();
        protected String message;

        public Builder parse(JSONObject o) throws JSONException {
            ID = o.getInt(JSON_ID);
            from = User.fromJSON(o.getJSONObject(JSON_FROM));
            if (o.isNull(JSON_TO)) {
                to = new User(User.PUBLIC_ID, User.PUBLIC_NAME);
            } else {
                to = Recipient.fromJSON(o.getJSONObject(JSON_TO));
            }
            message = o.getString(JSON_MESSAGE);
            date = new Date(o.getLong(JSON_DATE));

            if (o.isNull(JSON_CONDITION)) {
                condition = Condition.trueCondition();
            } else {
                condition = Condition.fromJSON(new JSONObject(o.getString(JSON_CONDITION)));
            }

            return this;
        }

        public Builder setID(int ID) {
            this.ID = ID;
            return this;
        }

        public Builder setFrom(User from) {
            this.from = from;
            return this;
        }

        public Builder setTo(Recipient to) {
            this.to = to;
            return this;
        }

        public Builder setDate(long date) {
            this.date = new Date(date);
            return this;
        }

        public Builder setDate(Date date) {
            this.date = date;
            return this;
        }

        public Builder setCondition(Condition condition) {
            this.condition = condition;
            return this;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        protected abstract Item build();
    }

    public void addObserver(Item.Observer observer) {
        this.observers.add(observer);
    }

    public boolean removeObserver(Item.Observer observer) {
        return this.observers.remove(observer);
    }

    public abstract static class Observer {
        public abstract void update(Item item);
    }
}
