package com.ucclkp.syosetureader.search;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ucclkp.syosetureader.R;

import java.util.ArrayList;

public class KwPanelListAdapter extends RecyclerView.Adapter
{
    private ArrayList<BindData> mDataList;
    private OnItemSelectListener mItemSelectListener;


    public static final int TYPE_HEAD = 0;
    public static final int TYPE_NORMAL = 1;


    public static class BindData
    {
        String value = "";
        String content = "";

        //类型。
        int viewType = TYPE_NORMAL;
    }


    public KwPanelListAdapter(Context context)
    {
        mDataList = new ArrayList<>();
    }


    public void setOnItemSelectListener(OnItemSelectListener l)
    {
        mItemSelectListener = l;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        switch (viewType)
        {
            case TYPE_HEAD:
            {
                View convertView = LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.list_item_search_kw_head, parent, false);
                convertView.setOnClickListener(mItemClickListener);
                convertView.setOnLongClickListener(mItemLongClickListener);

                return new HeadViewHolder(convertView);
            }

            case TYPE_NORMAL:
            {
                View convertView = LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.list_item_search_kw_keyword, parent, false);
                convertView.setOnClickListener(mItemClickListener);
                convertView.setOnLongClickListener(mItemLongClickListener);

                return new ItemViewHolder(convertView);
            }
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position)
    {
        int viewType = getItemViewType(position);
        switch (viewType)
        {
            case TYPE_HEAD:
            {
                BindData data = mDataList.get(position);
                HeadViewHolder hHolder = (HeadViewHolder) holder;

                hHolder.headTextView.setText(data.content);
                break;
            }

            case TYPE_NORMAL:
            {
                BindData data = mDataList.get(position);
                ItemViewHolder iHolder = (ItemViewHolder) holder;

                iHolder.contentTextView.setTag(data.value);
                iHolder.contentTextView.setText(data.content);
                break;
            }
        }
    }

    @Override
    public int getItemCount()
    {
        return mDataList.size();
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public int getItemViewType(int position)
    {
        return mDataList.get(position).viewType;
    }

    public BindData getItem(int position)
    {
        return mDataList.get(position);
    }


    public void addHeader(String title)
    {
        BindData data = new BindData();
        data.content = title;
        data.viewType = TYPE_HEAD;

        mDataList.add(data);
        notifyItemInserted(getItemCount() - 1);
    }

    public void addKeyword(String keyword, String value)
    {
        BindData data = new BindData();
        data.value = value;
        data.content = keyword;
        data.viewType = TYPE_NORMAL;

        mDataList.add(data);
        notifyItemInserted(getItemCount() - 1);
    }

    public void clear()
    {
        int size = getItemCount();
        if (size > 0)
        {
            mDataList.clear();
            notifyItemRangeRemoved(0, size);
        }
    }


    public static class HeadViewHolder extends RecyclerView.ViewHolder
    {
        public TextView headTextView;

        public HeadViewHolder(View itemView)
        {
            super(itemView);
            headTextView = (TextView) itemView.findViewById(
                    R.id.tv_search_kw_list_item_head);
        }
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder
    {
        public TextView contentTextView;

        public ItemViewHolder(View itemView)
        {
            super(itemView);
            contentTextView = (TextView) itemView.findViewById(
                    R.id.tv_search_kw_list_item_keyword);
        }
    }


    private View.OnClickListener mItemClickListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            if (mItemSelectListener != null)
                mItemSelectListener.onItemClick(v);
        }
    };

    private View.OnLongClickListener mItemLongClickListener = new View.OnLongClickListener()
    {
        @Override
        public boolean onLongClick(View v)
        {
            return mItemSelectListener != null
                    && mItemSelectListener.onItemLongClick(v);
        }
    };


    public interface OnItemSelectListener
    {
        void onItemClick(View itemView);

        boolean onItemLongClick(View itemView);
    }
}