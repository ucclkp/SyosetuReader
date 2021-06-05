package com.ucclkp.syosetureader.author;

import android.content.Context;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;
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
        String last_update = "";

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
            case AuthorPagerAdapter.FRAGMENT_PROFILE: {
                switch (mAuthorSite) {
                    case NORMAL:
                        return parseBasePage(htmldata.htmlCode);
                    case NOCTURNE:
                        return parseBase18Page(htmldata.htmlCode);
                }

            }

            case AuthorPagerAdapter.FRAGMENT_WORKS: {
                switch (mAuthorSite) {
                    case NORMAL:
                        return parseWorkPage(htmldata.htmlCode);
                    case NOCTURNE:
                        return parseWork18Page(htmldata.htmlCode);
                }
            }
        }

        return null;
    }

    private BaseData parseBasePage(String source) {
        BaseData data = new BaseData();

        String ctr_src = HtmlUtility.getTagContent(
                source, "<div\\s+class=\"l-container\"\\s*>", "div", false);
        if (ctr_src.isEmpty()) {
            return data;
        }

        String main_src = HtmlUtility.getTagContent(
                ctr_src, "<div\\s+class=\"l-main\"\\s*>", "div", false);
        if (main_src.isEmpty()) {
            return data;
        }

        String profile_src = HtmlUtility.getTagContent(
                main_src, "<div\\s+class=\"c-panel__body\"\\s*>", "div", false);
        if (profile_src.isEmpty()) {
            return data;
        }

        profile_src = HtmlUtility.getTagContent(
                profile_src, "<div\\s+class=\"c-panel__item\"\\s*>", "div", false);
        if (profile_src.isEmpty()) {
            return data;
        }

        data.data = new SpannableStringBuilder();
        data.data.append(Html.fromHtml(
                profile_src, mImageGetter, null));

        HtmlUtility.removeLB(data.data);
        String scheme = SyosetuUtility.getAuthorHomeUrl(mAuthorSite);
        SyosetuUtility.setUrlMovement(scheme, data.data, mUrlCallback);

        return data;
    }

    private BaseData parseBase18Page(String source) {
        BaseData data = new BaseData();

        String ctr_src = HtmlUtility.getTagContent(
                source, "<div\\s+id=\"container\"\\s*>", "div", false);
        if (ctr_src.isEmpty()) {
            return data;
        }

        String contents_src = HtmlUtility.getTagContent(
                ctr_src, "<div\\s+id=\"contents\"\\s*>", "div", false);
        if (contents_src.isEmpty()) {
            return data;
        }

        String main_src = HtmlUtility.getTagContent(
                contents_src, "<div\\s+id=\"main\"\\s*>", "div", false);
        if (main_src.isEmpty()) {
            return data;
        }

        String profile_src = HtmlUtility.getTagContent(
                main_src, "<div\\s+class=\"box_profile\"\\s*>", "div", false);
        if (profile_src.isEmpty()) {
            return data;
        }

        profile_src = HtmlUtility.getTagContent(
                profile_src, "<table\\s+class=\"profile\"\\s*>", "table", false);
        if (profile_src.isEmpty()) {
            return data;
        }

        data.data = new SpannableStringBuilder();

        ListParser listParser = new ListParser();
        listParser.set(profile_src, "<tr[\\s\\S]*?>", "tr");
        while (listParser.find()) {
            String title_src = listParser.getContent(false);
            String title = HtmlUtility.getTagContent(
                    title_src, "<th\\s+class=\"profile_title\"\\s*>", "th", false);
            String item = HtmlUtility.getTagContent(
                    title_src, "<td\\s+class=\"profile_item\"\\s*>", "td", false);
            data.data.append(Html.fromHtml(title)).append(" | ");
            data.data.append(Html.fromHtml(item)).append("\n");
        }

        HtmlUtility.removeLB(data.data);
        String scheme = SyosetuUtility.getAuthorHomeUrl(mAuthorSite);
        SyosetuUtility.setUrlMovement(scheme, data.data, mUrlCallback);

        return data;
    }

    private WorkData parseWorkPage(String source) {
        WorkData data = new WorkData();

        String ctr_src = HtmlUtility.getTagContent(
                source, "<div\\s+class=\"l-container\"\\s*>", "div", false);
        if (ctr_src.isEmpty()) {
            return data;
        }

        String main_src = HtmlUtility.getTagContent(
                ctr_src, "<div\\s+class=\"l-main\"\\s*>", "div", false);
        if (main_src.isEmpty()) {
            return data;
        }

        String pageInfo = HtmlUtility.getTagContent(
                main_src, "<div\\s+class=\"c-pager\"\\s*>", "div", false);
        if (!pageInfo.isEmpty()) {
            Pattern pattern = Pattern.compile(WorkPageNumber);
            Matcher matcher = pattern.matcher(pageInfo);
            while (matcher.find()) {
                String max_page = matcher.group(1);
                if (max_page != null) {
                    int curPage = HtmlUtility.intValue(max_page.trim(), 0);
                    if (curPage > mCurMaxPageNumber)
                        mCurMaxPageNumber = curPage;
                }
            }
        }

        String listSource = HtmlUtility.getTagContent(
                main_src, "<div\\s+class=\"c-novel-list\"\\s*>", "div", false);
        if (!listSource.isEmpty()) {
            ListParser listParser = new ListParser();
            listParser.set(listSource, "<div\\s+class=\"c-novel-list__item\"\\s*>","div");
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

    private WorkData parseWork18Page(String source) {
        WorkData data = new WorkData();

        String mainSource = HtmlUtility.getTagContent(
                source, WorkMainToken, "div", false);
        if (mainSource.isEmpty())
            return null;

        String pageInfo = HtmlUtility.getTagContent(
                mainSource, WorkPageNumberToken, "div", false);
        if (!pageInfo.isEmpty()) {
            Pattern pattern = Pattern.compile(Work18PageNumber);
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
                    parseWork18ListItem(listItemSource, workItem);
                    data.itemList.add(workItem);
                }
            }
        }

        return data;
    }

    private void parseWorkListItem(String source, WorkItem item) {
        //title
        String title = HtmlUtility.getTagContent(
                source, "<div\\s+class=\"c-novel-list__head\"\\s*>", "div", false);
        if (!title.isEmpty()) {
            Matcher matcher = Pattern.compile(UrlToken).matcher(title);
            if (matcher.find()) {
                item.novelUrl = matcher.group(1).trim();
                item.novelTitle = Html.fromHtml(
                        matcher.group(2).trim()).toString();
            }
        }

        //summary
        String summary = HtmlUtility.getTagContent(
                source, "<div\\s+class=\"c-novel-list__summary\"\\s*>", "div", false);
        if (!summary.isEmpty()) {
            summary = HtmlUtility.getTagContent(
                    source, "<div\\s+class=\"c-readmore__mask[\\s\\S]*?>", "div", false);
            if (!summary.isEmpty()) {
                item.summary = Html.fromHtml(
                        summary.trim()).toString();
            }
        }

        //status
        String status = HtmlUtility.getTagContent(
                source, "<div\\s+class=\"c-novel-list__status\"\\s*>", "div", false);
        if (!status.isEmpty()) {
            int[] position = new int[2];
            String extra = HtmlUtility.getTagContent(
                    status, 0, "<span\\s+class=\"c-label[\\s\\S]*?>", "span", false, position);
            item.type = Html.fromHtml(extra.trim()).toString();

            String number = HtmlUtility.getTagContent(
                    status, position[1], "<span\\s+class=\"c-novel-list__number\"\\s*>", "span", false, position);
            if (!number.isEmpty()) {
                item.type += "：";
                item.type += Html.fromHtml(number.trim()).toString();
            }
        }

        //detail
        String detail = HtmlUtility.getTagContent(
                source, "<div\\s+class=\"c-novel-list__detail\"\\s*>", "div", false);
        if (!detail.isEmpty()) {
            Matcher matcher = Pattern.compile(UrlToken).matcher(detail);
            if (matcher.find()) {
                item.novelInfoUrl = matcher.group(1).trim();
                item.novelInfoTitle = matcher.group(2).trim();
            }

            String genre = HtmlUtility.getTagContent(
                    detail, "<span\\s+class=\"c-novel-list__genre\"\\s*>", "span", false);
            item.genre = Html.fromHtml(genre.trim()).toString();

            String keikoku = HtmlUtility.getTagContent(
                    detail, "<span\\s+class=\"c-novel-list__keikoku\"\\s*>", "span", false);
            if (!keikoku.isEmpty()) {
                ListParser listParser = new ListParser();
                listParser.set(keikoku, "span");
                while (listParser.find()) {
                    String text = listParser.getContent(false);
                    if (!text.isEmpty()) {
                        item.keywordList.add(text);
                    }
                }
            }

            String last_update = HtmlUtility.getTagContent(
                    detail, "<div\\s+class=\"c-novel-list__lastup\"\\s*>", "div", false);
            item.last_update = Html.fromHtml(last_update.trim()).toString();
        }

        //additional
        String additional = HtmlUtility.getTagContent(
                source, "<div\\s+class=\"c-novel-list__additional\"\\s*>", "div", false);
        if (!additional.isEmpty()) {
            //keyword
            String keyword = HtmlUtility.getTagContent(
                    additional, "<span\\s+class=\"c-novel-list__keyword\"\\s*>", "span", false);
            if (!keyword.isEmpty()) {
                String[] keywordArray = keyword.split(SyosetuUtility.KeywordSplit);
                for (String s : keywordArray) {
                    if (!TextUtils.isEmpty(s))
                        item.keywordList.add(s);
                }
            }

            //reading time
            String readingTime = HtmlUtility.getTagContent(
                    additional, "<span\\s*>", "span", false);
            if (!readingTime.isEmpty()) {
                item.readingTime = readingTime;
            }
        }
    }

    private void parseWork18ListItem(String source, WorkItem item) {
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

    private final static String WorkMainToken = BaseMainToken;
    private final static String WorkPageNumberToken
            = "<div class=\"pager_idou\">";
    private final static String WorkPageNumber
            = "<a\\s+href=[\\s\\S]*?title=\"\\d*?ページ\"\\s*>(\\d*?)</a>";
    private final static String Work18PageNumber
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
