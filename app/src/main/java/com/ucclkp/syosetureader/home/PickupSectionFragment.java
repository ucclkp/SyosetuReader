package com.ucclkp.syosetureader.home;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ucclkp.syosetureader.DivideDecoration;
import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuUtility;

public class PickupSectionFragment extends Fragment implements HomePagerAdapter.OnPageRefreshListener
{
    private RecyclerView mPickupListView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private PickupListAdapter mPickupListAdapter;
    private LinearLayoutManager mLinearLayoutManager;

    private PickupParser mPickupParser;
    private boolean mIsDisableScrollListener;
    private int mCurPageNumber;


    public PickupSectionFragment()
    {
        mCurPageNumber = 1;
        mIsDisableScrollListener = false;
    }


    public static PickupSectionFragment newInstance()
    {
        PickupSectionFragment fragment = new PickupSectionFragment();
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
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        if (mPickupParser != null)
            mPickupParser.cancel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View parent = inflater.inflate(R.layout.fragment_section_pickup, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) parent.findViewById(R.id.srl_pickup_fragment);
        mSwipeRefreshLayout.setOnRefreshListener(mRefreshListener);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.color_blue,
                R.color.color_red,
                R.color.color_green,
                R.color.color_yellow);
        boolean isNightMode = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);
        if (isNightMode)
            mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.color_swipe_background);

        mPickupListAdapter = new PickupListAdapter(getContext());
        mPickupListAdapter.setOnItemSelectListener(mItemSelectListener);

        mLinearLayoutManager = new LinearLayoutManager(getContext());

        mPickupListView = (RecyclerView) parent.findViewById(R.id.rv_pickup_fragment);
        mPickupListView.setHasFixedSize(true);
        mPickupListView.setLayoutManager(mLinearLayoutManager);
        mPickupListView.setAdapter(mPickupListAdapter);
        mPickupListView.addItemDecoration(new DivideDecoration(getActivity()));
        mPickupListView.addOnItemTouchListener(mItemTouchListener);
        mPickupListView.addOnScrollListener(mListScrollListener);
        mPickupListView.setMotionEventSplittingEnabled(false);

        mPickupParser = new PickupParser();
        mPickupParser.setPipelineListener(mPickupParserListener);
        mPickupParser.enter(SyosetuUtility.getPickupUrl());

        if (mPickupParser.isInPipeline())
            mSwipeRefreshLayout.setRefreshing(true);

        return parent;
    }

    @Override
    public void onRefresh()
    {
        mPickupListAdapter.clear();

        mCurPageNumber = 1;
        mPickupParser.resetPageNumber();
        mPickupParser.enter(SyosetuUtility.getPickupUrl());
        if (mPickupParser.isInPipeline())
            mSwipeRefreshLayout.setRefreshing(true);
    }


    private PickupParser.OnPipelineListener<PickupParser.HomeData> mPickupParserListener
            = new HtmlDataPipeline.OnPipelineListener<PickupParser.HomeData>()
    {
        @Override
        public void onPostData(int exitCode, PickupParser.HomeData data)
        {
            if (exitCode != PickupParser.CODE_SUCCESS)
            {
                Toast.makeText(getContext(),
                        "Failed.", Toast.LENGTH_SHORT).show();
                mPickupListAdapter.setFootProgress(
                        PickupListAdapter.PROGRESS_ERROR);
            } else
            {
                mIsDisableScrollListener = false;
                mPickupListAdapter.removeFootProgress();

                for (int i = 0; i < data.itemList.size(); ++i)
                    mPickupListAdapter.add(data.itemList.get(i));

                if (mCurPageNumber < mPickupParser.getCurMaxPageNumber())
                    mPickupListAdapter.addFootProgress(
                            PickupListAdapter.PROGRESS_LOADING);
                else
                    mPickupListAdapter.addFootProgress(
                            PickupListAdapter.PROGRESS_NO_MORE_DATA);
            }

            mSwipeRefreshLayout.setRefreshing(false);
        }
    };


    private SwipeRefreshLayout.OnRefreshListener mRefreshListener = new SwipeRefreshLayout.OnRefreshListener()
    {
        @Override
        public void onRefresh()
        {
            mCurPageNumber = 1;
            mPickupListAdapter.clear();
            mPickupParser.resetPageNumber();
            mPickupParser.enter(SyosetuUtility.getPickupUrl());
        }
    };


    private PickupListAdapter.OnItemSelectListener mItemSelectListener
            = new PickupListAdapter.OnItemSelectListener()
    {
        @Override
        public void onItemClick(View itemView)
        {
            RecyclerView.ViewHolder holder
                    = mPickupListView.findContainingViewHolder(itemView);
            if (holder == null)
                return;

            int position = holder.getAdapterPosition();
            PickupListAdapter.BindData data = mPickupListAdapter.getItem(position);

            Bundle bundle = new Bundle();
            //bundle.putString();

            //Intent intent = new Intent(getActivity(), TiebaActivity.class);
            //intent.putExtras(bundle);
            //startActivity(intent);
        }

        @Override
        public boolean onItemLongClick(View itemView)
        {
            RecyclerView.ViewHolder holder
                    = mPickupListView.findContainingViewHolder(itemView);
            if (holder == null)
                return false;

            int position = holder.getAdapterPosition();
            PickupListAdapter.BindData data = mPickupListAdapter.getItem(position);

            return false;
        }
    };


    private RecyclerView.OnScrollListener mListScrollListener = new RecyclerView.OnScrollListener()
    {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState)
        {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy)
        {
            super.onScrolled(recyclerView, dx, dy);

            if (!mIsDisableScrollListener)
            {
                int totalItemCount = mPickupListAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastVisibleItemPosition();

                if (totalItemCount > 0 && lastVisiblePosition == totalItemCount - 1)
                {
                    int curMaxPageNumber = mPickupParser.getCurMaxPageNumber();
                    if (mCurPageNumber < curMaxPageNumber)
                    {
                        mCurPageNumber++;
                        mIsDisableScrollListener = true;

                        mPickupParser.enter(
                                SyosetuUtility.getPickupUrl()
                                        + mPickupParser.getPageNumberFix()
                                        + mCurPageNumber);
                    }
                }
            }
        }
    };

    private RecyclerView.OnItemTouchListener mItemTouchListener = new RecyclerView.OnItemTouchListener()
    {
        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e)
        {
            switch (e.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    break;
            }
            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e)
        {
        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept)
        {
        }
    };
}
