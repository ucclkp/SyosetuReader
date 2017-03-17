package com.ucclkp.syosetureader.author;


import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuUtility;
import com.ucclkp.syosetureader.recipientchip.RecipientChipSpan;

import java.util.ArrayList;
import java.util.List;

public class WorkListAdapter extends RecyclerView.Adapter
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

        String type = "";
        String genre = "";
        String summary = "";

        String novelInfoUrl = "";
        String novelInfoTitle = "";

        String attention = "";
        String readingTime = "";

        SpannableStringBuilder chips = null;

        View.OnClickListener infoListener = null;
        View.OnClickListener titleListener = null;

        //尾数据。
        int progressType = PROGRESS_LOADING;
    }


    WorkListAdapter(Context context)
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
                        parent.getContext()).inflate(R.layout.list_item_author_work, parent, false);
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

                String extra;
                if (!data.attention.isEmpty())
                    extra = data.attention + "\n" + data.readingTime;
                else
                    extra = data.readingTime;

                iHolder.titleTextView.setText(data.novelTitle);
                iHolder.typeTextView.setText(data.type);
                iHolder.titleLayoutView.setOnClickListener(data.titleListener);

                iHolder.novelInfoButton.setText(data.novelInfoTitle);
                iHolder.novelInfoButton.setOnClickListener(data.infoListener);

                iHolder.summaryTextView.setText(data.summary);
                iHolder.chipsTextView.setText(data.chips);
                iHolder.extraTextView.setText(extra);
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


    public void add(AuthorParser.WorkItem item)
    {
        BindData data = new BindData();

        data.novelUrl = item.novelUrl;
        data.novelTitle = item.novelTitle;
        data.type = item.type;
        data.summary = item.summary;
        data.novelInfoUrl = item.novelInfoUrl;
        data.novelInfoTitle = item.novelInfoTitle;
        data.genre = item.genre;
        data.attention = item.attention;
        data.readingTime = item.readingTime;

        data.titleListener = SyosetuUtility
                .clickOfTitle(item.novelUrl);
        data.infoListener = SyosetuUtility
                .clickOfInfo(item.novelInfoUrl);

        data.chips = new SpannableStringBuilder();

        //genre
        if (!item.genre.isEmpty())
        {
            String genrePrefix = mContext.getString(R.string.genre_prefix);
            data.chips.append(genrePrefix).append(item.genre);
            data.chips.setSpan(new RecipientChipSpan(ContextCompat.getColor(mContext, R.color.chip_color)),
                    genrePrefix.length(), data.chips.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        //keyword
        for (int i = 0; i < item.keywordList.size(); ++i)
        {
            if (i == 0)
            {
                if (!TextUtils.isEmpty(data.chips))
                    data.chips.append("\n");
                data.chips.append(mContext.getString(R.string.keyword_prefix));
            }

            String keyword = item.keywordList.get(i);
            data.chips.append(keyword).append(" ");
            data.chips.setSpan(new RecipientChipSpan(ContextCompat.getColor(
                    mContext, R.color.chip_color)),
                    data.chips.length() - keyword.length() - 1, data.chips.length() - 1,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            if (i == item.keywordList.size() - 1)
            {
                data.chips.delete(
                        data.chips.length() - 1,
                        data.chips.length());
            }
        }

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

        Button novelInfoButton;
        TextView summaryTextView;
        TextView chipsTextView;
        TextView extraTextView;


        ItemViewHolder(View itemView)
        {
            super(itemView);

            titleTextView = (TextView) itemView.findViewById(R.id.tv_list_item_author_work_title);
            typeTextView = (TextView) itemView.findViewById(R.id.tv_list_item_author_work_type);
            titleLayoutView = (LinearLayout) itemView.findViewById(R.id.ll_list_item_author_work_title);

            novelInfoButton = (Button) itemView.findViewById(R.id.bt_list_item_author_work_novelinfo);
            summaryTextView = (TextView) itemView.findViewById(R.id.tv_list_item_author_work_summary);
            chipsTextView = (TextView) itemView.findViewById(R.id.tv_list_item_author_work_chips);
            extraTextView = (TextView) itemView.findViewById(R.id.tv_list_item_author_work_extra);
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
