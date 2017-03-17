package com.ucclkp.syosetureader.download;


import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuBooks;
import com.ucclkp.syosetureader.UApplication;

import java.util.ArrayList;

class DownloadListAdapter extends RecyclerView.Adapter
{
    private int mSelectedCount;
    private boolean mIsShowCheckBox;
    private RecyclerView mListView;

    private ArrayList<BindData> mDataList;
    private OnItemSelectListener mItemSelectListener;

    static class BindData
    {
        String url = "";
        String state = "";
        String ncode = "";
        String title = "";
        String site = "";
        String type = "";
        boolean selected = false;

        int have = -1;
        int total = -1;
    }


    DownloadListAdapter(RecyclerView view)
    {
        mListView = view;
        mSelectedCount = 0;
        mIsShowCheckBox = false;
        mDataList = new ArrayList<>();
    }


    void setOnItemSelectListener(OnItemSelectListener l)
    {
        mItemSelectListener = l;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        View convertView = LayoutInflater.from(
                parent.getContext()).inflate(R.layout.list_item_download, parent, false);
        convertView.setOnClickListener(mItemClickListener);
        convertView.setOnLongClickListener(mItemLongClickListener);

        return new ItemViewHolder(convertView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position)
    {
        BindData data = mDataList.get(position);
        String site = data.type + "・" + data.site;

        ItemViewHolder iHolder = (ItemViewHolder) holder;
        iHolder.stateTextView.setText(data.state);
        iHolder.titleTextView.setText(data.title);
        iHolder.siteTextView.setText(site);

        ImageView imageView = iHolder.selectionImageView;

        if (mIsShowCheckBox)
        {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImageResource(data.selected ?
                    R.drawable.ic_selected : R.drawable.ic_unselect);
        }
        else
            imageView.setVisibility(View.GONE);

        if (data.have >= 0 && data.total >= 0)
        {
            iHolder.progressBar.setVisibility(View.VISIBLE);
            iHolder.controlButton.setVisibility(View.VISIBLE);
            iHolder.progressBar.setMax(data.total);
            iHolder.progressBar.setProgress(data.have);
        }
        else if (data.have == -2 && data.total == -2)
        {
            iHolder.progressBar.setVisibility(View.VISIBLE);
            iHolder.controlButton.setVisibility(View.GONE);
            iHolder.progressBar.setIndeterminate(true);
        }
        else
        {
            iHolder.progressBar.setVisibility(View.GONE);
            iHolder.controlButton.setVisibility(View.GONE);
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

    public BindData getItem(int position)
    {
        return mDataList.get(position);
    }

    public int getSelectedCount()
    {
        return mSelectedCount;
    }


    public void add(
            String ncode, String title, String url,
            String state, String site, String type, int have, int total)
    {
        BindData data = new BindData();
        data.url = url;
        data.state = state;
        data.title = title;
        data.site = site;
        data.type = type;
        data.ncode = ncode;
        data.have = have;
        data.total = total;

        mDataList.add(data);
        notifyItemInserted(getItemCount() - 1);
    }

    public void add(int index,
                    String ncode, String title, String url,
                    String state, String site, String type, int have, int total)
    {
        BindData data = new BindData();
        data.url = url;
        data.state = state;
        data.title = title;
        data.site = site;
        data.type = type;
        data.ncode = ncode;
        data.have = have;
        data.total = total;

        mDataList.add(index, data);
        notifyItemInserted(index);
    }

    public void modify(String ncode, boolean selected)
    {
        for (int i = 0; i < mDataList.size(); ++i)
        {
            BindData data = mDataList.get(i);
            if (data.ncode.equals(ncode)
                    && data.selected != selected)
            {
                if (selected)
                    ++mSelectedCount;
                else
                    --mSelectedCount;

                data.selected = selected;
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void modify(String ncode, int have, int total)
    {
        for (int i = 0; i < mDataList.size(); ++i)
        {
            BindData data = mDataList.get(i);
            if (data.ncode.equals(ncode))
            {
                data.have = have;
                data.total = total;
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void modify(
            String ncode, String title, String url,
            String state, String site, String type, int have, int total)
    {
        for (int i = 0; i < mDataList.size(); ++i)
        {
            BindData data = mDataList.get(i);
            if (data.ncode.equals(ncode))
            {
                data.url = url;
                data.state = state;
                data.title = title;
                data.site = site;
                data.type = type;
                data.ncode = ncode;
                data.have = have;
                data.total = total;
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void removeSelectedItem()
    {
        SyosetuBooks books = ((UApplication) ((AppCompatActivity) mListView
                .getContext()).getApplication()).getSyosetuBooks();
        books.begin();

        for (int i = 0; i < mDataList.size(); )
        {
            BindData data = mDataList.get(i);
            if (data.selected)
            {
                books.deleteBook(data.ncode);
                books.deleteSection(data.ncode);

                mDataList.remove(i);
                notifyItemRemoved(i);
            }
            else
                ++i;
        }

        books.successful();
        books.end();

        mSelectedCount = 0;
    }

    public void toggleSelect(String ncode)
    {
        for (int i = 0; i < mDataList.size(); ++i)
        {
            BindData data = mDataList.get(i);
            if (data.ncode.equals(ncode))
            {
                data.selected = !data.selected;
                notifyItemChanged(i);

                if (data.selected)
                    ++mSelectedCount;
                else
                    --mSelectedCount;
                return;
            }
        }
    }

    public void selectAll()
    {
        for (int i = 0; i < mDataList.size(); ++i)
            mDataList.get(i).selected = true;

        mSelectedCount = mDataList.size();
        notifyItemRangeChanged(0, mDataList.size());
    }

    public void showCheckBox(boolean show)
    {
        if (mIsShowCheckBox == show
                || (show && mDataList.size() == 0))
            return;

        mIsShowCheckBox = show;
        if (!mIsShowCheckBox)
        {
            for (int i = 0; i < mDataList.size(); ++i)
                mDataList.get(i).selected = false;
        }

        mSelectedCount = 0;
        notifyItemRangeChanged(0, mDataList.size());
    }

    public void clear()
    {
        int size = getItemCount();
        if (size > 0)
        {
            mDataList.clear();
            mSelectedCount = 0;
            notifyItemRangeRemoved(0, size);
        }
    }


    static class ItemViewHolder extends RecyclerView.ViewHolder
    {
        TextView stateTextView;
        TextView titleTextView;
        TextView siteTextView;
        ImageView selectionImageView;
        ProgressBar progressBar;
        ImageButton controlButton;

        ItemViewHolder(View itemView)
        {
            super(itemView);

            stateTextView = (TextView) itemView.findViewById(R.id.tv_list_item_download_state);
            titleTextView = (TextView) itemView.findViewById(R.id.tv_list_item_download_title);
            siteTextView = (TextView) itemView.findViewById(R.id.tv_list_item_download_site);
            selectionImageView = (ImageView) itemView.findViewById(R.id.iv_list_item_download_select);
            progressBar = (ProgressBar) itemView.findViewById(R.id.pb_list_item_download_progress);
            controlButton = (ImageButton) itemView.findViewById(R.id.ib_list_item_control_button);
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

    private View.OnLongClickListener mItemLongClickListener
            = new View.OnLongClickListener()
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
