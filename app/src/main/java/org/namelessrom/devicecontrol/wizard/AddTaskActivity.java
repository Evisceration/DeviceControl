/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.namelessrom.devicecontrol.wizard;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.negusoft.holoaccent.activity.AccentActivity;
import com.negusoft.holoaccent.dialog.AccentAlertDialog;
import com.negusoft.holoaccent.dialog.AccentDialogFragment;

import org.namelessrom.devicecontrol.Application;
import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.database.TaskerItem;
import org.namelessrom.devicecontrol.wizard.model.AbstractWizardModel;
import org.namelessrom.devicecontrol.wizard.model.ModelCallbacks;
import org.namelessrom.devicecontrol.wizard.model.Page;
import org.namelessrom.devicecontrol.wizard.ui.PageFragmentCallbacks;
import org.namelessrom.devicecontrol.wizard.ui.ReviewFragment;
import org.namelessrom.devicecontrol.wizard.ui.StepPagerStrip;

import java.util.List;

public class AddTaskActivity extends AccentActivity implements
        PageFragmentCallbacks,
        ReviewFragment.Callbacks,
        ModelCallbacks {

    public static final String ARG_ITEM = "arg_item";

    private Button mNextButton;
    private Button mPrevButton;

    private ViewPager      mPager;
    private StepPagerStrip mStepPagerStrip;
    private MyPagerAdapter mPagerAdapter;

    private boolean mEditingAfterReview;

    private AbstractWizardModel mWizardModel;

    private boolean mConsumePageSelectedEvent;

    private List<Page> mCurrentPageSequence;

    @Override public int getOverrideAccentColor() {
        return Application.get().getAccentColor();
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(Application.get().isDarkTheme() ? R.style.BaseThemeDark : R.style.BaseThemeLight);

        setContentView(R.layout.wizard_activity);

        mNextButton = (Button) findViewById(R.id.next_button);
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                onNextButton();
            }
        });

        mPrevButton = (Button) findViewById(R.id.prev_button);
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                mPager.setCurrentItem(mPager.getCurrentItem() - 1);
            }
        });

        mPager = (ViewPager) findViewById(R.id.pager);
        mStepPagerStrip = (StepPagerStrip) findViewById(R.id.strip);

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        final TaskerItem item = (TaskerItem) getIntent().getSerializableExtra(ARG_ITEM);
        if (item != null) {
            if (actionBar != null) {
                actionBar.setTitle(R.string.edit_task);
            }
            mWizardModel = new TaskerWizardModel(this, item);
        } else {
            mWizardModel = new TaskerWizardModel(this);
        }

        if (savedInstanceState != null) {
            mWizardModel.load(savedInstanceState.getBundle("model"));
        }

        mWizardModel.registerListener(this);

        mPagerAdapter = new MyPagerAdapter(getFragmentManager());
        mPager.setAdapter(mPagerAdapter);
        mStepPagerStrip.setOnPageSelectedListener(new StepPagerStrip.OnPageSelectedListener() {
            @Override
            public void onPageStripSelected(int position) {
                position = Math.min(mPagerAdapter.getCount() - 1, position);
                if (mPager.getCurrentItem() != position) {
                    mPager.setCurrentItem(position);
                }
            }
        });

        mPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mStepPagerStrip.setCurrentPage(position);

                if (mConsumePageSelectedEvent) {
                    mConsumePageSelectedEvent = false;
                    return;
                }

                mEditingAfterReview = false;
                updateBottomBar();
            }
        });

        onPageTreeChanged();
        updateBottomBar();
    }

    private void onNextButton() {
        if (mPager.getCurrentItem() == mCurrentPageSequence.size()) {
            AccentDialogFragment dg = new AccentDialogFragment() {
                @Override
                public Dialog onCreateDialog(Bundle savedInstanceState) {
                    return new AccentAlertDialog.Builder(getActivity())
                            .setMessage(R.string.submit_confirm_message)
                            .setPositiveButton(R.string.submit_confirm_button, mSubListener)
                            .setNegativeButton(android.R.string.cancel, null)
                            .create();
                }
            };
            dg.show(getFragmentManager(), "create_tasker_dialog");
        } else {
            if (mEditingAfterReview) {
                mPager.setCurrentItem(mPagerAdapter.getCount() - 1);
            } else {
                mPager.setCurrentItem(mPager.getCurrentItem() + 1);
            }
        }
    }

    @Override public void onBackPressed() {
        final int item = mPager.getCurrentItem();
        if (item > 0) {
            mPager.setCurrentItem(item - 1);
        } else {
            super.onBackPressed();
        }
    }

    @Override public void onSaveTask() { }

    @Override public void onItemSelected() {
        onNextButton();
    }

    private DialogInterface.OnClickListener mSubListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            mWizardModel.onSaveTask();
        }
    };

    @Override public void onPageTreeChanged() {
        mCurrentPageSequence = mWizardModel.getCurrentPageSequence();
        recalculateCutOffPage();
        mStepPagerStrip.setPageCount(mCurrentPageSequence.size() + 1); // + 1 = review step
        mPagerAdapter.notifyDataSetChanged();
        updateBottomBar();
    }

    private void updateBottomBar() {
        int position = mPager.getCurrentItem();
        if (position == mCurrentPageSequence.size()) {
            mNextButton.setText(R.string.finish);
            mNextButton.setBackgroundResource(R.drawable.finish_background);
        } else {
            mNextButton.setText(mEditingAfterReview
                    ? R.string.review
                    : R.string.next);
            mNextButton.setBackgroundResource(0);
            mNextButton.setEnabled(position != mPagerAdapter.getCutOffPage());
        }

        mPrevButton.setVisibility(position <= 0 ? View.INVISIBLE : View.VISIBLE);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        mWizardModel.unregisterListener(this);
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("model", mWizardModel.save());
    }

    @Override public AbstractWizardModel onGetModel() { return mWizardModel; }

    @Override public void onEditScreenAfterReview(final String key) {
        for (int i = mCurrentPageSequence.size() - 1; i >= 0; i--) {
            if (mCurrentPageSequence.get(i).getKey().equals(key)) {
                mConsumePageSelectedEvent = true;
                mEditingAfterReview = true;
                mPager.setCurrentItem(i);
                updateBottomBar();
                break;
            }
        }
    }

    @Override public void onPageDataChanged(final Page page) {
        if (page.isRequired()) {
            if (recalculateCutOffPage()) {
                mPagerAdapter.notifyDataSetChanged();
                updateBottomBar();
            }
        }
    }

    @Override public Page onGetPage(final String key) { return mWizardModel.findByKey(key); }

    private boolean recalculateCutOffPage() {
        // Cut off the pager adapter at first required page that isn't completed
        int cutOffPage = mCurrentPageSequence.size() + 1;
        Page page;
        for (int i = 0; i < mCurrentPageSequence.size(); i++) {
            page = mCurrentPageSequence.get(i);
            if (page.isRequired() && !page.isCompleted()) {
                cutOffPage = i;
                break;
            }
        }

        if (mPagerAdapter.getCutOffPage() != cutOffPage) {
            mPagerAdapter.setCutOffPage(cutOffPage);
            return true;
        }

        return false;
    }

    public class MyPagerAdapter extends FragmentStatePagerAdapter {
        private int      mCutOffPage;
        private Fragment mPrimaryItem;

        public MyPagerAdapter(final FragmentManager fm) { super(fm); }

        @Override
        public Fragment getItem(final int i) {
            if (i >= mCurrentPageSequence.size()) return new ReviewFragment();

            return mCurrentPageSequence.get(i).createFragment();
        }

        @Override public int getItemPosition(final Object object) {
            // TODO: be smarter about this
            if (object == mPrimaryItem) {
                // Re-use the current fragment (its position never changes)
                return POSITION_UNCHANGED;
            }

            return POSITION_NONE;
        }

        @Override public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mPrimaryItem = (Fragment) object;
        }

        @Override public int getCount() {
            if (mCurrentPageSequence == null) {
                return 0;
            }
            return Math.min(mCutOffPage + 1, mCurrentPageSequence.size() + 1);
        }

        public void setCutOffPage(int cutOffPage) {
            if (cutOffPage < 0) {
                cutOffPage = Integer.MAX_VALUE;
            }
            mCutOffPage = cutOffPage;
        }

        public int getCutOffPage() { return mCutOffPage; }
    }
}
