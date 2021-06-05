package com.ucclkp.syosetureader.author;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ucclkp.syosetureader.DivideDecoration;
import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;
import com.ucclkp.syosetureader.ImageDownloader;
import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuImageGetter;
import com.ucclkp.syosetureader.SyosetuUtility;
import com.ucclkp.syosetureader.UApplication;
import com.ucclkp.syosetureader.UrlDrawable;
import com.ucclkp.syosetureader.statictextview.StaticTextView;

import java.util.UUID;

public class AuthorInfoFragment extends Fragment {
    private int mPortion;
    private String mAuthorId;
    private String mPortionUrl;
    private SyosetuUtility.SyosetuSite mAuthorSite;
    private String mPageUUID;

    private int mCurPageNumber;
    private boolean mIsDisableScrollListener;

    private AuthorParser mAuthorParser;

    //author base.
    private StaticTextView mAuthorBaseTV;

    //author works.
    private RecyclerView mAuthorWorkList;
    private WorkListAdapter mWorkListAdapter;
    private LinearLayoutManager mWorkLinearLayoutManager;

    //common.
    private SwipeRefreshLayout mRefreshSRL;


    private final static String ARG_PORTION = "arg_portion";
    private final static String ARG_AUTHOR_URL = "arg_author_url";


    public AuthorInfoFragment() {
        mCurPageNumber = 1;
        mIsDisableScrollListener = false;
        mPageUUID = UUID.randomUUID().toString();
    }


