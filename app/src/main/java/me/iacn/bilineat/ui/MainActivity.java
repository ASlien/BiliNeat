package me.iacn.bilineat.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;

import java.util.Arrays;
import java.util.List;

import me.iacn.bilineat.R;
import me.iacn.bilineat.util.ReflectUtils;
import me.iacn.bilineat.util.StatusBarUtils;

public class MainActivity extends Activity implements ViewPager.OnPageChangeListener,
        BottomNavigationView.OnNavigationItemSelectedListener {

    private int mThemeColor;

    private ViewPager mPager;
    private SharedPreferences mSharePref;

    private BottomNavigationItemView[] mButtons;
    private View.OnClickListener mOnClickListener;
    private BottomNavigationView mBottomBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        mThemeColor = intent.getIntExtra("color", -298343);

        StatusBarUtils.setColor(this, mThemeColor);

        findView();
        initActionBar();
        initData();
        setListener();
        showExplainDialog();
    }

    private void findView() {
        mPager = (ViewPager) findViewById(R.id.view_pager);
        mBottomBar = (BottomNavigationView) findViewById(R.id.bottom_bar);
    }

    private void initActionBar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(mThemeColor);
        toolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_ab_back_material);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    private void initData() {
        // 底栏颜色设置
        ColorStateList colorList = getCheckedColorList();
        mBottomBar.setItemIconTintList(colorList);
        mBottomBar.setItemTextColor(colorList);

        mSharePref = getSharedPreferences("setting", MODE_WORLD_READABLE);

        BottomNavigationMenuView mMenuView = ReflectUtils.getObjectField(mBottomBar,
                "mMenuView", BottomNavigationMenuView.class);

        mButtons = ReflectUtils.getObjectField(mMenuView,
                "mButtons", BottomNavigationItemView[].class);

        mOnClickListener = ReflectUtils.getObjectField(mMenuView,
                "mOnClickListener", View.OnClickListener.class);

        final List<Fragment> pageList = Arrays.asList(
                new StateFragment(),
                new ActionFragment(),
                new AboutFragment());

        mPager.setAdapter(new FragmentPagerAdapter(getFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return pageList.get(position);
            }

            @Override
            public int getCount() {
                return pageList.size();
            }
        });
    }

    private void setListener() {
        mPager.addOnPageChangeListener(this);
        mBottomBar.setOnNavigationItemSelectedListener(this);
    }

    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        System.out.println(name);
        ColorStateList colorList = getCheckedColorList();

        switch (name) {
            case "CheckBox":
                CheckBox checkBox = new CheckBox(context, attrs);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    checkBox.setButtonTintList(colorList);
                }

                return checkBox;

            case "Button":
                Button button = new Button(context, attrs);
                button.setTextColor(mThemeColor);
                return button;

            case "CheckedTextView":
                CheckedTextView view = new CheckedTextView(context, attrs);
                int sdkInt = Build.VERSION.SDK_INT;

                if (sdkInt >= Build.VERSION_CODES.LOLLIPOP && sdkInt < Build.VERSION_CODES.M) {
                    // 5.0
                    view.setCheckMarkTintList(colorList);
                } else if (sdkInt >= Build.VERSION_CODES.M) {
                    // 6.0+
                    Drawable radioDrawable = ContextCompat.getDrawable(
                            this, android.support.v7.appcompat.R.drawable.abc_btn_radio_material);
                    radioDrawable.setTintList(colorList);
                    view.setCompoundDrawablesRelativeWithIntrinsicBounds(radioDrawable, null, null, null);
                }

                return view;

            default:
                return super.onCreateView(name, context, attrs);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    @Override
    public void onPageSelected(int position) {
        mOnClickListener.onClick(mButtons[position]);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_neat:
                mPager.setCurrentItem(0);
                break;

            case R.id.menu_action:
                mPager.setCurrentItem(1);
                break;

            case R.id.menu_about:
                mPager.setCurrentItem(2);
                break;

            default:
                return false;
        }

        return true;
    }

    private void showExplainDialog() {
        if (mSharePref.getBoolean("showed_explain", false)) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.first_open_title)
                .setMessage(R.string.first_open_hint_message)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.no_longer_remind, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mSharePref.edit().putBoolean("showed_explain", true).apply();
                    }
                })
                .show();
    }

    private ColorStateList getCheckedColorList() {
        int[][] states = new int[2][];
        states[0] = new int[]{android.R.attr.state_checked};
        states[1] = new int[]{-android.R.attr.state_checked};

        int[] colors = new int[2];
        colors[0] = mThemeColor;
        colors[1] = Color.parseColor("#6b6b6b"); // 取色得到的未勾选颜色

        return new ColorStateList(states, colors);
    }
}