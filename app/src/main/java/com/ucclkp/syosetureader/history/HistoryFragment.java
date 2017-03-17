package com.ucclkp.syosetureader.history;


import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ucclkp.syosetureader.MainActivity;
import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuLibrary;
import com.ucclkp.syosetureader.SyosetuUtility;
import com.ucclkp.syosetureader.UApplication;
import com.ucclkp.syosetureader.novel.NovelActivity;

public class HistoryFragment extends Fragment
{
    private RecyclerView mListView;
    private HistoryListAdapter mListAdapter;

    private TextView mTipTextView;

    private ActionMode mSelectionActionMode;


    public HistoryFragment()
    {
    }


    public static HistoryFragment newInstance()
    {
        HistoryFragment fragment = new HistoryFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
        }

        getActivity().setTitle("历史记录");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View parent = inflater.inflate(R.layout.fragment_history, container, false);

        TabLayout tabLayout = (TabLayout) getActivity().findViewById(R.id.tl_main_activity);
        tabLayout.setVisibility(View.GONE);

        Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.tb_main_activity);
        AppBarLayout.LayoutParams lp = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
        lp.setScrollFlags(0);
        toolbar.setLayoutParams(lp);

        mTipTextView = (TextView) parent.findViewById(R.id.tv_history_fragment_tip);

        mListView = (RecyclerView) parent.findViewById(R.id.rv_history_fragment_list);
        mListView.setHasFixedSize(true);
        mListView.setLayoutManager(new LinearLayoutManager(getContext()));
        mListView.setMotionEventSplittingEnabled(false);
        //mListView.addItemDecoration(new HistoryListDecoration(getContext()));

        mListAdapter = new HistoryListAdapter(mListView);
        mListAdapter.setOnItemSelectListener(mItemSelectListener);

        mListView.setAdapter(mListAdapter);

        return parent;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        refresh();
    }

    @Override
    public void onHiddenChanged(boolean hidden)
    {
        super.onHiddenChanged(hidden);

        if (!hidden)
        {
            getActivity().setTitle("历史记录");
        }
    }


    public void refresh()
    {
        if (loadHistory())
        {
            mListView.setVisibility(View.VISIBLE);
            mTipTextView.setVisibility(View.GONE);
        }
        else
        {
            mListView.setVisibility(View.GONE);
            mTipTextView.setVisibility(View.VISIBLE);
        }
    }

    private boolean loadHistory()
    {
        mListAdapter.clear();

        Cursor cursor = ((UApplication) getActivity().getApplication())
                .getSyosetuLibrary().getHis();
        if (cursor == null)
            return false;

        int ncodeIndex = cursor.getColumnIndex(SyosetuLibrary.COLUMN_NCODE);
        int urlIndex = cursor.getColumnIndex(SyosetuLibrary.COLUMN_URL);
        int nameIndex = cursor.getColumnIndex(SyosetuLibrary.COLUMN_NAME);
        int timeIndex = cursor.getColumnIndex(SyosetuLibrary.COLUMN_TIME);
        int siteIndex = cursor.getColumnIndex(SyosetuLibrary.COLUMN_SITE);
        int typeIndex = cursor.getColumnIndex(SyosetuLibrary.COLUMN_TYPE);

        do
        {
            String ncode = cursor.getString(ncodeIndex);
            String url = cursor.getString(urlIndex);
            String name = cursor.getString(nameIndex);
            String time = cursor.getString(timeIndex);
            String site = SyosetuUtility.getNovelSite(
                    getActivity(), SyosetuUtility.SyosetuSite.valueOf(cursor.getString(siteIndex)));
            String type = cursor.getString(typeIndex);

            mListAdapter.add(0, ncode, name, url, time, site, type);
        }
        while (cursor.moveToNext());

        cursor.close();

        return true;
    }


    private HistoryListAdapter.OnItemSelectListener mItemSelectListener =
            new HistoryListAdapter.OnItemSelectListener()
            {
                @Override
                public void onItemClick(View itemView)
                {
                    RecyclerView.ViewHolder holder
                            = mListView.findContainingViewHolder(itemView);
                    if (holder == null)
                        return;

                    int position = holder.getAdapterPosition();
                    HistoryListAdapter.BindData data = mListAdapter.getItem(position);

                    if (mSelectionActionMode == null)
                    {
                        Bundle bundle = new Bundle();
                        bundle.putString(NovelActivity.ARG_NOVEL_URL, data.url);

                        Intent intent = new Intent(getActivity(), NovelActivity.class);
                        intent.putExtras(bundle);
                        getActivity().startActivityForResult(intent, MainActivity.REQ_NOVEL_FROM_HIS);
                    }
                    else
                    {
                        mListAdapter.toggleSelect(data.ncode);
                        mSelectionActionMode.invalidate();
                    }
                }

                @Override
                public boolean onItemLongClick(View itemView)
                {
                    RecyclerView.ViewHolder holder
                            = mListView.findContainingViewHolder(itemView);
                    if (holder == null)
                        return false;

                    int position = holder.getAdapterPosition();
                    HistoryListAdapter.BindData data = mListAdapter.getItem(position);

                    if (mSelectionActionMode == null)
                    {
                        mListAdapter.showCheckBox(true);
                        mListAdapter.modify(data.ncode, true);

                        mSelectionActionMode = ((AppCompatActivity) getActivity())
                                .startSupportActionMode(mSelectionCallback);
                        return true;
                    }
                    else
                    {
                        mListAdapter.toggleSelect(data.ncode);
                        mSelectionActionMode.invalidate();
                        return true;
                    }
                }
            };


    private boolean canSelectAll()
    {
        return mListAdapter.getSelectedCount()
                != mListAdapter.getItemCount();
    }

    private boolean canDelete()
    {
        return mListAdapter.getSelectedCount() > 0;
    }


    private ActionMode.Callback mSelectionCallback = new ActionMode.Callback()
    {
        private final static int MENU_ITEM_ID_SELECT_ALL = 1;
        private final static int MENU_ITEM_ID_DELETE = 2;

        private final static int MENU_ITEM_ORDER_SELECT_ALL = 1;
        private final static int MENU_ITEM_ORDER_DELETE = 2;


        private boolean updateSelectAllMenuItem(Menu menu)
        {
            boolean canSelectAll = canSelectAll();
            boolean selectAllItemExistes = menu.findItem(MENU_ITEM_ID_SELECT_ALL) != null;
            if (canSelectAll && !selectAllItemExistes)
            {
                menu.add(Menu.NONE, MENU_ITEM_ID_SELECT_ALL, MENU_ITEM_ORDER_SELECT_ALL, "全选").
                        setAlphabeticShortcut('a').
                        setIcon(R.drawable.ic_action_select_all).
                        setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            else if (!canSelectAll && selectAllItemExistes)
                menu.removeItem(MENU_ITEM_ID_SELECT_ALL);
            else
                return false;

            return true;
        }

        private boolean updateDeleteMenuItem(Menu menu)
        {
            boolean canDelete = canDelete();
            boolean deleteItemExistes = menu.findItem(MENU_ITEM_ID_DELETE) != null;
            if (canDelete && !deleteItemExistes)
            {
                menu.add(Menu.NONE, MENU_ITEM_ID_DELETE, MENU_ITEM_ORDER_DELETE, "删除").
                        setAlphabeticShortcut('a').
                        setIcon(R.drawable.ic_action_delete).
                        setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            else if (!canDelete && deleteItemExistes)
                menu.removeItem(MENU_ITEM_ID_DELETE);
            else
                return false;

            return true;
        }


        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            mode.setTitle("已选择" + mListAdapter.getSelectedCount() + "项");
            mode.setSubtitle(null);
            mode.setTitleOptionalHint(true);

            updateSelectAllMenuItem(menu);
            updateDeleteMenuItem(menu);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu)
        {
            mode.setTitle("已选择" + mListAdapter.getSelectedCount() + "项");

            boolean updateSelectAll = updateSelectAllMenuItem(menu);
            boolean updateDelete = updateDeleteMenuItem(menu);

            return updateSelectAll || updateDelete;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item)
        {
            switch (item.getItemId())
            {
                case MENU_ITEM_ID_SELECT_ALL:
                    mListAdapter.selectAll();
                    mSelectionActionMode.invalidate();
                    return true;

                case MENU_ITEM_ID_DELETE:
                {
                    mListAdapter.removeSelectedItem();
                    if (mListAdapter.getItemCount() != 0)
                        mSelectionActionMode.invalidate();
                    else
                    {
                        mListView.setVisibility(View.GONE);
                        mTipTextView.setVisibility(View.VISIBLE);
                        mSelectionActionMode.finish();
                    }
                    return true;
                }
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode)
        {
            mListAdapter.showCheckBox(false);
            mSelectionActionMode = null;
        }
    };
}
