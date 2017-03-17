package com.ucclkp.syosetureader.novel;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;
import com.ucclkp.syosetureader.ImageDownloader;
import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuBooks;
import com.ucclkp.syosetureader.SyosetuImageGetter;
import com.ucclkp.syosetureader.SyosetuUtility;
import com.ucclkp.syosetureader.UApplication;
import com.ucclkp.syosetureader.UrlDrawable;
import com.ucclkp.syosetureader.statictextview.StaticTextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;


public class NovelSectionFragment extends Fragment
{
    private String mNumber;
    private String mPrevUrl;
    private String mNextUrl;
    private String mTitle;
    private String mNovelCode;
    private String mPageUUID;
    private SyosetuUtility.SyosetuSite mNovelSite;
    private SyosetuUtility.SyosetuSource mNovelSource;

    private int mTextSize;
    private String mLineSpacingAdd, mLineSpacingMult;
    private String mBackgroundId;

    private String mNovelSectionUrl;
    private NovelSectionParser mSectionParser;
    private OnNovelSectionRecord mRecorder;
    private SyosetuImageGetter mImageGetter;

    private Toolbar mToolbar;
    private AppBarLayout mAppBarLayout;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private StaticTextView mSectionContentTV;

    private NovelSectionParser.SectionData mSavedSectionData;


    private static final String SAVED_TITLE = "saved_title";
    private static final String SAVED_SUBTITLE = "saved_subtitle";
    private static final String SAVED_PREV_URL = "saved_prev_url";
    private static final String SAVED_NEXT_URL = "saved_next_url";
    private static final String SAVED_CONTENT = "saved_content";
    private static final String SAVED_CONTENT_LENGTH = "saved_content_length";
    private static final String SAVED_TEXT_OFFSET = "saved_text_offset";
    private static final String SAVED_OFFSET = "saved_offset";
    private static final String SAVED_NOVEL_SECTION_URL = "saved_novel_section_url";
    private static final String SAVED_NOVEL_SOURCE = "saved_novel_source";
    private static final String SAVED_NOVEL_SITE = "saved_novel_site";

    private static final String ARG_NOVEL_CODE = "arg_novel_code";
    private static final String ARG_NOVEL_SECTION_URL = "arg_novel_section_url";


    public NovelSectionFragment()
    {
        mNumber = "";
        mPrevUrl = "";
        mNextUrl = "";
        mTitle = "";
        mPageUUID = UUID.randomUUID().toString();
        mSavedSectionData = null;
    }


    public static NovelSectionFragment newInstance(String chapterUrl, String ncode)
    {
        NovelSectionFragment fragment = new NovelSectionFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NOVEL_CODE, ncode);
        args.putString(ARG_NOVEL_SECTION_URL, chapterUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        UApplication.imageDownloader.addOnDownloadListener(mImgDLListener);
        mRecorder = (OnNovelSectionRecord) context;
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        if (mSectionParser != null)
            mSectionParser.cancel();
        UApplication.imageDownloader.removeOnDownloadListener(mImgDLListener);
        mRecorder = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (getArguments() != null)
            mNovelCode = getArguments().getString(ARG_NOVEL_CODE);

        if (savedInstanceState != null
                && savedInstanceState.getString(SAVED_NOVEL_SECTION_URL) != null)
            mNovelSectionUrl = savedInstanceState.getString(SAVED_NOVEL_SECTION_URL);
        else
        {
            if (getArguments() != null)
                mNovelSectionUrl = getArguments().getString(ARG_NOVEL_SECTION_URL);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View parent = inflater.inflate(R.layout.fragment_novel_section, container, false);

        mSwipeRefreshLayout = (SwipeRefreshLayout) parent.findViewById(R.id.srl_fragment_novel_section);
        mSwipeRefreshLayout.setOnRefreshListener(mRefreshListener);
        mSwipeRefreshLayout.setColorSchemeResources(
                R.color.color_blue,
                R.color.color_red,
                R.color.color_green,
                R.color.color_yellow);
        int currentNightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES)
            mSwipeRefreshLayout.setProgressBackgroundColorSchemeResource(R.color.color_swipe_background);

        mToolbar = (Toolbar) getActivity().findViewById(R.id.tb_novel_activity);
        AppBarLayout.LayoutParams lp
                = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        lp.setScrollFlags(0);
        mToolbar.setLayoutParams(lp);

        mAppBarLayout = (AppBarLayout) getActivity().findViewById(R.id.abl_novel_activity);
        mAppBarLayout.setExpanded(true, false);

        SharedPreferences preferences = getActivity().getSharedPreferences(
                UApplication.PREF_FORMAT, Context.MODE_PRIVATE);
        mTextSize = preferences.getInt(
                UApplication.FONT_SIZE, FormatDialogFragment.DEFAULT_FONT_SIZE_DIP);
        mLineSpacingAdd = preferences.getString(
                UApplication.LINE_SPACING_ADD, FormatDialogFragment.DEFAULT_LSA_DIP);
        mLineSpacingMult = preferences.getString(
                UApplication.LINE_SPACING_MULT, FormatDialogFragment.DEFAULT_LSM);
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES)
            mBackgroundId = preferences.getString(
                    UApplication.BACKGROUND_NIGHT_ID, FormatDialogFragment.BACKGROUND_ID_NIGHT_DEFAULT);
        else
            mBackgroundId = preferences.getString(
                    UApplication.BACKGROUND_ID, FormatDialogFragment.BACKGROUND_ID_DEFAULT);

