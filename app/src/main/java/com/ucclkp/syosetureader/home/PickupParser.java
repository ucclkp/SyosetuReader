package com.ucclkp.syosetureader.home;

import android.text.Html;

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
    private SyosetuUtility.SyosetuSite mAuthorSite;

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
        String last_update = "";

        String novelInfoUrl = "";
        String novelInfoTitle = "";
        String extraMsg = "";
    }

    public void setPageType(SyosetuUtility.SyosetuSite site) {
        mAuthorSite = site;
    }

    @Override
    public HomeData onStartParse(RetrieveHtmlData htmldata)
    {
        HomeData data = new HomeData();

        switch (mAuthorSite) {
            case NORMAL:
                parseData(htmldata.htmlCode, data);
            case NOCTURNE:
                parseData18(htmldata.htmlCode, data);
        }

        return data;
    }

    private void parseData(String source, HomeData data)
    {
        String ctr_src = HtmlUtility.getTagContent(
                source, "<div\\s+class=\"l-container\"\\s*>", "div", false);
        if (ctr_src.isEmpty()) {
            return;
        }

        String main_src = HtmlUtility.getTagContent(
                ctr_src, "<div\\s+class=\"l-main-full\"\\s*>", "div", false);
        if (main_src.isEmpty()) {
            return;
        }

        String pageInfo = HtmlUtility.getTagContent(
                main_src, "<div\\s+class=\"c-pager-box\"\\s*>", "div", false);
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
        listParser.set(main_src, "<div\\s+class=\"c-card c-novel-detail-item\"\\s*>", "div");
        while (listParser.find())
        {
            String itemSource = listParser.getContent(false).trim();

            PickupItem pickupItem = new PickupItem();
            if (parseItem(itemSource, pickupItem))
                data.itemList.add(pickupItem);
        }
    }

    private void parseData18(String source, HomeData data)
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
            if (parseItem18(itemSource, pickupItem))
                data.itemList.add(pickupItem);
        }
    }

    private boolean parseItem(String itemSource, PickupItem item)
    {
        String header = HtmlUtility.getTagContent(
                itemSource, "<div\\s+class=\"c-card__header\"\\s*>", "div", false);
        if (header.isEmpty()) {
            return false;
        }

        int[] position = new int[2];
        String title = HtmlUtility.getTagContent(
                header, 0, "<div\\s+class=\"c-card__title\"\\s*>", "div", false, position);
        if (!title.isEmpty()) {
            Pattern pattern = Pattern.compile(ItemLinkToken);
            Matcher matcher = pattern.matcher(title);
            if (matcher.find()) {
                item.novelUrl = matcher.group(1).trim();
                item.novelTitle = Html.fromHtml(
                        matcher.group(2).trim()).toString();
            }

            pattern = Pattern.compile(ItemLinkToken);
            matcher = pattern.matcher(header);
            matcher.region(position[1], header.length());
            if (matcher.find()) {
                item.authorUrl = matcher.group(1).trim();
                item.authorName = Html.fromHtml(
                        matcher.group(2).trim()).toString();
            }
        }

        String novel_info = HtmlUtility.getTagContent(
                header, "<div\\s+class=\"c-novel-detail-item__novel-info\"\\s*>", "div", false);
        if (!novel_info.isEmpty()) {
            Pattern pattern = Pattern.compile(ItemLinkToken);
            Matcher matcher = pattern.matcher(novel_info);
            if (matcher.find()) {
                item.novelInfoUrl = matcher.group(1).trim();
                item.novelInfoTitle = Html.fromHtml(
                        matcher.group(2).trim()).toString();
            }

            String type = HtmlUtility.getTagContent(
                    novel_info, "<span\\s+class=\"c-novel-detail-item__noveltype\"\\s*>", "span", false);
            item.type = Html.fromHtml(type.trim()).toString();

            type = HtmlUtility.getTagContent(
                    novel_info, "<span\\s+class=\"c-novel-detail-item__genre\"\\s*>", "span", false);
            item.type += "  ";
            item.type += Html.fromHtml(type.trim()).toString();

            String last_update = HtmlUtility.getTagContent(
                    novel_info, "<div\\s+class=\"c-novel-detail-item__lastup\"\\s*>", "div", false);
            item.last_update = Html.fromHtml(last_update.trim()).toString();
        }

        String block_src = HtmlUtility.getTagContent(
                itemSource, "<div\\s+class=\"c-card__item-block\"\\s*>", "div", false);
        if (!block_src.isEmpty())
        {
            String summary_src = HtmlUtility.getTagContent(
                    block_src, "<div\\s+class=\"c-readmore__mask[\\s\\S]*?>", "div", false);
            item.summary = Html.fromHtml(summary_src).toString();
            item.summary = HtmlUtility.removeLB(item.summary.trim());
        }

        return true;
    }

    private boolean parseItem18(String itemSource, PickupItem item)
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
                } else if (index == 1)
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
                } else
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
            = "<div\\s+class=\"trackback_list\"\\s*>";

    private final static String PageNumberToken
            = "<div\\s+class=\"naviall_c\"\\s*>";

    private final static String ItemSepToken
            = "<div\\s+class=\"trackback_listdiv\"\\s*>";

    private final static String ItemLinkToken
            = "<a[\\s\\S]*?href=\"(.*?)\"[\\s\\S]*?>(.*?)</a>";

}