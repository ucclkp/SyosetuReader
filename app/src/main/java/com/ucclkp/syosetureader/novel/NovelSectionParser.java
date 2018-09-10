package com.ucclkp.syosetureader.novel;

import android.text.Html;
import android.text.SpannableStringBuilder;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;
import com.ucclkp.syosetureader.SyosetuImageGetter;
import com.ucclkp.syosetureader.SyosetuUtility;
import com.ucclkp.syosetureader.UApplication;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NovelSectionParser extends HtmlDataPipeline<NovelSectionParser.SectionData>
{
    private SyosetuImageGetter mImageGetter;


    public static class SectionData
    {
        public int length = 0;
        public String number = "";
        public String prevUrl = "";
        public String nextUrl = "";
        public String title = "";
        public SpannableStringBuilder sectionContent = null;
    }


    public NovelSectionParser(SyosetuImageGetter imageGetter)
    {
        mImageGetter = imageGetter;
    }


    @Override
    public SectionData onStartParse(RetrieveHtmlData htmldata)
    {
        SectionData data = new SectionData();
        String source = htmldata.htmlCode;

        SyosetuUtility.SyosetuSite site;
        if (htmldata.redirection)
            site = SyosetuUtility.getSiteFromNovelUrl(htmldata.location);
        else
            site = UApplication.syosetuSite;

        String contents = HtmlUtility.getTagContent(
                source, NovelSectionToken, "div", false);
        if (!contents.isEmpty())
        {
            String navContent = HtmlUtility.getTagContent(
                    contents, NovelSectionNavToken, "div", false);
            if (!navContent.isEmpty())
            {
                Pattern prevPattern = Pattern.compile(NovelSectionNavPrev);
                Matcher prevMatcher = prevPattern.matcher(navContent);
                if (prevMatcher.find())
                    data.prevUrl = SyosetuUtility.getNovelUrl(site) + prevMatcher.group(1).trim();

                Pattern nextPattern = Pattern.compile(NovelSectionNavNext);
                Matcher nextMatcher = nextPattern.matcher(navContent);
                if (nextMatcher.find())
                    data.nextUrl = SyosetuUtility.getNovelUrl(site) + nextMatcher.group(1).trim();
            }

            data.number = HtmlUtility.getTagContent(
                    contents, NovelSectionNumberToken, "div", false).trim();
            String subtitle = HtmlUtility.getTagContent(
                    contents, NovelSectionSubtitleToken, "p", false).trim();

            SpannableStringBuilder headSpan = new SpannableStringBuilder("");
            String headText = HtmlUtility.getTagContent(
                    contents, NovelHeadTextToken, "div", false);
            if (!headText.isEmpty())
            {
                headSpan.append(Html.fromHtml(headText, mImageGetter, null));
                headSpan.append("\n\n").append("==========").append("\n\n");
            }

            SpannableStringBuilder normalSpan = new SpannableStringBuilder("");
            String normalText = HtmlUtility.getTagContent(
                    contents, NovelTextToken, "div", false);
            if (!normalText.isEmpty())
                normalSpan.append(Html.fromHtml(normalText, mImageGetter, null));

            SpannableStringBuilder footSpan = new SpannableStringBuilder("");
            String footText = HtmlUtility.getTagContent(
                    contents, NovelFootTextToken, "div", false);
            if (!footText.isEmpty())
            {
                footSpan.append("\n\n").append("==========").append("\n\n");
                footSpan.append(Html.fromHtml(footText, mImageGetter, null));
            }

            headSpan.append(normalSpan).append(footSpan);

            data.length = normalSpan.toString().length();
            data.title = Html.fromHtml(subtitle).toString();
            data.sectionContent = headSpan;
        }

        return data;
    }


    private final static String NovelSectionToken
            = "<div id=\"novel_color\">";
    private final static String NovelSectionNavToken
            = "<div class=\"novel_bn\">";
    private final static String NovelSectionNumberToken
            = "<div id=\"novel_no\">";
    private final static String NovelSectionSubtitleToken
            = "<p\\s+class=\"novel_subtitle\"\\s*>";

    private final static String NovelSectionNavPrev
            = "<a href=\"(.*?)\">[\\s\\S]*?前へ[\\s\\S]*?</a>";
    private final static String NovelSectionNavNext
            = "[\\s\\S]*<a href=\"(.*?)\">[\\s\\S]*?次へ[\\s\\S]*?</a>";

    private final static String NovelHeadTextToken
            = "<div\\s+id=\"novel_p\"\\s+class=\"novel_view\"\\s*>";
    private final static String NovelTextToken
            = "<div\\s+id=\"novel_honbun\"\\s+class=\"novel_view\"\\s*>";
    private final static String NovelFootTextToken
            = "<div\\s+id=\"novel_a\"\\s+class=\"novel_view\"\\s*>";
}