        mSectionContentTV = (StaticTextView) parent.findViewById(R.id.tv_fragment_novel_section_content);
        mSectionContentTV.setTextSize(mTextSize);
        mSectionContentTV.setLineSpacing(Float.valueOf(mLineSpacingMult), Float.valueOf(mLineSpacingAdd));
        mSectionContentTV.setBackground(FormatDialogFragment.getBackgroundById(getContext(), mBackgroundId));

        return parent;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        boolean haveState = (savedInstanceState != null
                && savedInstanceState.getString(SAVED_TITLE) != null);

        mImageGetter = new SyosetuImageGetter(getActivity(), mPageUUID);

        Log.d("RetainDBG", "NovelSectionFragment.onViewCreated(" + haveState + ") invoked.");

        mToolbar.setSubtitle("");

        mSectionParser = new NovelSectionParser(mImageGetter);
        mSectionParser.setPipelineListener(mNovelParserListener);

        if (haveState)
        {
            int textOffset = savedInstanceState.getInt(SAVED_TEXT_OFFSET);
            int offset = savedInstanceState.getInt(SAVED_OFFSET);
            int length = savedInstanceState.getInt(SAVED_CONTENT_LENGTH);
            String title = savedInstanceState.getString(SAVED_TITLE);
            String number = savedInstanceState.getString(SAVED_SUBTITLE);
            String prevUrl = savedInstanceState.getString(SAVED_PREV_URL);
            String nextUrl = savedInstanceState.getString(SAVED_NEXT_URL);
            String content = savedInstanceState.getString(SAVED_CONTENT);
            SpannableStringBuilder contentSpanned =
                    new SpannableStringBuilder(Html.fromHtml(content, mImageGetter, null));

            mNovelSource = SyosetuUtility.SyosetuSource
                    .valueOf(savedInstanceState.getString(SAVED_NOVEL_SOURCE));
            mNovelSite = SyosetuUtility.SyosetuSite
                    .valueOf(savedInstanceState.getString(SAVED_NOVEL_SITE));

            getActivity().setTitle(title);
            //mToolbar.setTitle(title);
            mToolbar.setSubtitle(SyosetuUtility.constructSubtitle(
                    getActivity(), number, length, mNovelSource));

            mTitle = title;
            mPrevUrl = prevUrl;
            mNextUrl = nextUrl;

            SpannableStringBuilder text = HtmlUtility.processTitle(title);
            text.append("\n\n").append(contentSpanned);

            mSectionContentTV.setText(text);
            mSectionContentTV.setTextOffsetAtViewTop(textOffset, offset);
            mNumber = HtmlUtility.getUrlRear(mNovelSectionUrl);

            mSavedSectionData = new NovelSectionParser.SectionData();
            mSavedSectionData.title = title;
            mSavedSectionData.number = number;
            mSavedSectionData.prevUrl = prevUrl;
            mSavedSectionData.nextUrl = nextUrl;
            mSavedSectionData.sectionContent = contentSpanned;
            mSavedSectionData.length = length;
        }
        else
        {
            if (restoreFromDownload())
            {
                mNovelSource = SyosetuUtility.SyosetuSource.DOWNLOAD;
                loadDataToView(mSavedSectionData);
            }
            else if (restoreContent())
            {
                mNovelSource = SyosetuUtility.SyosetuSource.CACHE;
                loadDataToView(mSavedSectionData);
            }
            else
            {
                mToolbar.setTitle("小説章节");
                mSectionParser.enter(mNovelSectionUrl);
                mNovelSource = SyosetuUtility.SyosetuSource.NETWORK;

                if (mSectionParser.isInPipeline())
                    mSwipeRefreshLayout.setRefreshing(true);

                resetViews();
            }
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();

        Log.d("RetainDBG", "NovelSectionFragment.onStop() invoked.");

        if (mRecorder != null && !mNumber.isEmpty())
            mRecorder.requestRecord(
                    mNovelSectionUrl, mNumber,
                    mSectionContentTV.getTextOffsetAtViewTop());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.menu_novel_section_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        MenuItem prevMenuItem = menu.findItem(
                R.id.menu_novel_section_fragment_action_prev);
        prevMenuItem.setVisible(!mPrevUrl.isEmpty());

        MenuItem nextMenuItem = menu.findItem(
                R.id.menu_novel_section_fragment_action_next);
        nextMenuItem.setVisible(!mNextUrl.isEmpty());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_novel_section_fragment_action_prev:
            {
                navigateToPrev();
                return true;
            }

            case R.id.menu_novel_section_fragment_action_next:
            {
                navigateToNext();
                return true;
            }

            case R.id.menu_novel_section_fragment_action_format:
            {
                FormatDialogFragment formatBottomSheet
                        = new FormatDialogFragment();

                Bundle bundle = new Bundle();
                bundle.putInt(FormatDialogFragment.ARG_REQUEST_CODE, NovelActivity.FORMAT_RC_SECTION);
                bundle.putInt(FormatDialogFragment.ARG_CURRENT_FONT_SIZE, mTextSize);
                bundle.putString(FormatDialogFragment.ARG_CURRENT_LSA, mLineSpacingAdd);
                bundle.putString(FormatDialogFragment.ARG_CURRENT_LSM, mLineSpacingMult);
                bundle.putString(FormatDialogFragment.ARG_CURRENT_BACKGROUND_ID, mBackgroundId);

                formatBottomSheet.setArguments(bundle);
                formatBottomSheet.show(getFragmentManager(), "fragment_format_bottom_sheet");
                return true;
            }

            case R.id.menu_novel_section_fragment_action_copy:
            {
                ((ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE))
                        .setPrimaryClip(
                                ClipData.newPlainText(null, mNovelCode));
                Toast.makeText(getActivity(), "小说编号已复制到剪贴板", Toast.LENGTH_SHORT).show();
                return true;
            }

            case R.id.menu_novel_section_fragment_action_copy_url:
            {
                ((ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE))
                        .setPrimaryClip(
                                ClipData.newPlainText(null, mNovelSectionUrl));
                Toast.makeText(getActivity(), "小说Url已复制到剪贴板", Toast.LENGTH_SHORT).show();
                return true;
            }

            case R.id.menu_novel_section_fragment_action_open_browser:
            {
                UApplication.chromeCustomTabsManager.startChromeTab(
                        getContext(), mNovelSectionUrl);
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);

        Log.d("RetainDBG", "NovelSectionFragment.onSaveInstanceState() invoked.");

        if (mSavedSectionData != null)
        {
            int textOffset = mSectionContentTV.getTextOffsetAtViewTop();
            int offset = mSectionContentTV.getOffsetFromCurTextOffset();

            outState.putString(SAVED_TITLE, mSavedSectionData.title);
            outState.putString(SAVED_SUBTITLE, mSavedSectionData.number);
            outState.putString(SAVED_PREV_URL, mSavedSectionData.prevUrl);
            outState.putString(SAVED_NEXT_URL, mSavedSectionData.nextUrl);
            outState.putString(SAVED_CONTENT, Html.toHtml(mSavedSectionData.sectionContent));
            outState.putInt(SAVED_CONTENT_LENGTH, mSavedSectionData.length);
            outState.putInt(SAVED_TEXT_OFFSET, textOffset);
            outState.putInt(SAVED_OFFSET, offset);

            outState.putString(SAVED_NOVEL_SECTION_URL, mNovelSectionUrl);
            outState.putString(SAVED_NOVEL_SOURCE, mNovelSource.name());
            outState.putString(SAVED_NOVEL_SITE, mNovelSite.name());
        }
    }


