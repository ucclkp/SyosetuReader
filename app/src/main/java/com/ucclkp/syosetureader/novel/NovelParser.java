package com.ucclkp.syosetureader.novel;

import android.text.Html;
import android.text.SpannableStringBuilder;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;
import com.ucclkp.syosetureader.SyosetuImageGetter;
import com.ucclkp.syosetureader.SyosetuUtility;
import com.ucclkp.syosetureader.UApplication;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NovelParser extends HtmlDataPipeline<NovelParser.NovelData>
{
    private SyosetuImageGetter mImageGetter;


    public final static int NT_CHAPTER = 0;
    public final static int NT_SECTION = 1;


    public static class NovelData
    {
        public int length = 0;
        public String headTitle = "";
        public String headAuthor = "";
        public String headAuthorUrl = "";
        public String headAttention = "";
        public String novelInfoUrl = "";
        public String novelFeelUrl = "";
        public String novelReviewUrl = "";
        public SpannableStringBuilder headSummary = null;
        public ArrayList<NovelChOrSeData> chOrSeList = new ArrayList<>();
    }

    public static class NovelChOrSeData
    {
        public int type = NT_SECTION;

        public String sectionUrl = "";
        public String sectionTime = "";
        public String sectionTitle = "";

        public String chapterTitle = "";
    }


    public NovelParser(SyosetuImageGetter imageGetter)
    {
        mImageGetter = imageGetter;
    }


    @Override
    public NovelData onStartParse(RetrieveHtmlData htmldata)
    {
        NovelData data = new NovelData();
        String source = htmldata.htmlCode;

        SyosetuUtility.SyosetuSite site;
        if (htmldata.redirection)
            site = SyosetuUtility.getSiteFromNovelUrl(htmldata.location);
        else
            site = UApplication.syosetuSite;

        int topBarIndex = 0;
        Matcher novelInfoMatcher = Pattern
                .compile(NovelInfoUrlToken).matcher(source);
        if (novelInfoMatcher.find())
        {
            data.novelInfoUrl = novelInfoMatcher.group(1).trim();
            topBarIndex = novelInfoMatcher.end();
        }

        Matcher feelMatcher = Pattern
                .compile(FeelUrlToken).matcher(source);
        feelMatcher.region(topBarIndex, source.length());
        if (feelMatcher.find())
        {
            data.novelFeelUrl = feelMatcher.group(1).trim();
            topBarIndex = feelMatcher.end();
        }

        Matcher reviewMatcher = Pattern
                .compile(ReviewUrlToken).matcher(source);
        reviewMatcher.region(topBarIndex, source.length());
        if (reviewMatcher.find())
            data.novelReviewUrl = reviewMatcher.group(1).trim();

        data.headAttention = HtmlUtility.getTagContent(
                source, NovelHeadAttentionToken, "div", false);
        data.headAttention = Html.fromHtml(data.headAttention).toString().trim();

        data.headTitle = HtmlUtility.getTagContent(
                source, NovelHeadTitleToken, "p", false).trim();
        data.headTitle = Html.fromHtml(data.headTitle).toString().trim();

        data.headAuthor = HtmlUtility.getTagContent(
                source, NovelHeadAuthorToken, "div", false);
        Matcher authorMatcher = Pattern.compile(UrlToken).matcher(data.headAuthor);
        if (authorMatcher.find())
        {
            data.headAuthorUrl = authorMatcher.group(1).trim();
            data.headAuthor = Html.fromHtml(
                    authorMatcher.group(2).trim()).toString();
        }

        String summaryText = HtmlUtility.getTagContent(
                source, NovelHeadSummaryToken, "div", false).trim();
        if (!summaryText.isEmpty())
            data.headSummary = new SpannableStringBuilder(Html.fromHtml(summaryText));

        String content = HtmlUtility.getTagContent(
                source, NovelContentsToken, "div", false);
        if (!content.isEmpty())
        {
            Pattern pattern = Pattern.compile(NovelChOrSeToken);
            Matcher matcher = pattern.matcher(content);
            while (matcher.find())
            {
                String group = matcher.group(0);
                if (group.matches("^<\\s*div[\\s\\S]*"))
                {
                    int end = HtmlUtility.getTagEndIndex(
                            content, "div", matcher.end(0), false);
                    String chapterTitle = content.substring(matcher.end(0), end);

                    NovelChOrSeData cosData = new NovelChOrSeData();
                    cosData.type = NT_CHAPTER;
                    cosData.chapterTitle = chapterTitle;
                    data.chOrSeList.add(cosData);
                }
                else if (group.matches("^<\\s*dl[\\s\\S]*"))
                {
                    int end = HtmlUtility.getTagEndIndex(
                            content, "dl", matcher.end(0), false);
                    String sectionContent = content.substring(matcher.end(0), end);

                    String url = "";
                    String title = "";
                    String titleWithUrl = HtmlUtility.getTagContent(
                            sectionContent, NovelSectionTitleToken, "dd", false);
                    Pattern tuPattern = Pattern.compile(UrlToken);
                    Matcher tuMatcher = tuPattern.matcher(titleWithUrl);
                    if (tuMatcher.find())
                    {
                        url = SyosetuUtility.getNovelUrl(site) + tuMatcher.group(1).trim();
                        title = Html.fromHtml(tuMatcher.group(2).trim()).toString();
                    }

                    String time = "";
                    String editTime = "";
                    String timeContent = HtmlUtility.getTagContent(
                            sectionContent, NovelSectionTimeToken, "dt", false);
                    Pattern timePattern = Pattern.compile(NovelSectionEditTime);
                    Matcher timeMatcher = timePattern.matcher(timeContent);
                    if (timeMatcher.find())
                    {
                        time = timeContent.substring(0, timeMatcher.start(0)).trim();
                        editTime = timeMatcher.group(1).trim() + "(改)";
                    }
                    else
                        time = timeContent;

                    NovelChOrSeData cosData = new NovelChOrSeData();
                    cosData.type = NT_SECTION;
                    cosData.sectionUrl = url;
                    cosData.sectionTime = time + "  " + editTime;
                    cosData.sectionTitle = title;
                    data.chOrSeList.add(cosData);
                }
            }
        }
        else
        {
            SpannableStringBuilder headSpan = new SpannableStringBuilder("");
            String headText = HtmlUtility.getTagContent(
                    source, NovelHeadTextToken, "div", false);
            if (!headText.isEmpty())
            {
                headSpan.append(Html.fromHtml(headText, mImageGetter, null));
                headSpan.append("\n\n").append("==========").append("\n\n");
            }

            SpannableStringBuilder normalSpan = new SpannableStringBuilder("");
            String normalText = HtmlUtility.getTagContent(
                    source, NovelTextToken, "div", false);
            if (!normalText.isEmpty())
                normalSpan.append(Html.fromHtml(normalText, mImageGetter, null));

            SpannableStringBuilder footSpan = new SpannableStringBuilder("");
            String footText = HtmlUtility.getTagContent(
                    source, NovelFootTextToken, "div", false);
            if (!footText.isEmpty())
            {
                footSpan.append("\n\n").append("==========").append("\n\n");
                footSpan.append(Html.fromHtml(footText, mImageGetter, null));
            }

            data.length = normalSpan.toString().length();
            data.headSummary = headSpan
                    .append(normalSpan)
                    .append(footSpan);
        }

        return data;
    }


    private final static String NovelHeadAttentionToken
            = "<\\s*div\\s+class\\s*=\\s*\"\\s*contents1\\s*\"\\s*>";
    private final static String NovelHeadTitleToken
            = "<\\s*p\\s+class\\s*=\\s*\"\\s*novel_title\\s*\"\\s*>";
    private final static String NovelHeadAuthorToken
            = "<\\s*div\\s+class=\\s*\"\\s*novel_writername\\s*\"\\s*>";
    private final static String NovelHeadSummaryToken
            = "<\\s*div\\s+id=\\s*\"\\s*novel_ex\\s*\"\\s*>";
    private final static String NovelHeadSeriesToken
            = "<\\s*p\\s+class\\s*=\\s*\"\\s*series_title\\s*\"\\s*>";

    private final static String NovelContentsToken
            = "<\\s*div\\s+class=\\s*\"\\s*index_box\\s*\"\\s*>";
    private final static String NovelChOrSeToken
            = "<\\s*div\\s+class=\\s*\"\\s*chapter_title\\s*\"\\s*>|<\\s*dl\\s+class\\s*=\\s*\"\\s*novel_sublist2\\s*\"\\s*>";
    private final static String NovelSectionTitleToken
            = "<\\s*dd\\s+class\\s*=\\s*\"\\s*subtitle\\s*\"\\s*>";
    private final static String NovelSectionTimeToken
            = "<\\s*dt\\s+class=\\s*\"\\s*long_update\\s*\"\\s*>";

    private final static String NovelHeadTextToken
            = "<\\s*div\\s+id\\s*=\\s*\"\\s*novel_p\\s*\"\\s+class\\s*=\\s*\"\\s*novel_view\\s*\"\\s*>";
    private final static String NovelTextToken
            = "<\\s*div\\s+id\\s*=\\s*\"\\s*novel_honbun\"\\s+class\\s*=\\s*\"\\s*novel_view\\s*\"\\s*>";
    private final static String NovelFootTextToken
            = "<\\s*div\\s+id\\s*=\\s*\"\\s*novel_a\\s*\"\\s+class=\\s*\"\\s*novel_view\\s*\"\\s*>";

    private final static String UrlToken
            = "<\\s*a[\\s\\S]*?href\\s*=\\s*\"(.*?)\"[\\s\\S]*?>(.*?)<\\s*/\\s*a\\s*>";
    private final static String NovelSectionEditTime
            = "<\\s*span\\s+title\\s*=\\s*\"(.*?)改稿\\s*\"\\s*>";

    private final static String NovelInfoUrlToken
            = "<\\s*li\\s*><\\s*a\\s+href\\s*=\\s*\"(.*?)\"\\s*>小説情報<\\s*/\\s*a\\s*><\\s*/\\s*li\\s*>";
    private final static String FeelUrlToken
            = "<\\s*li\\s*><\\s*a\\s+href\\s*=\\s*\"(.*?)\"\\s*>感想<\\s*/\\s*a\\s*><\\s*/\\s*li\\s*>";
    private final static String ReviewUrlToken
            = "<\\s*li\\s*><\\s*a\\s+href\\s*=\\s*\"(.*?)\"\\s*>レビュー<\\s*/\\s*a\\s*><\\s*/\\s*li\\s*>";
}