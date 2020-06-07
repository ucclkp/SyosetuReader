package com.ucclkp.syosetureader.search;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.AppBarLayout;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.ucclkp.syosetureader.HtmlDataPipeline;
import com.ucclkp.syosetureader.R;
import com.ucclkp.syosetureader.SyosetuUtility;
import com.ucclkp.syosetureader.UApplication;
import com.ucclkp.syosetureader.UNormalSearchView;
import com.ucclkp.syosetureader.USearchView;
import com.ucclkp.syosetureader.behavior.FABScrollAwareBehavior;

import java.net.URLEncoder;
import java.util.List;


public class SearchFragment extends Fragment
{
    private USearchView mSearchView;
    private UNormalSearchView mSearchAssist;
    private FloatingActionButton mSearchFAB;
    private FABScrollAwareBehavior mFABBehavior;

    private AppCompatSpinner mOrderSpinner;
    private AppCompatSpinner mTypeSpinner;

    private CheckBox mIllustCB;
    private CheckBox mPickupCB;
    private CheckBox mExSuspendCB;

    private CheckBox mSearchFromTitleCB;
    private CheckBox mSearchFromSummaryCB;
    private CheckBox mSearchFromKwCB;
    private CheckBox mSearchFromAuthorCB;

    private CheckBox mIsR15CB;
    private CheckBox mIsCruelCB;
    private CheckBox mIsBLCB;
    private CheckBox mIsGLCB;
    private CheckBox mIsReincarnationCB;
    private CheckBox mIsTransferCB;

    private CheckBox mIsNotR15CB;
    private CheckBox mIsNotCruelCB;
    private CheckBox mIsNotBLCB;
    private CheckBox mIsNotGLCB;
    private CheckBox mIsNotReincarnationCB;
    private CheckBox mIsNotTransferCB;

    private CardView mAdvCardView;
    private SwitchCompat mAdvSwitch;
    private LinearLayout mExpConConditionLL;

    private View mKwPanelShadow;
    private CardView mKwPanelPlate;
    private FrameLayout mKeywordPanel;
    private KeywordBar mKeywordBar;
    private RecyclerView mKwListView;
    private ProgressBar mKwLoadingPB;

    private boolean mIsKwPanelHiding;
    private KwPanelParser mKwPanelParser;
    private KwPanelListAdapter mKwListAdapter;
    private GridLayoutManager mKwListLayoutManager;
    private KwPanelParser.KeywordData mKeywordBarData;


    public SearchFragment()
    {
    }


    public static SearchFragment newInstance()
    {
        SearchFragment fragment = new SearchFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
        }
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        if (mKwPanelParser != null)
            mKwPanelParser.cancel();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View parent = inflater.inflate(R.layout.fragment_search, container, false);

        TabLayout tabLayout = getActivity().findViewById(R.id.tl_main_activity);
        tabLayout.setVisibility(View.GONE);

        Toolbar toolbar = getActivity().findViewById(R.id.tb_main_activity);
        AppBarLayout.LayoutParams lp = (AppBarLayout.LayoutParams) toolbar.getLayoutParams();
        lp.setScrollFlags(0);
        toolbar.setLayoutParams(lp);

        mSearchAssist = getActivity().findViewById(R.id.unsv_search_assist);

        mSearchFAB = getActivity().findViewById(R.id.fab_search);
        CoordinatorLayout.LayoutParams fabLayoutParams
                = (CoordinatorLayout.LayoutParams) mSearchFAB.getLayoutParams();
        mFABBehavior = (FABScrollAwareBehavior) fabLayoutParams.getBehavior();

        mOrderSpinner = parent.findViewById(R.id.sp_base_sort);
        mTypeSpinner = parent.findViewById(R.id.sp_base_type);

        mIllustCB = parent.findViewById(R.id.cb_base_illustration);
        mPickupCB = parent.findViewById(R.id.cb_base_pickup);
        mExSuspendCB = parent.findViewById(R.id.cb_base_ex_suspend);

        mSearchFromTitleCB = parent.findViewById(R.id.cb_adv_searchfrom_title);
        mSearchFromSummaryCB = parent.findViewById(R.id.cb_adv_searchfrom_summary);
        mSearchFromKwCB = parent.findViewById(R.id.cb_adv_searchfrom_kw);
        mSearchFromAuthorCB = parent.findViewById(R.id.cb_adv_searchfrom_author);

