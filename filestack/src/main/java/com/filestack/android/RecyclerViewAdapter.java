package com.filestack.android;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.filestack.android.internal.SourceInfo;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder> {

    ArrayList mValues;
    Context mContext;
    protected ItemListener mListener;

    public RecyclerViewAdapter(Context context, ArrayList values, ItemListener itemListener) {

        mValues = values;
        mContext = context;
        mListener=itemListener;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView textView;
        public ImageView imageView;
        public RelativeLayout relativeLayout;
        SourceInfo item;

        public ViewHolder(View v) {

            super(v);

//            v.setOnClickListener(this);
            textView = (TextView) v.findViewById(R.id.filestack__textView);
            imageView = (ImageView) v.findViewById(R.id.filestack__imageView);
            relativeLayout = (RelativeLayout) v.findViewById(R.id.filestack__relativeLayout);

        }




//        @Override
//        public void onClick(View view) {
//            if (mListener != null) {
//                mListener.onItemClick(item);
//            }
//        }
    }

    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.filestack__list_item, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, final int position) {
        SourceInfo store = (SourceInfo) mValues.get(position);
        holder.textView.setText(store.getIconId());
        holder.imageView.setImageResource(store.getTextId());
        holder.relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onItemClick(position);
            }
        });
//        holder.storeImg.setImageResource(store.getStoreImg());
    }
//
//    @Override
//    public void onBindViewHolder(ViewHolder Vholder, int position) {
//        Vholder.setData(mValues.get(position));
//
//    }

    @Override
    public int getItemCount() {

        return mValues.size();
    }

    public interface ItemListener {
        void onItemClick(int pos);
    }
}