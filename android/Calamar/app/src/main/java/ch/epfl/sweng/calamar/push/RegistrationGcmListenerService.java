/**
 * Copyright 2015 Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * >>>>>>> dc774c0b4e22aff9aa7e58950946e847eb05e6d8:android/Calamar/app/src/main/java/ch/epfl/sweng/calamar/push/RegistrationGcmListenerService.java
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.epfl.sweng.calamar.push;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import ch.epfl.sweng.calamar.MainActivity;
import ch.epfl.sweng.calamar.R;
import ch.epfl.sweng.calamar.chat.ChatFragment;
import ch.epfl.sweng.calamar.item.Item;
import ch.epfl.sweng.calamar.recipient.User;

public final class RegistrationGcmListenerService extends GcmListenerService {

    private static final String TAG = "RegGcmListenerService";
    public static final String RETRIEVE = "RETRIEVE";
    private static final String BUNDLE_TYPE = "type";
    private static final String BUNDLE_EXTRA = "extra";
    private static final int REQUEST_CODE = 0;
    private static final int NOTIFICATION_ID = 0;

    /**
     * Called when message is received.
     *
     * @param from SenderID of the sender.
     * @param data Data bundle containing message data as key/value pairs.
     *             For Set of keys use data.keySet().
     */
    // [START receive_message]
    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = getString(R.string.you_received_new);
        String action = MainActivity.ACTION_OPEN_CHAT;
        String pushType = data.getString(BUNDLE_TYPE);
        String title;
        User fromUser;

        //extract from User Data
        try {
            JSONObject resp = new JSONObject(data.getString(BUNDLE_EXTRA));
            fromUser = User.fromJSON(resp);
        } catch (JSONException e) {
            Log.e(TAG, getString(R.string.json_extract_failed));
            return; // push corrupted
        }


        if (pushType != null && pushType.equals(RETRIEVE)) {
            message += getString(R.string.contact);
            title = "contact";
        } else {
            Item.Type type = Item.Type.valueOf(pushType);
            //Log.d(TAG, "From: " + from);
            Log.d(TAG, getString(R.string.message_type, type));
            title = "item";

            switch (type) {
                case SIMPLETEXTITEM:
                    message += getString(R.string.chat_item);
                    break;
                case FILEITEM:
                    message += getString(R.string.file_item);
                    break;
                case IMAGEITEM:
                    message += getString(R.string.image_item);
                    break;
                default:
                    Log.e(TAG, getString(R.string.unexpected_item_type, type.name()));
            }
        }

        // Send a broadcast message to ChatFragment$ChatBroadcastReceiver
        Intent i = new Intent();
        i.setAction(ChatFragment.ChatBroadcastReceiver.INTENT_FILTER);
        i.putExtra(ChatFragment.ChatBroadcastReceiver.BROADCAST_EXTRA_USER, fromUser.getName());
        i.putExtra(ChatFragment.ChatBroadcastReceiver.BROADCAST_EXTRA_ID, String.valueOf(fromUser.getID()));
        i.putExtra(ChatFragment.ChatBroadcastReceiver.BROADCAST_EXTRA_TYPE,pushType);
        sendBroadcast(i);

        sendNotification(message, action, title);

    }
    // [END receive_message]

    /**
     * Create and show a simple notification containing the received GCM message.
     *
     * @param message GCM message received.
     */
    private void sendNotification(String message,String action,String title) {
        //TODO improve the methods
        Log.i(TAG, getString(R.string.notification_message, message));
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(MainActivity.TABKEY, MainActivity.TabID.CHAT.ordinal());
        PendingIntent pendingIntent = PendingIntent.getActivity(this, REQUEST_CODE, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getString(R.string.new_item_notification) + title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.calamar);


        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }
}