        mIsR15CB = parent.findViewById(R.id.cb_adv_isr15);
        mIsCruelCB = parent.findViewById(R.id.cb_adv_iscruel);
        mIsBLCB = parent.findViewById(R.id.cb_adv_isbl);
        mIsGLCB = parent.findViewById(R.id.cb_adv_isgl);
        mIsReincarnationCB = parent.findViewById(R.id.cb_adv_isre);
        mIsTransferCB = parent.findViewById(R.id.cb_adv_istrans);

        mIsNotR15CB = parent.findViewById(R.id.cb_adv_notr15);
        mIsNotCruelCB = parent.findViewById(R.id.cb_adv_notcruel);
        mIsNotBLCB = parent.findViewById(R.id.cb_adv_notbl);
        mIsNotGLCB = parent.findViewById(R.id.cb_adv_notgl);
        mIsNotReincarnationCB = parent.findViewById(R.id.cb_adv_notre);
        mIsNotTransferCB = parent.findViewById(R.id.cb_adv_nottrans);

        mAdvCardView = parent.findViewById(R.id.cv_adv_settings);
        mExpConConditionLL = parent.findViewById(R.id.ll_adv_exp_con_condition);

        mAdvSwitch = parent.findViewById(R.id.sc_switch_adv);
        mAdvSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                    mAdvCardView.setVisibility(View.VISIBLE);
                else
                    mAdvCardView.setVisibility(View.GONE);
            }
        });

        mKwPanelShadow = parent.findViewById(R.id.v_search_keyword_shadow);
        mKwPanelShadow.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (mKeywordPanel.getVisibility() == View.VISIBLE
                        && !mIsKwPanelHiding)
                    hideKeywordPanel();
            }
        });

        mKwPanelPlate = parent.findViewById(R.id.cv_search_keyword_plate);
        mKeywordPanel = parent.findViewById(R.id.fl_keyword_panel);
        mKeywordBar = parent.findViewById(R.id.kb_search_kw_panel_bar);
        mKeywordBar.setSelection(0);
        mKeywordBar.setOnItemSelectListener(mKwBarItemSelectListener);

        mKwLoadingPB = parent.findViewById(R.id.pb_search_kw_panel_load);

        mKwListAdapter = new KwPanelListAdapter(getContext());
        mKwListAdapter.setOnItemSelectListener(mKeywordSelectListener);

        mKwListLayoutManager = new GridLayoutManager(getContext(), 3);
        mKwListLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup()
        {
            @Override
            public int getSpanSize(int position)
            {
                int viewType = mKwListAdapter.getItemViewType(position);
                if (viewType == KwPanelListAdapter.TYPE_HEAD)
                    return 3;

                return 1;
            }
        });

        mKwListView = parent.findViewById(R.id.rv_search_kw_panel_list);
        mKwListView.setAdapter(mKwListAdapter);
        mKwListView.setLayoutManager(mKwListLayoutManager);
        mKwListView.setHasFixedSize(true);

        mKwPanelParser = new KwPanelParser(getContext());
        mKwPanelParser.setPipelineListener(mKwPanelParseListener);
        mKwPanelParser.enter(SyosetuUtility.SEARCH_URL);

        return parent;
    }

    @Override
    public void onHiddenChanged(boolean hidden)
    {
        super.onHiddenChanged(hidden);

        if (!hidden)
        {
            mFABBehavior.mEnabled = true;

            mSearchFAB.show();
            mSearchAssist.setVisibility(View.VISIBLE);

            switch (UApplication.syosetuSite)
            {
                case NORMAL:
                    mPickupCB.setVisibility(View.VISIBLE);
                    mExpConConditionLL.setVisibility(View.VISIBLE);
                    break;

                case NOCTURNE:
                    mPickupCB.setVisibility(View.GONE);
                    mExpConConditionLL.setVisibility(View.GONE);
                    break;
            }
        } else
        {
            if (mSearchFAB != null)
            {
                mSearchFAB.hide();
                mFABBehavior.mEnabled = false;
            }

            if (mSearchAssist != null)
                mSearchAssist.setVisibility(View.GONE);

            if (mKeywordPanel != null)
            {
                mKwPanelPlate.setVisibility(View.INVISIBLE);
                mKwPanelShadow.setVisibility(View.INVISIBLE);
                mKeywordPanel.setVisibility(View.INVISIBLE);
            }
        }
    }


    public void setSearchView(USearchView sv)
    {
        mSearchView = sv;
        mSearchView.setOnButtonClickListener(mButtonClickListener);
    }


    public String getSearchUrl(String key)
    {
        String keyEncoded = "";
        try
        {
            keyEncoded = URLEncoder.encode(key, "utf-8");
        } catch (Exception e)
        {
            Toast.makeText(getContext(),
                    "Key encode failed.", Toast.LENGTH_SHORT).show();
        }

        String exceptKeyEncoded = "";
        try
        {
            exceptKeyEncoded = URLEncoder.encode(
                    mSearchAssist.getText().toString(), "utf-8");
        } catch (Exception e)
        {
            Toast.makeText(getContext(),
                    "Except key encode failed.", Toast.LENGTH_SHORT).show();
        }

        switch (UApplication.syosetuSite)
        {
            case NORMAL:
                return SyosetuUtility.getSearchUrl() + "?"
                        + "mintime="
                        + "&maxtime="
                        + "&minlen="
                        + "&maxlen="
                        + "&minlastup="
                        + "&maxlastup="
                        + "&sasie=" + parseIsIllust()
                        + "&ispickup=" + parseIsPickup()
                        + "&isr15=" + parseIsR15()
                        + "&iszankoku=" + parseIsCruel()
                        + "&isbl=" + parseIsBL()
                        + "&isgl=" + parseIsGL()
                        + "&istensei=" + parseIsReincarnation()
                        + "&istenni=" + parseIsTransfer()
                        + "&stop=" + parseIsExSuspend()
                        + "&notr15=" + parseIsNotR15()
                        + "&notzankoku=" + parseIsNotCruel()
                        + "&notbl=" + parseIsNotBL()
                        + "&notgl=" + parseIsNotGL()
                        + "&nottensei=" + parseIsNotReincarnation()
                        + "&nottenni=" + parseIsNotTransfer()
                        + "&order=" + parseOrderOption()
                        + "&type=" + parseTypeOption()
                        + "&genre="
                        + "&word=" + keyEncoded
                        + "&notword=" + exceptKeyEncoded
                        + "&title=" + parseIsSearchFromTitle()
                        + "&ex=" + parseIsSearchFromSummary()
                        + "&keyword=" + parseIsSearchFromKw()
                        + "&wname=" + parseIsSearchFromAuthor();

            case NOCTURNE:
                return SyosetuUtility.getSearchUrl() + "?"
                        + "mintime="
                        + "&maxtime="
                        + "&minlen="
                        + "&maxlen="
                        + "&sasie=" + parseIsIllust()
                        + "&stop=" + parseIsExSuspend()
                        + "&order=" + parseOrderOption()
                        + "&type=" + parseTypeOption()
                        + "&word=" + keyEncoded
                        + "&notword=" + exceptKeyEncoded
                        + "&title=" + parseIsSearchFromTitle()
                        + "&ex=" + parseIsSearchFromSummary()
                        + "&keyword=" + parseIsSearchFromKw()
                        + "&wname=" + parseIsSearchFromAuthor();
        }

        return "";
    }


    private USearchView.OnButtonClickListener mButtonClickListener
            = new USearchView.OnButtonClickListener()
    {
        @Override
        public void onCloseClick()
        {
        }

        @Override
        public void onKeywordClick()
        {
            if (mKeywordPanel.getVisibility() != View.VISIBLE)
            {
                if (!mKwPanelParser.isInPipeline()
                        && mKeywordBarData == null)
                {
                    mKwLoadingPB.setVisibility(View.VISIBLE);
                    mKwListView.setVisibility(View.GONE);
                    mKwPanelParser.enter(SyosetuUtility.SEARCH_URL);
                }

                showKeywordPanel();
            } else if (mKeywordPanel.getVisibility() == View.VISIBLE
                    && !mIsKwPanelHiding)
                hideKeywordPanel();
        }
    };

    private KwPanelParser.OnPipelineListener<KwPanelParser.KeywordData> mKwPanelParseListener
            = new HtmlDataPipeline.OnPipelineListener<KwPanelParser.KeywordData>()
    {
        @Override
        public void onPostData(int exitCode, KwPanelParser.KeywordData data)
        {
            if (exitCode == HtmlDataPipeline.CODE_SUCCESS)
            {
                mKeywordBarData = data;

                switch (mKeywordBar.getSelection())
                {
                    case 0:
                        fillKeywordList(mKeywordBarData.stockList);
                        break;
                    case 1:
                        fillKeywordList(mKeywordBarData.recommendList);
                        break;
                    case 2:
                        fillKeywordList(mKeywordBarData.replayList);
                        break;
                }
            }

            mKwLoadingPB.setVisibility(View.GONE);
            mKwListView.setVisibility(View.VISIBLE);
        }
    };

    private KeywordBar.OnItemSelectListener mKwBarItemSelectListener
            = new KeywordBar.OnItemSelectListener()
    {
        @Override
        public void onItemSelected(int position)
        {
            if (mKeywordBarData == null)
                return;

            switch (position)
            {
                case 0:
                    fillKeywordList(mKeywordBarData.stockList);
                    break;
                case 1:
                    fillKeywordList(mKeywordBarData.recommendList);
                    break;
                case 2:
                    fillKeywordList(mKeywordBarData.replayList);
                    break;
            }
        }
    };

    private KwPanelListAdapter.OnItemSelectListener mKeywordSelectListener
            = new KwPanelListAdapter.OnItemSelectListener()
    {
        @Override
        public void onItemClick(View itemView)
        {
            String keyword = (String) itemView.getTag();
            if (mSearchView.hasFocus())
            {
                mSearchView.appendTextWithSpace(keyword);
            } else if (mSearchAssist.hasFocus())
            {
                mSearchAssist.appendTextWithSpace(keyword);
            }
        }

        @Override
        public boolean onItemLongClick(View itemView)
        {
            return false;
        }
    };


    private void fillKeywordList(List<KwPanelParser.KwAtom> list)
    {
        mKwListAdapter.clear();

        for (int i = 0; i < list.size(); ++i)
        {
            KwPanelParser.KwAtom atom = list.get(i);
            switch (atom.type)
            {
                case KwPanelParser.KT_TITLE:
                    mKwListAdapter.addHeader(atom.title);
                    break;

                case KwPanelParser.KT_KEYWORD:
                    mKwListAdapter.addKeyword(atom.keyword, atom.value);
                    break;
            }
        }
    }


    private void showKeywordPanel()
    {
        mKeywordPanel.setVisibility(View.VISIBLE);

        View myView = mKwPanelPlate;

        int cx = myView.getRight();
        int cy = myView.getTop();
        int finalRadius = (int) Math.sqrt(
                myView.getWidth() * myView.getWidth()
                        + myView.getHeight() * myView.getHeight());

        Animator circuleAnim = ViewAnimationUtils.createCircularReveal(
                myView, cx, cy, 0, finalRadius);

        mKwPanelShadow.setVisibility(View.VISIBLE);
        mKwPanelShadow.setAlpha(0.f);
        mKwPanelShadow.animate()
                .alpha(1.f)
                .setDuration(circuleAnim.getDuration())
                .setListener(null);

        myView.setVisibility(View.VISIBLE);
        circuleAnim.start();

        mIsKwPanelHiding = false;
    }

    private void hideKeywordPanel()
    {
        final View myView = mKwPanelPlate;

        int cx = myView.getRight();
        int cy = myView.getTop();
        int initialRadius = (int) Math.sqrt(
                myView.getWidth() * myView.getWidth()
                        + myView.getHeight() * myView.getHeight());

        Animator anim = ViewAnimationUtils.createCircularReveal(
                myView, cx, cy, initialRadius, 0);

        anim.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(Animator animation)
            {
                super.onAnimationEnd(animation);

                myView.setVisibility(View.INVISIBLE);
                mKwPanelShadow.setVisibility(View.INVISIBLE);
                mKeywordPanel.setVisibility(View.INVISIBLE);

                mIsKwPanelHiding = false;
            }

            @Override
            public void onAnimationCancel(Animator animation)
            {
                super.onAnimationCancel(animation);

                mIsKwPanelHiding = false;
            }
        });

        mKwPanelShadow.animate()
                .alpha(0.f)
                .setDuration(anim.getDuration())
                .setListener(null);

        anim.start();

        mIsKwPanelHiding = true;
    }


    private String parseOrderOption()
    {
        String orderOption = (String) mOrderSpinner.getSelectedItem();

        if (orderOption.equals(getString(R.string.sort_notorder)))
            return "notorder";
        else if (orderOption.equals(getString(R.string.sort_new)))
            return "new";
        else if (orderOption.equals(getString(R.string.sort_weekly)))
            return "weekly";
        else if (orderOption.equals(getString(R.string.sort_favnovelcnt)))
            return "favnovelcnt";
        else if (orderOption.equals(getString(R.string.sort_reviewcnt)))
            return "reviewcnt";
        else if (orderOption.equals(getString(R.string.sort_hyoka)))
            return "hyoka";
        else if (orderOption.equals(getString(R.string.sort_dailypoint)))
            return "dailypoint";
        else if (orderOption.equals(getString(R.string.sort_weeklypoint)))
            return "weeklypoint";
        else if (orderOption.equals(getString(R.string.sort_monthlypoint)))
            return "monthlypoint";
        else if (orderOption.equals(getString(R.string.sort_quarterpoint)))
            return "quarterpoint";
        else if (orderOption.equals(getString(R.string.sort_yearlypoint)))
            return "yearlypoint";
        else if (orderOption.equals(getString(R.string.sort_hyokacnt)))
            return "hyokacnt";
        else if (orderOption.equals(getString(R.string.sort_lengthdesc)))
            return "lengthdesc";
        else if (orderOption.equals(getString(R.string.sort_lengthasc)))
            return "lengthasc";
        else if (orderOption.equals(getString(R.string.sort_ncodedesc)))
            return "ncodedesc";
        else if (orderOption.equals(getString(R.string.sort_old)))
            return "old";

        return "";
    }

    private String parseTypeOption()
    {
        String typeOption = (String) mTypeSpinner.getSelectedItem();

        if (typeOption.equals(getString(R.string.type_all)))
            return "";
        else if (typeOption.equals(getString(R.string.type_short_only)))
            return "t";
        else if (typeOption.equals(getString(R.string.type_end_only)))
            return "ter";
        else if (typeOption.equals(getString(R.string.type_serializing_only)))
            return "r";
        else if (typeOption.equals(getString(R.string.type_all_serial)))
            return "re";
        else if (typeOption.equals(getString(R.string.type_end_serial_only)))
            return "er";

        return "";
    }


    private String parseIsIllust()
    {
        if (mIllustCB.isChecked())
            return "1-";
        else
            return "";
    }

    private String parseIsPickup()
    {
        if (mPickupCB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsExSuspend()
    {
        if (mExSuspendCB.isChecked())
            return "1";
        else
            return "";
    }


    private String parseIsSearchFromTitle()
    {
        if (mSearchFromTitleCB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsSearchFromSummary()
    {
        if (mSearchFromSummaryCB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsSearchFromKw()
    {
        if (mSearchFromKwCB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsSearchFromAuthor()
    {
        if (mSearchFromAuthorCB.isChecked())
            return "1";
        else
            return "";
    }


    private String parseIsR15()
    {
        if (mIsR15CB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsCruel()
    {
        if (mIsCruelCB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsBL()
    {
        if (mIsBLCB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsGL()
    {
        if (mIsGLCB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsReincarnation()
    {
        if (mIsReincarnationCB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsTransfer()
    {
        if (mIsTransferCB.isChecked())
            return "1";
        else
            return "";
    }


    private String parseIsNotR15()
    {
        if (mIsNotR15CB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsNotCruel()
    {
        if (mIsNotCruelCB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsNotBL()
    {
        if (mIsNotBLCB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsNotGL()
    {
        if (mIsNotGLCB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsNotReincarnation()
    {
        if (mIsNotReincarnationCB.isChecked())
            return "1";
        else
            return "";
    }

    private String parseIsNotTransfer()
    {
        if (mIsNotTransferCB.isChecked())
            return "1";
        else
            return "";
    }
}