package com.ucclkp.syosetureader;

import android.content.Context;
import android.content.Intent;
import android.os.Parcel;
import android.text.Spannable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.view.View;

import com.ucclkp.syosetureader.author.AuthorActivity;
import com.ucclkp.syosetureader.novel.NovelActivity;
import com.ucclkp.syosetureader.novelinfo.NovelInfoActivity;

import java.text.DecimalFormat;

public class SyosetuUtility
{
    public final static String HOME_URL = "http://syosetu.com";
    public final static String READ_URL = "http://yomou.syosetu.com";
    public final static String BLOG_URL = "http://blog.syosetu.com";
    public final static String NOVEL_URL = "http://ncode.syosetu.com";

    public final static String PICKUP_URL = HOME_URL + "/pickup/list";
    public final static String POP_KEYWORD_URL = READ_URL + "/search/keyword";
    public final static String RANKING_URL = READ_URL + "/rank/genretop";
    public final static String SEARCH_URL = READ_URL + "/search.php";
    public final static String DETAIL_SEARCH_URL = READ_URL + "/search/cross/";

    public final static String R18_HOME_URL = "http://noc.syosetu.com";
    public final static String R18_NOVEL_URL = "http://novel18.syosetu.com";

    public final static String R18_PICKUP_URL = R18_HOME_URL + "/pickup/list";
    public final static String R18_POP_KEYWORD_URL = R18_HOME_URL + "/search/classified";
    public final static String R18_RANKING_URL = R18_HOME_URL + "/rank/top";
    public final static String R18_SEARCH_URL = R18_HOME_URL + "/search/search/search.php";

    public final static String AUTHOR_HOME_URL = "http://mypage.syosetu.com";
    public final static String NOVEL_INFO_URL = "http://ncode.syosetu.com/novelview";

    public final static String R18_AUTHOR_HOME_URL = "http://xmypage.syosetu.com";
    public final static String R18_NOVEL_INFO_URL = "http://novel18.syosetu.com/novelview";


    public final static String KeywordSplit = "\\s+|[　]";


    public enum SyosetuSite
    {
        NORMAL,
        NOCTURNE
    }

    public enum SyosetuType
    {
        SERIES,
        SHORT
    }

    public enum SyosetuSource
    {
        DOWNLOAD,
        CACHE,
        NETWORK
    }


    public static SyosetuSite getSiteFromNovelUrl(String url)
    {
        if (url.startsWith(NOVEL_URL))
            return SyosetuSite.NORMAL;
        else if (url.startsWith(R18_NOVEL_URL))
            return SyosetuSite.NOCTURNE;

        return UApplication.syosetuSite;
    }

    public static SyosetuSite getSiteFromAuthorUrl(String url)
    {
        if (url.startsWith(AUTHOR_HOME_URL))
            return SyosetuSite.NORMAL;
        else if (url.startsWith(R18_AUTHOR_HOME_URL))
            return SyosetuSite.NOCTURNE;

        return UApplication.syosetuSite;
    }

    public static String getTypeStr(Context context, boolean isShort)
    {
        if (isShort)
            return context.getString(R.string.type_short);
        else
            return context.getString(R.string.type_series);
    }


    public static String getHomeUrl()
    {
        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                return HOME_URL;
            case NOCTURNE:
                return R18_HOME_URL;
        }

