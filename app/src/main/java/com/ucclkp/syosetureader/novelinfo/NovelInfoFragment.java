package com.ucclkp.syosetureader.novelinfo;


import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatDelegate;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TextAppearanceSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.HtmlUtility;
import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuUtility;

public class NovelInfoFragment extends Fragment {
    private String mNovelInfoUrl;
    private NovelInfoParser mNovelInfoParser;

    private TextView mTitleTextView;
    private TextView mTypeTextView;
    private Button mAuthorButton;
    private TextView mListTextView;
    private LinearLayout mForumLayout;
    private SwipeRefreshLayout mRefreshSRL;


    private final static String ARG_NOVEL_INFO_URL = "arg_novel_info_url";


    public NovelInfoFragment() {
    }

    public static NovelInfoFragment newInstance(String url) {
        NovelInfoFragment fragment = new NovelInfoFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NOVEL_INFO_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mNovelInfoUrl = getArguments().getString(ARG_NOVEL_INFO_URL);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mNovelInfoParser != null)
            mNovelInfoParser.cancel();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View parent = inflater.inflate(R.layout.fragment_novel_info, container, false);

        mRefreshSRL = parent.findViewById(R.id.srl_novelinfo_refresher);
        mRefreshSRL.setOnRefreshListener(mRefreshListener);
        mRefreshSRL.setColorSchemeResources(
                R.color.color_blue,
                R.color.color_red,
                R.color.color_green,
                R.color.color_yellow);
        boolean isNightMode = (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES);
        if (isNightMode)
            mRefreshSRL.setProgressBackgroundColorSchemeResource(R.color.color_swipe_background);

        mTitleTextView = parent.findViewById(R.id.tv_novelinfo_title);
        mTypeTextView = parent.findViewById(R.id.tv_novelinfo_type);
        mListTextView = parent.findViewById(R.id.tv_novelinfo_list);
        mAuthorButton = parent.findViewById(R.id.bt_novelinfo_author);
        mForumLayout = parent.findViewById(R.id.cl_novelinfo_forum);

        return parent;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mNovelInfoParser = new NovelInfoParser(getActivity());
        mNovelInfoParser.setPipelineListener(mParserListener);
        mNovelInfoParser.enter(mNovelInfoUrl);

        if (mNovelInfoParser.isInPipeline())
            mRefreshSRL.setRefreshing(true);
    }


    private SwipeRefreshLayout.OnRefreshListener mRefreshListener
            = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            mForumLayout.setVisibility(View.INVISIBLE);
            mNovelInfoParser.enter(mNovelInfoUrl);
        }
    };


    private NovelInfoParser.OnPipelineListener<NovelInfoParser.NovelInfoData> mParserListener
            = new HtmlDataPipeline.OnPipelineListener<NovelInfoParser.NovelInfoData>() {
        @Override
        public void onPostData(int exitCode, NovelInfoParser.NovelInfoData data) {
            mRefreshSRL.setRefreshing(false);

            if (exitCode != HtmlDataPipeline.CODE_SUCCESS) {
                Toast.makeText(getActivity(),
                        "Failed", Toast.LENGTH_SHORT).show();
            } else {
                String type = data.ncode + "  " + data.type;

                mTitleTextView.setText(data.novelTitle);
                mTypeTextView.setText(type);

                if (data.authorUrl.isEmpty())
                    mAuthorButton.setVisibility(View.GONE);
                else {
                    mAuthorButton.setText(data.authorName);
                    mAuthorButton.setOnClickListener(
                            SyosetuUtility.clickOfAuthor(data.authorUrl, data.authorName));
                    mAuthorButton.setVisibility(View.VISIBLE);
                }

                SpannableStringBuilder listData = new SpannableStringBuilder();

                for (int i = 0; i < data.itemList1.size(); ++i) {
                    NovelInfoParser.NovelInfoItem item = data.itemList1.get(i);

                    SpannableString titleSpaned = new SpannableString(item.title);
                    titleSpaned.setSpan(
                            new TextAppearanceSpan(getActivity(), R.style.NovelInfoTitleAppearance),
                            0, titleSpaned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    listData.append(titleSpaned).append("\n").append(item.content).append("\n\n");
                }

                for (int i = 0; i < data.itemList2.size(); ++i) {
                    NovelInfoParser.NovelInfoItem item = data.itemList2.get(i);

                    SpannableString titleSpaned = new SpannableString(item.title);
                    titleSpaned.setSpan(
                            new TextAppearanceSpan(getActivity(), R.style.NovelInfoTitleAppearance),
                            0, titleSpaned.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                    listData.append(titleSpaned).append("\n").append(item.content).append("\n\n");
                }

                HtmlUtility.removeLB(listData);
                mListTextView.setText(listData);

                mForumLayout.setVisibility(View.VISIBLE);
            }
        }
    };
}
