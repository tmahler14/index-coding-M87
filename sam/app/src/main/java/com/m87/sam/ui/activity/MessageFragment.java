package com.m87.sam.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.m87.sam.R;
import com.m87.sam.ui.pojos.ChatMessage;
import com.m87.sam.ui.util.Logger;
import com.m87.sdk.ProximityEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class MessageFragment extends Fragment {

    public static ArrayList<ChatMessage> messageList = new ArrayList<ChatMessage>();
    public static ArrayAdapter messageListAdapter;
    private ListView messageListView;
    private Context context;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        this.context = activity.getApplication().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_message, null);
        messageListView = (ListView) mRootView.findViewById(R.id.lv_messages);
        messageListAdapter = new MessageArrayAdapter(this.context, R.layout.message_receive_list_item);
        messageListView.setAdapter(messageListAdapter);
        return mRootView;
    }

    public class MessageArrayAdapter extends ArrayAdapter<ChatMessage>
    {
        private Context context;

        public MessageArrayAdapter(Context context, int textViewResourceId)
        {
            super(context, textViewResourceId);
            this.context = context;
        }

        public ChatMessage getItem(int index) {
            return messageList.get(index);
        }

        public int getCount() {
            return messageList.size();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ChatMessage chatMessageObj = getItem(position);
            View row = convertView;
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            if (chatMessageObj.received) {
                row = inflater.inflate(R.layout.message_receive_list_item, parent, false);
            }else{
                row = inflater.inflate(R.layout.message_sent_list_item, parent, false);
            }

            // Get views
            TextView chatText = (TextView) row.findViewById(R.id.msgr);
            TextView chatTime = (TextView) row.findViewById(R.id.msgr_info);

            // Set text
            chatText.setText(chatMessageObj.message);
            SimpleDateFormat df = new SimpleDateFormat("h:mm a");
            chatTime.setText(df.format(chatMessageObj.timestamp.getTime()));

            return row;
        }
    }
}
