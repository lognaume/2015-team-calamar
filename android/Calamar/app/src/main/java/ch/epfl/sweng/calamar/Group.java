package ch.epfl.sweng.calamar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by LPI on 16.10.2015.
 */
//TODO add JSON methods (compose, builder.parse etc.) for users and type
public final class Group extends Recipient {
    private final List<User> users;
    //type = group, virtual field ^^

    public Group(int ID, String name) {
        this(ID, name, new ArrayList<User>());
    }

    @Override
    public JSONObject toJSON() throws JSONException {
        //TODO
        return null;
    }

    public Group(int ID, String name, List<User> users) {
        super(ID, name);
        if(null == users) {
            throw new IllegalArgumentException("field 'users' cannot be null");
        }
        this.users = new ArrayList<>(users);//User is immutable
    }
}
