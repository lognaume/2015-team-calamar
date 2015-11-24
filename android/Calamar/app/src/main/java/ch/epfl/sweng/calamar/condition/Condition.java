package ch.epfl.sweng.calamar.condition;

import org.json.JSONArray;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import ch.epfl.sweng.calamar.R;

/**
 * Created by pierre on 10/27/15.
 */
public abstract class Condition {

    private Boolean value = false;
    private final Set<Observer> observers = new HashSet<>();

    private static JSONArray concatArray(JSONArray... arrays)
            throws JSONException {
        JSONArray result = new JSONArray();
        for (JSONArray array : arrays) {
            for (int i = 0; i < array.length(); i++) {
                result.put(array.get(i));
            }
        }
        return result;
    }

    /**
     * compose this Condition in the json object
     *
     * @param json jsonObject to put this in
     * @throws JSONException
     */
    protected abstract void compose(JSONObject json) throws JSONException;

    /**
     * get a JSON description of this
     *
     * @return JSONObject describing this
     * @throws JSONException
     */
    public JSONObject toJSON() throws JSONException {
        JSONObject ret = new JSONObject();
        this.compose(ret);
        ret.accumulate("metadata", getMetadata());
        return ret;
    }

    @Override
    public abstract String toString();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Condition)) return false;
        Condition that = (Condition) o;
        return value == that.value && type().equals(that.type());
    }

    public abstract String type();

    public View getView(Context context)
    {
        return new FrameLayout(context) {
            Paint paint = new Paint();

            {
                addObserver(new Observer() {
                    @Override
                    public void update(Condition condition) {
                        invalidate();
                    }
                });
            }

            @Override
            public void onDraw(Canvas canvas) {
                paint.setColor(getValue() ? Color.GREEN : Color.RED);
                paint.setStrokeWidth(3);
                canvas.drawRect(0, 0, getWidth(), getHeight(), paint);
            }
        };
    }


    /**
     * set a value for this condition. If newValue differs from old, notify observers
     *
     * @param newValue new value to set
     */
    protected void setValue(Boolean newValue) {
        if (value != newValue) {
            value = newValue;
            for (Observer o : observers) {
                o.update(this);
            }
        }
    }

    public boolean getValue() {
        return value;
    }

    public JSONArray getMetadata() throws JSONException { return new JSONArray(); }

    /**
     * create a Condition from a JSONObject
     *
     * @param json Object in JSON format
     * @return the desired condition Condition
     * @throws JSONException
     * @throws IllegalArgumentException
     */
    public static Condition fromJSON(JSONObject json) throws JSONException, IllegalArgumentException {
        if (null == json || json.isNull("type")) {
            throw new IllegalArgumentException("malformed json, either null or no 'type' value");
        }
        Condition cond;
        String type = json.getString("type");
        switch (type) {
            case "position":
                cond = PositionCondition.fromJSON(json);
                break;
            case "and":
                cond = and(fromJSON(json.getJSONObject("a")), fromJSON(json.getJSONObject("b")));
                break;
            case "or":
                cond = or(fromJSON(json.getJSONObject("a")), fromJSON(json.getJSONObject("b")));
                break;
            case "not":
                cond = not(fromJSON(json.getJSONObject("val")));
                break;
            case "true":
                cond = trueCondition();
                break;
            case "false":
                cond = falseCondition();
                break;
            default:
                throw new IllegalArgumentException("Unexpected Item type (" + type + ")");
        }
        return cond;
    }

    /**
     * Create an always true condition
     *
     * @return true condition
     */
    public static Condition trueCondition() {
        return new Condition() {
            {
                this.setValue(true);
            }

            @Override
            protected void compose(JSONObject json) throws JSONException {
                json.accumulate("type", "true");
            }

            @Override
            public String toString() {
                return "(true)";
            }

            @Override
            public String type() {
                return "true";
            }

            @Override
            public View getView(Context context)
            {
                FrameLayout view = (FrameLayout)(super.getView(context));
                TextView tv = new TextView(context);
                tv.setText(context.getResources().getString(R.string.condition_true));
                view.addView(tv);
                return view;
            }
        };
    }


    /**
     * Create an always false condition
     *
     * @return false condition
     */
    public static Condition falseCondition() {
        return new Condition() {
            {
                setValue(false);
            }

            @Override
            protected void compose(JSONObject json) throws JSONException {
                json.accumulate("type", "false");
            }

            @Override
            public String toString() {
                return "(false)";
            }

            @Override
            public String type() {
                return "false";
            }

            @Override
            public View getView(Context context)
            {
                FrameLayout view = (FrameLayout)(super.getView(context));
                TextView tv = new TextView(context);
                tv.setText(context.getResources().getString(R.string.condition_false));
                view.addView(tv);
                return view;
            }
        };
    }

    /**
     * create a condition that represent the intersection of two conditions
     *
     * @param c1 first condition
     * @param c2 second condition
     * @return a new condition that is the intersection of c1 and c2
     */
    public static Condition and(final Condition c1, final Condition c2) {
        return new Condition() {
            //constructor
            {
                setValue(c1.value && c2.value);
                Condition.Observer o = new Observer() {

                    @Override
                    public void update(Condition c) {
                        setValue(c1.value && c2.value);
                    }
                };
                c1.addObserver(o);
                c2.addObserver(o);
            }


            @Override
            protected void compose(JSONObject json) throws JSONException {
                json.accumulate("type", "and");
                json.accumulate("a", c1.toJSON());
                json.accumulate("b", c2.toJSON());
            }

            @Override
            public String toString() {
                return "(" + c1.toString() + " && " + c2.toString() + ")";
            }

            @Override
            public String type() {
                return "and";
            }

            @Override
            public JSONArray getMetadata() throws JSONException { return concatArray(c1.getMetadata(), c2.getMetadata()); }
            public View getView(Context context)
            {
                FrameLayout view = (FrameLayout)(super.getView(context));
                LinearLayout LL = new LinearLayout(context);
                LL.setOrientation(LinearLayout.VERTICAL);
                TextView tv = new TextView(context);
                tv.setText(context.getResources().getString(R.string.condition_and));
                LL.addView(c1.getView(context), 0);
                LL.addView(tv, 1);
                LL.addView(c2.getView(context), 2);
                view.addView(LL);
                return view;
            }
        };
    }

    /**
     * create a condition that represent the union of two conditions
     *
     * @param c1 first condition
     * @param c2 second condition
     * @return a new condition that is the union of c1 and c2
     */
    public static Condition or(final Condition c1, final Condition c2) {
        return new Condition() {
            //constructor
            {
                setValue(c1.value || c2.value);
                Condition.Observer o = new Observer() {

                    @Override
                    public void update(Condition c) {
                        setValue(c1.value || c2.value);
                    }
                };
                c1.addObserver(o);
                c2.addObserver(o);
            }

            @Override
            protected void compose(JSONObject json) throws JSONException {
                json.accumulate("type", "or");
                json.accumulate("a", c1.toJSON());
                json.accumulate("b", c2.toJSON());
            }

            @Override
            public String toString() {
                return "(" + c1.toString() + " || " + c2.toString() + ")";
            }

            @Override
            public String type() {
                return "or";
            }

            @Override
            public JSONArray getMetadata() throws JSONException { return concatArray(c1.getMetadata(), c2.getMetadata()); }

            public View getView(Context context)
            {
                FrameLayout view = (FrameLayout)(super.getView(context));
                LinearLayout LL = new LinearLayout(context);
                LL.setOrientation(LinearLayout.VERTICAL);
                TextView tv = new TextView(context);
                tv.setText(context.getResources().getString(R.string.condition_or));
                LL.addView(c1.getView(context), 0);
                LL.addView(tv, 1);
                LL.addView(c2.getView(context), 2);
                view.addView(LL);
                return view;
            }
        };
    }

    /**
     * negats a condition
     *
     * @param c condition to negate
     * @return a condition that is true when c is false and false when c is true
     */
    public static Condition not(final Condition c) {
        return new Condition() {
            //constructor
            {
                setValue(!c.value);
                Condition.Observer o = new Observer() {

                    @Override
                    public void update(Condition c) {
                        setValue(!c.value);
                    }
                };
                c.addObserver(o);
            }

            @Override
            protected void compose(JSONObject json) throws JSONException {
                json.accumulate("type", "not");
                json.accumulate("val", c.toJSON());
            }

            @Override
            public String toString() {
                return "(!" + c.toString() + ")";
            }

            @Override
            public String type() {
                return "not";
            }

            // TODO How to deal metadata with not operator ?

            @Override
            public View getView(Context context)
            {
                FrameLayout view = (FrameLayout)(super.getView(context));
                LinearLayout LL = new LinearLayout(context);
                LL.setOrientation(LinearLayout.VERTICAL);
                TextView tv = new TextView(context);
                tv.setText(context.getResources().getString(R.string.condition_not));
                LL.addView(tv, 0);
                LL.addView(c.getView(context), 1);
                view.addView(LL);
                return view;
            }
        };
    }

    /**
     * A Builder for {@link Condition}, has no build() method since Item isn't instantiable,
     * is used by the child builders (in {@link PositionCondition} or...) to build the "Condition
     * part of the object". currently only used to parse JSON
     */
    public static class Builder {

        public Builder parse(JSONObject o) throws JSONException {
            return this;
        }
    }

    public void addObserver(Condition.Observer observer) {
        this.observers.add(observer);
    }

    public boolean removeObserver(Condition.Observer observer) {
        return this.observers.remove(observer);
    }

    public abstract static class Observer {
        public abstract void update(Condition condition);
    }
}