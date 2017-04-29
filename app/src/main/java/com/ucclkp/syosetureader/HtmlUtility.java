package com.ucclkp.syosetureader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.AlignmentSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlUtility
{
    public static int getTagEndIndex(
            String source, String tagName, int startIndex, boolean includeEndTag)
    {
        int acc = 1;

        Pattern pattern = Pattern.compile(
                "(<\\s*" + tagName
                        + "[^>]*?/\\s*>|<\\s*"
                        + tagName
                        + "|<\\s*/\\s*"
                        + tagName + "\\s*>)");

        Matcher matcher = pattern.matcher(source);
        matcher = matcher.region(startIndex, source.length());
        while (matcher.find())
        {
            String mt = matcher.group(1);
            mt = mt.replaceAll("\\s", "");
            if (mt.equals("<" + tagName))
            {
                ++acc;
            } else if (mt.equals("</" + tagName + ">"))
            {
                --acc;
                if (acc == 0)
                {
                    return includeEndTag ? matcher.end(1) : matcher.start(1);
                }
            }
        }

        return -1;
    }

    public static String getTagContent(
            String source, String tagTokenRegex, String tagName, boolean includeMatchTag)
    {
        return getTagContent(source, 0, tagTokenRegex, tagName, includeMatchTag);
    }

    public static String getTagContent(
            String source, int startIndex, String tagTokenRegex, String tagName, boolean includeMatchTag)
    {
        return getTagContent(source, startIndex, tagTokenRegex,
                tagName, includeMatchTag, null);
    }

    public static String getTagContent(
            String source, int startIndex, String tagTokenRegex,
            String tagName, boolean includeMatchTag, int[] position)
    {
        String content = "";

        Pattern pattern = Pattern.compile(tagTokenRegex);
        Matcher matcher = pattern.matcher(source);
        matcher = matcher.region(startIndex, source.length());
        if (matcher.find())
        {
            int tagStartIndex = includeMatchTag ? matcher.start() : matcher.end();
            int tagEndIndex = getTagEndIndex(source, tagName, matcher.end(), includeMatchTag);
            if (tagEndIndex >= 0)
            {
                if (position != null)
                {
                    position[0] = tagStartIndex;
                    position[1] = tagEndIndex;
                }
                content = source.substring(tagStartIndex, tagEndIndex);
            }
        }

        return content;
    }


    public static String getUrlRear(String url)
    {
        String rear;
        if (url.endsWith("/"))
        {
            int startIndex = url.lastIndexOf("/", url.length() - 2);
            rear = url.substring(startIndex + 1, url.length() - 1);
        } else
        {
            int startIndex = url.lastIndexOf("/");
            rear = url.substring(startIndex + 1);
        }

        return rear;
    }


    public static SpannableStringBuilder processTitle(String title)
    {
        SpannableStringBuilder text
                = new SpannableStringBuilder(title);
        text.setSpan(new RelativeSizeSpan(1.3f),
                0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new StyleSpan(Typeface.BOLD),
                0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER),
                0, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        return text;
    }


    public static String decodeHtmlUnicode(String unicodeStr)
    {
        if (unicodeStr == null)
            return null;

        StringBuilder retBuf = new StringBuilder();
        int maxLoop = unicodeStr.length();
        for (int i = 0; i < maxLoop; i++)
        {
            if (unicodeStr.charAt(i) == '\\')
            {
                if ((i < maxLoop - 5)
                        && ((unicodeStr.charAt(i + 1) == 'u') || (unicodeStr
                        .charAt(i + 1) == 'U')))
                    try
                    {
                        retBuf.append((char) Integer.parseInt(
                                unicodeStr.substring(i + 2, i + 6), 16));
                        i += 5;
                    } catch (NumberFormatException localNumberFormatException)
                    {
                        retBuf.append(unicodeStr.charAt(i));
                    }
                else
                    retBuf.append(unicodeStr.charAt(i));
            } else
            {
                retBuf.append(unicodeStr.charAt(i));
            }
        }
        return retBuf.toString();
    }

    public static String decodeUrl(String url)
    {
        String decodedUrl = "";
        try
        {
            decodedUrl = URLDecoder.decode(url, "utf-8");
        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        return decodedUrl;
    }

    public static String encodeUrl(String url)
    {
        String encodedUrl = "";
        try
        {
            encodedUrl = URLEncoder.encode(url, "utf-8");
        } catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }

        return encodedUrl;
    }


    public static Bitmap getBitmapFromUrl(String imgUrl)
    {
        URL url;
        InputStream in = null;
        HttpURLConnection connection = null;

        try
        {
            url = new URL(imgUrl);

            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            connection.setRequestMethod("GET");
            connection.connect();

            in = connection.getInputStream();
            return BitmapFactory.decodeStream(in);
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        } finally
        {
            if (connection != null)
                connection.disconnect();

            try
            {
                if (in != null)
                    in.close();
            } catch (IOException e1)
            {
                e1.printStackTrace();
            }
        }
    }


    public static int intValue(String intStr, int def)
    {
        int result;

        try
        {
            result = Integer.valueOf(intStr);
        } catch (NumberFormatException e)
        {
            result = def;
        }

        return result;
    }

    public static boolean boolValue(String boolStr, boolean def)
    {
        if (boolStr.length() > 0)
        {
            if (boolStr.equals("0") || boolStr.equals("false"))
                return false;
            else if (boolStr.equals("1") || boolStr.equals("true"))
                return true;
        }

        return def;
    }

    public static String removeLB(String content)
    {
        int index = content.length() - 1;
        while (index >= 0 && content.charAt(index) == '\n')
            --index;

        if (index < content.length() - 1)
            return content.substring(index + 1);
        else
            return content;
    }

    public static void removeLB(SpannableStringBuilder content)
    {
        int index = content.length() - 1;
        while (index >= 0 && content.charAt(index) == '\n')
            --index;

        if (index < content.length() - 1)
            content.delete(index + 1, content.length());
    }
}