    public static AuthorInfoFragment newInstance(
            int portion, String authorUrl) {
        AuthorInfoFragment fragment = new AuthorInfoFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_PORTION, portion);
        args.putString(ARG_AUTHOR_URL, authorUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        UApplication.imageDownloader.addOnDownloadListener(mImgDLListener);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mAuthorParser != null)
            mAuthorParser.cancel();
        UApplication.imageDownloader.removeOnDownloadListener(mImgDLListener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPortion = getArguments().getInt(ARG_PORTION);
            String authorUrl = getArguments().getString(ARG_AUTHOR_URL);

            mAuthorId = HtmlUtility.getUrlRear(authorUrl);
            mAuthorSite = SyosetuUtility.getSiteFromAuthorUrl(authorUrl);
            mPortionUrl = getSectionUrl() + mAuthorId;
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        switch (mPortion) {
            case AuthorPagerAdapter.FRAGMENT_PROFILE: {
                View parent = inflater.inflate(
                        R.layout.fragment_author_base, container, false);

                mRefreshSRL = parent.findViewById(R.id.srl_author_base_refresher);
                mRefreshSRL.setOnRefreshListener(mRefreshListener);
                mRefreshSRL.setColorSchemeResources(
                        R.color.color_blue,
                        R.color.color_red,
                        R.color.color_green,
                        R.color.color_yellow);
                boolean isNightMode = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);
                if (isNightMode)
                    mRefreshSRL.setProgressBackgroundColorSchemeResource(R.color.color_swipe_background);

                mAuthorBaseTV = parent.findViewById(R.id.stv_author_base_content);

                return parent;
            }

            case AuthorPagerAdapter.FRAGMENT_WORKS: {
                View parent = inflater.inflate(
                        R.layout.fragment_author_works, container, false);

                mWorkListAdapter = new WorkListAdapter(getActivity());
                mWorkLinearLayoutManager = new LinearLayoutManager(getActivity());

                mRefreshSRL = parent.findViewById(R.id.srl_author_works_refresher);
                mRefreshSRL.setOnRefreshListener(mRefreshListener);
                mRefreshSRL.setColorSchemeResources(
                        R.color.color_blue,
                        R.color.color_red,
                        R.color.color_green,
                        R.color.color_yellow);
                boolean isNightMode = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);
                if (isNightMode)
                    mRefreshSRL.setProgressBackgroundColorSchemeResource(R.color.color_swipe_background);

                mAuthorWorkList = parent.findViewById(R.id.rv_author_works_list);
                mAuthorWorkList.setAdapter(mWorkListAdapter);
                mAuthorWorkList.setLayoutManager(mWorkLinearLayoutManager);
                mAuthorWorkList.addItemDecoration(new DivideDecoration(getActivity()));
                mAuthorWorkList.addOnScrollListener(mListScrollListener);
                mAuthorWorkList.setMotionEventSplittingEnabled(false);

                return parent;
            }
        }

        return null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mAuthorParser = new AuthorParser(getActivity(),
                new SyosetuImageGetter(getActivity(), mPageUUID), mUrlCallback);
        mAuthorParser.setPageType(mPortion, mAuthorSite);
        mAuthorParser.setPipelineListener(mParserListener);

        if (!TextUtils.isEmpty(mPortionUrl)) {
            mAuthorParser.enter(mPortionUrl);

            if (mAuthorParser.isInPipeline())
                mRefreshSRL.setRefreshing(true);
        }
    }


    private SwipeRefreshLayout.OnRefreshListener mRefreshListener
            = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            switch (mPortion) {
                case AuthorPagerAdapter.FRAGMENT_PROFILE:
                    mAuthorBaseTV.setText("");
                    break;

                case AuthorPagerAdapter.FRAGMENT_WORKS:
                    mWorkListAdapter.clear();
                    mCurPageNumber = 1;
                    mAuthorParser.resetPageNumber();
                    break;
            }

            mAuthorParser.enter(mPortionUrl);
        }
    };


    private AuthorParser.OnPipelineListener<Object> mParserListener
            = new HtmlDataPipeline.OnPipelineListener<Object>() {
        @Override
        public void onPostData(int exitCode, Object data) {
            mRefreshSRL.setRefreshing(false);

            if (exitCode != HtmlDataPipeline.CODE_SUCCESS) {
                Toast.makeText(getActivity(),
                        "Failed", Toast.LENGTH_SHORT).show();

                switch (mPortion) {
                    case AuthorPagerAdapter.FRAGMENT_WORKS:
                        mWorkListAdapter.setFootProgress(
                                WorkListAdapter.PROGRESS_ERROR);
                        break;
                }
                return;
            }

            switch (mPortion) {
                case AuthorPagerAdapter.FRAGMENT_PROFILE: {
                    AuthorParser.BaseData baseData
                            = (AuthorParser.BaseData) data;
                    mAuthorBaseTV.setText(baseData.data);
                    break;
                }

                case AuthorPagerAdapter.FRAGMENT_WORKS: {
                    AuthorParser.WorkData workData
                            = (AuthorParser.WorkData) data;

                    mIsDisableScrollListener = false;
                    mWorkListAdapter.removeFootProgress();

                    for (int i = 0; i < workData.itemList.size(); ++i)
                        mWorkListAdapter.add(workData.itemList.get(i));

                    if (mCurPageNumber < mAuthorParser.getCurMaxPageNumber())
                        mWorkListAdapter.addFootProgress(
                                WorkListAdapter.PROGRESS_LOADING);
                    else
                        mWorkListAdapter.addFootProgress(
                                WorkListAdapter.PROGRESS_NO_MORE_DATA);

                    break;
                }
            }
        }
    };


    private RecyclerView.OnScrollListener mListScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);

            if (!mIsDisableScrollListener) {
                int totalItemCount = mWorkListAdapter.getItemCount();
                int lastVisiblePosition = mWorkLinearLayoutManager.findLastVisibleItemPosition();

                if (totalItemCount > 0 && lastVisiblePosition == totalItemCount - 1) {
                    int curMaxPageNumber = mAuthorParser.getCurMaxPageNumber();
                    if (mCurPageNumber < curMaxPageNumber) {
                        mCurPageNumber++;
                        mIsDisableScrollListener = true;

                        mAuthorParser.enter(mPortionUrl
                                + "?p=" + mCurPageNumber);
                    }
                }
            }
        }
    };

    private SyosetuUtility.UrlCallback mUrlCallback = new SyosetuUtility.UrlCallback() {
        @Override
        public void onClick(String url, View widget) {
            UApplication.chromeCustomTabsManager
                    .startChromeTab(getActivity(), url);
        }
    };

    private ImageDownloader.OnDownloadListener mImgDLListener
            = new ImageDownloader.OnDownloadListener() {
        @Override
        public void onDownloadComplete(String pageId, ImageDownloader.ImageResult result) {
            if (mPageUUID.equals(pageId)
                    && mPortion == AuthorPagerAdapter.FRAGMENT_PROFILE) {
                Spanned spannedText = (Spanned) mAuthorBaseTV.getText();
                ImageSpan[] imgSpans = spannedText.getSpans(0, spannedText.length(), ImageSpan.class);
                for (ImageSpan span : imgSpans) {
                    UrlDrawable urlDrawable = (UrlDrawable) span.getDrawable();
                    if (urlDrawable.mState != UrlDrawable.State.STATE_COMPLETED
                            && urlDrawable.mSource.equals(result.imageUrl)) {
                        int height = urlDrawable.mDrawable.getBounds().height();

                        urlDrawable.mDrawable = new BitmapDrawable(getResources(), result.bitmap);

                        float factor = (float) height / urlDrawable.mDrawable.getIntrinsicHeight();
                        int width = (int) (urlDrawable.mDrawable.getIntrinsicWidth() * factor);
                        urlDrawable.mDrawable.setBounds(0, 0, width, height);

                        urlDrawable.mState = UrlDrawable.State.STATE_COMPLETED;
                        urlDrawable.setBounds(urlDrawable.mDrawable.getBounds());
                        mAuthorBaseTV.requestLayout();
                        mAuthorBaseTV.invalidate();
                    }
                }
            }
        }
    };


    public String getSectionUrl() {
        switch (mPortion) {
            case AuthorPagerAdapter.FRAGMENT_PROFILE: {
                switch (mAuthorSite) {
                    case NORMAL:
                        return "http://mypage.syosetu.com/mypage/profile/userid/";
                    case NOCTURNE:
                        return "http://xmypage.syosetu.com/";
                }
            }

            case AuthorPagerAdapter.FRAGMENT_WORKS: {
                switch (mAuthorSite) {
                    case NORMAL:
                        return "http://mypage.syosetu.com/mypage/novellist/userid/";
                    case NOCTURNE:
                        return "http://xmypage.syosetu.com/mypage/novellist/xid/";
                }
            }
        }

        return "";
    }
}
