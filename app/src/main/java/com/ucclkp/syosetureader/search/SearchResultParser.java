package com.ucclkp.syosetureader.search;

import android.text.Html;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;
import com.ucclkp.syosetureader.SyosetuUtility;
import com.ucclkp.syosetureader.UApplication;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SearchResultParser extends HtmlDataPipeline<SearchResultParser.SearchData>
{
    private int mCurMaxPageNumber = 1;


    static class SearchData
    {
        ArrayList<SearchedItem> itemList = new ArrayList<>();
    }

    static class SearchedItem
    {
        String novelUrl = "";
        String novelTitle = "";

        String authorUrl = "";
        String authorName = "";

        String novelInfoUrl = "";
        String novelInfoTitle = "";
        String extraMsg = "";

        String type = "";
        String summary = "";

        String genre = "";
        String genreType = "";
        ArrayList<String> keywordList = new ArrayList<>();

        String lastUpdatedDate = "";
        String readingTime = "";
        String weekUniqueUser = "";
        String review = "";
        String overallPoint = "";
        String rankUserCount = "";
        String rankPoint = "";
        String bookmarkCount = "";
        String ncode = "";

        boolean isPCContribute = false;
        boolean isMBContribute = false;
        boolean hasIllustration = false;
    }


    @Override
    public SearchData onStartParse(RetrieveHtmlData htmldata)
    {
        SearchData data = new SearchData();
        parseData(htmldata.htmlCode, data);
        return data;
    }

    private void parseData(String source, SearchData dataList)
    {
        String pageInfo = HtmlUtility.getTagContent(source,
                SyosetuUtility.getSearchResultPageNumberTokenRegex(),
                SyosetuUtility.getSearchResultPageNumberTokenTag(), false);
        if (!pageInfo.isEmpty())
        {
            Pattern pattern = Pattern.compile(PageNumber);
            Matcher matcher = pattern.matcher(pageInfo);
            while (matcher.find())
            {
                int curPage = HtmlUtility.intValue(matcher.group(1).trim(), 0);
                if (curPage > mCurMaxPageNumber)
                    mCurMaxPageNumber = curPage;
            }
        }

        ListParser listParser = new ListParser();
        listParser.set(source, ListItemToken, "div");
        while (listParser.find())
        {
            SearchedItem item = new SearchedItem();
            String itemSource = listParser.getContent(false).trim();

            switch (UApplication.syosetuSite)
            {
                case NORMAL:
                    parseItem(itemSource, item);
                    break;

                case NOCTURNE:
                    parseItem18(itemSource, item);
                    break;
            }

            dataList.itemList.add(item);
        }
    }


    private boolean parseItem(String itemSource, SearchedItem item)
    {
        int position[] = new int[2];
        String novelTitleDiv = HtmlUtility.getTagContent(
                itemSource, 0, NovelTitleToken, "div", false, position);
        if (novelTitleDiv.isEmpty())
            return false;

        Pattern pattern = Pattern.compile(ItemLinkToken);
        Matcher matcher = pattern.matcher(novelTitleDiv);
        if (matcher.find())
        {
            item.novelUrl = matcher.group(1).trim();
            item.novelTitle = matcher.group(2).trim();
        }

        int tableStartIndex = itemSource.indexOf("<table>", position[1]);
        String novelBarSource = itemSource.substring(
                position[1], tableStartIndex);

        int index = 0;
        pattern = Pattern.compile(ItemLinkToken);
        matcher = pattern.matcher(novelBarSource);
        while (matcher.find())
        {
            if (index == 0)
            {
                item.authorUrl = matcher.group(1).trim();
                item.authorName = matcher.group(2).trim();
            }
            else if (index == 1)
            {
                item.novelInfoUrl = matcher.group(1).trim();
                item.novelInfoTitle = matcher.group(2).trim();
                break;
            }

            ++index;
        }

        if (index == 0)
            return false;
        else
            item.extraMsg = novelBarSource.substring(matcher.end());

        //type
        int lastIndex = 0;
        String type = HtmlUtility.getTagContent(
                itemSource, tableStartIndex, ItemTypeToken, "td", false, position);
        if (!type.isEmpty())
        {
            lastIndex = position[1];
            item.type = type.trim().replaceAll("<\\s*br\\s*/\\s*>", "");
        }

        //summary
        String summary = HtmlUtility.getTagContent(
                itemSource, lastIndex, ItemSummaryToken, "div", false, position);
        if (!summary.isEmpty())
        {
            lastIndex = position[1];
            item.summary = Html.fromHtml(summary.trim()).toString();
        }

        //genre
        matcher = Pattern.compile(ItemGenreToken).matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            lastIndex = matcher.end();
            String genre = matcher.group(1);
            if (genre != null && !genre.isEmpty())
            {
                matcher = Pattern.compile(ItemLinkToken).matcher(genre);
                if (matcher.find())
                {
                    item.genre = matcher.group(2).trim();
                    item.genreType = genre.substring(matcher.end());
                }
            }
        }

        //keyword
        matcher = Pattern.compile("最終更新日：").matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            String keywords = itemSource.substring(lastIndex, matcher.end());
            lastIndex = matcher.end();

            matcher = Pattern.compile(ItemLinkToken).matcher(keywords);
            while (matcher.find())
                item.keywordList.add(matcher.group(2).trim());
        }

        //lastupdate, readingtime
        matcher = Pattern.compile(ItemReadingTimeToken).matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            item.lastUpdatedDate = itemSource.substring(
                    lastIndex, matcher.start()).trim();
            lastIndex = matcher.end();

            item.readingTime = matcher.group(1).trim();
        }

        //unique user, review
        matcher = Pattern.compile(ItemUUAndRVToken).matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            lastIndex = matcher.end();
            item.weekUniqueUser = matcher.group(1).trim();
            item.review = matcher.group(2).trim();
        }

        //images
        if (itemSource.indexOf(ItemPCContriImageUrl, lastIndex) > 0)
            item.isPCContribute = true;
        if (itemSource.indexOf(ItemMBContriImageUrl, lastIndex) > 0)
            item.isMBContribute = true;
        if (itemSource.indexOf(ItemIllustration, lastIndex) > 0)
            item.hasIllustration = true;

        matcher = Pattern.compile(ItemOverallToken).matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            lastIndex = matcher.end();
            item.overallPoint = matcher.group(1).trim();
        }

        matcher = Pattern.compile(ItemRankUserCountToken).matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            lastIndex = matcher.end();
            item.rankUserCount = matcher.group(1).trim();
        }

        matcher = Pattern.compile(ItemRankPointToken).matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            lastIndex = matcher.end();
            item.rankPoint = matcher.group(1).trim();
        }

        matcher = Pattern.compile(ItemBookmarkToken).matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            lastIndex = matcher.end();
            item.bookmarkCount = matcher.group(1).trim();
        }

        return true;
    }

    private boolean parseItem18(String itemSource, SearchedItem item)
    {
        int position[] = new int[2];
        String novelTitleDiv = HtmlUtility.getTagContent(
                itemSource, 0, NovelTitleToken, "div", false, position);
        if (novelTitleDiv.isEmpty())
            return false;

        Pattern pattern = Pattern.compile(ItemLinkToken);
        Matcher matcher = pattern.matcher(novelTitleDiv);
        if (matcher.find())
        {
            item.novelUrl = matcher.group(1).trim();
            item.novelTitle = Html.fromHtml(
                    matcher.group(2).trim()).toString();
        }

        int tableStartIndex = itemSource.indexOf("<table>", position[1]);
        String novelBarSource = itemSource.substring(
                position[1], tableStartIndex);

        //author
        pattern = Pattern.compile("作者：(.*?)／\\s*<\\s*a");
        matcher = pattern.matcher(novelBarSource);
        if (matcher.find())
        {
            item.authorUrl = "";
            item.authorName = Html.fromHtml(
                    matcher.group(1).trim()).toString();
        }

        //novel info
        pattern = Pattern.compile(ItemLinkToken);
        matcher = pattern.matcher(novelBarSource);
        if (matcher.find())
        {
            item.novelInfoUrl = matcher.group(1).trim();
            item.novelInfoTitle = matcher.group(2).trim();
        }

        //type
        int lastIndex = 0;
        String type = HtmlUtility.getTagContent(
                itemSource, tableStartIndex, ItemTypeToken, "td", false, position);
        if (!type.isEmpty())
        {
            lastIndex = position[1];
            item.type = type.trim().replaceAll("<\\s*br\\s*/\\s*>", "");
        }

        //summary
        String summary = HtmlUtility.getTagContent(
                itemSource, lastIndex, ItemSummary18Token, "td", false, position);
        if (!summary.isEmpty())
        {
            lastIndex = position[1];
            item.summary = Html.fromHtml(summary.trim()).toString();
        }

        //keyword
        matcher = Pattern.compile("最終掲載：").matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            String keywords = itemSource.substring(lastIndex, matcher.end());
            lastIndex = matcher.end();

            matcher = Pattern.compile(ItemLinkToken).matcher(keywords);
            while (matcher.find())
                item.keywordList.add(matcher.group(2).trim());
        }

        //lastupdate
        matcher = Pattern.compile("<\\s*br\\s*/\\s*>\\s*Nコード：").matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            item.lastUpdatedDate = itemSource.substring(lastIndex, matcher.start()).trim();
            lastIndex = matcher.end();
        }

        //readingtime
        matcher = Pattern.compile(ItemReadingTimeToken).matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            item.ncode = itemSource.substring(lastIndex, matcher.start()).trim();
            lastIndex = matcher.end();

            item.readingTime = matcher.group(1).trim();
        }

        //images
        if (itemSource.indexOf("パソコンのみで投稿", lastIndex) > 0)
            item.isPCContribute = true;
        if (itemSource.indexOf(ItemMBContriImageUrl, lastIndex) > 0)
            item.isMBContribute = true;
        if (itemSource.indexOf(ItemIllustration, lastIndex) > 0)
            item.hasIllustration = true;

        //unique user, review
        matcher = Pattern.compile(ItemUUAndRVToken).matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            lastIndex = matcher.end();
            item.weekUniqueUser = matcher.group(1).trim();
            item.review = matcher.group(2).trim();
        }

        //overall
        matcher = Pattern.compile(ItemOverallToken).matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            lastIndex = matcher.end();
            item.overallPoint = matcher.group(1).trim();
        }

        //rank user count
        matcher = Pattern.compile(ItemRankUserCountToken).matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            lastIndex = matcher.end();
            item.rankUserCount = matcher.group(1).trim();
        }

        //rank point
        matcher = Pattern.compile(ItemRankPointToken).matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            lastIndex = matcher.end();
            item.rankPoint = matcher.group(1).trim();
        }

        //bookmark
        matcher = Pattern.compile(ItemBookmarkToken).matcher(itemSource);
        matcher.region(lastIndex, itemSource.length());
        if (matcher.find())
        {
            lastIndex = matcher.end();
            item.bookmarkCount = matcher.group(1).trim();
        }

        return true;
    }

    void resetMaxPageNumber()
    {
        mCurMaxPageNumber = 1;
    }

    int getCurMaxPageNumber()
    {
        return mCurMaxPageNumber;
    }


    private final static String ListItemToken
            = "<\\s*div\\s+class\\s*=\\s*\"\\s*searchkekka_box\\s*\"\\s*>";

    private final static String PageNumberToken
            = "<\\s*div\\s+class\\s*=\\s*\"\\s*searchdate_box\\s*\"\\s*>";
    private final static String PageNumber
            = "<\\s*a\\s+href\\s*=[\\s\\S]*?>(\\d*?)<\\s*/\\s*a\\s*>";

    private final static String NovelTitleToken
            = "<\\s*div\\s+class\\s*=\\s*\"\\s*novel_h\\s*\"\\s*>";

    private final static String ItemLinkToken
            = "<\\s*a[\\s\\S]*?href\\s*=\\s*\"(.*?)\"[\\s\\S]*?>(.*?)<\\s*/\\s*a\\s*>";

    private final static String ItemTypeToken
            = "<\\s*td\\s+class\\s*=\\s*\"\\s*left\\s*\"[\\s\\S]*?>";
    private final static String ItemSummaryToken
            = "<\\s*div\\s+class\\s*=\\s*\"\\s*ex\\s*\"\\s*>";
    private final static String ItemSummary18Token
            = "<\\s*td\\s+class\\s*=\\s*\"\\s*ex\\s*\"\\s*>";
    private final static String ItemGenreToken
            = "ジャンル：(.*?)<\\s*br\\s*/\\s*>\\s*キーワード：";
    private final static String ItemReadingTimeToken
            = "\\s*?<\\s*span\\s+class\\s*=\\s*\"\\s*marginleft\\s*\"\\s*>\\s*読了時間：(.*?)<\\s*/\\s*span\\s*>";
    private final static String ItemUUAndRVToken
            = "週別ユニークユーザ：\\s*?(.*?人)[\\S\\s]*?レビュー数：\\s*?(.*?件)";
    private final static String ItemOverallToken
            = "総合評価ポイント：\\s*(.*?)\\s*<\\s*/\\s*span\\s*>";
    private final static String ItemRankUserCountToken
            = "評価人数：\\s*(.*?)\\s*<\\s*span";
    private final static String ItemRankPointToken
            = "評価点：\\s*(.*?)\\s*<\\s*/\\s*span\\s*>";
    private final static String ItemBookmarkToken
            = "ブックマーク：\\s*(.*?)\\s*<\\s*/\\s*span\\s*>";

    private final static String ItemPCContriImageUrl
            = "http://static.syosetu.com/sub/yomouview/images/pc_toko.gif";
    private final static String ItemMBContriImageUrl
            = "http://static.syosetu.com/sub/yomouview/images/k_toko.gif";
    private final static String ItemIllustration
            = "http://static.syosetu.com/view/images/sasie.gif";
}