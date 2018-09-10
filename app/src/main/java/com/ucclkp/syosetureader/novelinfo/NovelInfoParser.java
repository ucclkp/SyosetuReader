package com.ucclkp.syosetureader.novelinfo;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;
import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuUtility;
import com.ucclkp.syosetureader.recipientchip.RecipientChipSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class NovelInfoParser extends HtmlDataPipeline<NovelInfoParser.NovelInfoData>
{
    private Context mContext;

    private final static int TYPE_NORMAL = 0;
    private final static int TYPE_AUTHOR = 1;
    private final static int TYPE_GENRE = 0;
    private final static int TYPE_KEYWORDS = 0;


    static class NovelInfoData
    {
        String ncode = "";

        String novelUrl = "";
        String novelTitle = "";

        String type = "";

        String authorUrl = "";
        String authorName = "";

        List<NovelInfoItem> itemList1 = new ArrayList<>();
        List<NovelInfoItem> itemList2 = new ArrayList<>();
    }

    static class NovelInfoItem
    {
        String title = "";
        SpannableStringBuilder content = null;
    }


    NovelInfoParser(Context context)
    {
        mContext = context;
    }


    @Override
    public NovelInfoData onStartParse(RetrieveHtmlData htmldata)
    {
        NovelInfoData data = new NovelInfoData();

        String mainSource = HtmlUtility.getTagContent(
                htmldata.htmlCode, ContentMainToken, "div", false);
        if (!mainSource.isEmpty())
        {
            int lastIndex = 0;
            int[] position = new int[2];

            //ncode
            Matcher matcher = Pattern.compile(NovelCodeToken).matcher(mainSource);
            if (matcher.find())
            {
                data.ncode = matcher.group(1).trim();
                lastIndex = matcher.end();
            }

            //title, url
            matcher = Pattern.compile(NovelTitleToken).matcher(mainSource);
            matcher.region(lastIndex, mainSource.length());
            if (matcher.find())
            {
                data.novelUrl = matcher.group(1).trim();
                data.novelTitle = Html.fromHtml(matcher.group(2).trim()).toString();
                lastIndex = matcher.end();
            }

            //pre info
            String preInfoSource = HtmlUtility.getTagContent(
                    mainSource, lastIndex, PreInfoToken, "div", false, position);
            if (!preInfoSource.isEmpty())
            {
                int preInfoLastIndex = 0;

                matcher = Pattern.compile(NovelTypeToken).matcher(preInfoSource);
                matcher.region(preInfoLastIndex, preInfoSource.length());
                if (matcher.find())
                {
                    data.type = matcher.group(1).trim();
                    preInfoLastIndex = matcher.end();
                }

                matcher = Pattern.compile(UrlToken).matcher(preInfoSource);
                matcher.region(preInfoLastIndex, preInfoSource.length());
                if (matcher.find())
                {
                    data.type += ": " + preInfoSource.substring(preInfoLastIndex, matcher.start());
                    preInfoLastIndex = matcher.end();
                }

                lastIndex = position[1];
            }

            //table1
            String novelTable1Source = HtmlUtility.getTagContent(
                    mainSource, lastIndex, NovelTable1Token, "table", false, position);
            if (!novelTable1Source.isEmpty())
            {
                ListParser listParser = new ListParser();
                listParser.set(novelTable1Source, "<\\s*tr[\\s\\S]*?>", "tr");
                while (listParser.find())
                {
                    String listItem = listParser.getContent(false);
                    if (!listItem.isEmpty())
                    {
                        String title = HtmlUtility.getTagContent(
                                listItem, "<\\s*th[\\s\\S]*?>", "th", false);
                        String content = HtmlUtility.getTagContent(
                                listItem, "<\\s*td[\\s\\S]*?>", "td", false);

                        Matcher authorMatcher = Pattern.compile(AuthorUrlToken).matcher(content);
                        if (authorMatcher.find())
                        {
                            data.authorUrl = authorMatcher.group(1).trim();
                            data.authorName = authorMatcher.group(2).trim();
                            continue;
                        }

                        Matcher genreMatcher = Pattern.compile(GenreToken).matcher(title);
                        if (genreMatcher.find())
                        {
                            NovelInfoItem novelInfoItem = new NovelInfoItem();
                            novelInfoItem.title = title;
                            novelInfoItem.content = new SpannableStringBuilder(Html.fromHtml(content).toString());
                            novelInfoItem.content.setSpan(new RecipientChipSpan(
                                            ContextCompat.getColor(mContext, R.color.chip_color)),
                                    0, novelInfoItem.content.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                            data.itemList1.add(novelInfoItem);
                            continue;
                        }

                        Matcher keywordsMatcher = Pattern.compile(KeywordsToken).matcher(title);
                        if (keywordsMatcher.find())
                        {
                            NovelInfoItem novelInfoItem = new NovelInfoItem();
                            novelInfoItem.title = title;
                            novelInfoItem.content = new SpannableStringBuilder();

                            String[] keywords = Html.fromHtml(content).toString().split(SyosetuUtility.KeywordSplit);
                            for (int i = 0; i < keywords.length; ++i)
                            {
                                if (TextUtils.isEmpty(keywords[i]))
                                    continue;

                                novelInfoItem.content.append(keywords[i]).append(" ");
                                novelInfoItem.content.setSpan(new RecipientChipSpan(ContextCompat.getColor(
                                        mContext, R.color.chip_color)),
                                        novelInfoItem.content.length() - keywords[i].length() - 1,
                                        novelInfoItem.content.length() - 1,
                                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }

                            data.itemList1.add(novelInfoItem);
                            continue;
                        }

                        NovelInfoItem novelInfoItem = new NovelInfoItem();
                        novelInfoItem.title = title;
                        novelInfoItem.content = new SpannableStringBuilder(Html.fromHtml(content));

                        data.itemList1.add(novelInfoItem);
                    }
                }

                lastIndex = position[1];
            }

            //table2
            String novelTable2Source = HtmlUtility.getTagContent(
                    mainSource, lastIndex, NovelTable2Token, "table", false, position);
            if (!novelTable2Source.isEmpty())
            {
                ListParser listParser = new ListParser();
                listParser.set(novelTable2Source, "<\\s*tr[\\s\\S]*?>", "tr");
                while (listParser.find())
                {
                    String listItem = listParser.getContent(false);
                    if (!listItem.isEmpty())
                    {
                        String title = HtmlUtility.getTagContent(
                                listItem, "<\\s*th[\\s\\S]*?>", "th", false);
                        String content = HtmlUtility.getTagContent(
                                listItem, "<\\s*td[\\s\\S]*?>", "td", false);

                        NovelInfoItem novelInfoItem = new NovelInfoItem();
                        novelInfoItem.title = title;
                        novelInfoItem.content = new SpannableStringBuilder(Html.fromHtml(content));

                        data.itemList2.add(novelInfoItem);
                    }
                }

                lastIndex = position[1];
            }
        }

        return data;
    }


    private final static String UrlToken
            = "<a[\\s\\S]*?href=\"(.*?)\"[\\s\\S]*?>(.*?)</a>";

    private final static String ContentMainToken
            = "<div\\s+id=\"contents_main\"\\s*>";
    private final static String NovelCodeToken
            = "<p\\s+title=\"Nコード\"\\s+id=\"ncode\"\\s*>(.*?)</p>";
    private final static String NovelTitleToken
            = "<h1><a[\\s\\S]*?href=\"(.*?)\"[\\s\\S]*?>(.*?)</a></h1>";

    private final static String PreInfoToken
            = "<div\\s+id=\"pre_info\"\\s*>";
    private final static String NovelTypeToken
            = "<span\\s+id=\"noveltype[\\s\\S]*?>(.*?)</span>";

    private final static String NovelTable1Token
            = "<table\\s+id=\"noveltable1\"\\s*>";
    private final static String AuthorUrlToken
            = "<a[\\s\\S]*?href=\"(http://[x]*?mypage\\.syosetu\\.com[\\s\\S]*?)\"\\s*>(.*?)</a>";
    private final static String GenreToken
            = "ジャンル";
    private final static String KeywordsToken
            = "キーワード";

    private final static String NovelTable2Token
            = "<table\\s+id=\"noveltable2\"\\s*>";
}