package com.ucclkp.syosetureader.search;

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


class SearchResultListAdapter extends RecyclerView.Adapter
{
    private Context mContext;

    private boolean mHasFooter;
    private ArrayList<BindData> mDataList;
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

        String novelInfoUrl = "";
        String novelInfoTitle = "";
        String extraMsg = "";

        String type = "";
        String summary = "";

        SpannableStringBuilder chips = null;
        SpannableStringBuilder contriChips = null;
        String others = "";

        View.OnClickListener infoListener = null;
        View.OnClickListener titleListener = null;
        View.OnClickListener authorListener = null;

        //尾数据。
        int progressType = PROGRESS_LOADING;
    }


    SearchResultListAdapter(Context context)
    {
        mContext = context;

        mHasFooter = false;
        mDataList = new ArrayList<>();
    }


    void setOnItemSelectListener(OnItemSelectListener l)
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
                        parent.getContext()).inflate(R.layout.list_item_search_result, parent, false);
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

                iHolder.titleTextView.setText(data.novelTitle);
                iHolder.typeTextView.setText(data.type + "  " + data.extraMsg);
                iHolder.titleLayoutView.setOnClickListener(data.titleListener);

                if (data.authorListener != null)
                {
                    iHolder.authorButton.setText(data.authorName);
                    iHolder.authorButton.setOnClickListener(data.authorListener);
                    iHolder.authorButton.setVisibility(View.VISIBLE);
                    iHolder.authorTextView.setVisibility(View.GONE);
                }
                else
                {
                    iHolder.authorTextView.setText(data.authorName);
                    iHolder.authorTextView.setVisibility(View.VISIBLE);
                    iHolder.authorButton.setVisibility(View.GONE);
                }

                if (!TextUtils.isEmpty(data.contriChips))
                {
                    iHolder.contriTextView.setText(data.contriChips);
                    iHolder.contriTextView.setVisibility(View.VISIBLE);
                }
                else
                    iHolder.contriTextView.setVisibility(View.GONE);

                iHolder.novelInfoButton.setText(data.novelInfoTitle);
                iHolder.novelInfoButton.setOnClickListener(data.infoListener);

                iHolder.summaryTextView.setText(data.summary);

                if (!TextUtils.isEmpty(data.chips))
                {
                    iHolder.chipsTextView.setText(data.chips);
                    iHolder.chipsTextView.setVisibility(View.VISIBLE);
                }
                else
                    iHolder.chipsTextView.setVisibility(View.GONE);

                iHolder.othersTextView.setText(data.others);
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


    public void add(SearchResultParser.SearchedItem item)
    {
        BindData data = new BindData();

        data.novelUrl = item.novelUrl;
        data.novelTitle = item.novelTitle;
        data.type = item.type;
        data.extraMsg = item.extraMsg;

        data.authorUrl = item.authorUrl;
        data.authorName = item.authorName;
        data.novelInfoUrl = item.novelInfoUrl;
        data.novelInfoTitle = item.novelInfoTitle;

        data.summary = item.summary;

        data.titleListener
                = SyosetuUtility.clickOfTitle(item.novelUrl);
        data.authorListener
                = SyosetuUtility.clickOfAuthor(item.authorUrl, item.authorName);
        data.infoListener
                = SyosetuUtility.clickOfInfo(item.novelInfoUrl);

        data.chips = new SpannableStringBuilder();

        //genre
        if (!item.genre.isEmpty())
        {
            String genrePrefix = mContext.getString(R.string.genre_prefix);
            data.chips.append(genrePrefix);
            data.chips.append(item.genre).append(item.genreType);
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

        //pc,mb or illust
        if (item.isPCContribute
                || item.isMBContribute
                || item.hasIllustration)
        {
            data.contriChips = new SpannableStringBuilder();

            if (item.isPCContribute)
            {
                String title = mContext.getString(R.string.pc_contribute);
                data.contriChips.append(title).append(" ");
                data.contriChips.setSpan(new RecipientChipSpan(
                                ContextCompat.getColor(mContext, R.color.chip_pc_color)),
                        data.contriChips.length() - title.length() - 1,
                        data.contriChips.length() - 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (item.isMBContribute)
            {
                String title = mContext.getString(R.string.mb_contribute);
                data.contriChips.append(title).append(" ");
                data.contriChips.setSpan(new RecipientChipSpan(
                                ContextCompat.getColor(mContext, R.color.chip_mb_color)),
                        data.contriChips.length() - title.length() - 1,
                        data.contriChips.length() - 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            if (item.hasIllustration)
            {
                String title = mContext.getString(R.string.illust_contribute);
                data.contriChips.append(title).append(" ");
                data.contriChips.setSpan(new RecipientChipSpan(
                                ContextCompat.getColor(mContext, R.color.chip_illust_color)),
                        data.contriChips.length() - title.length() - 1,
                        data.contriChips.length() - 1,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            data.contriChips.delete(
                    data.contriChips.length() - 1,
                    data.contriChips.length());
        }

        data.others = mContext.getString(R.string.lastupdate_prefix)
                + item.lastUpdatedDate + "\n"
                + (item.ncode.isEmpty() ? "" : (mContext.getString(R.string.ncode_prefix) + item.ncode + "\n"))
                + mContext.getString(R.string.readtime_prefix)
                + item.readingTime + "\n"
                + mContext.getString(R.string.weekuu_prefix)
                + item.weekUniqueUser + "\n"
                + mContext.getString(R.string.review_prefix)
                + item.review + "\n"
                + mContext.getString(R.string.overall_prefix)
                + item.overallPoint + "\n"
                + mContext.getString(R.string.rankuc_prefix)
                + item.rankUserCount + "\n"
                + mContext.getString(R.string.rankpoint_prefix)
                + item.rankPoint + "\n"
                + mContext.getString(R.string.bookmark_prefix)
                + item.bookmarkCount;

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


    static class ItemViewHolder extends RecyclerView.ViewHolder
    {
        TextView titleTextView;
        TextView typeTextView;
        LinearLayout titleLayoutView;

        TextView contriTextView;

        Button authorButton;
        TextView authorTextView;
        Button novelInfoButton;
        TextView summaryTextView;

        TextView chipsTextView;
        TextView othersTextView;

        public ItemViewHolder(View itemView)
        {
            super(itemView);

            titleTextView = (TextView) itemView.findViewById(R.id.tv_list_item_sr_title);
            typeTextView = (TextView) itemView.findViewById(R.id.tv_list_item_sr_type);
            titleLayoutView = (LinearLayout) itemView.findViewById(R.id.ll_list_item_sr_title);

            contriTextView = (TextView) itemView.findViewById(R.id.tv_list_item_sr_contri_chips);

            authorButton = (Button) itemView.findViewById(R.id.bt_list_item_sr_author);
            authorTextView = (TextView) itemView.findViewById(R.id.tv_list_item_sr_author_text);
            novelInfoButton = (Button) itemView.findViewById(R.id.bt_list_item_sr_novelinfo);
            summaryTextView = (TextView) itemView.findViewById(R.id.tv_list_item_sr_summary);

            chipsTextView = (TextView) itemView.findViewById(R.id.tv_list_item_sr_chips);
            othersTextView = (TextView) itemView.findViewById(R.id.tv_list_item_sr_others);
        }
    }

    static class ProgressViewHolder extends RecyclerView.ViewHolder
    {
        public TextView textView;
        public ProgressBar progressBar;

        public ProgressViewHolder(View itemView)
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
