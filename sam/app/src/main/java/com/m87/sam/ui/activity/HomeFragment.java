package com.m87.sam.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.m87.sam.R;
import com.m87.sam.ui.fragment.NeighborsFragment;
import com.m87.sdk.ProximityEntry;

import java.util.ArrayList;

public class HomeFragment extends Fragment {

    public static ArrayList<ProximityEntry> neighborList = new ArrayList<ProximityEntry>();
    public static ArrayAdapter neighborListAdapter;
    private ListView neighborListView;
    private Context context;

    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        this.context = activity.getApplication().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup mRootView = (ViewGroup) inflater.inflate(R.layout.fragment_home, null);
        neighborListView = (ListView) mRootView.findViewById(R.id.lv_home_neighbors);
        neighborListAdapter = new RouteArrayAdapter(this.context, neighborList);
        neighborListView.setAdapter(neighborListAdapter);
        neighborListView.setClickable(true);
        neighborListView.setOnItemClickListener(new neighborClickListener());
        return mRootView;
    }

    public class neighborClickListener implements AdapterView.OnItemClickListener
    {
        public void onItemClick(AdapterView parentView, View childView, int position, long id)
        {
            // TODO: Add interaction with neighbour
        }
    }

    public void display()
    {
        neighborListAdapter.notifyDataSetChanged();
    }

    public String findExpressionById(int id)
    {
        for (ProximityEntry n : neighborList)
        {
            if (n.id() == id) return n.expression();
        }
        return "";
    }

    public class RouteArrayAdapter extends ArrayAdapter<ProximityEntry>
    {
        private Context context;
        private  ArrayList routingTable;

        public RouteArrayAdapter(Context context, ArrayList routingTable)
        {
            super(context, R.layout.section_route_table, routingTable);
            this.context = context;
            this.routingTable = routingTable;
        }

        private int getTextColor(int hop_count)
        {
            switch (hop_count)
            {
                case 1:  return Color.argb(255, 135, 197, 77);
                case 2:  return Color.argb(255, 133, 165, 45);
                case 3:  return Color.argb(255, 200, 128, 36);
                case 4:  return Color.argb(255, 168,  96,  4);
                case 5:  return Color.argb(255, 135, 135, 135);
                default: return Color.argb(255, 255, 255, 255);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            LayoutInflater inflater =
                    (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView;
            if (convertView == null) rowView = inflater.inflate(R.layout.neighbor_table, parent, false);
            else                     rowView = convertView;

            final ProximityEntry n = (ProximityEntry) this.routingTable.get(position);

            TextView exprStr = (TextView) rowView.findViewById(R.id.neighbors_expr_str);
            exprStr.setText("" + n.expression());
            exprStr.setContentDescription(getString(R.string.uit_neighbor_expr_str_cell));

            TextView metadata = (TextView) rowView.findViewById(R.id.neighbors_metadata);
            metadata.setText("" + n.metaData());
            metadata.setContentDescription(getString(R.string.uit_neighbor_metadata_cell));

            TextView hops = (TextView) rowView.findViewById(R.id.neighbors_hops);
            hops.setText("" + n.range());
            hops.setContentDescription(getString(R.string.uit_neighbor_hops_cell));

            TextView met = (TextView) rowView.findViewById(R.id.neighbors_met);
            met.setText("" + n.metrics());
            met.setContentDescription(getString(R.string.uit_neighbor_met_cell));

            TextView conn = (TextView) rowView.findViewById(R.id.neighbors_conn);
            conn.setText("" + n.connStatus());
            conn.setContentDescription(getString(R.string.uit_neighbor_conn_cell));

            int tf = n.isSelf() ? Typeface.BOLD : Typeface.NORMAL;
            exprStr .setTypeface(null, tf);
            metadata.setTypeface(null, tf);
            hops    .setTypeface(null, tf);
            met     .setTypeface(null, tf);
            conn    .setTypeface(null, tf);

            int color = getTextColor(n.range());
            exprStr .setTextColor(color);
            metadata.setTextColor(color);
            hops    .setTextColor(color);
            met     .setTextColor(color);
            conn    .setTextColor(color);

            rowView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    View msgView = getActivity().getLayoutInflater().inflate(R.layout.send_msg, null);

                    final TextView exprStr  = (TextView) msgView.findViewById(R.id.send_msg_expr_str);
                    final EditText msgInput = (EditText) msgView.findViewById(R.id.send_msg_msg);

                    exprStr.setText("To: "+String.valueOf(n.expression()));
                    new AlertDialog.Builder(getActivity())
                            .setTitle(R.string.send_msg)
                            .setView(msgView)
                            .setPositiveButton(android.R.string.ok,
                                    new DialogInterface.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which)
                                        {
                                            String msg = msgInput.getText().toString();

                                            if (msg.length() == 0) return;
                                            ((Sam)getActivity()).newMsg(n.id(), msg);
                                            dialog.dismiss();
                                        }
                                    })
                            .setNegativeButton(android.R.string.cancel,
                                    new DialogInterface.OnClickListener()
                                    {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which)
                                        {
                                            dialog.cancel();
                                        }
                                    })
                            .create()
                            .show();
                }
            });

            return rowView;
        }
    }
}