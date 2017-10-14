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
            = "<\\s*div\\s+id\\s*=\\s*\"\\s*novel_color\\s*\"\\s*>";

    private final static String NovelSectionNavToken
            = "<\\s*div\\s+class\\s*=\\s*\"\\s*novel_bn\\s*\"\\s*>";
    private final static String NovelSectionNumberToken
            = "<\\s*div\\s+id\\s*=\\s*\"\\s*novel_no\\s*\"\\s*>";
    private final static String NovelSectionSubtitleToken
            = "<\\s*p\\s+class\\s*=\\s*\"\\s*novel_subtitle\\s*\"\\s*>";

    private final static String NovelSectionNavPrev
            = "<\\s*a\\s+href\\s*=\\s*\"(.*?)\"\\s*>[\\s\\S]*?前へ[\\s\\S]*?<\\s*/\\s*a\\s*>";
    private final static String NovelSectionNavNext
            = "[\\s\\S]*<\\s*a\\s+href\\s*=\\s*\"(.*?)\"\\s*>[\\s\\S]*?次へ[\\s\\S]*?<\\s*/\\s*a\\s*>";

    private final static String NovelHeadTextToken
            = "<\\s*div\\s+id\\s*=\\s*\"\\s*novel_p\\s*\"\\s+class\\s*=\\s*\"\\s*novel_view\\s*\"\\s*>";
    private final static String NovelTextToken
            = "<\\s*div\\s+id\\s*=\\s*\"\\s*novel_honbun\"\\s+class\\s*=\\s*\"\\s*novel_view\\s*\"\\s*>";
    private final static String NovelFootTextToken
            = "<\\s*div\\s+id\\s*=\\s*\"\\s*novel_a\\s*\"\\s+class=\\s*\"\\s*novel_view\\s*\"\\s*>";
}