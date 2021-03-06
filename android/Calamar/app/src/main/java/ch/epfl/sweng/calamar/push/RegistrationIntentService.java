/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ch.epfl.sweng.calamar.push;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GcmPubSub;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

import ch.epfl.sweng.calamar.CalamarApplication;
import ch.epfl.sweng.calamar.R;
import ch.epfl.sweng.calamar.client.DatabaseClientException;
import ch.epfl.sweng.calamar.client.DatabaseClientLocator;

public final class RegistrationIntentService extends IntentService {

    public static final String SENT_TOKEN_TO_SERVER = "sent_token_to_server_gcm";
    private static final String REGISTRATION_COMPLETE = "registration_complete_gcm";
    private static final String TAG = "RegIntentService";
    private static final String[] TOPICS = {"global"};
    private static final String TOPICS_STR = "/topics/";

    /**
     * Creates a new RegistrationIntentService
     */
    public RegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        try {
            // [START register_for_gcm]
            // Initially this call goes out to the network to retrieve the token, subsequent calls
            // are local.
            // R.string.gcm_defaultSenderId (the Sender ID) is typically derived from google-services.json.
            // See https://developers.google.com/cloud-messaging/android/start for details on this file.
            // [START get_token]
            InstanceID instanceID = InstanceID.getInstance(this);
            String token = instanceID.getToken(getString(R.string.gcm_defaultSenderId),
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            // [END get_token]
            Log.i(TAG, getString(R.string.gcm_registration_token, token));
            sendRegistrationToServer(token);

            // Subscribe to topic channels
            subscribeTopics(token);

            // You should store a boolean that indicates whether the generated token has been
            // sent to your server. If the boolean is false, send the token to your server,
            // otherwise your server should have already received the token.
            CalamarApplication.getInstance().setTokenSent(true);
            // [END register_for_gcm]
        } catch (Exception e) {
            Log.d(TAG, getString(R.string.failed_token_refresh), e);
            e.printStackTrace();
            // If an exception happens while fetching the new token or updating our registration data
            // on a third-party server, this ensures that we'll attempt the update at a later time.
            CalamarApplication.getInstance().setTokenSent(false);
        }
        // Notify UI that registration has completed, so the progress indicator can be hidden.
        Intent registrationComplete = new Intent(REGISTRATION_COMPLETE);
        LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
    }

    /**
     * Persist registration to third-party servers.
     * <p/>
     * Modify this method to associate the user's GCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        final String accountName = CalamarApplication.getInstance().getCurrentUserName();
        Log.i(TAG, getString(R.string.token_name_is, token, accountName));
        //  client.send(token, accountName);
        new createNewUserTask(accountName, token).execute();
    }

    /**
     * Subscribe to any GCM topics of interest, as defined by the TOPICS constant.
     *
     * @param token GCM token
     * @throws IOException if unable to reach the GCM PubSub service
     */
    // [START subscribe_topics]
    private void subscribeTopics(String token) throws IOException {
        GcmPubSub pubSub = GcmPubSub.getInstance(this);
        for (String topic : TOPICS) {
            pubSub.subscribe(token, TOPICS_STR + topic, null);
        }
    }
    // [END subscribe_topics]

    /**
     * AsyncTask to create a new User and tell it to the server
     */
    private class createNewUserTask extends AsyncTask<Void, Void, Integer> {
        private String name = null;
        private String token = null;

        public createNewUserTask(String name, String token) {
            this.name = name;
            this.token = token;
        }

        @Override
        protected Integer doInBackground(Void... v) {
            try {
                //Get the device id.
                return DatabaseClientLocator.getDatabaseClient().newUser(name,
                        token);//"aaaaaaaaaaaaaaaa",354436053190805
            } catch (DatabaseClientException e) {
                e.printStackTrace();
                // and make use of context safe
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer id) {
            Handler mHandler = new Handler(getMainLooper());
            if (id != null) {
                CalamarApplication.getInstance().setCurrentUserID(id);

                //show toast
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Show toast
                        Context context = getApplicationContext();
                        Toast toast = Toast.makeText(context, context.getString(R.string.connected_as_toast, name),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });


            } else {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Show toast
                        Context context = getApplicationContext();
                        Toast toast = Toast.makeText(context, context.getString(R.string.account_not_connected),
                                Toast.LENGTH_SHORT);
                        toast.show();
                    }
                });

            }
        }
    }
}