package com.ucclkp.syosetureader.search;


import android.os.Bundle;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ucclkp.syosetureader.DivideDecoration;
import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuUtility;
import com.ucclkp.syosetureader.UApplication;


public class SearchResultFragment extends Fragment
{
    private String mSearchKey;
    private String mSearchUrl;

    private SearchResultParser mResultParser;
    private LinearLayoutManager mLinearLayoutManager;
    private SearchResultListAdapter mSearchResultListAdapter;
    private boolean mIsDisableScrollListener;
    private int mCurPageNumber;

    private RecyclerView mSearchResultListView;
    private SwipeRefreshLayout mSwipeRefreshLayout;


    private static final String ARG_SEARCH_KEY = "arg_search_key";
    private static final String ARG_SEARCH_URL = "arg_search_url";


    public SearchResultFragment()
    {
        mCurPageNumber = 1;
        mIsDisableScrollListener = false;
    }


    public static SearchResultFragment newInstance(String key, String searchUrl)
    {
        SearchResultFragment fragment = new SearchResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SEARCH_KEY, key);
        args.putString(ARG_SEARCH_URL, searchUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            mSearchKey = getArguments().getString(ARG_SEARCH_KEY);
            mSearchUrl = getArguments().getString(ARG_SEARCH_URL);
        }

        getActivity().setTitle("検索：" + mSearchKey);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        if (mResultParser != null)
            mResultParser.cancel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View parent = inflater.inflate(R.layout.fragment_search_result, container, false);

        TabLayout tabLayout = getActivity().findViewById(R.id.tl_main_activity);
        tabLayout.setVisibility(View.GONE);

        Toolbar toolbar = getActivity().findViewById(R.id.tb_main_activity);
        AppBarLayout.LayoutParams lp = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
        lp.setScrollFlags(0);
        toolbar.setLayoutParams(lp);

        mSwipeRefreshLayout = parent.findViewById(R.id.srl_search_result_fragment);
        mSwipeRefreshLayout.setOnRefreshListener(mRefreshListener);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.color_blue,
                R.color.color_red,
                R.color.color_green,
                R.color.color_yellow);
        boolean isNightMode = UApplication.isNightMode(getContext());
        if (isNightMode)
            mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.color_swipe_background);

        mSearchResultListAdapter = new SearchResultListAdapter(getContext());
        mSearchResultListAdapter.setOnItemSelectListener(mItemSelectListener);

        mLinearLayoutManager = new LinearLayoutManager(getContext());

        mSearchResultListView = parent.findViewById(R.id.rv_search_result_fragment);
        mSearchResultListView.setHasFixedSize(true);
        mSearchResultListView.setLayoutManager(mLinearLayoutManager);
        mSearchResultListView.setAdapter(mSearchResultListAdapter);
        mSearchResultListView.addItemDecoration(new DivideDecoration(getActivity()));
        mSearchResultListView.addOnScrollListener(mListScrollListener);
        mSearchResultListView.setMotionEventSplittingEnabled(false);

        mResultParser = new SearchResultParser();
        mResultParser.setPipelineListener(mPickupParserListener);
        mResultParser.enter(mSearchUrl);

        if (mResultParser.isInPipeline())
            mSwipeRefreshLayout.setRefreshing(true);

        return parent;
    }

    @Override
    public void onHiddenChanged(boolean hidden)
    {
        super.onHiddenChanged(hidden);

        if (!hidden)
        {
            getActivity().setTitle("検索：" + mSearchKey);
        }
    }


    public void refresh()
    {
        String params = mSearchUrl.substring(mSearchUrl.indexOf("?"));
        mSearchUrl = SyosetuUtility.getSearchUrl() + params;

        mSearchResultListAdapter.clear();

        mCurPageNumber = 1;
        mResultParser.resetMaxPageNumber();
        mResultParser.enter(mSearchUrl);
        if (mResultParser.isInPipeline())
            mSwipeRefreshLayout.setRefreshing(true);
    }


    private SearchResultParser.OnPipelineListener<SearchResultParser.SearchData> mPickupParserListener
            = new HtmlDataPipeline.OnPipelineListener<SearchResultParser.SearchData>()
    {
        @Override
        public void onPostData(int exitCode, SearchResultParser.SearchData data)
        {
            if (getActivity() == null)
                return;

            if (exitCode != SearchResultParser.CODE_SUCCESS)
            {
                Toast.makeText(getContext(),
                        "Failed.", Toast.LENGTH_SHORT).show();
                mSearchResultListAdapter.setFootProgress(
                        SearchResultListAdapter.PROGRESS_ERROR);
            } else
            {
                mIsDisableScrollListener = false;
                mSearchResultListAdapter.removeFootProgress();

                for (int i = 0; i < data.itemList.size(); ++i)
                    mSearchResultListAdapter.add(data.itemList.get(i));

                if (mCurPageNumber < mResultParser.getCurMaxPageNumber())
                    mSearchResultListAdapter.addFootProgress(
                            SearchResultListAdapter.PROGRESS_LOADING);
                else
                    mSearchResultListAdapter.addFootProgress(
                            SearchResultListAdapter.PROGRESS_NO_MORE_DATA);
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
            mSearchResultListAdapter.clear();
            mResultParser.resetMaxPageNumber();
            mResultParser.enter(mSearchUrl);
        }
    };


    private SearchResultListAdapter.OnItemSelectListener mItemSelectListener
            = new SearchResultListAdapter.OnItemSelectListener()
    {
        @Override
        public void onItemClick(View itemView)
        {
            RecyclerView.ViewHolder holder
                    = mSearchResultListView.findContainingViewHolder(itemView);
            if (holder == null)
                return;

            int position = holder.getAdapterPosition();
            SearchResultListAdapter.BindData data = mSearchResultListAdapter.getItem(position);

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
                    = mSearchResultListView.findContainingViewHolder(itemView);
            if (holder == null)
                return false;

            int position = holder.getAdapterPosition();
            SearchResultListAdapter.BindData data = mSearchResultListAdapter.getItem(position);

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
                int totalItemCount = mSearchResultListAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastVisibleItemPosition();

                if (totalItemCount > 0 && lastVisiblePosition == totalItemCount - 1)
                {
                    int curMaxPageNumber = mResultParser.getCurMaxPageNumber();
                    if (mCurPageNumber < curMaxPageNumber)
                    {
                        mCurPageNumber++;
                        mIsDisableScrollListener = true;

                        mResultParser.enter(mSearchUrl
                                + "&p=" + mCurPageNumber);
                    }
                }
            }
        }
    };
}