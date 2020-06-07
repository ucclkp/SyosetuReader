package com.ucclkp.syosetureader.novel;


import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuUtility;

import java.util.ArrayList;

class NovelListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context mContext;

    private boolean mHasFooter;
    private ArrayList<BindData> mDataList;
    private OnItemSelectListener mItemSelectListener;

    private int mTextPrimaryColor;
    private int mTextSecondaryColor;


    static final int TYPE_HEAD = 0;
    static final int TYPE_NORMAL = 1;
    static final int TYPE_CHAPTER = 2;
    static final int TYPE_PROGRESSBAR = 3;
    static final int TYPE_PREV_TIP = 4;

    static final int PROGRESS_LOADING = 0;
    static final int PROGRESS_NO_MORE_DATA = 1;
    static final int PROGRESS_ERROR = 2;


    static class BindData {
        //TYPE_HEAD数据。
        String headAttention = "";
        String headTitle = "";
        String headAuthor = "";
        Spanned headSummary = null;
        View.OnClickListener authorListener = null;

        //TYPE_NORMAL数据。
        String sectionUrl = "";
        String sectionName = "";
        String sectionTime = "";
        boolean viewed = false;

        //TYPE_PREV_TIP数据。
        String tip = "";

        //TYPE_CHAPTER数据。
        String chapterTitle = "";

        //TYPE_PROGRESSBAR数据。
        int progressType = PROGRESS_LOADING;

        //类型。
        int viewType = TYPE_NORMAL;
    }


    NovelListAdapter(Context context) {
        mContext = context;

        mHasFooter = false;
        mDataList = new ArrayList<>();

        TypedArray a = context.obtainStyledAttributes(
                new int[]{android.R.attr.textColorPrimary,
                        android.R.attr.textColorSecondary});
        mTextPrimaryColor = a.getColor(a.getIndex(0), Color.BLACK);
        mTextSecondaryColor = a.getColor(a.getIndex(1), Color.GRAY);
        a.recycle();
    }


    void setOnItemSelectListener(OnItemSelectListener l) {
        mItemSelectListener = l;
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        switch (viewType) {
            case TYPE_HEAD: {
                View convertView = LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.list_item_novel_head, parent, false);
                convertView.setOnClickListener(mItemClickListener);
                convertView.setOnLongClickListener(mItemLongClickListener);

                return new HeadViewHolder(convertView);
            }

            case TYPE_PREV_TIP: {
                View convertView = LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.list_item_novel_prev_tip, parent, false);
                convertView.setOnClickListener(mItemClickListener);
                convertView.setOnLongClickListener(mItemLongClickListener);

                return new TipViewHolder(convertView);
            }

            case TYPE_NORMAL: {
                View convertView = LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.list_item_novel_section, parent, false);
                convertView.setOnClickListener(mItemClickListener);
                convertView.setOnLongClickListener(mItemLongClickListener);

                return new ItemViewHolder(convertView);
            }

            case TYPE_CHAPTER: {
                View convertView = LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.list_item_novel_chapter, parent, false);
                convertView.setOnClickListener(mItemClickListener);
                convertView.setOnLongClickListener(mItemLongClickListener);

                return new ChapterViewHolder(convertView);
            }

            default:
            case TYPE_PROGRESSBAR: {
                View convertView = LayoutInflater.from(
                        parent.getContext()).inflate(R.layout.list_item_progressbar, parent, false);
                convertView.setOnClickListener(mItemClickListener);
                convertView.setOnLongClickListener(mItemLongClickListener);

                return new ProgressViewHolder(convertView);
            }
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        switch (viewType) {
            case TYPE_HEAD: {
                BindData data = mDataList.get(position);
                HeadViewHolder hHolder = (HeadViewHolder) holder;

                if (data.headAttention.length() > 0) {
                    if (hHolder.attentionTextView.getVisibility() != View.VISIBLE)
                        hHolder.attentionTextView.setVisibility(View.VISIBLE);
                    hHolder.attentionTextView.setText(data.headAttention);
                } else {
                    if (hHolder.attentionTextView.getVisibility() != View.GONE)
                        hHolder.attentionTextView.setVisibility(View.GONE);
                }

                hHolder.titleTextView.setText(data.headTitle);

                if (data.authorListener != null) {
                    hHolder.authorButton.setText(data.headAuthor);
                    hHolder.authorButton.setOnClickListener(data.authorListener);
                    hHolder.authorButton.setVisibility(View.VISIBLE);
                    hHolder.authorTextView.setVisibility(View.GONE);
                } else {
                    hHolder.authorTextView.setText(data.headAuthor);
                    hHolder.authorTextView.setVisibility(View.VISIBLE);
                    hHolder.authorButton.setVisibility(View.GONE);
                }

                hHolder.summaryTextView.setText(data.headSummary);

                break;
            }

            case TYPE_PREV_TIP: {
                BindData data = mDataList.get(position);
                TipViewHolder tHolder = (TipViewHolder) holder;

                tHolder.tipTextView.setText(data.tip);
                tHolder.titleTextView.setText(data.sectionName);

                break;
            }

            case TYPE_NORMAL: {
                BindData data = mDataList.get(position);
                ItemViewHolder iHolder = (ItemViewHolder) holder;

                if (data.viewed) {
                    iHolder.timeTextView.setTextColor(
                            ContextCompat.getColor(mContext, R.color.text_color_viewed));
                    iHolder.contentTextView.setTextColor(
                            ContextCompat.getColor(mContext, R.color.text_color_viewed));
                } else {
                    iHolder.timeTextView.setTextColor(
                            mTextSecondaryColor);
                    iHolder.contentTextView.setTextColor(
                            mTextPrimaryColor);
                }

                iHolder.timeTextView.setText(data.sectionTime);
                iHolder.contentTextView.setText(data.sectionName);
                break;
            }

            case TYPE_CHAPTER: {
                BindData data = mDataList.get(position);
                ChapterViewHolder cHolder = (ChapterViewHolder) holder;

                cHolder.chapterTextView.setText(data.chapterTitle);
                break;
            }

            case TYPE_PROGRESSBAR: {
                BindData data = mDataList.get(position);
                ProgressViewHolder pHolder = (ProgressViewHolder) holder;

                switch (data.progressType) {
                    case PROGRESS_LOADING:
                        pHolder.textView.setVisibility(View.GONE);
                        pHolder.progressBar.setVisibility(View.VISIBLE);
                        break;

                    case PROGRESS_NO_MORE_DATA:
                        pHolder.textView.setText(
                                mContext.getString(R.string.list_footer_nomore));
                        pHolder.textView.setVisibility(View.VISIBLE);
                        pHolder.progressBar.setVisibility(View.GONE);
                        break;

                    case PROGRESS_ERROR:
                        pHolder.textView.setText(
                                mContext.getString(R.string.list_footer_error));
                        pHolder.textView.setVisibility(View.VISIBLE);
                        pHolder.progressBar.setVisibility(View.GONE);
                        break;
                }
                break;
            }
        }
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return mDataList.get(position).viewType;
    }

    public BindData getItem(int position) {
        return mDataList.get(position);
    }

    public void addHeader(
            String attention, String title,
            String author, String authorUrl, Spanned summary) {
        BindData data = new BindData();
        data.headAttention = attention;
        data.headTitle = title;
        data.headAuthor = author;
        data.headSummary = summary;
        data.viewType = TYPE_HEAD;

        data.authorListener
                = SyosetuUtility.clickOfAuthor(authorUrl, author);

        mDataList.add(data);
        notifyItemInserted(getItemCount() - 1);
    }

    public void addSection(
            String title, String time, String url, boolean viewed) {
        BindData data = new BindData();
        data.sectionUrl = url;
        data.sectionName = title;
        data.sectionTime = time;
        data.viewType = TYPE_NORMAL;
        data.viewed = viewed;

        mDataList.add(data);
        notifyItemInserted(getItemCount() - 1);
    }

    public void setPrevTip(String title, String tip, String url) {
        if (mDataList.size() == 0)
            return;

        BindData data = mDataList.get(1);
        if (data == null || data.viewType != TYPE_PREV_TIP) {
            data = new BindData();
            data.sectionUrl = url;
            data.sectionName = title;
            data.tip = tip;
            data.viewType = TYPE_PREV_TIP;

            mDataList.add(1, data);
            notifyItemInserted(1);
        } else {
            data.sectionUrl = url;
            data.sectionName = title;
            data.tip = tip;

            notifyItemChanged(1);
        }
    }

    public BindData modifySection(String url, boolean viewed) {
        for (int i = 0; i < mDataList.size(); ++i) {
            if (mDataList.get(i).viewType == TYPE_NORMAL
                    && mDataList.get(i).sectionUrl.equals(url)) {
                if (mDataList.get(i).viewed != viewed) {
                    mDataList.get(i).viewed = viewed;
                    notifyItemChanged(i);
                }
                return mDataList.get(i);
            }
        }

        return null;
    }

    public void addChapter(String title) {
        BindData data = new BindData();
        data.chapterTitle = title;
        data.viewType = TYPE_CHAPTER;

        mDataList.add(data);
        notifyItemInserted(getItemCount() - 1);
    }

    public void addFootProgress(int type) {
        if (!mHasFooter) {
            BindData data = new BindData();
            data.progressType = type;

            mHasFooter = true;
            mDataList.add(data);
            notifyItemInserted(getItemCount() - 1);
        }
    }

    public void setFootProgress(int type) {
        if (mHasFooter) {
            BindData data = mDataList.get(getItemCount() - 1);
            data.progressType = type;

            notifyItemChanged(getItemCount() - 1);
        }
    }

    public void removeFootProgress() {
        if (mHasFooter) {
            mHasFooter = false;
            mDataList.remove(getItemCount() - 1);
            notifyItemRemoved(getItemCount());
        }
    }

    public void clear() {
        int size = getItemCount();
        if (size > 0) {
            mDataList.clear();
            notifyItemRangeRemoved(0, size);
        }
    }


    static class HeadViewHolder extends RecyclerView.ViewHolder {
        TextView attentionTextView;
        TextView titleTextView;
        Button authorButton;
        TextView authorTextView;
        TextView summaryTextView;

        HeadViewHolder(View itemView) {
            super(itemView);

            attentionTextView = itemView
                    .findViewById(R.id.tv_item_novel_head_attention);
            titleTextView = itemView
                    .findViewById(R.id.tv_item_novel_head_title);
            authorButton = itemView
                    .findViewById(R.id.bt_item_novel_head_author);
            authorTextView = itemView
                    .findViewById(R.id.tv_item_novel_head_author);
            summaryTextView = itemView
                    .findViewById(R.id.tv_item_novel_head_summary);
        }
    }

    private static class TipViewHolder extends RecyclerView.ViewHolder {
        TextView tipTextView;
        TextView titleTextView;

        TipViewHolder(View itemView) {
            super(itemView);

            tipTextView = itemView
                    .findViewById(R.id.tv_list_item_prev_tip_tip);
            titleTextView = itemView
                    .findViewById(R.id.tv_list_item_prev_tip_title);
        }
    }

    private static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView timeTextView;
        TextView contentTextView;

        ItemViewHolder(View itemView) {
            super(itemView);

            timeTextView = itemView
                    .findViewById(R.id.tv_item_novel_section_time);
            contentTextView = itemView
                    .findViewById(R.id.tv_item_novel_section_title);
        }
    }

    private static class ChapterViewHolder extends RecyclerView.ViewHolder {
        TextView chapterTextView;

        ChapterViewHolder(View itemView) {
            super(itemView);

            chapterTextView = itemView
                    .findViewById(R.id.tv_item_novel_chapter);
        }
    }

    private static class ProgressViewHolder extends RecyclerView.ViewHolder {
        TextView textView;
        ProgressBar progressBar;

        ProgressViewHolder(View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.tv_list_foot);
            progressBar = itemView.findViewById(R.id.pb_list_foot);
        }
    }


    private View.OnClickListener mItemClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (mItemSelectListener != null)
                mItemSelectListener.onItemClick(v);
        }
    };

    private View.OnLongClickListener mItemLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            return mItemSelectListener != null
                    && mItemSelectListener.onItemLongClick(v);
        }
    };


    public interface OnItemSelectListener {
        void onItemClick(View itemView);

        boolean onItemLongClick(View itemView);
    }
}
