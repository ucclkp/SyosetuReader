package com.ucclkp.syosetureader.novel;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ucclkp.syosetureader.AgeCertificationDialogFragment;
import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;
import com.ucclkp.syosetureader.ImageDownloader;
import com.ucclkp.syosetureader.MainActivity;
import com.ucclkp.syosetureader.NovelDownloadService;
import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuBooks;
import com.ucclkp.syosetureader.SyosetuImageGetter;
import com.ucclkp.syosetureader.SyosetuLibrary;
import com.ucclkp.syosetureader.SyosetuUtility;
import com.ucclkp.syosetureader.UApplication;
import com.ucclkp.syosetureader.UrlDrawable;
import com.ucclkp.syosetureader.author.AuthorActivity;
import com.ucclkp.syosetureader.novelinfo.NovelInfoActivity;
import com.ucclkp.syosetureader.statictextview.StaticTextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static android.content.Context.MODE_PRIVATE;

public class NovelFragment extends Fragment
{
    //最近阅读的章节。
    private String mCurrentSection;
    //最近阅读的章节的位置。
    private int mSectionOffset;
    //已阅读过的章节。
    private ArrayList<String> mViewedSection;

    private int mTextSize;
    private String mLineSpacingAdd, mLineSpacingMult;
    private String mBackgroundId;

    private String mPageUUID;
    private String mNovelUrl;
    private String mNovelCode;
    private String mNovelTitle;
    private SyosetuUtility.SyosetuSite mNovelSite;
    private SyosetuUtility.SyosetuSource mNovelSource;

    private int mResultCode;
    private boolean mIsFavorite;
    private boolean mIsShortNovel;
    private boolean mIsFirstLoad;
    private boolean mIsFavoriteInit;

    private NovelParser mNovelParser;
    private NovelListAdapter mSectionListAdapter;
    private LinearLayoutManager mLinearLayoutManager;
    private DownloadServiceConnection mDLServiceConnection;
    private SyosetuImageGetter mImageGetter;

    //Activity View.
    private Toolbar mToolbar;
    private AppBarLayout mAppBarLayout;
    //连载小说相关View.
    private RecyclerView mSectionListView;
    private SwipeRefreshLayout mNovelSRL;
    //短篇小说相关View.
    private StaticTextView mShortNovelSTV;
    private SwipeRefreshLayout mShortNovelSRL;

    private NovelParser.NovelData mSavedNovelData;


    private static final String SAVED_CURRENT_SECTION = "saved_current_section";
    private static final String SAVED_SECTION_OFFSET = "saved_section_offset";
    private static final String SAVED_VIEWED_SECTION = "saved_viewed_section";
    private static final String SAVED_IS_SHORT = "saved_is_short";
    private static final String SAVED_IS_FAVORITE = "saved_is_favorite";
    private static final String SAVED_IS_FAVORITE_INIT = "saved_is_favorite_init";
    private static final String SAVED_IS_FIRST_LOAD = "saved_is_first_load";
    private static final String SAVED_RESULT_CODE = "saved_result_code";
    private static final String SAVED_ATTENTION = "saved_attention";
    private static final String SAVED_TITLE = "saved_title";
    private static final String SAVED_AUTHOR = "saved_author";
    private static final String SAVED_AUTHOR_URL = "saved_author_url";
    private static final String SAVED_SUMMARY = "saved_summary";
    private static final String SAVED_LIST_DATA = "saved_list_data";
    private static final String SAVED_CONTENT_LENGTH = "saved_content_length";
    private static final String SAVED_NOVEL_URL = "saved_novel_url";
    private static final String SAVED_NOVEL_SOURCE = "saved_novel_source";
    private static final String SAVED_NOVEL_SITE = "saved_novel_site";
    private static final String SAVED_NOVEL_INFO_URL = "saved_novel_info_url";
    private static final String SAVED_NOVEL_FEEL_URL = "saved_novel_feel_url";
    private static final String SAVED_NOVEL_REVIEW_URL = "saved_novel_review";

    private static final String ARG_NOVEL_URL = "novel_url";

    private static final int AC_RC_CACHE = 1;
    private static final int AC_RC_NO_NEED_CACHE = 2;


    public NovelFragment()
    {
        mResultCode = MainActivity.RC_NONE;
        mPageUUID = UUID.randomUUID().toString();
        mSavedNovelData = null;

        mCurrentSection = "";
        mSectionOffset = 0;
        mViewedSection = new ArrayList<>();
        mIsFirstLoad = true;
        mIsShortNovel = false;
    }


