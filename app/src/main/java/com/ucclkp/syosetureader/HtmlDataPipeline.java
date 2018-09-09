package com.ucclkp.syosetureader;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

/**
 * 获取HTML数据的流水线。
 *
 * @param <Parsed> 由 {@link #onStartParse(RetrieveHtmlData)} 返回的数据类型。
 */
public abstract class HtmlDataPipeline<Parsed> {
    private int mExitCode;
    private PipelineWorker mWorker;
    private RetrieveHtmlData mHtmlData;
    private OnPipelineListener<Parsed> mPipelineListener;


    public final static int CODE_SUCCESS = 0;
    public final static int CODE_FETCH_FAILED = 1;
    public final static int CODE_PARSE_FAILED = 3;

    public final static int HTTP_ERROR_NONE = 0;
    public final static int HTTP_ERROR_OTHER = 1;
    public final static int HTTP_ERROR_IO = 2;
    public final static int HTTP_ERROR_ILLEGAL_URL = 3;


    public static class RetrieveHtmlData {
        public int code = HTTP_ERROR_NONE;
        public boolean redirection = false;

        public String url = "";
        public String location = "";
        public String htmlCode = "";
        public String htmlErrorMsg = "";
        public Map<String, List<String>> requestHeaders = new HashMap<>();
        public Map<String, List<String>> responseHeaders = new HashMap<>();
    }


    public HtmlDataPipeline() {
        mExitCode = CODE_SUCCESS;
    }


    /**
     * 当准备开始管线时被调用。运行于<b>UI线程</b>中。
     * 是所有工作的开始，第一个被调用。
     */
    public void onStartPipeline() {
    }

    /**
     * 当管线结束时被调用。运行于<b>UI线程</b>中。
     * 是所有工作的结束，最后一个被调用。
     */
    public void onFinishPipeline() {
    }


    /**
     * 当准备获取Html数据时被调用。运行于<b>工作线程</b>中。
     * 是工作线程的第一个任务，在 {@link #onStartPipeline()} 之后调用。
     * 开发者必须在此方法中设置连接参数。
     *
     * @param requestedUrl 提交给工作线程的url参数。
     * @param connection   负责获取数据的 Connection。
     */
    public void onStartFetch(
            String requestedUrl,
            HttpsURLConnection connection) {
    }

    /**
     * 当获取Html数据结束时被调用，准备解析数据。运行于<b>工作线程</b>中。
     * 是工作线程的第一个任务，在 {@link #onStartPipeline()} 之后调用。
     * 若前一个工作出现错误 ，则此方法不会被调用。
     *
     * @param htmldata 获取到的Html数据。
     * @return 解析后的数据。
     */
    public abstract Parsed onStartParse(RetrieveHtmlData htmldata);

    /**
     * 当工作线程返回后被调用，该方法必定会被调用。运行于<b>UI线程</b>中。
     *
     * @param exitCode 包含返回信息的返回代码。
     */
    public void onPostData(int exitCode, Parsed data) {
        if (mPipelineListener != null)
            mPipelineListener.onPostData(exitCode, data);
    }


    public boolean enter(String url) {
        if (mWorker != null) {
            if (mWorker.isFinished())
                mWorker = null;
            else
                mWorker.cancel(false);
        }

        mHtmlData = null;
        mExitCode = CODE_SUCCESS;

        mWorker = new PipelineWorker();
        mWorker.execute(url);

        return true;
    }

    public void cancel() {
        if (mWorker != null) {
            if (mWorker.isFinished())
                mWorker = null;
            else
                mWorker.cancel(false);
        }
    }

