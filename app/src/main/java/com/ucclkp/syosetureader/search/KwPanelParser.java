package com.ucclkp.syosetureader.search;

import android.content.Context;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KwPanelParser extends HtmlDataPipeline<KwPanelParser.KeywordData> {
    private Context mContext;

    public class KeywordData {
        ArrayList<KwAtom> stockList = new ArrayList<>();
        ArrayList<KwAtom> recommendList = new ArrayList<>();
        ArrayList<KwAtom> replayList = new ArrayList<>();
    }

    public class KwAtom {
        String title;
        String value;
        String keyword;

        int type = KT_KEYWORD;
    }


    public final static int KT_TITLE = 0;
    public final static int KT_KEYWORD = 1;


    public KwPanelParser(Context context) {
        mContext = context;
    }


    @Override
    public KeywordData onStartParse(RetrieveHtmlData htmlData) {
        String source = htmlData.htmlCode;
        KeywordData data = new KeywordData();

        String stockContent = HtmlUtility.getTagContent(
                source, KeywordStockToken, "div", false);
        if (!stockContent.isEmpty()) {
            parseKeywordList(stockContent, data.stockList);
        }

        String recommendContent = HtmlUtility.getTagContent(
                source, KeywordRecommendToken, "div", false);
        if (!recommendContent.isEmpty()) {
            parseKeywordList(recommendContent, data.recommendList);
        }

        String replayContent = HtmlUtility.getTagContent(
                source, KeywordReplayToken, "div", false);
        if (!replayContent.isEmpty()) {
            parseKeywordList(replayContent, data.replayList);
        }

        return data;
    }


    private void parseKeywordList(String contentSource, List<KwAtom> dataList) {
        ListParser lp = new ListParser();
        lp.set(contentSource, "tr");
        while (lp.find()) {
            String typeContent = lp.getContent(false);

            KwAtom atom = new KwAtom();
            atom.type = KT_TITLE;
            atom.title = HtmlUtility.getTagContent(typeContent, "th", false);
            atom.title = atom.title.replaceAll("<br />", "");
            dataList.add(atom);

            ListParser word_lp = new ListParser();
            word_lp.set(typeContent, "li");
            while (word_lp.find()) {
                String wordContent = word_lp.getContent(false);

                Pattern pattern = Pattern.compile(KeywordValue);
                Matcher matcher = pattern.matcher(wordContent);
                if (matcher.find()) {
                    KwAtom word_atom = new KwAtom();
                    word_atom.type = KT_KEYWORD;
                    word_atom.keyword = HtmlUtility.getTagContent(wordContent, "span", false);
                    word_atom.value = matcher.group(1);
                    dataList.add(word_atom);
                }
            }
        }
    }


    private final static String KeywordStockToken
            = "<div[\\S\\s]*?id=\"keywordBox1\"[\\S\\s]*?>";
    private final static String KeywordRecommendToken
            = "<div[\\S\\s]*?id=\"keywordBox2\"[\\S\\s]*?>";
    private final static String KeywordReplayToken
            = "<div[\\S\\s]*?id=\"keywordBox3\"[\\S\\s]*?>";

    private final static String KeywordValue
            = "<input[\\S\\s]*?value=\"(.*?)\"[\\S\\s]*?/>";
}