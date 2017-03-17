package com.ucclkp.syosetureader.home;

import android.text.Html;
import android.text.TextUtils;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;
import com.ucclkp.syosetureader.SyosetuUtility;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PickupParser extends HtmlDataPipeline<PickupParser.HomeData>
{
    private int mCurMaxPageNumber = 1;
    private String mPageNumberFix = "";


    static class HomeData
    {
        ArrayList<PickupItem> itemList = new ArrayList<>();
    }

    static class PickupItem
    {
        String novelUrl = "";
        String authorUrl = "";

        String novelTitle = "";
        String authorName = "";

        String type = "";
        String summary = "";

        String novelInfoUrl = "";
        String novelInfoTitle = "";
        String extraMsg = "";
    }


    @Override
    public HomeData onStartParse(RetrieveHtmlData htmldata)
    {
        HomeData data = new HomeData();
        parseData(htmldata.htmlCode, data);

        return data;
    }

    private void parseData(String source, HomeData data)
    {
        String pageInfo = HtmlUtility.getTagContent(
                source, PageNumberToken, "div", false);
        if (!pageInfo.isEmpty())
        {
            Pattern pattern = Pattern.compile(SyosetuUtility.getPickupPageNumberRegex());
            Matcher matcher = pattern.matcher(pageInfo);
            while (matcher.find())
            {
                mPageNumberFix = "/" + matcher.group(1).trim();

                int curPage = HtmlUtility.intValue(matcher.group(2).trim(), 0);
                if (curPage > mCurMaxPageNumber)
                    mCurMaxPageNumber = curPage;
            }
        }

        ListParser listParser = new ListParser();
        listParser.set(source, ListItemToken, "div");
        while (listParser.find())
        {
            String itemSource = listParser.getContent(false).trim();

            PickupItem pickupItem = new PickupItem();
            if (parseItem(itemSource, pickupItem))
                data.itemList.add(pickupItem);
        }
    }


    private boolean parseItem(String itemSource, PickupItem item)
    {
        int[] position = new int[2];
        String firstSep = HtmlUtility.getTagContent(
                itemSource, 0, ItemSepToken, "div", false, position);
        if (!firstSep.isEmpty())
        {
            int index = 0;
            Pattern pattern = Pattern.compile(ItemLinkToken);
            Matcher matcher = pattern.matcher(firstSep);
            while (matcher.find())
            {
                if (index == 0)
                {
                    item.novelUrl = matcher.group(1).trim();
                    item.novelTitle = Html.fromHtml(
                            matcher.group(2).trim()).toString();
                }
                else if (index == 1)
                {
                    item.authorUrl = matcher.group(1).trim();
                    item.authorName = Html.fromHtml(
                            matcher.group(2).trim()).toString();
                    break;
                }

                ++index;
            }

            if (index == 0)
                return false;
            else
            {
                item.type = firstSep.substring(matcher.end());
                if (item.type.startsWith("／"))
                    item.type = item.type.substring(1);
            }

            String secondSep = HtmlUtility.getTagContent(
                    itemSource, position[1], ItemSepToken, "div", false);
            if (!secondSep.isEmpty())
            {
                Matcher secMatcher = Pattern.compile(ItemLinkToken)
                        .matcher(secondSep);
                if (secMatcher.find())
                {
                    item.summary = Html.fromHtml(secondSep.substring(
                            0, secMatcher.start())).toString();
                    item.novelInfoUrl = secMatcher.group(1).trim();
                    item.novelInfoTitle = secMatcher.group(2).trim();
                    item.extraMsg = secondSep.substring(secMatcher.end()).trim();
                }
                else
                    item.summary = Html.fromHtml(secondSep).toString();

                item.summary = HtmlUtility.removeLB(item.summary.trim());
            }

            return true;
        }

        return false;
    }

    void resetPageNumber()
    {
        mCurMaxPageNumber = 1;
    }

    int getCurMaxPageNumber()
    {
        return mCurMaxPageNumber;
    }

    String getPageNumberFix()
    {
        return mPageNumberFix;
    }


    private final static String ListItemToken
            = "<\\s*div\\s+class\\s*=\\s*\"\\s*trackback_list\\s*\"\\s*>";

    private final static String PageNumberToken
            = "<\\s*div\\s+class\\s*=\\s*\"\\s*naviall_c\\s*\"\\s*>";

    private final static String ItemSepToken
            = "<\\s*div\\s+class\\s*=\\s*\"\\s*trackback_listdiv\\s*\"\\s*>";

    private final static String ItemLinkToken
            = "<\\s*a[\\s\\S]*?href\\s*=\\s*\"(.*?)\"[\\s\\S]*?>(.*?)<\\s*/\\s*a\\s*>";
}