    public Parsed enterSync(String url) {
        if (mWorker != null && !mWorker.isFinished())
            return null;

        mHtmlData = null;
        mExitCode = CODE_SUCCESS;

        HtmlDataPipeline.this.onStartPipeline();

        URL contentUrl;
        InputStream in = null;
        HttpsURLConnection connection = null;
        RetrieveHtmlData data = new RetrieveHtmlData();

        data.url = toHttps(url);

        try {
            contentUrl = new URL(data.url);
            connection = (HttpsURLConnection) contentUrl.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(false);

            HtmlDataPipeline.this.onStartFetch(data.url, connection);

            data.requestHeaders.putAll(connection.getRequestProperties());

            in = connection.getInputStream();

            data.responseHeaders.putAll(connection.getHeaderFields());
            data.htmlCode = readInputStream(in, "utf-8");

            String originUrl = contentUrl.toString();
            String redirectUrl = connection.getURL().toString();
            if (!originUrl.equals(redirectUrl)) {
                data.redirection = true;
                data.location = redirectUrl;
            }
        } catch (MalformedURLException me) {
            data.code = HTTP_ERROR_ILLEGAL_URL;
            data.htmlErrorMsg = me.getMessage();
        } catch (IOException ioe) {
            data.code = HTTP_ERROR_IO;
            data.htmlErrorMsg = ioe.getMessage();
        } catch (Exception e) {
            data.code = HTTP_ERROR_OTHER;
            data.htmlErrorMsg = e.getMessage();
        } finally {
            if (connection != null)
                connection.disconnect();

            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    data.code = HTTP_ERROR_IO;
                    data.htmlErrorMsg = e1.getMessage();
                }
            }
        }

        mHtmlData = data;

        Parsed pData;
        if (data.code != HTTP_ERROR_NONE) {
            mExitCode = CODE_FETCH_FAILED;
            pData = null;
        } else {
            pData = HtmlDataPipeline.this.onStartParse(data);
            if (pData == null)
                mExitCode = CODE_PARSE_FAILED;
        }

        return pData;
    }

    public void setPipelineListener(OnPipelineListener<Parsed> l) {
        mPipelineListener = l;
    }


    public boolean isInPipeline() {
        return !mWorker.isFinished();
    }

    public RetrieveHtmlData getHtmlData() {
        return mHtmlData;
    }


    private class PipelineWorker extends AsyncTask<String, Integer, Parsed> {
        private boolean mIsFinished = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mIsFinished = false;
            if (!isCancelled())
                HtmlDataPipeline.this.onStartPipeline();
        }

        @Override
        protected Parsed doInBackground(String... params) {
            InputStream in = null;
            HttpsURLConnection connection = null;
            RetrieveHtmlData data = new RetrieveHtmlData();

            data.url = toHttps(params[0]);

            try {
                URL url = new URL(data.url);

                connection = (HttpsURLConnection) url.openConnection();
                connection.setConnectTimeout(10 * 1000);
                connection.setReadTimeout(10 * 1000);
                connection.setRequestMethod("GET");

                if (isCancelled()) return null;

                HtmlDataPipeline.this.onStartFetch(data.url, connection);

                data.requestHeaders.putAll(connection.getRequestProperties());

                in = connection.getInputStream();

                data.responseHeaders.putAll(connection.getHeaderFields());
                data.htmlCode = readInputStream(in, "utf-8");

                String originUrl = url.toString();
                String redirectUrl = connection.getURL().toString();
                if (!originUrl.equals(redirectUrl)) {
                    data.redirection = true;
                    data.location = redirectUrl;
                }
            } catch (MalformedURLException me) {
                data.code = HTTP_ERROR_ILLEGAL_URL;
                data.htmlErrorMsg = me.getMessage();
            } catch (IOException ioe) {
                data.code = HTTP_ERROR_IO;
                data.htmlErrorMsg = ioe.getMessage();
            } catch (Exception e) {
                data.code = HTTP_ERROR_OTHER;
                data.htmlErrorMsg = e.getMessage();
            } finally {
                if (connection != null)
                    connection.disconnect();

                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e1) {
                        data.code = HTTP_ERROR_IO;
                        data.htmlErrorMsg = e1.getMessage();
                    }
                }
            }

            mHtmlData = data;

            if (data.code != HTTP_ERROR_NONE) {
                mExitCode = CODE_FETCH_FAILED;
                return null;
            }

            if (isCancelled()) return null;

            Parsed pData = HtmlDataPipeline.this.onStartParse(data);
            if (pData == null) {
                mExitCode = CODE_PARSE_FAILED;
                return null;
            }

            return pData;
        }

        @Override
        protected void onPostExecute(Parsed data) {
            super.onPostExecute(data);

            HtmlDataPipeline.this.onPostData(mExitCode, data);
            HtmlDataPipeline.this.onFinishPipeline();

            mIsFinished = true;
        }

        @Override
        protected void onCancelled(Parsed parsed) {
            super.onCancelled(parsed);

            mIsFinished = true;
        }

        public boolean isFinished() {
            return mIsFinished;
        }
    }


    private String readInputStream(InputStream in, String charset) throws Exception {
        String inputLine;
        String resultData = "";
        InputStreamReader isr = new InputStreamReader(in, charset);
        BufferedReader bufferReader = new BufferedReader(isr);

        while ((inputLine = bufferReader.readLine()) != null)
            resultData += inputLine;

        return resultData;
    }

    private String toHttps(String url) {
        if (url.startsWith("http://"))
            url = url.replace("http://", "https://");
        return url;
    }


    public static class ListParser {
        private String mSource;
        private String mSplitTagName;

        private Matcher mMatcher;


        public void set(String source, String splitTagName) {
            String splitRegex = "<\\s*" + splitTagName + "\\s*>";

            mSource = source;
            mSplitTagName = splitTagName;
            mMatcher = Pattern.compile(splitRegex).matcher(source);
        }

        public void set(String source, String splitRegex, String splitTagName) {
            mSource = source;
            mSplitTagName = splitTagName;
            mMatcher = Pattern.compile(splitRegex).matcher(source);
        }

        public boolean find() {
            return mMatcher.find();
        }

        public void reset() {
            mMatcher.reset();
        }

        public String group(int group) {
            String result;

            try {
                result = mMatcher.group(group);
            } catch (Exception e) {
                return "";
            }

            return result;
        }

        public String getContent(boolean includeMatchTag) {
            int start;

            try {
                if (includeMatchTag)
                    start = mMatcher.start();
                else
                    start = mMatcher.end();
            } catch (Exception e) {
                return "";
            }

            int end = HtmlUtility.getTagEndIndex(mSource, mSplitTagName, mMatcher.end(), includeMatchTag);

            return mSource.substring(start, end);
        }
    }


    public interface OnPipelineListener<Parsed> {
        void onPostData(int exitCode, Parsed data);
    }
}