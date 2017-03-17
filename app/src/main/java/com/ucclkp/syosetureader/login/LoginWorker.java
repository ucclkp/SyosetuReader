package com.ucclkp.syosetureader.login;


import android.os.AsyncTask;
import android.text.Html;
import android.text.TextUtils;

import com.ucclkp.syosetureader.HtmlUtility;
import com.ucclkp.syosetureader.UApplication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

class LoginWorker
{
    private String mUserName;
    private String mPassword;
    private UserLoginTask mLoginTask;
    private OnLoginEventListener mListener;


    void login(String userName, String password)
    {
        mUserName = userName;
        mPassword = password;

        if (mLoginTask == null)
        {
            mLoginTask = new UserLoginTask();
            mLoginTask.execute();
        }
    }

    void cancel()
    {
        if (mLoginTask != null)
            mLoginTask.cancel(false);
    }

    void setOnLoginEventListener(OnLoginEventListener l)
    {
        mListener = l;
    }


    private class UserLoginTask extends AsyncTask<Void, Void, Boolean>
    {
        private String mErrorMessage = "";

        @Override
        protected Boolean doInBackground(Void... params)
        {
            URL url;
            InputStream in = null;
            OutputStream out = null;
            HttpsURLConnection connection = null;

            boolean succeed = true;
            String htmlCode = null;

            try
            {
                url = new URL("https://ssl.syosetu.com/login/login");
                connection = (HttpsURLConnection) url.openConnection();
                connection.setConnectTimeout(10 * 1000);
                connection.setReadTimeout(10 * 1000);
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                if (isCancelled())
                    return false;

                out = connection.getOutputStream();

                String postData = "id=" + mUserName + "&pass=" + mPassword;
                out.write(postData.getBytes());
                out.flush();

                in = connection.getInputStream();
                htmlCode = readInputStream(in, "utf-8");
            }
            catch (MalformedURLException me)
            {
                me.printStackTrace();
                mErrorMessage = me.toString();
                succeed = false;
            }
            catch (IOException ioe)
            {
                ioe.printStackTrace();
                mErrorMessage = ioe.toString();
                succeed = false;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                mErrorMessage = e.toString();
                succeed = false;
            }
            finally
            {
                if (connection != null)
                    connection.disconnect();

                if (in != null)
                {
                    try
                    {
                        in.close();
                    }
                    catch (IOException e1)
                    {
                        e1.printStackTrace();
                    }
                }

                if (out != null)
                {
                    try
                    {
                        out.close();
                    }
                    catch (IOException e1)
                    {
                        e1.printStackTrace();
                    }
                }
            }

            if (isCancelled())
                return false;

            if (!succeed)
                return false;

            //failed, unknown.
            if (TextUtils.isEmpty(htmlCode))
            {
                if (UApplication.cookieManager.isLogined())
                    return true;
                else
                {
                    mErrorMessage = "Unknown";
                    return false;
                }
            }

            String errorMsg = parseErrorMsg(htmlCode);
            if (errorMsg != null)
            {
                mErrorMessage = errorMsg;
                return false;
            }

            mErrorMessage="Unknown";
            return UApplication.cookieManager.isLogined();
        }

        @Override
        protected void onPostExecute(Boolean success)
        {
            mLoginTask = null;
            if (success)
            {
                if (mListener != null)
                    mListener.onLoginSucceed();
            }
            else
            {
                if (mListener != null)
                    mListener.onLoginFailed(mErrorMessage);
            }
        }

        @Override
        protected void onCancelled(Boolean success)
        {
            mLoginTask = null;

            if (mListener != null)
                mListener.onLoginCancelled();
        }

        private String readInputStream(InputStream in, String charset) throws Exception
        {
            String inputLine;
            String resultData = "";
            InputStreamReader isr = new InputStreamReader(in, charset);
            BufferedReader bufferReader = new BufferedReader(isr);

            while ((inputLine = bufferReader.readLine()) != null)
                resultData += inputLine;

            return resultData;
        }

        private String parseErrorMsg(String htmlCode)
        {
            final String ErrorToken = "エラーが発生しました。";
            final String ErrorMsgToken = "<\\s*span\\s+class\\s*=\\s*\"\\s*attention\\s*\"\\s*>";

            Matcher matcher = Pattern.compile(ErrorToken).matcher(htmlCode);
            if (matcher.find())
            {
                String errorMsg = HtmlUtility.getTagContent(
                        htmlCode, matcher.end(), ErrorMsgToken, "span", false).trim();
                if (errorMsg.isEmpty())
                    return "エラーが発生しました";
                else
                    return Html.fromHtml(errorMsg).toString();
            }

            return null;
        }
    }


    interface OnLoginEventListener
    {
        void onLoginSucceed();

        void onLoginFailed(String errMsg);

        void onLoginCancelled();
    }
}
