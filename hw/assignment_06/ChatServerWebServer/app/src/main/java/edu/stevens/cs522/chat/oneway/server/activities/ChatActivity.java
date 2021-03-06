/*********************************************************************
 * Chat server: accept chat messages from clients.
 * <p/>
 * Sender name and GPS coordinates are encoded
 * in the messages, and stripped off upon receipt.
 * <p/>
 * Copyright (c) 2012 Stevens Institute of Technology
 **********************************************************************/
package edu.stevens.cs522.chat.oneway.server.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import java.util.UUID;

import edu.stevens.cs522.chat.oneway.server.R;
import edu.stevens.cs522.chat.oneway.server.adapters.MessageAdapter;
import edu.stevens.cs522.chat.oneway.server.contracts.MessageContract;
import edu.stevens.cs522.chat.oneway.server.entities.Message;
import edu.stevens.cs522.chat.oneway.server.managers.IQueryListener;
import edu.stevens.cs522.chat.oneway.server.managers.QueryBuilder;
import edu.stevens.cs522.chat.oneway.server.managers.TypedCursor;
import edu.stevens.cs522.chat.oneway.server.requests.ServiceHelper;
import edu.stevens.cs522.chat.oneway.server.utils.CommonSettings;
import edu.stevens.cs522.chat.oneway.server.utils.ResultReceiverWrapper;

public class ChatActivity extends Activity {

    final static public String TAG = ChatActivity.class.getCanonicalName();
    final static public long DEFAULT_USER_ID = 0;
    final static public String DEFAULT_USER_NAME = "no name";
    final static public int PREFERENCES_REQUEST = 1;

    private long userId;
    private String userName;
    private UUID registrationID;


    /*
     * TODO: Declare UI.
     */
//    ArrayList<Message> messageList;
    private ListView msgList;
    private Button sendButton;
    private EditText destinationHost;
    private EditText destinationPort;
    private EditText messageText;
    private MessageAdapter cursorAdapter;

    private ResultReceiverWrapper registerResultReceiverWrapper;
    private ResultReceiverWrapper.IReceiver registerResultReceiver;
    private ResultReceiverWrapper postMessageResultReceiverWrapper;
    private ResultReceiverWrapper.IReceiver postMessageResultReceiver;


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        /**
         * Let's be clear, this is a HACK to allow you to do network communication on the main thread.
         * This WILL cause an ANR, and is only provided to simplify the pedagogy.  We will see how to do
         * this right in a future assignment (using a Service managing background threads).
         */
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        userId = prefs.getLong(CommonSettings.PREF_KEY_USERID, DEFAULT_USER_ID);
        userName = prefs.getString(CommonSettings.PREF_KEY_USERNAME, DEFAULT_USER_NAME);
        String uuidString = prefs.getString(CommonSettings.PREF_KEY_REGISTRATION_ID, "");
        if (!uuidString.isEmpty())
            registrationID = UUID.fromString(uuidString);



        cursorAdapter = new MessageAdapter(this, null);
        msgList = (ListView) findViewById(R.id.main_lst_messages);
        msgList.setAdapter(cursorAdapter);

        QueryBuilder.executeQuery(TAG,
                this,
                MessageContract.CONTENT_URI,
                MessageContract.CURSOR_LOADER_ID,
                MessageContract.DEFAULT_ENTITY_CREATOR,
                new IQueryListener<Message>() {
                    @Override
                    public void handleResults(TypedCursor<Message> cursor) {
                        cursorAdapter.swapCursor(cursor.getCursor());
                    }

                    @Override
                    public void closeResults() {
                        cursorAdapter.swapCursor(null);
                    }

                });
//        clientPort = Integer.valueOf(prefs.getString(PreferencesActivity.PREF_KEY_PORT, String.valueOf(DEFAULT_CLIENT_PORT)));

        messageText = (EditText) findViewById(R.id.main_edt_message);
        sendButton = (Button) findViewById(R.id.main_btn_send);
        sendButton.setEnabled(registrationID != null);
        sendButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = messageText.getText().toString();
                if (!msg.isEmpty()) {
                    ServiceHelper helper = new ServiceHelper();
                    helper.postMessageAsync(ChatActivity.this,
                            registrationID,
                            userId,
                            userName,
                            msg,
                            postMessageResultReceiverWrapper);
                }
            }
        });

        registerResultReceiverWrapper = new ResultReceiverWrapper(new Handler());
        registerResultReceiver = new ResultReceiverWrapper.IReceiver() {
            @Override
            public void onReceiveResult(int resultCode, Bundle resultData) {
                Log.d(TAG, String.valueOf(resultCode));
                long id = resultData.getLong(ServiceHelper.EXTRA_REGISTER_RESULT_ID);
                UUID uuid = UUID.fromString(resultData.getString(ServiceHelper.EXTRA_REGISTER_REG_ID));

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ChatActivity.this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(CommonSettings.PREF_KEY_REGISTRATION_ID, uuid.toString());
                editor.putLong(CommonSettings.PREF_KEY_USERID, id);
                editor.apply();

                registrationID = uuid;
                userId = id;

                Toast.makeText(getApplicationContext(), "Registration Succeeded", Toast.LENGTH_LONG).show();
                if(!sendButton.isEnabled()) {
                    sendButton.setEnabled(true);
                }
            }
        };

        postMessageResultReceiverWrapper = new ResultReceiverWrapper(new Handler());
        postMessageResultReceiver = new ResultReceiverWrapper.IReceiver() {
            @Override
            public void onReceiveResult(int resultCode, Bundle resultData) {
                messageText.setText("");
                updateListView();
            }
        };
