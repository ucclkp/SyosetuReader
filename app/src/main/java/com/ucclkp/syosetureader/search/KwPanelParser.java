package com.ucclkp.syosetureader.search;

import android.content.Context;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KwPanelParser extends HtmlDataPipeline<KwPanelParser.KeywordData>
{
    private Context mContext;

    public class KeywordData
    {
        ArrayList<KwAtom> stockList = new ArrayList<>();
        ArrayList<KwAtom> recommandList = new ArrayList<>();
        ArrayList<KwAtom> replayList = new ArrayList<>();
    }

    public class KwAtom
    {
        String title;

        String value;
        String keyword;

        int type = KT_KEYWORD;
    }


    public final static int KT_TITLE = 0;
    public final static int KT_KEYWORD = 1;


    public KwPanelParser(Context context)
    {
        mContext = context;
    }


    @Override
    public KeywordData onStartParse(RetrieveHtmlData htmldata)
    {
        String source = htmldata.htmlCode;
        KeywordData data = new KeywordData();

        String stockContent = HtmlUtility.getTagContent(
                source, KeywordStockToken, "div", false);
        if (!stockContent.isEmpty())
        {
            parseKeywordList(stockContent, data.stockList);
        }

        String recommandContent = HtmlUtility.getTagContent(
                source, KeywordRecommandToken, "div", false);
        if (!recommandContent.isEmpty())
        {
            parseKeywordList(recommandContent, data.recommandList);
        }

        String replayContent = HtmlUtility.getTagContent(
                source, KeywordReplayToken, "div", false);
        if (!replayContent.isEmpty())
        {
            parseKeywordList(replayContent, data.replayList);
        }

        return data;
    }


    private void parseKeywordList(String contentSource, List<KwAtom> dataList)
    {
        Pattern pattern = Pattern.compile(KeywordTitle + "|" + KeywordSelf);
        Matcher matcher = pattern.matcher(contentSource);
        while (matcher.find())
        {
            String hitted = matcher.group(0).trim();
            if (hitted.matches("^<\\s*h[\\s\\S]*?"))
            {
                KwAtom atom = new KwAtom();
                atom.type = KT_TITLE;
                atom.title = matcher.group(1).trim();

                dataList.add(atom);
            } else if (hitted.matches("^<\\s*span[\\s\\S]*?"))
            {
                KwAtom atom = new KwAtom();
                atom.type = KT_KEYWORD;
                atom.value = matcher.group(2);
                atom.keyword = matcher.group(3);

                dataList.add(atom);
            }
        }
    }


    private final static String KeywordStockToken
            = "<\\s*div\\s+id\\s*=\\s*\"\\s*keywordBox1\\s*\""
            + "\\s+class\\s*=\\s*\"\\s*keyword_box\\s*\""
            + "\\s+data-id\\s*=\\s*\"\\s*1\\s*\"\\s*>";
    private final static String KeywordRecommandToken
            = "<\\s*div\\s+id\\s*=\\s*\"\\s*keywordBox2\\s*\""
            + "\\s+class\\s*=\\s*\"\\s*keyword_box\\s*\""
            + "\\s+data-id\\s*=\\s*\"\\s*2\\s*\"\\s*>";
    private final static String KeywordReplayToken
            = "<\\s*div\\s+id\\s*=\\s*\"\\s*keywordBox3\\s*\""
            + "\\s+class\\s*=\\s*\"\\s*keyword_box\\s*\""
            + "\\s+data-id\\s*=\\s*\"\\s*3\\s*\"\\s*>";

    private final static String KeywordTitle
            = "<\\s*h[\\d]+?\\s*>(.*?)<\\s*/\\s*h[\\d]+?\\s*>";
    private final static String KeywordSelf
            = "<\\s*span\\s+class\\s*=[\\s\\S]*?>\\s*"
            + "<\\s*label\\s*>\\s*"
            + "<\\s*input\\s+type=\\s*\"\\s*checkbox\\s*\" value=\"(.*?)\" class\\s*=\\s*\"\\s*norimono\\s*\""
            + "\\s+data-type\\s*=\\s*\"\\s*word\\s*\"\\s*/\\s*>(.*?)<\\s*/\\s*label\\s*>\\s*<\\s*/\\s*span\\s*>";
}