    public static NovelFragment newInstance(String url)
    {
        NovelFragment fragment = new NovelFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NOVEL_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        UApplication.imageDownloader.addOnDownloadListener(mImgDLListener);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        if (mNovelParser != null)
            mNovelParser.cancel();
        UApplication.imageDownloader.removeOnDownloadListener(mImgDLListener);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null
                && savedInstanceState.getString(SAVED_NOVEL_URL) != null)
        {
            mNovelUrl = savedInstanceState.getString(SAVED_NOVEL_URL);
            mNovelCode = HtmlUtility.getUrlRear(mNovelUrl);
            mIsFavorite = savedInstanceState.getBoolean(SAVED_IS_FAVORITE);
            mIsFavoriteInit = savedInstanceState.getBoolean(SAVED_IS_FAVORITE_INIT);

            getActivity().setTitle(mNovelCode);
        } else
        {
            if (getArguments() != null)
            {
                mNovelUrl = getArguments().getString(ARG_NOVEL_URL);
                if (mNovelUrl != null)
                {
                    mNovelCode = HtmlUtility.getUrlRear(mNovelUrl);
                    mIsFavoriteInit = mIsFavorite
                            = ((UApplication) getActivity().getApplication())
                            .getSyosetuLibrary().hasFav(mNovelCode);
                    getActivity().setTitle(mNovelCode);
                }
            }
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View parent = inflater.inflate(R.layout.fragment_novel, container, false);

        mNovelSRL = parent.findViewById(R.id.srl_novel);
        mNovelSRL.setOnRefreshListener(mRefreshListener);
        mNovelSRL.setColorSchemeResources(
                R.color.color_blue,
                R.color.color_red,
                R.color.color_green,
                R.color.color_yellow);
        boolean isNightMode = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);
        if (isNightMode)
            mNovelSRL.setProgressBackgroundColorSchemeResource(R.color.color_swipe_background);

        mShortNovelSRL = parent.findViewById(R.id.srl_novel_short);
        mShortNovelSRL.setOnRefreshListener(mRefreshListener);
        mShortNovelSRL.setColorSchemeResources(
                R.color.color_blue,
                R.color.color_red,
                R.color.color_green,
                R.color.color_yellow);
        if (isNightMode)
            mShortNovelSRL.setProgressBackgroundColorSchemeResource(R.color.color_swipe_background);

        mToolbar = getActivity().findViewById(R.id.tb_novel_activity);
        AppBarLayout.LayoutParams lp
                = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        lp.setScrollFlags(0);
        mToolbar.setLayoutParams(lp);

        mAppBarLayout = getActivity().findViewById(R.id.abl_novel_activity);
        mAppBarLayout.setExpanded(true, false);

        mSectionListAdapter = new NovelListAdapter(getContext());
        mSectionListAdapter.setOnItemSelectListener(mSectionSelectListener);

        mLinearLayoutManager = new LinearLayoutManager(getContext());

        mSectionListView = parent.findViewById(R.id.rv_novel);
        mSectionListView.setHasFixedSize(true);
        mSectionListView.setLayoutManager(mLinearLayoutManager);
        mSectionListView.setAdapter(mSectionListAdapter);
        mSectionListView.setMotionEventSplittingEnabled(false);
        //mSectionListView.addItemDecoration(mSearchResultListDecoration);
        //mSectionListView.addOnItemTouchListener(mItemTouchListener);
        //mSectionListView.addOnScrollListener(mListScrollListener);

        SharedPreferences preferences = getActivity().getSharedPreferences(
                UApplication.PREF_FORMAT, MODE_PRIVATE);
        mTextSize = preferences.getInt(
                UApplication.FONT_SIZE, FormatDialogFragment.DEFAULT_FONT_SIZE_DIP);
        mLineSpacingAdd = preferences.getString(
                UApplication.LINE_SPACING_ADD, FormatDialogFragment.DEFAULT_LSA_DIP);
        mLineSpacingMult = preferences.getString(
                UApplication.LINE_SPACING_MULT, FormatDialogFragment.DEFAULT_LSM);

        if (isNightMode)
            mBackgroundId = preferences.getString(
                    UApplication.BACKGROUND_NIGHT_ID, FormatDialogFragment.BACKGROUND_ID_NIGHT_DEFAULT);
        else
            mBackgroundId = preferences.getString(
                    UApplication.BACKGROUND_ID, FormatDialogFragment.BACKGROUND_ID_DEFAULT);

        mShortNovelSTV = parent.findViewById(R.id.stv_novel_short);
        mShortNovelSTV.setTextSize(mTextSize);
        mShortNovelSTV.setLineSpacing(Float.valueOf(mLineSpacingMult), Float.valueOf(mLineSpacingAdd));
        mShortNovelSTV.setBackground(FormatDialogFragment.getBackgroundById(getContext(), mBackgroundId));

        return parent;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        boolean hasState = (savedInstanceState != null
                && savedInstanceState.getString(SAVED_CURRENT_SECTION) != null);

        mImageGetter = new SyosetuImageGetter(getActivity(), mPageUUID);

        Log.d("RetainDBG", "NovelFragment.onViewCreated(" + hasState + ") invoked.");

        mNovelParser = new NovelParser(mImageGetter);
        mNovelParser.setPipelineListener(mNovelParserListener);

