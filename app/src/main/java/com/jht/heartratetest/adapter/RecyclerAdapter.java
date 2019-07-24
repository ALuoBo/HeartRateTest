package com.jht.heartratetest.adapter;

import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.jht.heartratetest.R;

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerAdapter.MyViewHodler> {
    @NonNull
    @Override
    public MyViewHodler onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_item_recycler, parent, false);
        return new MyViewHodler(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHodler holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    class MyViewHodler extends RecyclerView.ViewHolder {
        private TextView tvDeviceName, tvDeviceAddress;

        public MyViewHodler(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.device_name);
            tvDeviceAddress = itemView.findViewById(R.id.device_address);
        }
    }
}
