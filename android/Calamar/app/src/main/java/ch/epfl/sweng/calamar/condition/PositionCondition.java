package ch.epfl.sweng.calamar.condition;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ch.epfl.sweng.calamar.CalamarApplication;
import ch.epfl.sweng.calamar.MainActivity;
import ch.epfl.sweng.calamar.R;
import ch.epfl.sweng.calamar.map.GPSProvider;
import ch.epfl.sweng.calamar.map.MapFragment;

/**
 * Created by pierre on 10/27/15.
 */
public final class PositionCondition extends Condition {

    private final static int MIN_PRECISION = 50;
    private final static int DEFAULT_RADIUS = 20;
    private final static int MIN_LAT = -90;
    private final static int MAX_LAT = 90;
    private final static int MIN_LON = -180;
    private final static int MAX_LON = 180;
    private final static String JSON_LAT = "latitude";
    private final static String JSON_LON = "longitude";
    private final static String JSON_RADIUS = "radius";

    public static final String TAG = PositionCondition.class.getSimpleName();


    private final Location location;
    private final double radius;

    /**
     * construct a PositionCondition from a location and a radius
     *
     * @param location a Location object
     * @param radius   the radius as a double
     */
    public PositionCondition(Location location, double radius) {
        if (null == location) {
            throw new IllegalArgumentException(CalamarApplication.getInstance().getString(R.string.positioncondition_location_null));
        }
        this.location = location;
        this.radius = radius;
        setValue(false);
        GPSProvider.getInstance().addObserver(new GPSProvider.Observer() {
            @Override
            public void update(Location newLocation) {
                if (newLocation.getAccuracy() <= MIN_PRECISION) {
                    setValue(newLocation.distanceTo(getLocation()) < getRadius());
                }
                if (getValue()) {
                    GPSProvider.getInstance().removeObserver(this);
                }
            }
        });
    }


    /**
     * Constructs a PositionCondition with a Location
     *
     * @param location An object Location
     */
    public PositionCondition(Location location) {
        this(location, DEFAULT_RADIUS);
    }

    /**
     * construct a PositionCondition from a latitude, a longitude and a radius
     *
     * @param latitude  the latitude as a double
     * @param longitude the longitude as a double
     * @param radius    the radius as a double
     */
    public PositionCondition(double latitude, double longitude, double radius) {
        this(makeLocation(latitude, longitude), radius);
    }

    /**
     * make a Location from its latitude and longitude
     *
     * @param latitude  the latitude as a double
     * @param longitude the longitude as a double
     * @return Location in this place
     */
    private static Location makeLocation(double latitude, double longitude) {
        if (MIN_LAT > latitude || latitude > MAX_LAT || MIN_LON > longitude || longitude > MAX_LON) {
            throw new IllegalArgumentException(CalamarApplication.getInstance().getString(R.string.invalid_longitude_latitude));
        }

        Location loc = new Location(CalamarApplication.getInstance().getString(R.string.calamar_location_provider));
        loc.setLatitude(latitude);
        loc.setLongitude(longitude);
        return loc;
    }


    @Override
    public Location getLocation() {
        return location;
    }

    public double getRadius() {
        return radius;
    }

    @Override
    public boolean hasLocation() {
        return true;
    }

    /**
     * compose this Condition in the json object
     *
     * @param json jsonObject to put this in
     * @throws JSONException
     */
    @Override
    protected void compose(JSONObject json) throws JSONException {
        json.accumulate(JSON_TYPE, getType().name());
        json.accumulate(JSON_LAT, location.getLatitude());
        json.accumulate(JSON_LON, location.getLongitude());
        json.accumulate(JSON_RADIUS, radius);
    }

    @Override
    public String toString() {
        return "position : (" + location.getLatitude() + " , " + location.getLongitude() + " , " + radius + ")";
    }

    @Override
    public Type getType() {
        return Type.POSITIONCONDITION;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionCondition)) return false;
        PositionCondition that = (PositionCondition) o;
        return super.equals(that) && location.getLatitude() == that.location.getLatitude() && location.getLongitude() == that.location.getLongitude() && radius == that.radius;
    }

    @Override
    public int hashCode() {
        return super.hashCode() * 83 + (int) Math.round(location.getLatitude()) * 137 + (int) (location.getLongitude()) * 191 + (int) Math.round(radius) * 317;
    }

    @Override
    public JSONArray getMetadata() throws JSONException {
        JSONArray array = new JSONArray();
        JSONObject jObject = new JSONObject();
        jObject.accumulate(JSON_TYPE, getType().name());
        jObject.accumulate(JSON_LAT, location.getLatitude());
        jObject.accumulate(JSON_LON, location.getLongitude());
        array.put(jObject);
        return array;
    }

    @Override
    public View getView(final Activity context) {
        LinearLayout view = (LinearLayout) (super.getView(context));
        view.setOrientation(LinearLayout.VERTICAL);

        // TODO make this looks better, and inflate from xml instead of like now :
        TextView positionText = new TextView(context);
        positionText.setText(this.toString());
        positionText.setTextSize(15);
        view.addView(positionText, 0);
        if (!context.getClass().equals(MainActivity.class)) {
            Button button = new Button(context);
            button.setText(context.getResources().getString(R.string.condition_position));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(CalamarApplication.getInstance(), MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    intent.putExtra(MainActivity.TABKEY, MainActivity.TabID.MAP.ordinal());

                    // TODO ideally put location and find way to retrieve it, I think this must be easy ?
                    // intent.putExtra(MapFragment.POSITIONKEY, getLocation());
                    intent.putExtra(MapFragment.LATITUDEKEY,
                            PositionCondition.this.getLocation().getLatitude());
                    intent.putExtra(MapFragment.LONGITUDEKEY,
                            PositionCondition.this.getLocation().getLongitude());

                    CalamarApplication.getInstance().startActivity(intent);
                }
            });
            view.addView(button, 1);
        }
        return view;
    }

    /**
     * create a Condition from a JSONObject
     *
     * @param json Object in JSON format
     * @return the desired condition Condition
     * @throws JSONException
     * @throws IllegalArgumentException
     */
    public static Condition fromJSON(JSONObject json) throws JSONException {
        return new PositionCondition.Builder().parse(json).build();
    }

    /**
     * A Builder for {@link PositionCondition}, currently only used to parse JSON
     *
     * @see Condition.Builder
     */
    public static class Builder extends Condition.Builder {

        private double latitude, longitude;
        private double radius;

        @Override
        public Builder parse(JSONObject json) throws JSONException {
            super.parse(json);
            String type = json.getString(JSON_TYPE);
            if (Type.valueOf(type) != Type.POSITIONCONDITION) {
                throw new IllegalArgumentException(CalamarApplication.getInstance().getString(R.string.expected_but_was, Type.POSITIONCONDITION.name(), type));
            }
            latitude = json.getDouble(JSON_LAT);
            longitude = json.getDouble(JSON_LON);
            radius = json.getDouble(JSON_RADIUS);
            return this;
        }

        /**
         * Builds the Condition
         *
         * @return The built PositionCondition
         */
        public PositionCondition build() {
            return new PositionCondition(latitude, longitude, radius);
        }
    }
}