        return "";
    }

    public static String getHomeUrl(SyosetuSite site)
    {
        switch (site)
        {
            case NORMAL:
                return HOME_URL;
            case NOCTURNE:
                return R18_HOME_URL;
        }

        return "";
    }

    public static String getNovelUrl()
    {
        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                return NOVEL_URL;
            case NOCTURNE:
                return R18_NOVEL_URL;
        }

        return "";
    }

    public static String getNovelUrl(SyosetuSite site)
    {
        switch (site)
        {
            case NORMAL:
                return NOVEL_URL;
            case NOCTURNE:
                return R18_NOVEL_URL;
        }

        return "";
    }

    public static String getNovelSite(Context context)
    {
        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                return context.getString(R.string.site_novel);
            case NOCTURNE:
                return context.getString(R.string.site_novel18);
        }

        return "";
    }

    public static String getNovelSite(Context context, SyosetuSite site)
    {
        switch (site)
        {
            case NORMAL:
                return context.getString(R.string.site_novel);
            case NOCTURNE:
                return context.getString(R.string.site_novel18);
        }

        return "";
    }

    public static String getAuthorHomeUrl()
    {
        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                return AUTHOR_HOME_URL;
            case NOCTURNE:
                return R18_AUTHOR_HOME_URL;
        }

        return "";
    }

    public static String getAuthorHomeUrl(SyosetuSite site)
    {
        switch (site)
        {
            case NORMAL:
                return AUTHOR_HOME_URL;
            case NOCTURNE:
                return R18_AUTHOR_HOME_URL;
        }

        return "";
    }

    public static String getNovelInfoUrl()
    {
        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                return NOVEL_INFO_URL;
            case NOCTURNE:
                return R18_NOVEL_INFO_URL;
        }

        return "";
    }

    public static String getNovelInfoUrl(SyosetuSite site)
    {
        switch (site)
        {
            case NORMAL:
                return NOVEL_INFO_URL;
            case NOCTURNE:
                return R18_NOVEL_INFO_URL;
        }

        return "";
    }

    public static String getPickupUrl()
    {
        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                return PICKUP_URL;
            case NOCTURNE:
                return R18_PICKUP_URL;
        }

        return "";
    }

    public static String getPickupUrl(SyosetuSite site)
    {
        switch (site)
        {
            case NORMAL:
                return PICKUP_URL;
            case NOCTURNE:
                return R18_PICKUP_URL;
        }

        return "";
    }

    public static String getSearchUrl()
    {
        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                return SEARCH_URL;
            case NOCTURNE:
                return R18_SEARCH_URL;
        }

        return "";
    }

    public static String getSearchUrl(SyosetuSite site)
    {
        switch (site)
        {
            case NORMAL:
                return SEARCH_URL;
            case NOCTURNE:
                return R18_SEARCH_URL;
        }

        return "";
    }

    public static String getDetailSearchUrl()
    {
        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                return DETAIL_SEARCH_URL;
        }

        return "";
    }

    public static String getDetailSearchUrl(SyosetuSite site)
    {
        switch (site)
        {
            case NORMAL:
                return DETAIL_SEARCH_URL;
        }

        return "";
    }

    public static String getSearchResultHost()
    {
        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                return READ_URL;
            case NOCTURNE:
                return R18_HOME_URL;
        }

        return "";
    }

    public static String getSearchResultHost(SyosetuSite site)
    {
        switch (site)
        {
            case NORMAL:
                return READ_URL;
            case NOCTURNE:
                return R18_HOME_URL;
        }

        return "";
    }

    public static String getPickupPageNumberRegex()
    {
        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                return "<\\s*a\\s+href\\s*=\\s*\"(.*?)\\d*?\"\\s+title\\s*=\\s*\"page\\s+\\d*?\"\\s*>(\\d*?)<\\s*/\\s*a\\s*>";
            case NOCTURNE:
                return "<\\s*a\\s+href\\s*=\\s*\"(.*?)\\d*?\"\\s+title\\s*=\\s*\"\\d*?\\s+ページ\"\\s*>(\\d*?)<\\s*/\\s*a\\s*>";
        }

        return "";
    }

    public static String getPickupPageNumberRegex(SyosetuSite site)
    {
        switch (site)
        {
            case NORMAL:
                return "<\\s*a\\s+href\\s*=\\s*\"(.*?)\\d*?\"\\s+title\\s*=\\s*\"page\\s+\\d*?\"\\s*>(\\d*?)<\\s*/\\s*a\\s*>";
            case NOCTURNE:
                return "<\\s*a\\s+href\\s*=\\s*\"(.*?)\\d*?\"\\s+title\\s*=\\s*\"\\d*?\\s+ページ\"\\s*>(\\d*?)<\\s*/\\s*a\\s*>";
        }

        return "";
    }

    public static String getSearchResultPageNumberTokenRegex()
    {
        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                return "<\\s*div\\s+class\\s*=\\s*\"\\s*searchdate_box\\s*\"\\s*>";
            case NOCTURNE:
                return "<\\s*p\\s+class\\s*=\\s*\"\\s*pager\\s*\"\\s*>";
        }

        return "";
    }

    public static String getSearchResultPageNumberTokenRegex(SyosetuSite site)
    {
        switch (site)
        {
            case NORMAL:
                return "<\\s*div\\s+class\\s*=\\s*\"\\s*searchdate_box\\s*\"\\s*>";
            case NOCTURNE:
                return "<\\s*p\\s+class\\s*=\\s*\"\\s*pager\\s*\"\\s*>";
        }

        return "";
    }

    public static String getSearchResultPageNumberTokenTag()
    {
        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                return "div";
            case NOCTURNE:
                return "p";
        }

        return "";
    }

    public static String getSearchResultPageNumberTokenTag(SyosetuSite site)
    {
        switch (site)
        {
            case NORMAL:
                return "div";
            case NOCTURNE:
                return "p";
        }

        return "";
    }


    public static View.OnClickListener clickOfTitle(final String url)
    {
        if (TextUtils.isEmpty(url)) return null;

        return new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(v.getContext(), NovelActivity.class);
                intent.putExtra(NovelActivity.ARG_NOVEL_URL, url);
                v.getContext().startActivity(intent);
            }
        };
    }

    public static View.OnClickListener clickOfAuthor(final String url, final String name)
    {
        if (TextUtils.isEmpty(url)) return null;

        return new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(v.getContext(), AuthorActivity.class);
                intent.putExtra(AuthorActivity.ARG_AUTHOR_URL, url);
                intent.putExtra(AuthorActivity.ARG_AUTHOR_NAME, name);
                v.getContext().startActivity(intent);
            }
        };
    }

    public static View.OnClickListener clickOfInfo(final String url)
    {
        if (TextUtils.isEmpty(url)) return null;

        return new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(v.getContext(), NovelInfoActivity.class);
                intent.putExtra(NovelInfoActivity.ARG_NOVEL_INFO_URL, url);
                v.getContext().startActivity(intent);
            }
        };
    }


    public static void setUrlMovement(String scheme, Spannable content, UrlCallback callback)
    {
        URLSpan[] spans = content.getSpans(0, content.length(), URLSpan.class);
        for (URLSpan span : spans)
        {
            int urlStart = content.getSpanStart(span);
            int urlEnd = content.getSpanEnd(span);
            int urlFlags = content.getSpanFlags(span);

            String originUrl = span.getURL();
            if (!originUrl.startsWith("http://"))
                originUrl = scheme + originUrl;

            SyosetuUrlSpan syosetuUrlSpan = new SyosetuUrlSpan(originUrl);
            syosetuUrlSpan.setCallback(callback);

            content.setSpan(syosetuUrlSpan, urlStart, urlEnd, urlFlags);
            content.removeSpan(span);
        }
    }

    public static String constructSubtitle(
            Context context, String index, int length, SyosetuSource source)
    {
        String result = "";
        String lengthStr;

        if (length >= 0)
        {
            if (length >= 1000)
            {
                lengthStr = new DecimalFormat(".0")
                        .format((float) length / 1000) + "k";
            } else
                lengthStr = String.valueOf(length);

            if (index == null || index.isEmpty())
                result = lengthStr + "字";
            else
                result = index + "  " + lengthStr + "字";

            result += "  ";
        }

        switch (source)
        {
            case CACHE:
                result += context.getString(R.string.source_cache);
                break;
            case DOWNLOAD:
                result += context.getString(R.string.source_download);
                break;
            case NETWORK:
                result += context.getString(R.string.source_network);
                break;
        }

        return result;
    }

    public static String constructSectionId(
            String ncode, String sectionUrl)
    {
        return ncode + ":" + HtmlUtility.getUrlRear(sectionUrl);
    }


    public static class SyosetuUrlSpan extends URLSpan
    {
        private UrlCallback mCallback;

        public SyosetuUrlSpan(String url)
        {
            super(url);
        }

        public SyosetuUrlSpan(Parcel src)
        {
            super(src);
        }


        public void setCallback(UrlCallback callback)
        {
            mCallback = callback;
        }


        @Override
        public void updateDrawState(TextPaint ds)
        {
            ds.setColor(ds.linkColor);
            ds.setUnderlineText(false);
        }

        @Override
        public void onClick(View widget)
        {
            String url = getURL();
            if (mCallback != null)
            {
                url = HtmlUtility.decodeUrl(url);
                mCallback.onClick(url, widget);
            }
        }
    }


    public interface UrlCallback
    {
        void onClick(String url, View widget);
    }
}