        if (hasState)
        {
            mCurrentSection = savedInstanceState.getString(SAVED_CURRENT_SECTION);
            mSectionOffset = savedInstanceState.getInt(SAVED_SECTION_OFFSET);

            mViewedSection = new ArrayList<>();
            ArrayList<String> arrayList = savedInstanceState.getStringArrayList(SAVED_VIEWED_SECTION);
            if (arrayList != null)
                mViewedSection.addAll(arrayList);

            mIsShortNovel = savedInstanceState.getBoolean(SAVED_IS_SHORT);
            mIsFavorite = savedInstanceState.getBoolean(SAVED_IS_FAVORITE);
            mIsFavoriteInit = savedInstanceState.getBoolean(SAVED_IS_FAVORITE_INIT);
            mIsFirstLoad = savedInstanceState.getBoolean(SAVED_IS_FIRST_LOAD);
            mResultCode = savedInstanceState.getInt(SAVED_RESULT_CODE);
            mNovelSource = SyosetuUtility.SyosetuSource
                    .valueOf(savedInstanceState.getString(SAVED_NOVEL_SOURCE));
            mNovelSite = SyosetuUtility.SyosetuSite
                    .valueOf(savedInstanceState.getString(SAVED_NOVEL_SITE));

            getActivity().setResult(mResultCode);

            mSavedNovelData = new NovelParser.NovelData();

            String attention = savedInstanceState.getString(SAVED_ATTENTION);
            String title = savedInstanceState.getString(SAVED_TITLE);
            String author = savedInstanceState.getString(SAVED_AUTHOR);
            String authorUrl = savedInstanceState.getString(SAVED_AUTHOR_URL);
            String summary = savedInstanceState.getString(SAVED_SUMMARY);
            String listData = savedInstanceState.getString(SAVED_LIST_DATA);
            int contentLength = savedInstanceState.getInt(SAVED_CONTENT_LENGTH);

            mSavedNovelData.headAttention = attention;
            mSavedNovelData.headTitle = title;
            mSavedNovelData.headAuthor = author;
            mSavedNovelData.headAuthorUrl = authorUrl;
            mSavedNovelData.headSummary =
                    new SpannableStringBuilder(Html.fromHtml(summary));
            mSavedNovelData.length = contentLength;
            loadListFromJSON(listData, mSavedNovelData.chOrSeList);

            mSavedNovelData.novelInfoUrl = savedInstanceState.getString(SAVED_NOVEL_INFO_URL);
            mSavedNovelData.novelFeelUrl = savedInstanceState.getString(SAVED_NOVEL_FEEL_URL);
            mSavedNovelData.novelReviewUrl = savedInstanceState.getString(SAVED_NOVEL_REVIEW_URL);

            loadDataToView(mSavedNovelData);
        } else
        {
            if (restoreFromDownload())
            {
                mNovelSource = SyosetuUtility.SyosetuSource.DOWNLOAD;

                if (mNovelSite != SyosetuUtility.SyosetuSite.NORMAL)
                {
                    boolean[] result = new boolean[2];
                    showAgeCertDialog(AC_RC_NO_NEED_CACHE, result);
                    if (result[0])
                        getActivity().finish();
                    else
                    {
                        if (result[1])
                            loadDataToView(mSavedNovelData);
                    }
                } else
                    loadDataToView(mSavedNovelData);
            } else if (restoreContent())
            {
                mNovelSource = SyosetuUtility.SyosetuSource.CACHE;

                if (mNovelSite != SyosetuUtility.SyosetuSite.NORMAL)
                {
                    boolean[] result = new boolean[2];
                    showAgeCertDialog(AC_RC_NO_NEED_CACHE, result);
                    if (result[0])
                        getActivity().finish();
                    else
                    {
                        if (result[1])
                            loadDataToView(mSavedNovelData);
                    }
                } else
                    loadDataToView(mSavedNovelData);
            } else
            {
                mNovelSource = SyosetuUtility.SyosetuSource.NETWORK;
                mNovelParser.enter(mNovelUrl);

                if (mNovelParser.isInPipeline())
                    mNovelSRL.setRefreshing(true);
            }
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden)
    {
        super.onHiddenChanged(hidden);

        if (!hidden)
        {
            mToolbar.setTitle(mNovelCode);
            if (mSavedNovelData != null && mIsShortNovel)
                mToolbar.setSubtitle(SyosetuUtility.constructSubtitle(
                        getActivity(), null, mSavedNovelData.length, mNovelSource));
            else if (mSavedNovelData != null)
                mToolbar.setSubtitle(SyosetuUtility.constructSubtitle(
                        getActivity(), null, -1, mNovelSource));
            else
                mToolbar.setSubtitle("");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.menu_novel_content_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        MenuItem menuItem = menu.findItem(
                R.id.menu_novel_content_fragment_action_fav);
        if (mIsFavorite)
            menuItem.setIcon(R.drawable.ic_favorite);
        else
            menuItem.setIcon(R.drawable.ic_favorite_border);
        menuItem.setVisible(mSavedNovelData != null);

        MenuItem formatMenuItem = menu.findItem(
                R.id.menu_novel_content_fragment_action_format);
        formatMenuItem.setVisible(mIsShortNovel);

        MenuItem downloadMenuItem = menu.findItem(
                R.id.menu_novel_content_fragment_action_dl);
        downloadMenuItem.setVisible(mSavedNovelData != null);

        MenuItem authorMenuItem = menu.findItem(
                R.id.menu_novel_content_fragment_action_author);
        authorMenuItem.setVisible(
                mSavedNovelData != null
                        && !mSavedNovelData.headAuthorUrl.isEmpty());

        MenuItem novelInfoMenuItem = menu.findItem(
                R.id.menu_novel_content_fragment_action_novelinfo);
        novelInfoMenuItem.setVisible(mSavedNovelData != null);

        MenuItem feelMenuItem = menu.findItem(
                R.id.menu_novel_content_fragment_action_feel);
        feelMenuItem.setVisible(mSavedNovelData != null);

        MenuItem reviewMenuItem = menu.findItem(
                R.id.menu_novel_content_fragment_action_review);
        reviewMenuItem.setVisible(mSavedNovelData != null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_novel_content_fragment_action_fav:
            {
                toggleFavorite();
                return true;
            }

            case R.id.menu_novel_content_fragment_action_dl:
            {
                if (mDLServiceConnection == null)
                {
                    mDLServiceConnection = new DownloadServiceConnection();

                    Intent intent = new Intent(
                            getActivity().getApplicationContext(), NovelDownloadService.class);
                    getActivity().getApplicationContext()
                            .bindService(intent, mDLServiceConnection, Context.BIND_AUTO_CREATE);
                } else
                {
                    if (UApplication.dlServiceController.startDownload(mNovelUrl, mIsShortNovel))
                        Toast.makeText(getActivity(), "准备下载...", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), "已经处于下载队列中", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            case R.id.menu_novel_content_fragment_action_author:
            {
                Intent intent = new Intent(getActivity(), AuthorActivity.class);
                intent.putExtra(AuthorActivity.ARG_AUTHOR_URL, mSavedNovelData.headAuthorUrl);
                intent.putExtra(AuthorActivity.ARG_AUTHOR_NAME, mSavedNovelData.headAuthor);
                startActivity(intent);
                return true;
            }

            case R.id.menu_novel_content_fragment_action_novelinfo:
            {
                Intent intent = new Intent(getActivity(), NovelInfoActivity.class);
                intent.putExtra(NovelInfoActivity.ARG_NOVEL_INFO_URL, mSavedNovelData.novelInfoUrl);
                startActivity(intent);
                return true;
            }

            case R.id.menu_novel_content_fragment_action_feel:
            {
                Toast.makeText(getContext(), "功能开发中", Toast.LENGTH_SHORT).show();
                return true;
            }

            case R.id.menu_novel_content_fragment_action_review:
            {
                Toast.makeText(getContext(), "功能开发中", Toast.LENGTH_SHORT).show();
                return true;
            }

            case R.id.menu_novel_content_fragment_action_format:
            {
                FormatDialogFragment formatBottomSheet
                        = new FormatDialogFragment();

                Bundle bundle = new Bundle();
                bundle.putInt(FormatDialogFragment.ARG_REQUEST_CODE, NovelActivity.FORMAT_RC_SHORT);
                bundle.putInt(FormatDialogFragment.ARG_CURRENT_FONT_SIZE, mTextSize);
                bundle.putString(FormatDialogFragment.ARG_CURRENT_LSA, mLineSpacingAdd);
                bundle.putString(FormatDialogFragment.ARG_CURRENT_LSM, mLineSpacingMult);
                bundle.putString(FormatDialogFragment.ARG_CURRENT_BACKGROUND_ID, mBackgroundId);

                formatBottomSheet.setArguments(bundle);
                formatBottomSheet.show(getFragmentManager(), "fragment_format_bottom_sheet");
                return true;
            }

            case R.id.menu_novel_content_fragment_action_copy_ncode:
            {
                ((ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE))
                        .setPrimaryClip(
                                ClipData.newPlainText(null, mNovelCode));
                Toast.makeText(getActivity(), "小说编号已复制到剪贴板", Toast.LENGTH_SHORT).show();
                return true;
            }

            case R.id.menu_novel_content_fragment_action_copy_url:
            {
                ((ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE))
                        .setPrimaryClip(
                                ClipData.newPlainText(null, mNovelUrl));
                Toast.makeText(getActivity(), "小说Url已复制到剪贴板", Toast.LENGTH_SHORT).show();
                return true;
            }

            case R.id.menu_novel_content_fragment_action_open_browser:
                UApplication.chromeCustomTabsManager.startChromeTab(getContext(), mNovelUrl);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        Log.d("RetainDBG", "NovelFragment.onSaveInstanceState() invoked.");

        if (mSavedNovelData != null)
        {
            outState.putString(SAVED_CURRENT_SECTION, mCurrentSection);
            outState.putInt(SAVED_SECTION_OFFSET, mSectionOffset);
            outState.putStringArrayList(SAVED_VIEWED_SECTION, mViewedSection);

            outState.putBoolean(SAVED_IS_SHORT, mIsShortNovel);
            outState.putBoolean(SAVED_IS_FAVORITE, mIsFavorite);
            outState.putBoolean(SAVED_IS_FAVORITE_INIT, mIsFavoriteInit);
            outState.putBoolean(SAVED_IS_FIRST_LOAD, mIsFirstLoad);
            outState.putInt(SAVED_RESULT_CODE, mResultCode);

            outState.putString(SAVED_ATTENTION, mSavedNovelData.headAttention);
            outState.putString(SAVED_TITLE, mSavedNovelData.headTitle);
            outState.putString(SAVED_AUTHOR, mSavedNovelData.headAuthor);
            outState.putString(SAVED_AUTHOR_URL, mSavedNovelData.headAuthorUrl);
            outState.putString(SAVED_SUMMARY, Html.toHtml(mSavedNovelData.headSummary));
            outState.putString(SAVED_LIST_DATA, saveListToJSON(mSavedNovelData.chOrSeList));
            outState.putInt(SAVED_CONTENT_LENGTH, mSavedNovelData.length);
            outState.putString(SAVED_NOVEL_INFO_URL, mSavedNovelData.novelInfoUrl);
            outState.putString(SAVED_NOVEL_FEEL_URL, mSavedNovelData.novelFeelUrl);
            outState.putString(SAVED_NOVEL_REVIEW_URL, mSavedNovelData.novelReviewUrl);

            outState.putString(SAVED_NOVEL_URL, mNovelUrl);
            outState.putString(SAVED_NOVEL_SOURCE, mNovelSource.name());
            outState.putString(SAVED_NOVEL_SITE, mNovelSite.name());
        }
    }


    /**
     * 显示年龄认证对话框。
     *
     * @param result 元素个数为2. 第一个元素若为true, 则直接调用getActivity().finish(),
     *               否则查看第二个元素。若第二个元素为true, 则可无视此方法，继续原来的逻辑；
     *               若为false, 则直接返回，此时对话框将显示。
     */
    private void showAgeCertDialog(int reqCode, boolean[] result)
    {
        SharedPreferences prefs = getActivity().getSharedPreferences(
                UApplication.PREF_CONFIG, MODE_PRIVATE);
        if (prefs.getBoolean(UApplication.NOMORE_HINT18, false))
        {
            if (!prefs.getBoolean(UApplication.REMED_OVER18, false))
            {
                Toast.makeText(getActivity(), "您已选择：非18",
                        Toast.LENGTH_SHORT).show();
                result[0] = true;
            } else
            {
                result[0] = false;
                result[1] = true;
            }
        } else
        {
            AgeCertificationDialogFragment acDialogFragment
                    = AgeCertificationDialogFragment.newInstance(reqCode);
            acDialogFragment.setOnAgeCertListener(mAgeCertListener);
            acDialogFragment.show(getFragmentManager(), "fragment_ac_dialog");

            result[0] = false;
            result[1] = false;
        }
    }


    private void saveToHistory()
    {
        String type = SyosetuUtility.getTypeStr(
                getActivity(), mIsShortNovel);

        String viewed = "";
        if (!mIsShortNovel)
        {
            for (int i = 0; i < mViewedSection.size(); ++i)
                viewed += mViewedSection.get(i) + ",";
        }

        String curTime = DateFormat
                .format("yyyy-MM-dd HH:mm", new Date()).toString();

        ((UApplication) getActivity().getApplication())
                .getSyosetuLibrary().insertHisLast(
                mNovelCode, mNovelUrl, mNovelTitle, curTime, mNovelSite.name(),
                type, viewed, mCurrentSection, mSectionOffset);
    }

    private void loadHistory()
    {
        Cursor cursor = ((UApplication) getActivity().getApplication())
                .getSyosetuLibrary().getHis(mNovelCode);
        if (cursor != null)
        {
            mResultCode |= MainActivity.RC_REFRESH_HIS;
            getActivity().setResult(mResultCode);

            mCurrentSection = cursor.getString(
                    cursor.getColumnIndex(SyosetuLibrary.COLUMN_CURRENT));
            mSectionOffset = cursor.getInt(
                    cursor.getColumnIndex(SyosetuLibrary.COLUMN_OFFSET));
            String viewed = cursor.getString(
                    cursor.getColumnIndex(SyosetuLibrary.COLUMN_VIEWED));

            if (!viewed.isEmpty())
            {
                String[] viewedSections = viewed.split(",");
                for (int i = 0; i < viewedSections.length; ++i)
                {
                    if (!viewedSections[i].isEmpty())
                        mViewedSection.add(viewedSections[i]);
                }
            }

            cursor.close();
        }
    }

    private void toggleFavorite()
    {
        if (mIsFavorite)
        {
            mIsFavorite = false;
            ((UApplication) getActivity().getApplication())
                    .getSyosetuLibrary().deleteFav(mNovelCode);
            getActivity().invalidateOptionsMenu();
        } else
        {
            mIsFavorite = true;

            String type;
            if (mIsShortNovel)
                type = getString(R.string.type_short);
            else
                type = getString(R.string.type_series);

            String curTime = DateFormat
                    .format("yyyy-MM-dd HH:mm", new Date()).toString();

            ((UApplication) getActivity().getApplication())
                    .getSyosetuLibrary().insertFavUni(
                    mNovelCode, mNovelUrl, mNovelTitle, curTime, mNovelSite.name(), type);
            getActivity().invalidateOptionsMenu();
        }

        if (mIsFavorite != mIsFavoriteInit)
        {
            mResultCode |= MainActivity.RC_REFRESH_FAV;
            getActivity().setResult(mResultCode);
        } else
        {
            mResultCode &= ~MainActivity.RC_REFRESH_FAV;
            getActivity().setResult(mResultCode);
        }
    }

    public void recieveNovelRecord(String sectionUrl, String number, int offset)
    {
        Log.d("RetainDBG", "NovelFragment.recieveNovelRecord(" + number + ") invoked.");

        mCurrentSection = number;
        mSectionOffset = offset;
        if (!mViewedSection.contains(number))
            mViewedSection.add(number);

        NovelListAdapter.BindData bindData
                = mSectionListAdapter.modifySection(sectionUrl, true);
        if (bindData != null)
        {
            mSectionListAdapter.setPrevTip("上次看到：" + bindData.sectionName,
                    "点击这里继续阅读", sectionUrl);
        }
    }

    public void notifyNovelActivityExit()
    {
        Log.d("RetainDBG", "NovelFragment.notifyNovelActivityExit() invoked.");

        if (mNovelTitle != null
                && !mNovelTitle.isEmpty())
            saveToHistory();
    }

    public void notifyFontChanged(String fontName)
    {
        Typeface typeface = null;
        if (fontName.equals(getContext().getString(R.string.font_default)))
            typeface = null;

        mShortNovelSTV.setTypeface(typeface);
    }

    public void notifyFontSizeChanged(int size)
    {
        mTextSize = size;
        mShortNovelSTV.setTextSize(size);
    }

    public void notifyLineSpacingChanged(String mult, String add)
    {
        mLineSpacingAdd = add;
        mLineSpacingMult = mult;
        mShortNovelSTV.setLineSpacing(Float.valueOf(mult), Float.valueOf(add));
    }

    public void notifyBackgroundChanged(Drawable drawable, String name)
    {
        mBackgroundId = name;
        mShortNovelSTV.setBackground(drawable);
    }


    public static String saveListToJSON(
            List<NovelParser.NovelChOrSeData> dataList)
    {
        JSONArray jsonArray = new JSONArray();

        for (int i = 0; i < dataList.size(); ++i)
        {
            JSONObject jsonObject = new JSONObject();
            NovelParser.NovelChOrSeData data = dataList.get(i);

            try
            {
                jsonObject.put("type", data.type);
                jsonObject.put("chapter_title", data.chapterTitle);
                jsonObject.put("section_url", data.sectionUrl);
                jsonObject.put("section_time", data.sectionTime);
                jsonObject.put("section_title", data.sectionTitle);

                jsonArray.put(jsonObject);
            } catch (JSONException e)
            {
                e.printStackTrace();
            }
        }

        return jsonArray.toString();
    }

    public static void loadListFromJSON(
            String json,
            List<NovelParser.NovelChOrSeData> dataList)
    {
        try
        {
            JSONArray jsonArray = new JSONArray(json);

            for (int i = 0; i < jsonArray.length(); ++i)
            {
                NovelParser.NovelChOrSeData data
                        = new NovelParser.NovelChOrSeData();

                JSONObject jsonObject = jsonArray.getJSONObject(i);
                data.type = jsonObject.getInt("type");
                data.chapterTitle = jsonObject.getString("chapter_title");
                data.sectionUrl = jsonObject.getString("section_url");
                data.sectionTime = jsonObject.getString("section_time");
                data.sectionTitle = jsonObject.getString("section_title");

                dataList.add(data);
            }
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private void cacheContent()
    {
        if (mSavedNovelData == null)
            return;

        JSONObject jsonObject = new JSONObject();

        try
        {
            jsonObject.put("cache_length", mSavedNovelData.length);
            jsonObject.put("cache_attention", mSavedNovelData.headAttention);
            jsonObject.put("cache_author", mSavedNovelData.headAuthor);
            jsonObject.put("cache_author_url", mSavedNovelData.headAuthorUrl);
            jsonObject.put("cache_title", mSavedNovelData.headTitle);
            jsonObject.put("cache_summary", Html.toHtml(mSavedNovelData.headSummary));
            jsonObject.put("cache_list", saveListToJSON(mSavedNovelData.chOrSeList));
            jsonObject.put("cache_novel_info_url", mSavedNovelData.novelInfoUrl);
            jsonObject.put("cache_novel_feel_url", mSavedNovelData.novelFeelUrl);
            jsonObject.put("cache_novel_review_url", mSavedNovelData.novelReviewUrl);
            jsonObject.put("cache_url", mNovelUrl);
            jsonObject.put("cache_site", mNovelSite.name());

            ((UApplication) getActivity().getApplication())
                    .getCacheManager().putText(mNovelUrl, jsonObject.toString());
        } catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private boolean restoreContent()
    {
        String cached = ((UApplication) getActivity().getApplication())
                .getCacheManager().getText(mNovelUrl);
        if (cached != null)
        {
            try
            {
                JSONObject jsonObject = new JSONObject(cached);
                mSavedNovelData = new NovelParser.NovelData();

                mSavedNovelData.length = jsonObject.getInt("cache_length");
                mSavedNovelData.headAttention = jsonObject.getString("cache_attention");
                mSavedNovelData.headAuthor = jsonObject.getString("cache_author");
                mSavedNovelData.headAuthorUrl = jsonObject.getString("cache_author_url");
                mSavedNovelData.headTitle = jsonObject.getString("cache_title");
                mSavedNovelData.headSummary = new SpannableStringBuilder(
                        Html.fromHtml(jsonObject.getString("cache_summary"), mImageGetter, null));
                loadListFromJSON(jsonObject.getString("cache_list"), mSavedNovelData.chOrSeList);

                mSavedNovelData.novelInfoUrl = jsonObject.getString("cache_novel_info_url");
                mSavedNovelData.novelFeelUrl = jsonObject.getString("cache_novel_feel_url");
                mSavedNovelData.novelReviewUrl = jsonObject.getString("cache_novel_review_url");

                mNovelUrl = jsonObject.getString("cache_url");
                mNovelSite = SyosetuUtility.SyosetuSite.valueOf(jsonObject.getString("cache_site"));

                return true;
            } catch (JSONException e)
            {
                e.printStackTrace();
            }
        }

        return false;
    }

    private boolean restoreFromDownload()
    {
        SyosetuBooks books = ((UApplication) getActivity().getApplication())
                .getSyosetuBooks();
        Cursor cursor = books.getBook(mNovelCode);
        if (cursor != null)
        {
            mSavedNovelData = new NovelParser.NovelData();

            mSavedNovelData.length = cursor.getInt(cursor.getColumnIndex(SyosetuBooks.COLUMN_LENGTH));
            mSavedNovelData.headAttention = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_ATTENTION));
            mSavedNovelData.headAuthor = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_AUTHOR));
            mSavedNovelData.headAuthorUrl = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_AUTHOR_URL));
            mSavedNovelData.headTitle = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_NAME));
            mSavedNovelData.headSummary = new SpannableStringBuilder(
                    Html.fromHtml(cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_SUMMARY)), mImageGetter, null));

            String listData = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_LIST));
            if (listData != null && !listData.isEmpty())
                loadListFromJSON(listData, mSavedNovelData.chOrSeList);

            mSavedNovelData.novelInfoUrl = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_INFO_URL));
            mSavedNovelData.novelFeelUrl = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_FEEL_URL));
            mSavedNovelData.novelReviewUrl = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_REVIEW_URL));

            mNovelUrl = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_URL));
            mNovelSite = SyosetuUtility.SyosetuSite.valueOf(
                    cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_SITE)));

            cursor.close();
            return true;
        }

        return false;
    }

    private void loadDataToView(NovelParser.NovelData data)
    {
        if (mIsFirstLoad)
            loadHistory();

        HtmlUtility.removeLB(data.headSummary);
        HtmlUtility.removeLB(data.headAttention);

        mNovelTitle = data.headTitle;
        mIsShortNovel = data.chOrSeList.isEmpty();
        if (mIsShortNovel)
        {
            mNovelSRL.setVisibility(View.GONE);
            mShortNovelSRL.setVisibility(View.VISIBLE);

            mToolbar.setSubtitle(
                    SyosetuUtility.constructSubtitle(getActivity(), null, data.length, mNovelSource));

            SpannableStringBuilder text
                    = HtmlUtility.processTitle(data.headTitle);
            text.append("\n\n")
                    .append(data.headAuthor).append("\n\n")
                    .append(data.headSummary);
            if (!TextUtils.isEmpty(data.headAttention))
                text.insert(0, "\n\n").insert(0, data.headAttention);

            mShortNovelSTV.setText(text);
        } else
        {
            mNovelSRL.setVisibility(View.VISIBLE);
            mShortNovelSTV.setVisibility(View.GONE);

            mToolbar.setSubtitle(
                    SyosetuUtility.constructSubtitle(getActivity(), null, -1, mNovelSource));

            mSectionListAdapter.addHeader(data.headAttention, data.headTitle,
                    data.headAuthor, data.headAuthorUrl, data.headSummary);

            NovelParser.NovelChOrSeData prevData = null;

            for (int i = 0; i < data.chOrSeList.size(); ++i)
            {
                NovelParser.NovelChOrSeData cosData
                        = data.chOrSeList.get(i);

                if (cosData.type == NovelParser.NT_CHAPTER)
                    mSectionListAdapter.addChapter(cosData.chapterTitle);
                else if (cosData.type == NovelParser.NT_SECTION)
                {
                    String sectionId = HtmlUtility.getUrlRear(cosData.sectionUrl);
                    boolean viewed = mViewedSection
                            .contains(sectionId);

                    if (!mCurrentSection.isEmpty()
                            && sectionId.equals(mCurrentSection))
                        prevData = cosData;

                    mSectionListAdapter.addSection(
                            cosData.sectionTitle,
                            cosData.sectionTime,
                            cosData.sectionUrl, viewed);
                }
            }

            if (prevData != null)
                mSectionListAdapter.setPrevTip(
                        "上次看到：" + prevData.sectionTitle,
                        "点击这里继续阅读",
                        prevData.sectionUrl);
        }

        getActivity().invalidateOptionsMenu();
        mIsFirstLoad = false;
    }


    private AgeCertificationDialogFragment.OnAgeCertListener mAgeCertListener
            = new AgeCertificationDialogFragment.OnAgeCertListener()
    {
        @Override
        public void onGranted(int reqCode, boolean nomoreHint)
        {
            if (reqCode == AC_RC_CACHE)
            {
                loadDataToView(mSavedNovelData);
                cacheContent();
            } else if (reqCode == AC_RC_NO_NEED_CACHE)
            {
                loadDataToView(mSavedNovelData);
            }
        }

        @Override
        public void onDenied(int reqCode, boolean nomoreHint)
        {
            Toast.makeText(getActivity(), "您已选择：非18",
                    Toast.LENGTH_SHORT).show();
            getActivity().finish();
        }
    };


    private SwipeRefreshLayout.OnRefreshListener mRefreshListener = new SwipeRefreshLayout.OnRefreshListener()
    {
        @Override
        public void onRefresh()
        {
            mToolbar.setSubtitle("");
            mNovelSource = SyosetuUtility.SyosetuSource.NETWORK;

            mSavedNovelData = null;
            mSectionListAdapter.clear();
            mShortNovelSTV.setText("");
            getActivity().invalidateOptionsMenu();
            mNovelParser.enter(mNovelUrl);
        }
    };


    private NovelParser.OnPipelineListener<NovelParser.NovelData> mNovelParserListener
            = new HtmlDataPipeline.OnPipelineListener<NovelParser.NovelData>()
    {
        @Override
        public void onPostData(int exitCode, NovelParser.NovelData data)
        {
            if (exitCode == HtmlDataPipeline.CODE_SUCCESS)
            {
                if (mNovelParser.getHtmlData().redirection)
                    mNovelUrl = mNovelParser.getHtmlData().location;
                mNovelSite = SyosetuUtility.getSiteFromNovelUrl(mNovelUrl);

                mSavedNovelData = data;

                if (mNovelSite != SyosetuUtility.SyosetuSite.NORMAL)
                {
                    boolean[] result = new boolean[2];
                    showAgeCertDialog(AC_RC_CACHE, result);
                    if (result[0])
                        getActivity().finish();
                    else
                    {
                        if (result[1])
                        {
                            loadDataToView(mSavedNovelData);
                            cacheContent();
                        }
                    }
                } else
                {
                    loadDataToView(mSavedNovelData);
                    cacheContent();
                }
            } else
            {
                Toast.makeText(
                        getContext(), "Failed.", Toast.LENGTH_SHORT).show();
            }

            mNovelSRL.setRefreshing(false);
            mShortNovelSRL.setRefreshing(false);
        }
    };

    private NovelListAdapter.OnItemSelectListener mSectionSelectListener
            = new NovelListAdapter.OnItemSelectListener()
    {
        @Override
        public void onItemClick(View itemView)
        {
            RecyclerView.ViewHolder holder
                    = mSectionListView.findContainingViewHolder(itemView);
            if (holder == null)
                return;

            int viewType = holder.getItemViewType();
            if (viewType == NovelListAdapter.TYPE_NORMAL
                    || viewType == NovelListAdapter.TYPE_PREV_TIP)
            {
                int position = holder.getAdapterPosition();

                NovelListAdapter.BindData data
                        = mSectionListAdapter.getItem(position);

                getFragmentManager().beginTransaction()
                        .hide(NovelFragment.this)
                        .add(R.id.fl_novel_replace_content,
                                NovelSectionFragment.newInstance(data.sectionUrl, mNovelCode),
                                NovelActivity.FRAGMENT_NOVEL_SECTION)
                        .addToBackStack(null)
                        .commit();
            }
        }

        @Override
        public boolean onItemLongClick(View itemView)
        {
            return false;
        }
    };


    private ImageDownloader.OnDownloadListener mImgDLListener
            = new ImageDownloader.OnDownloadListener()
    {
        @Override
        public void onDownloadComplete(String pageId, ImageDownloader.ImageResult result)
        {
            RecyclerView.ViewHolder viewHolder
                    = mSectionListView.findViewHolderForAdapterPosition(0);
            if (mPageUUID.equals(pageId)
                    && viewHolder != null
                    && viewHolder.getItemViewType() == NovelListAdapter.TYPE_HEAD)
            {
                NovelListAdapter.HeadViewHolder headViewHolder
                        = (NovelListAdapter.HeadViewHolder) viewHolder;
                Spanned spannedText = (Spanned) headViewHolder.summaryTextView.getText();
                ImageSpan[] imgSpans = spannedText.getSpans(0, spannedText.length(), ImageSpan.class);
                for (ImageSpan span : imgSpans)
                {
                    UrlDrawable urlDrawable = (UrlDrawable) span.getDrawable();
                    if (urlDrawable.mState != UrlDrawable.State.STATE_COMPLETED
                            && urlDrawable.mSource.equals(result.imageUrl))
                    {
                        int height = urlDrawable.mDrawable.getBounds().height();

                        urlDrawable.mDrawable = new BitmapDrawable(getResources(), result.bitmap);

                        float factor = (float) height / urlDrawable.mDrawable.getIntrinsicHeight();
                        int width = (int) (urlDrawable.mDrawable.getIntrinsicWidth() * factor);
                        urlDrawable.mDrawable.setBounds(0, 0, width, height);

                        urlDrawable.mState = UrlDrawable.State.STATE_COMPLETED;
                        urlDrawable.setBounds(urlDrawable.mDrawable.getBounds());
                        headViewHolder.summaryTextView.requestLayout();
                        headViewHolder.summaryTextView.invalidate();
                    }
                }
            }
        }
    };


    private class DownloadServiceConnection implements ServiceConnection
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            NovelDownloadService.ControlBridge controller
                    = (NovelDownloadService.ControlBridge) service;
            UApplication.dlServiceController = controller;

            if (controller.startDownload(mNovelUrl, mIsShortNovel))
                Toast.makeText(getActivity(), "准备下载...", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getActivity(), "已经处于下载队列中", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            UApplication.dlServiceController = null;
        }
    }
}