//        broadcastRceiver = new MessageReceiver();
//        resultReceicerWrapper = new ResultReceiverWrapper(new Handler());
//        resultReceiver = new ResultReceiverWrapper.IReceiver() {
//            @Override
//            public void onReceiveResult(int resultCode, Bundle resultData) {
//                String toastMessage = "";
//                switch (resultCode) {
//                    case ChatSendService.RESULT_RECEIVER_RESULT_CODE_SUCCESS:
//                        toastMessage = "Your message was sent!";
//                        messageText.setText("");
//                        break;
//                    case ChatSendService.RESULT_RECEIVER_RESULT_CODE_ERROR:
//                        toastMessage = "Sorry, an unexpected error has occurred. Try again.";
//                        break;
//                    default:
//                        throw new IllegalArgumentException("Unknown Result Code: " + resultCode);
//                }
//                Toast.makeText(getApplicationContext(), toastMessage, Toast.LENGTH_LONG).show();
//
//            }
//        };
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onStart() {
        super.onStart();
//        client.connect();
//        Intent receiverIntent = new Intent(this, ChatReceiverService.class);
//        receiverIntent.putExtra(ChatReceiverService.EXTRA_SOCKET_PORT, clientPort);
//        startService(receiverIntent);
//
//        IntentFilter filter = new IntentFilter(ChatReceiverService.NEW_MESSAGE_BROADCAST);
//        registerReceiver(broadcastRceiver, filter);

    }

    @Override
    protected void onStop() {
//        unregisterReceiver(broadcastRceiver);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        registerResultReceiverWrapper.setReceiver(null);
        postMessageResultReceiverWrapper.setReceiver(null);
//        stopService(new Intent(this, ChatReceiverService.class));
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerResultReceiverWrapper.setReceiver(registerResultReceiver);
        postMessageResultReceiverWrapper.setReceiver(postMessageResultReceiver);
//        resultReceicerWrapper.setReceiver(resultReceiver);
//        Intent bindIntent = new Intent(this, ChatSendService.class);
//        bindService(bindIntent, conn, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onPause() {
        super.onPause();
//        unbindService(conn);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
//            case R.id.chat_menu_contacts:
//                startActivity(new Intent(this, ContactBookActivity.class));
//                return true;
            case R.id.chat_menu_prefs:
                startActivityForResult(new Intent(this, PreferencesActivity.class), PREFERENCES_REQUEST);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        // TODO Handle results from the Search and Checkout activities.
        Log.d(TAG, "returned from preferences");
        switch (requestCode) {
            case PREFERENCES_REQUEST:
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                userName = prefs.getString(CommonSettings.PREF_KEY_USERNAME, DEFAULT_USER_NAME);

                if (!prefs.contains(CommonSettings.PREF_KEY_REGISTRATION_ID)) {

                    final UUID uuid = UUID.randomUUID();
//                final UUID uuid = UUID.fromString("54947df8-0e9e-4471-a2f9-9af509fb5889");

                    ServiceHelper helper = new ServiceHelper();
                    helper.registerAsync(this, userName, uuid, registerResultReceiverWrapper);
                }

                break;
        }
    }

    public void updateListView() {
        QueryBuilder.executeQuery(TAG,
                ChatActivity.this,
                MessageContract.CONTENT_URI,
                MessageContract.CURSOR_LOADER_ID,
                MessageContract.DEFAULT_ENTITY_CREATOR,
                new IQueryListener<Message>() {
                    @Override
                    public void handleResults(TypedCursor<Message> cursor) {
                        cursorAdapter.swapCursor(cursor.getCursor());
                    }

                    @Override
                    public void closeResults() {
                        cursorAdapter.swapCursor(null);
                    }

                });
    }


//    public class MessageReceiver extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            QueryBuilder.executeQuery(TAG,
//                    ChatActivity.this,
//                    MessageContract.CONTENT_URI,
//                    MessageContract.CURSOR_LOADER_ID,
//                    MessageContract.DEFAULT_ENTITY_CREATOR,
//                    new IQueryListener<Message>() {
//                        @Override
//                        public void handleResults(TypedCursor<Message> cursor) {
//                            cursorAdapter.swapCursor(cursor.getCursor());
//                        }
//
//                        @Override
//                        public void closeResults() {
//                            cursorAdapter.swapCursor(null);
//                        }
//
//                    });
//        }
//    }
}