    private void navigateToPrev()
    {
        if (!mPrevUrl.isEmpty())
        {
            mNumber = "";
            mNovelSectionUrl = mPrevUrl;

            if (restoreFromDownload())
            {
                mNovelSource = SyosetuUtility.SyosetuSource.DOWNLOAD;
                loadDataToView(mSavedSectionData);
            }
            else if (restoreContent())
            {
                mNovelSource = SyosetuUtility.SyosetuSource.CACHE;
                loadDataToView(mSavedSectionData);
            }
            else
            {
                resetViews();
                mToolbar.setTitle("小説章节");
                mToolbar.setSubtitle("");
                mNovelSource = SyosetuUtility.SyosetuSource.NETWORK;

                mSwipeRefreshLayout.setRefreshing(true);
                mSectionParser.enter(mNovelSectionUrl);
            }
        }
    }

    private void navigateToNext()
    {
        if (!mNextUrl.isEmpty())
        {
            mNumber = "";
            mNovelSectionUrl = mNextUrl;

            if (restoreFromDownload())
            {
                mNovelSource = SyosetuUtility.SyosetuSource.DOWNLOAD;
                loadDataToView(mSavedSectionData);
            }
            else if (restoreContent())
            {
                mNovelSource = SyosetuUtility.SyosetuSource.CACHE;
                loadDataToView(mSavedSectionData);
            }
            else
            {
                resetViews();
                mToolbar.setTitle("小説章节");
                mToolbar.setSubtitle("");
                mNovelSource = SyosetuUtility.SyosetuSource.NETWORK;

                mSwipeRefreshLayout.setRefreshing(true);
                mSectionParser.enter(mNovelSectionUrl);
            }
        }
    }

