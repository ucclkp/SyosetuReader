package com.ucclkp.syosetureader.novel;

import android.text.Html;
import android.text.SpannableStringBuilder;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;
import com.ucclkp.syosetureader.SyosetuImageGetter;
import com.ucclkp.syosetureader.SyosetuUtility;
import com.ucclkp.syosetureader.UApplication;

import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * 小说章节列表页面/短篇页面解析
 */
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
        data.headSummary = new SpannableStringBuilder();

        String source = htmlData.htmlCode;
        try {
            Document doc = Jsoup.parse(source);
            Elements top_bar_elements = doc.select("header[class=c-menu l-scrollheader js-scrollheader] > nav[class=c-menu__body] > a");
            // 作品情報
            data.novelInfoUrl = top_bar_elements.size() > 1 ? top_bar_elements.get(1).attr("href") : "";

            // 感想
            data.novelFeelUrl = top_bar_elements.size() > 2 ? top_bar_elements.get(2).attr("href") : "";

            // レビュー
            data.novelReviewUrl = top_bar_elements.size() > 3 ? top_bar_elements.get(3).attr("href") : "";

            // 顶部警告/注意栏位
            StringBuilder annoc_str_builder = new StringBuilder();
            Elements annocs = doc.select("div[class=l-container] > main[class=l-main] > div[class=c-announce-box] > div");
            for (Element annoc : annocs) {
                if (annoc.attr("class").equals("c-announce")) {
                    annoc_str_builder.append(Html.fromHtml(annoc.toString(), Html.FROM_HTML_MODE_LEGACY));
                }
            }
            data.headAttention = annoc_str_builder.toString();

            Element ep_list_ele = null;
            Element body_ele = null;

            // 小说标题
            Elements title_eles = doc.select("div[class=l-container] > main[class=l-main] > article[class=p-novel] > h1");
            Element title_ele = title_eles.first();
            if (title_ele != null) {
                data.headTitle = title_ele.text().trim();
            }

            Elements content_eles = doc.select("div[class=l-container] > main[class=l-main] > article[class=p-novel] > div");
            for (Element content_ele : content_eles) {
                // 作者
                if (content_ele.attr("class").equals("p-novel__author")) {
                    data.headAuthor = content_ele.text().trim();
                    Element author_url_ele = content_ele.selectFirst("a");
                    if (author_url_ele != null) {
                        data.headAuthor = author_url_ele.text().trim();
                        data.headAuthorUrl = author_url_ele.attr("href");
                    }
                }

                // 小说简介
                if (content_ele.attr("class").equals("p-novel__summary")) {
                    data.headSummary = new SpannableStringBuilder(Html.fromHtml(content_ele.toString(), Html.FROM_HTML_MODE_LEGACY));
                }

                // 小说章节列表（连载小说）
                if (content_ele.attr("class").equals("p-eplist")) {
                    ep_list_ele = content_ele;
                }
                // 小说正文（短篇小说）
                if (content_ele.attr("class").equals("p-novel__body")) {
                    body_ele = content_ele;
                }
            }

            if (ep_list_ele != null) {
                // 小说章节列表（连载小说）
                for (Element ep_ele : ep_list_ele.children()) {
                    if (ep_ele.attr("class").equals("p-eplist__chapter-title")) {
                        // 章标题
                        NovelChOrSeData cosData = new NovelChOrSeData();
                        cosData.type = NT_CHAPTER;
                        cosData.chapterTitle = ep_ele.text();
                        data.chOrSeList.add(cosData);
                    } else if (ep_ele.attr("class").equals("p-eplist__sublist")) {
                        // 节
                        NovelChOrSeData cosData = new NovelChOrSeData();
                        cosData.type = NT_SECTION;

                        for (Element ep_item_ele : ep_ele.children()) {
                            // 节标题
                            if (ep_item_ele.attr("class").equals("p-eplist__subtitle")) {
                                cosData.sectionUrl = getFullURL(htmlData, ep_item_ele.attr("href"));
                                cosData.sectionTitle = ep_item_ele.text();

                            }
                            // 节时间
                            if (ep_item_ele.attr("class").equals("p-eplist__update")) {
                                cosData.sectionTime = ep_item_ele.ownText();
                                Element mod_ele = ep_item_ele.selectFirst("span");
                                if (mod_ele != null) {
                                    String mod_title = mod_ele.attr("title");
                                    if (mod_title.isEmpty()) {
                                        cosData.sectionTime += "（改）";
                                    } else {
                                        cosData.sectionTime += "（" + mod_title.substring(0, mod_title.length() - 1) + "）";
                                    }
                                }
                            }
                        }

                        data.chOrSeList.add(cosData);
                    }
                }
            } else if (body_ele != null) {
                // 小说正文（短篇小说）
                SpannableStringBuilder headSpan = new SpannableStringBuilder();
                SpannableStringBuilder normalSpan = new SpannableStringBuilder();
                SpannableStringBuilder footSpan = new SpannableStringBuilder();

                Elements sen_eles = body_ele.children();
                for (Element e : sen_eles) {
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
                data.length = SyosetuUtility.getCharCount(normalSpan.toString());
                data.headSummary = headSpan
                        .append(normalSpan)
                        .append(footSpan);
            }
        } catch (Exception e) {
            return null;
        }

        return data;
    }

}