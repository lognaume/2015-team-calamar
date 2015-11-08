package ch.epfl.sweng.calamar;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

//TODO Support other item types

/**
 * This activity manages the chat between two users (or in a group)
 */
public class ChatActivity extends AppCompatActivity implements View.OnClickListener {
    private EditText editText;
    private Button sendButton;
    private Button refreshButton;
    private List<Item> messagesHistory;
    private ListView messagesContainer;
    private ChatAdapter adapter;

    private ItemClient client;

    private Recipient correspondent;

    private CalamarApplication app;

    private SQLiteDatabaseHandler databaseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        app = ((CalamarApplication) getApplication()).getInstance();

        Intent intent = getIntent();
        String correspondentName = intent.getStringExtra(ChatUsersListActivity.EXTRA_CORRESPONDENT_NAME);
        int correspondentID = intent.getIntExtra(ChatUsersListActivity.EXTRA_CORRESPONDENT_ID,-1); // -1 = default value

        correspondent = new User(correspondentID,correspondentName);

        client = ItemClientLocator.getItemClient();

        editText = (EditText) findViewById(R.id.messageEdit);
        sendButton = (Button) findViewById(R.id.chatSendButton);
        refreshButton = (Button) findViewById(R.id.refreshButton);

        messagesHistory = new ArrayList<>();
        messagesContainer = (ListView) findViewById(R.id.messagesContainer);
        adapter = new ChatAdapter(this, messagesHistory);
        messagesContainer.setAdapter(adapter);

        TextView recipient = (TextView) findViewById(R.id.recipientLabel);
        recipient.setText(correspondent.getName());

        refreshButton.setOnClickListener(this);
        sendButton.setOnClickListener(this);

        databaseHandler = app.getDB();

        boolean offline = true;
        refresh(offline);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.chatSendButton) {
            sendTextItem();
        } else if (v.getId() == R.id.refreshButton) {
            refresh(false);
        } else {
            throw new IllegalArgumentException("Got an unexpected view Id in Onclick");
        }
    }

    /**
     * Gets all messages and display them
     */
    private void refresh(boolean offline) {
        new refreshTask(app.getCurrentUser(), offline).execute(client);
    }

    /**
     * Sends a new text message
     */
    private void sendTextItem() {
        String message = editText.getText().toString();
        Item textMessage = new SimpleTextItem(1,app.getCurrentUser(),correspondent,new Date(),message);
        editText.setText("");
        new sendItemTask(textMessage).execute(client);
    }


    /**
     * Async task for sending a message.
     */
    private class sendItemTask extends AsyncTask<ItemClient, Void, Integer> {

        private final Item item;

        public sendItemTask(Item item) {
            this.item = item;
        }

        @Override
        protected Integer doInBackground(ItemClient... itemClients) {
            try {
                return itemClients[0].send(item);
            } catch (ItemClientException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Integer id) {
            if (id != null) {
                item.setID(id);
                adapter.add(item);
                adapter.notifyDataSetChanged();
                messagesContainer.setSelection(messagesContainer.getCount() - 1);
                databaseHandler.addItem(item);
            } else {
                Toast.makeText(getApplicationContext(), getString(R.string.item_send_error),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * Async task for refreshing / getting new messages.
     */
    private class refreshTask extends AsyncTask<ItemClient, Void, List<Item>> {

        private final Recipient recipient;
        private final boolean offline;

        public refreshTask(Recipient recipient, boolean offline) {
            this.recipient = recipient;
            this.offline = offline;
        }

        @Override
        protected List<Item> doInBackground(ItemClient... itemClients) {
            if (offline) {
                return databaseHandler.getItemsForContact(correspondent);
            } else {
                try {
                    List<Item> items = itemClients[0].getAllItems(recipient, new Date(app.getLastItemsRefresh()));
                    databaseHandler.addItems(items);
                    return itemClients[0].getAllItems(recipient, new Date(app.getLastItemsRefresh()));
                } catch (ItemClientException e) {
                    //TODO : TOAST
                    e.printStackTrace();
                    return null;
                }
            }
        }

        @Override
        protected void onPostExecute(List<Item> items) {
            if (items != null) {
                adapter.add(items);
                adapter.notifyDataSetChanged();
                messagesContainer.setSelection(messagesContainer.getCount() - 1);
                if (!offline) {
                    app.setLastItemsRefresh(new Date());
                }
            }
        }

    }
}
