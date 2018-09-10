package com.ucclkp.syosetureader.author;

import android.content.Context;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;
import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuImageGetter;
import com.ucclkp.syosetureader.SyosetuUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class AuthorParser extends HtmlDataPipeline<Object> {
    private int mPortion;
    private int mCurMaxPageNumber = 1;
    private SyosetuUtility.SyosetuSite mAuthorSite;

    private Context mContext;
    private SyosetuImageGetter mImageGetter;
    private SyosetuUtility.UrlCallback mUrlCallback;


    static class BaseData {
        SpannableStringBuilder data = null;
    }

    static class WorkData {
        List<WorkItem> itemList = new ArrayList<>();
    }

    static class WorkItem {
        String novelUrl = "";
        String novelTitle = "";
        String summary = "";

        String genre = "";
        String type = "";

        String novelInfoUrl = "";
        String novelInfoTitle = "";

        String attention = "";
        String readingTime = "";

        List<String> keywordList = new ArrayList<>();
    }


    AuthorParser(Context context,
                 SyosetuImageGetter imageGetter,
                 SyosetuUtility.UrlCallback callback) {
        mContext = context;
        mImageGetter = imageGetter;
        mUrlCallback = callback;
    }


    public void setPageType(int portion, SyosetuUtility.SyosetuSite site) {
        mPortion = portion;
        mAuthorSite = site;
    }


    void resetPageNumber() {
        mCurMaxPageNumber = 1;
    }

    int getCurMaxPageNumber() {
        return mCurMaxPageNumber;
    }


    @Override
    public Object onStartParse(RetrieveHtmlData htmldata) {
        switch (mPortion) {
            case AuthorPagerAdapter.FRAGMENT_BASE: {
                switch (mAuthorSite) {
                    case NORMAL:
                        return parseBasePage(htmldata.htmlCode);
                    case NOCTURNE:
                        return parseBase18Page(htmldata.htmlCode);
                }

            }

            case AuthorPagerAdapter.FRAGMENT_WORKS:
                return parseWorkPage(htmldata.htmlCode);
        }

        return null;
    }

    private BaseData parseBasePage(String source) {
        BaseData data = new BaseData();

        String mainSource = HtmlUtility.getTagContent(
                source, BaseMainToken, "div", false);
        if (!mainSource.isEmpty()) {
            String profileSource = HtmlUtility.getTagContent(
                    mainSource, BaseProfileToken, "dl", false);
            if (!profileSource.isEmpty()) {
                data.data = new SpannableStringBuilder();

                Matcher matcher = Pattern.compile(
                        BaseProfileTitle + "|" + BaseProfileContent).matcher(profileSource);
                while (matcher.find()) {
                    if (matcher.group().matches("^<\\s*dt[\\s\\S]*?")) {
                        String title = matcher.group(1).trim();
                        SpannableString titleSpaned = new SpannableString(
                                Html.fromHtml(title).toString());
                        titleSpaned.setSpan(
                                new TextAppearanceSpan(mContext, R.style.NovelInfoTitleAppearance),
                                0, titleSpaned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                        data.data.append(titleSpaned).append("\n");
                    } else if (matcher.group().matches("^<\\s*dd[\\s\\S]*?")) {
                        int end = HtmlUtility.getTagEndIndex(
                                profileSource, "dd", matcher.end(), false);
                        if (end != -1) {
                            String content = profileSource.substring(matcher.end(), end);
                            data.data.append(Html.fromHtml(
                                    content, mImageGetter, null)).append("\n\n");
                        }
                    }
                }

                HtmlUtility.removeLB(data.data);
                String scheme = SyosetuUtility.getAuthorHomeUrl(mAuthorSite);
                SyosetuUtility.setUrlMovement(scheme, data.data, mUrlCallback);
            }
        }

        return data;
    }

    private BaseData parseBase18Page(String source) {
        BaseData data = new BaseData();

        String subSource = HtmlUtility.getTagContent(
                source, Base18SubToken, "div", false);
        if (!subSource.isEmpty()) {
            String profileSource = HtmlUtility.getTagContent(
                    subSource, Base18ProfileToken, "ul", false);
            if (!profileSource.isEmpty()) {
                data.data = new SpannableStringBuilder();

                ListParser listParser = new ListParser();
                listParser.set(profileSource, Base18ProfileTitle, "li");
                while (listParser.find()) {
                    String title = listParser.getContent(false);
                    data.data.append(Html.fromHtml(title)).append("\n");
                }

                HtmlUtility.removeLB(data.data);
                String scheme = SyosetuUtility.getAuthorHomeUrl(mAuthorSite);
                SyosetuUtility.setUrlMovement(scheme, data.data, mUrlCallback);
            }
        }

        return data;
    }

    private WorkData parseWorkPage(String source) {
        WorkData data = new WorkData();

        String mainSource = HtmlUtility.getTagContent(
                source, WorkMainToken, "div", false);
        if (mainSource.isEmpty())
            return null;

        String pageInfo = HtmlUtility.getTagContent(
                mainSource, WorkPageNumberToken, "div", false);
        if (!pageInfo.isEmpty()) {
            Pattern pattern = Pattern.compile(WorkPageNumber);
            Matcher matcher = pattern.matcher(pageInfo);
            while (matcher.find()) {
                int curPage = HtmlUtility.intValue(matcher.group(1).trim(), 0);
                if (curPage > mCurMaxPageNumber)
                    mCurMaxPageNumber = curPage;
            }
        }

        String listSource = HtmlUtility.getTagContent(
                mainSource, WorkListToken, "div", false);
        if (!listSource.isEmpty()) {
            ListParser listParser = new ListParser();
            listParser.set(listSource, "ul");
            while (listParser.find()) {
                String listItemSource = listParser.getContent(false);
                if (!listItemSource.isEmpty()) {
                    WorkItem workItem = new WorkItem();
                    parseWorkListItem(listItemSource, workItem);
                    data.itemList.add(workItem);
                }
            }
        }

        return data;
    }

    private void parseWorkListItem(String source, WorkItem item) {
        int lastIndex = 0;
        int position[] = new int[2];

        //title
        String title = HtmlUtility.getTagContent(
                source, WorkTitleToken, "li", false);
        if (!title.isEmpty()) {
            Matcher matcher = Pattern.compile(UrlToken).matcher(title);
            if (matcher.find()) {
                item.novelUrl = matcher.group(1).trim();
                item.novelTitle = Html.fromHtml(
                        matcher.group(2).trim()).toString();
                lastIndex = matcher.end();
            }
        }

        //summary
        String summary = HtmlUtility.getTagContent(
                source, lastIndex, WorkSummaryToken, "li", false, position);
        if (!summary.isEmpty()) {
            item.summary = Html.fromHtml(
                    summary.trim()).toString();
            lastIndex = position[1];
        }

        //extra
        String extra = HtmlUtility.getTagContent(
                source, lastIndex, WorkExtraToken, "li", false, position);
        if (!extra.isEmpty()) {
            int extraLastIndex = 0;
            int extraPosition[] = new int[2];

            Matcher matcher = Pattern.compile(WorkGenreToken).matcher(extra);
            if (matcher.find()) {
                item.genre = Html.fromHtml(
                        matcher.group(1).trim()).toString();
                extraLastIndex = matcher.end();
            }

            matcher = Pattern.compile(WorkTypeToken).matcher(extra);
            matcher.region(extraLastIndex, extra.length());
            if (matcher.find()) {
                item.type = Html.fromHtml(
                        matcher.group(1).trim()).toString();
                extraLastIndex = matcher.end();
            }

            String info = HtmlUtility.getTagContent(
                    extra, extraLastIndex, WorkInfoToken, "p", true, extraPosition);
            if (!info.isEmpty()) {
                item.type += extra.substring(
                        extraLastIndex, extraPosition[0]).trim();

                matcher = Pattern.compile(UrlToken).matcher(info);
                if (matcher.find()) {
                    item.novelInfoUrl = matcher.group(1).trim();
                    item.novelInfoTitle = matcher.group(2).trim();
                }
            }

            lastIndex = position[1];
        }

        //keyword
        String keyword = HtmlUtility.getTagContent(
                source, lastIndex, WorkKeywordToken, "li", false, position);
        if (!keyword.isEmpty()) {
            String[] keywordArray = keyword.split(SyosetuUtility.KeywordSplit);
            for (int i = 0; i < keywordArray.length; ++i) {
                if (!TextUtils.isEmpty(keywordArray[i]))
                    item.keywordList.add(keywordArray[i]);
            }

            lastIndex = position[1];
        }

        //attention
        String attention = HtmlUtility.getTagContent(
                source, lastIndex, WorkKeywordToken, "li", false, position);
        if (!attention.isEmpty()) {
            Matcher matcher = Pattern.compile(WorkAttentionToken).matcher(attention);
            while (matcher.find())
                item.attention += matcher.group(1).trim() + "  ";
            item.attention = item.attention.trim();
            lastIndex = position[1];
        }

        //reading time
        String readingTime = HtmlUtility.getTagContent(
                source, lastIndex, WorkReadingTimeToken, "li", false, position);
        if (!readingTime.isEmpty()) {
            item.readingTime = readingTime;
            lastIndex = position[1];
        }
    }


    private final static String BaseMainToken
            = "<div\\s+id=\"main\"\\s*>";
    private final static String BaseProfileToken
            = "<dl\\s+class=\"profile\"\\s*>";
    private final static String BaseProfileTitle
            = "<dt[\\s\\S]*?>(.*?)</dt>";
    private final static String BaseProfileContent
            = "<dd[\\s\\S]*?>";

    private final static String Base18SubToken
            = "<div\\s+id=\"sub\"\\s*>";
    private final static String Base18ProfileToken
            = "<ul\\s+id=\"profile\"\\s*>";
    private final static String Base18ProfileTitle
            = "<li[\\s\\S]*?>";

    private final static String WorkMainToken = BaseMainToken;
    private final static String WorkPageNumberToken
            = "<div class=\"pager_idou\">";
    private final static String WorkPageNumber
            = "<a\\s+href=[\\s\\S]*?title=\"page\\s+\\d*?\"\\s*>(\\d*?)</a>";
    private final static String WorkListToken
            = "<div\\s+id=\"novellist\">";

    private final static String WorkTitleToken
            = "<li\\s+class=\"title\"\\s*>";
    private final static String WorkSummaryToken
            = "<li\\s+class=\"ex\"\\s*>";
    private final static String WorkExtraToken
            = "<li\\s+class=\"date1\"\\s*>";
    private final static String WorkGenreToken
            = "<span\\s+class=\"genre\"\\s*>(.*?)</span>";
    private final static String WorkTypeToken
            = "<span\\s+class=\"type\">(.*?)</span>";
    private final static String WorkInfoToken
            = "<p\\s+class=\"info\">";
    private final static String WorkKeywordToken
            = "<li\\s+class=\"keyword\"\\s*>";
    private final static String WorkReadingTimeToken
            = "<li\\s+class=\"date\">";
    private final static String WorkAttentionToken
            = "<span>(.*?)</span>";

    private final static String UrlToken
            = "<a[\\s\\S]*?href=\"(.*?)\"[\\s\\S]*?>(.*?)</a>";
}