    public void notifyFontChanged(String fontName)
    {
        Typeface typeface = null;
        if (fontName.equals(getContext().getString(R.string.font_default)))
            typeface = null;

        mSectionContentTV.setTypeface(typeface);
    }

    public void notifyFontSizeChanged(int size)
    {
        mTextSize = size;
        mSectionContentTV.setTextSize(size);
    }

    public void notifyLineSpacingChanged(String mult, String add)
    {
        mLineSpacingAdd = add;
        mLineSpacingMult = mult;
        mSectionContentTV.setLineSpacing(Float.valueOf(mult), Float.valueOf(add));
    }

    public void notifyBackgroundChanged(Drawable drawable, String name)
    {
        mBackgroundId = name;
        mSectionContentTV.setBackground(drawable);
    }

    private void resetViews()
    {
        mPrevUrl = "";
        mNextUrl = "";
        mTitle = "";
        mSavedSectionData = null;

        mSectionContentTV.setText("");

        getActivity().invalidateOptionsMenu();
    }


    private void cacheContent()
    {
        if (mSavedSectionData == null)
            return;

        JSONObject jsonObject = new JSONObject();

        try
        {
            jsonObject.put("cache_length", mSavedSectionData.length);
            jsonObject.put("cache_title", mSavedSectionData.title);
            jsonObject.put("cache_number", mSavedSectionData.number);
            jsonObject.put("cache_prevUrl", mSavedSectionData.prevUrl);
            jsonObject.put("cache_nextUrl", mSavedSectionData.nextUrl);
            jsonObject.put("cache_content", Html.toHtml(mSavedSectionData.sectionContent));
            jsonObject.put("cache_url", mNovelSectionUrl);
            jsonObject.put("cache_site", mNovelSite.name());

            ((UApplication) getActivity().getApplication())
                    .getCacheManager().putText(
                    SyosetuUtility.constructSectionId(mNovelCode, mNovelSectionUrl),
                    jsonObject.toString());
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
    }

    private boolean restoreContent()
    {
        String cached = ((UApplication) getActivity().getApplication())
                .getCacheManager().getText(
                        SyosetuUtility.constructSectionId(
                                mNovelCode, mNovelSectionUrl));
        if (cached != null)
        {
            try
            {
                JSONObject jsonObject = new JSONObject(cached);
                mSavedSectionData = new NovelSectionParser.SectionData();

                mSavedSectionData.length = jsonObject.getInt("cache_length");
                mSavedSectionData.title = jsonObject.getString("cache_title");
                mSavedSectionData.number = jsonObject.getString("cache_number");
                mSavedSectionData.prevUrl = jsonObject.getString("cache_prevUrl");
                mSavedSectionData.nextUrl = jsonObject.getString("cache_nextUrl");
                mSavedSectionData.sectionContent = new SpannableStringBuilder(
                        Html.fromHtml(jsonObject.getString("cache_content"), mImageGetter, null));

                mNovelSectionUrl = jsonObject.getString("cache_url");
                mNovelSite = SyosetuUtility.SyosetuSite.valueOf(jsonObject.getString("cache_site"));

                return true;
            }
            catch (JSONException e)
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
        Cursor cursor = books.getSection(
                mNovelCode, SyosetuUtility.constructSectionId(
                        mNovelCode, mNovelSectionUrl));
        if (cursor != null)
        {
            mSavedSectionData = new NovelSectionParser.SectionData();

            mSavedSectionData.length = cursor.getInt(cursor.getColumnIndex(SyosetuBooks.COLUMN_LENGTH));
            mSavedSectionData.title = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_NAME));
            mSavedSectionData.number = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_NUMBER));
            mSavedSectionData.prevUrl = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_PREV_URL));
            mSavedSectionData.nextUrl = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_NEXT_URL));
            mSavedSectionData.sectionContent = new SpannableStringBuilder(
                    Html.fromHtml(cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_CONTENT)), mImageGetter, null));

            mNovelSectionUrl = cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_URL));
            mNovelSite = SyosetuUtility.SyosetuSite.valueOf(
                    cursor.getString(cursor.getColumnIndex(SyosetuBooks.COLUMN_SITE)));

            cursor.close();
            return true;
        }

        return false;
    }

    private void loadDataToView(NovelSectionParser.SectionData data)
    {
        mToolbar.setTitle(data.title);
        mToolbar.setSubtitle(
                SyosetuUtility.constructSubtitle(
                        getActivity(), data.number, data.length, mNovelSource));

        mPrevUrl = data.prevUrl;
        mNextUrl = data.nextUrl;
        mTitle = data.title;

        SpannableStringBuilder content
                = HtmlUtility.processTitle(data.title);
        content.append("\n\n").append(data.sectionContent);

        mSectionContentTV.setText(content);

        getActivity().invalidateOptionsMenu();

        mNumber = HtmlUtility.getUrlRear(mNovelSectionUrl);

        if (mRecorder != null)
            mRecorder.requestRecord(
                    mNovelSectionUrl, mNumber,
                    mSectionContentTV.getTextOffsetAtViewTop());
    }


    private SwipeRefreshLayout.OnRefreshListener mRefreshListener
            = new SwipeRefreshLayout.OnRefreshListener()
    {
        @Override
        public void onRefresh()
        {
            resetViews();
            mToolbar.setSubtitle("");
            mNovelSource = SyosetuUtility.SyosetuSource.NETWORK;
            mSectionParser.enter(mNovelSectionUrl);
        }
    };


    private NovelSectionParser.OnPipelineListener<NovelSectionParser.SectionData> mNovelParserListener
            = new NovelSectionParser.OnPipelineListener<NovelSectionParser.SectionData>()
    {
        @Override
        public void onPostData(int exitCode, NovelSectionParser.SectionData data)
        {
            if (exitCode == HtmlDataPipeline.CODE_SUCCESS)
            {
                if (mSectionParser.getHtmlData().redirection)
                    mNovelSectionUrl = mSectionParser.getHtmlData().location;
                mNovelSite = SyosetuUtility.getSiteFromNovelUrl(mNovelSectionUrl);

                mSavedSectionData = data;
                loadDataToView(data);
                cacheContent();
            }
            else
            {
                mTitle = "";
                mPrevUrl = "";
                mNextUrl = "";

                Toast.makeText(
                        getContext(), "Failed.", Toast.LENGTH_SHORT).show();
            }

            mSwipeRefreshLayout.setRefreshing(false);
        }
    };


    private ImageDownloader.OnDownloadListener mImgDLListener
            = new ImageDownloader.OnDownloadListener()
    {
        @Override
        public void onDownloadComplete(String pageId, ImageDownloader.ImageResult result)
        {
            if (mPageUUID.equals(pageId))
            {
                Spanned spannedText = (Spanned) mSectionContentTV.getText();
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
                        mSectionContentTV.requestLayout();
                        mSectionContentTV.invalidate();
                    }
                }
            }
        }
    };


    interface OnNovelSectionRecord
    {
        void requestRecord(String sectionUrl, String number, int offset);
    }
}
