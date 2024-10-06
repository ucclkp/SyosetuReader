package com.ucclkp.syosetureader.novel;

import android.text.Html;
import android.text.SpannableStringBuilder;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.SyosetuImageGetter;
import com.ucclkp.syosetureader.SyosetuUtility;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 连载小说每节页面解析。
 */
public class NovelSectionParser extends HtmlDataPipeline<NovelSectionParser.SectionData>
{
    private final SyosetuImageGetter mImageGetter;


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
        data.sectionContent = new SpannableStringBuilder();

        String source = htmldata.htmlCode;
        try {
            Document doc = Jsoup.parse(source);
            Elements content_elements = doc.select("div[class=l-container] > main[class=l-main] > article[class=p-novel] > div");
            for (Element content_ele : content_elements) {
                // 前进/后退
                if (content_ele.attr("class").equals("c-pager c-pager--center")) {
                    for (Element pager_ele : content_ele.children()) {
                        if (pager_ele.attr("class").equals("c-pager__item c-pager__item--before")) {
                            data.prevUrl = getFullURL(htmldata, pager_ele.attr("href"));
                        } else if (pager_ele.attr("class").equals("c-pager__item c-pager__item--next")) {
                            data.nextUrl = getFullURL(htmldata, pager_ele.attr("href"));
                        }
                    }
                }

                // 页码
                if (content_ele.attr("class").equals("p-novel__number js-siori")) {
                    data.number = content_ele.text().trim();
                }

                // 正文
                if (content_ele.attr("class").equals("p-novel__body")) {
                    SpannableStringBuilder headSpan = new SpannableStringBuilder();
                    SpannableStringBuilder normalSpan = new SpannableStringBuilder();
                    SpannableStringBuilder footSpan = new SpannableStringBuilder();

                    for (Element e : content_ele.children()) {
                        if (e.attr("class").equals("js-novel-text p-novel__text p-novel__text--preface")) {
                            // 前言
                            headSpan.append(Html.fromHtml(e.toString(), Html.FROM_HTML_MODE_LEGACY, mImageGetter, null));
                            headSpan.append("\n\n").append("==========").append("\n\n");
                        } else if (e.attr("class").equals("js-novel-text p-novel__text p-novel__text--afterword")) {
                            // 后记
                            footSpan.append("\n\n").append("==========").append("\n\n");
                            footSpan.append(Html.fromHtml(e.toString(), Html.FROM_HTML_MODE_LEGACY, mImageGetter, null));
                        } else {
                            // 正文
                            normalSpan.append(Html.fromHtml(e.toString(), Html.FROM_HTML_MODE_LEGACY, mImageGetter, null));
                        }
                    }

                    headSpan.append(normalSpan).append(footSpan);

                    data.sectionContent = headSpan;
                    data.length = SyosetuUtility.getCharCount(normalSpan.toString());
                }
            }

            // 标题
            Element title_element = doc.selectFirst("div[class=l-container] > main[class=l-main] > article[class=p-novel] > h1");
            if (title_element != null) {
                data.title = Html.fromHtml(title_element.text(), Html.FROM_HTML_MODE_LEGACY).toString();
            }
        } catch (Exception e) {
            return null;
        }

        return data;
    }

}