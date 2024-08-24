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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

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
    public NovelData onStartParse(RetrieveHtmlData htmlData)
    {
        NovelData data = new NovelData();
        String source = htmlData.htmlCode;

        SyosetuUtility.SyosetuSite site;
        if (htmlData.redirection)
            site = SyosetuUtility.getSiteFromNovelUrl(htmlData.location);
        else
            site = UApplication.syosetuSite;

        try {
            Document doc = Jsoup.parse(source);
            Elements top_bar_elements = doc.select("div[id=novel_header] > ul[id=head_nav] > li");
            // 作品情報
            if (top_bar_elements.size() > 1) {
                Element el = top_bar_elements.get(1).selectFirst("a");
                if (el != null) {
                    data.novelInfoUrl = el.attr("href").trim();
                }
            }

            // 感想
            if (top_bar_elements.size() > 2) {
                Element el = top_bar_elements.get(2).selectFirst("a");
                if (el != null) {
                    data.novelFeelUrl = el.attr("href").trim();
                }
            }

            // レビュー
            if (top_bar_elements.size() > 3) {
                Element el = top_bar_elements.get(3).selectFirst("a");
                if (el != null) {
                    data.novelReviewUrl = el.attr("href").trim();
                }
            }
        } catch (Exception e) {
            return null;
        }

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
                } else if (group.matches("^<\\s*dl[\\s\\S]*"))
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
                    } else
                        time = timeContent;

                    NovelChOrSeData cosData = new NovelChOrSeData();
                    cosData.type = NT_SECTION;
                    cosData.sectionUrl = url;
                    cosData.sectionTime = time + "  " + editTime;
                    cosData.sectionTitle = title;
                    data.chOrSeList.add(cosData);
                }
            }
        } else
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

            data.length = SyosetuUtility.getCharCount(normalSpan.toString());
            data.headSummary = headSpan
                    .append(normalSpan)
                    .append(footSpan);
        }

        return data;
    }


    private final static String NovelHeadAttentionToken
            = "<div\\s+class=\"contents1\"\\s*>";
    private final static String NovelHeadTitleToken
            = "<p\\s+class=\"novel_title\"\\s*>";
    private final static String NovelHeadAuthorToken
            = "<div\\s+class=\"novel_writername\"\\s*>";
    private final static String NovelHeadSummaryToken
            = "<div\\s+id=\"novel_ex\"\\s*>";
    private final static String NovelHeadSeriesToken
            = "<p\\s+class=\"series_title\"\\s*>";

    private final static String NovelContentsToken
            = "<div\\s+class=\"index_box\">";
    private final static String NovelChOrSeToken
            = "<div\\s+class=\"chapter_title\"\\s*>|<dl\\s+class=\"novel_sublist2\"\\s*>";
    private final static String NovelSectionTitleToken
            = "<dd\\s+class=\"subtitle\"\\s*>";
    private final static String NovelSectionTimeToken
            = "<dt\\s+class=\"long_update\"\\s*>";

    private final static String NovelHeadTextToken
            = "<div\\s+id=\"novel_p\"\\s+class=\"novel_view\"\\s*>";
    private final static String NovelTextToken
            = "<div\\s+id=\"novel_honbun\"\\s+class=\"novel_view\"\\s*>";
    private final static String NovelFootTextToken
            = "<div\\s+id=\"novel_a\"\\s+class=\"novel_view\"\\s*>";

    private final static String UrlToken
            = "<a[\\s\\S]*?href=\"(.*?)\"[\\s\\S]*?>(.*?)</a>";
    private final static String NovelSectionEditTime
            = "<span\\s+title=\"(.*?)改稿\"\\s*>";
}