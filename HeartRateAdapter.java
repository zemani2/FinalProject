package com.example.anxietyByHeartRate;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class HeartRateAdapter extends BaseAdapter {
    private Context context;
    private List<Map<String, String>> heartRateList;

    public HeartRateAdapter(Context context, List<Map<String, String>> heartRateList) {
        this.context = context;
        this.heartRateList = heartRateList;
    }

    @Override
    public int getCount() {
        return heartRateList.size();
    }

    @Override
    public Object getItem(int position) {
        return heartRateList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.list_item_heart_rate, null);
        }

        TextView heartRateTextView = view.findViewById(R.id.heartRateTextView);
        TextView timestampTextView = view.findViewById(R.id.timestampTextView);

        Map<String, String> heartRateData = heartRateList.get(position);
        String heartRate = heartRateData.get(DBHelper.COL_HEART_RATE);
        String timestamp = heartRateData.get(DBHelper.COL_TIMESTAMP);

        heartRateTextView.setText(heartRate);
        timestampTextView.setText(timestamp);

        return view;
    }
}
