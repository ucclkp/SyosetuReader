package com.ucclkp.syosetureader;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.CookieStore;
import java.net.HttpCookie;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SyosetuCookieManager
{
    private CookieManager mCookieManager;


    private final static String COOKIE_NAME = "cookie_name";
    private final static String COOKIE_VALUE = "cookie_value";
    private final static String COOKIE_COMMENT = "cookie_comment";
    private final static String COOKIE_COMMENT_URL = "cookie_comment_url";
    private final static String COOKIE_DISCARD = "cookie_discard";             //boolean
    private final static String COOKIE_DOMAIN = "cookie_domain";
    private final static String COOKIE_MAX_AGE = "cookie_max_age";              //long
    private final static String COOKIE_PATH = "cookie_path";
    private final static String COOKIE_PORT_LIST = "cookie_port_list";
    private final static String COOKIE_SECURE = "cookie_secure";          //boolean
    private final static String COOKIE_VERSION = "cookie_version";         //int
    private final static String COOKIE_ASSO_URI = "cookie_asso_uri";


    public SyosetuCookieManager()
    {
        mCookieManager = new CookieManager();
        mCookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);
        CookieHandler.setDefault(mCookieManager);
    }


    public synchronized void saveCookieToLocal(Context context)
    {
        List<Map<URI, List<HttpCookie>>> cookieList = new ArrayList<>();

        CookieStore store = mCookieManager.getCookieStore();

        List<URI> uris = store.getURIs();
        List<HttpCookie> remainCookies = store.getCookies();
        ArrayList<HttpCookie> remCookies = new ArrayList<>();
        remCookies.addAll(remainCookies);

        for (URI uri : uris)
        {
            Map<URI, List<HttpCookie>> map = new HashMap<>();

            List<HttpCookie> uriCookies = store.get(uri);
            map.put(uri, uriCookies);
            cookieList.add(map);

            for (HttpCookie cookie : uriCookies)
            {
                for (int i = 0; i < remCookies.size(); ++i)
                {
                    if (remCookies.get(i).equals(cookie))
                    {
                        remCookies.remove(i);
                        break;
                    }
                }
            }
        }

        JSONArray array = new JSONArray();

        for (Map<URI, List<HttpCookie>> map : cookieList)
        {
            Set<URI> keySet = map.keySet();
            Iterator<URI> iterator = keySet.iterator();
            while (iterator.hasNext())
            {
                URI uri = iterator.next();
                List<HttpCookie> cookies = map.get(uri);
                for (HttpCookie cookie : cookies)
                {
                    JSONObject object = convertToJSON(cookie, uri);
                    array.put(object);
                }
            }
        }

        for (HttpCookie cookie : remCookies)
        {
            JSONObject object = convertToJSON(cookie, null);
            array.put(object);
        }


        SharedPreferences prefs
                = context.getSharedPreferences("syosetu_cookies", Context.MODE_PRIVATE);
        prefs.edit().putString("body", array.toString()).apply();
    }

    public synchronized void loadCookiesFromLocal(Context context)
    {
        SharedPreferences prefs
                = context.getSharedPreferences("syosetu_cookies", Context.MODE_PRIVATE);
        String jsonData = prefs.getString("body", null);
        if (jsonData == null)
            return;

        try
        {
            JSONArray array = new JSONArray(jsonData);
            for (int i = 0; i < array.length(); ++i)
            {
                String name = null;
                String value = null;
                String comment = null;
                String commentUrl = null;
                boolean discard = false;
                String domain = null;
                long maxAge = -1L;
                String path = null;
                String portList = null;
                boolean secure = false;
                int version = 0;
                String uriStr = null;

                JSONObject object = array.getJSONObject(i);
                Iterator<String> iterator = object.keys();
                while (iterator.hasNext())
                {
                    String key = iterator.next();
                    switch (key)
                    {
                        case COOKIE_NAME:
                            name = object.getString(COOKIE_NAME);
                            break;
                        case COOKIE_VALUE:
                            value = object.getString(COOKIE_VALUE);
                            break;
                        case COOKIE_COMMENT:
                            comment = object.getString(COOKIE_COMMENT);
                            break;
                        case COOKIE_COMMENT_URL:
                            commentUrl = object.getString(COOKIE_COMMENT_URL);
                            break;
                        case COOKIE_DISCARD:
                            discard = object.getBoolean(COOKIE_DISCARD);
                            break;
                        case COOKIE_DOMAIN:
                            domain = object.getString(COOKIE_DOMAIN);
                            break;
                        case COOKIE_MAX_AGE:
                            maxAge = object.getLong(COOKIE_MAX_AGE);
                            break;
                        case COOKIE_PATH:
                            path = object.getString(COOKIE_PATH);
                            break;
                        case COOKIE_PORT_LIST:
                            portList = object.getString(COOKIE_PORT_LIST);
                            break;
                        case COOKIE_SECURE:
                            secure = object.getBoolean(COOKIE_SECURE);
                            break;
                        case COOKIE_VERSION:
                            version = object.getInt(COOKIE_VERSION);
                            break;
                        case COOKIE_ASSO_URI:
                            uriStr = object.getString(COOKIE_ASSO_URI);
                            break;
                    }
                }

                HttpCookie cookie = new HttpCookie(name, value);
                cookie.setComment(comment);
                cookie.setCommentURL(commentUrl);
                cookie.setDiscard(discard);
                cookie.setDomain(domain);
                cookie.setMaxAge(maxAge);
                cookie.setPath(path);
                cookie.setPortlist(portList);
                cookie.setSecure(secure);
                cookie.setVersion(version);

                URI uri = null;
                if (uriStr != null)
                    uri = URI.create(uriStr);

                mCookieManager.getCookieStore().add(uri, cookie);
            }
        } catch (Exception e)
        {
            e.printStackTrace();
            Toast.makeText(context, "loadCookiesFromLocal() error", Toast.LENGTH_SHORT).show();
        }
    }

    public synchronized void clearAll(Context context)
    {
        SharedPreferences prefs
                = context.getSharedPreferences("syosetu_cookies", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();

        mCookieManager.getCookieStore().removeAll();
    }

    public synchronized void addOver18Cookie()
    {
        HttpCookie cookie = new HttpCookie("over18", "yes");
        cookie.setPath("/");
        cookie.setDomain(".syosetu.com");
        cookie.setMaxAge(86400 * 365);
        cookie.setVersion(0);
        mCookieManager.getCookieStore().add(null, cookie);
    }

    public synchronized boolean hasOver18Cookie()
    {
        CookieStore store = mCookieManager.getCookieStore();
        List<HttpCookie> cookies = store.getCookies();
        for (HttpCookie cookie : cookies)
        {
            String name = cookie.getName();
            String value = cookie.getValue();
            if (!TextUtils.isEmpty(name) && name.equalsIgnoreCase("over18")
                    && !TextUtils.isEmpty(value) && value.equalsIgnoreCase("yes"))
                return true;
        }

        return false;
    }

    public synchronized boolean isLogined()
    {
        CookieStore store = mCookieManager.getCookieStore();
        List<HttpCookie> cookies = store.getCookies();
        for (HttpCookie cookie : cookies)
        {
            String name = cookie.getName();
            String value = cookie.getValue();
            if (!TextUtils.isEmpty(name) && name.equalsIgnoreCase("userl")
                    && !TextUtils.isEmpty(value))
                return true;
        }

        return false;
    }

    private JSONObject convertToJSON(HttpCookie cookie, URI uri)
    {
        JSONObject object = new JSONObject();

        try
        {
            object.put(COOKIE_NAME, cookie.getName());
            object.put(COOKIE_VALUE, cookie.getValue());
            object.put(COOKIE_COMMENT, cookie.getComment());
            object.put(COOKIE_COMMENT_URL, cookie.getCommentURL());
            object.put(COOKIE_DISCARD, cookie.getDiscard());
            object.put(COOKIE_DOMAIN, cookie.getDomain());
            object.put(COOKIE_MAX_AGE, cookie.getMaxAge());
            object.put(COOKIE_PATH, cookie.getPath());
            object.put(COOKIE_PORT_LIST, cookie.getPortlist());
            object.put(COOKIE_SECURE, cookie.getSecure());
            object.put(COOKIE_VERSION, cookie.getVersion());
            object.put(COOKIE_ASSO_URI, uri != null ? uri.toString() : null);
        } catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }

        return object;
    }
}
