package com.ucclkp.syosetureader.home;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuUtility;

import java.util.ArrayList;
import java.util.List;

class PickupListAdapter extends RecyclerView.Adapter
{
    private Context mContext;

    private boolean mHasFooter;
    private List<BindData> mDataList;
    private OnItemSelectListener mItemSelectListener;


    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_PROGRESSBAR = 1;

    public static final int PROGRESS_LOADING = 0;
    public static final int PROGRESS_NO_MORE_DATA = 1;
    public static final int PROGRESS_ERROR = 2;


    static class BindData
    {
        String novelUrl = "";
        String novelTitle = "";

        String authorUrl = "";
        String authorName = "";

        String type = "";
        String summary = "";

        String novelInfoUrl = "";
        String novelInfoTitle = "";
        String extraMsg = "";

        View.OnClickListener infoListener = null;
        View.OnClickListener titleListener = null;
        View.OnClickListener authorListener = null;

        //尾数据。
        int progressType = PROGRESS_LOADING;
    }


    PickupListAdapter(Context context)
    {
        mContext = context;

        mHasFooter = false;
        mDataList = new ArrayList<>();
    }


    public void setOnItemSelectListener(OnItemSelectListener l)
    {
        mItemSelectListener = l;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        switch (viewType)
        {
            case TYPE_NORMAL:
            {
                View convertView = LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.list_item_pickup, parent, false);
                convertView.setOnClickListener(mItemClickListener);
                convertView.setOnLongClickListener(mItemLongClickListener);
                return new ItemViewHolder(convertView);
            }

            case TYPE_PROGRESSBAR:
            {
                View convertView = LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.list_item_progressbar, parent, false);
                convertView.setOnClickListener(mItemClickListener);
                convertView.setOnLongClickListener(mItemLongClickListener);
                return new ProgressViewHolder(convertView);
            }
        }

        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position)
    {
        int viewType = getItemViewType(position);
        switch (viewType)
        {
            case TYPE_NORMAL:
            {
                BindData data = mDataList.get(position);
                ItemViewHolder iHolder = (ItemViewHolder) holder;

                String novelInfo = data.novelInfoTitle + data.extraMsg;

                iHolder.titleTextView.setText(data.novelTitle);
                iHolder.typeTextView.setText(data.type);
                iHolder.titleLayoutView.setOnClickListener(data.titleListener);
                iHolder.authorButton.setText(data.authorName);
                iHolder.authorButton.setOnClickListener(data.authorListener);

                if (novelInfo.isEmpty())
                {
                    iHolder.novelInfoButton.setVisibility(View.GONE);
                } else
                {
                    iHolder.novelInfoButton.setVisibility(View.VISIBLE);
                    iHolder.novelInfoButton.setText(novelInfo);
                    iHolder.novelInfoButton.setOnClickListener(data.infoListener);
                }

                iHolder.summaryTextView.setText(data.summary);
                break;
            }

            case TYPE_PROGRESSBAR:
            {
                BindData data = mDataList.get(position);
                ProgressViewHolder pHolder = (ProgressViewHolder) holder;

                switch (data.progressType)
                {
                    case PROGRESS_LOADING:
                        pHolder.textView.setVisibility(View.GONE);
                        pHolder.progressBar.setVisibility(View.VISIBLE);
                        break;

                    case PROGRESS_NO_MORE_DATA:
                        pHolder.textView.setText(mContext.getString(R.string.list_footer_nomore));
                        pHolder.textView.setVisibility(View.VISIBLE);
                        pHolder.progressBar.setVisibility(View.GONE);
                        break;

                    case PROGRESS_ERROR:
                        pHolder.textView.setText(mContext.getString(R.string.list_footer_error));
                        pHolder.textView.setVisibility(View.VISIBLE);
                        pHolder.progressBar.setVisibility(View.GONE);
                        break;
                }
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
        if (position == getItemCount() - 1 && mHasFooter)
            return TYPE_PROGRESSBAR;

        return TYPE_NORMAL;
    }

    public BindData getItem(int position)
    {
        return mDataList.get(position);
    }


    public void addFootProgress(int type)
    {
        if (!mHasFooter)
        {
            BindData data = new BindData();
            data.progressType = type;

            mHasFooter = true;
            mDataList.add(data);
            notifyItemInserted(getItemCount() - 1);
        }
    }

    public void setFootProgress(int type)
    {
        if (mHasFooter)
        {
            BindData data = mDataList.get(getItemCount() - 1);
            data.progressType = type;

            notifyItemChanged(getItemCount() - 1);
        }
    }

    public void removeFootProgress()
    {
        if (mHasFooter)
        {
            mHasFooter = false;
            mDataList.remove(getItemCount() - 1);
            notifyItemRemoved(getItemCount());
        }
    }


    public void add(PickupParser.PickupItem item)
    {
        BindData data = new BindData();

        data.novelUrl = item.novelUrl;
        data.novelTitle = item.novelTitle;
        data.authorUrl = item.authorUrl;
        data.authorName = item.authorName;
        data.type = item.type;
        data.summary = item.summary;
        data.novelInfoUrl = item.novelInfoUrl;
        data.novelInfoTitle = item.novelInfoTitle;
        data.extraMsg = item.extraMsg;

        data.titleListener = SyosetuUtility
                .clickOfTitle(item.novelUrl);
        data.authorListener = SyosetuUtility
                .clickOfAuthor(item.authorUrl, item.authorName);
        data.infoListener = SyosetuUtility
                .clickOfInfo(item.novelInfoUrl);

        mDataList.add(data);
        notifyItemInserted(getItemCount() - 1);
    }

    public void clear()
    {
        int size = getItemCount();
        if (size > 0)
        {
            mHasFooter = false;

            mDataList.clear();
            notifyItemRangeRemoved(0, size);
        }
    }


    private static class ItemViewHolder extends RecyclerView.ViewHolder
    {
        TextView titleTextView;
        TextView typeTextView;
        LinearLayout titleLayoutView;

        Button authorButton;
        Button novelInfoButton;
        TextView summaryTextView;


        ItemViewHolder(View itemView)
        {
            super(itemView);

            titleTextView = (TextView) itemView.findViewById(R.id.tv_list_item_pickup_title);
            typeTextView = (TextView) itemView.findViewById(R.id.tv_list_item_pickup_type);
            titleLayoutView = (LinearLayout) itemView.findViewById(R.id.ll_list_item_pickup_title);

            authorButton = (Button) itemView.findViewById(R.id.bt_list_item_pickup_author);
            novelInfoButton = (Button) itemView.findViewById(R.id.bt_list_item_pickup_novelinfo);

            summaryTextView = (TextView) itemView.findViewById(R.id.tv_list_item_pickup_summary);
        }
    }

    private static class ProgressViewHolder extends RecyclerView.ViewHolder
    {
        TextView textView;
        ProgressBar progressBar;

        ProgressViewHolder(View itemView)
        {
            super(itemView);

            textView = (TextView) itemView.findViewById(R.id.tv_list_foot);
            progressBar = (ProgressBar) itemView.findViewById(R.id.pb_list_foot);
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
