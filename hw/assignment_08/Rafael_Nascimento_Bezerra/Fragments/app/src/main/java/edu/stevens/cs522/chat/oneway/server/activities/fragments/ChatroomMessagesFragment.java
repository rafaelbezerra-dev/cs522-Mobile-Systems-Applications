package edu.stevens.cs522.chat.oneway.server.activities.fragments;


import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import edu.stevens.cs522.chat.oneway.server.R;
import edu.stevens.cs522.chat.oneway.server.adapters.MessageRowAdapter;
import edu.stevens.cs522.chat.oneway.server.entities.Chatroom;
import edu.stevens.cs522.chat.oneway.server.managers.IQueryListener;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChatroomMessagesFragment extends Fragment {


    public interface IChatroomMessagesFragmentListener {
        public void getChatroomMessagesAsync(Chatroom chatroom);

        public Cursor getMessageListCursor();
    }

    private static final String TAG = ChatroomMessagesFragment.class.getCanonicalName();
    public static final String CHATROOM_DETAILS_KEY = TAG + "chatroom_details";


    private TextView infoMsgView;
    private ListView listView;

    IQueryListener queryListener;
    MessageRowAdapter cursorAdapter;
    IChatroomMessagesFragmentListener listener;
    private boolean isActivityCreated;
    private Chatroom chatroom;

    public ChatroomMessagesFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.frag__chatroom_messages, container, false);

        cursorAdapter = new MessageRowAdapter(getActivity(), null);
        infoMsgView = (TextView) view.findViewById(R.id.frag__chatroom_messages_info);
        listView = (ListView) view.findViewById(R.id.frag__chatroom_messages_list);
        listView.setAdapter(cursorAdapter);

        Bundle args = getArguments();
        if (args != null && args.containsKey(CHATROOM_DETAILS_KEY)) {
            chatroom = args.getParcelable(CHATROOM_DETAILS_KEY);
            infoMsgView.setVisibility(View.GONE);
        } else {
            infoMsgView.setText(getResources().getText(R.string.frag__chatroom_messages_no_chat));
            infoMsgView.setVisibility(View.VISIBLE);

            if (view.findViewById(R.id.frag__chatroom_messages_container) != null) {
                view.findViewById(R.id.frag__chatroom_messages_container).setVisibility(View.GONE);
            }
        }


        return view;
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.listener = (IChatroomMessagesFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(this.listener.toString()
                    + " must implement IChatroomMessagesFragmentListener");
        }

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.isActivityCreated = true;
        if (this.chatroom != null) {
//            this.listener.getChatroomMessagesAsync(this.chatroom);
            this.setListCursor(this.listener.getMessageListCursor());
        }


    }

    @Override
    public void onDetach() {
        super.onDetach();
        this.listener = new IChatroomMessagesFragmentListener() {
            @Override
            public void getChatroomMessagesAsync(Chatroom chatroom) {
            }

            @Override
            public Cursor getMessageListCursor() {
                return null;
            }
        };

    }

    public IQueryListener getQueryListener() {
        return queryListener;
    }

    public void setListCursor(Cursor cursor) {
        if (cursorAdapter != null)
            cursorAdapter.swapCursor(cursor);
        if (infoMsgView != null) {
            if (cursor != null && cursor.getCount() > 0) {
                infoMsgView.setVisibility(View.GONE);
            }
            else {
                infoMsgView.setText(getResources().getText(R.string.frag__chatroom_messages_empty_list));
                infoMsgView.setVisibility(View.VISIBLE);
            }
        }
    }

}