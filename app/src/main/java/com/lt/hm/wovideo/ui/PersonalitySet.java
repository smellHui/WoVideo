package com.lt.hm.wovideo.ui;

import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.TextView;
import android.widget.Toast;

import com.lt.hm.wovideo.R;
import com.lt.hm.wovideo.adapter.category.CategoryAdapter;
import com.lt.hm.wovideo.base.BaseActivity;
import com.lt.hm.wovideo.interf.OnCateItemListener;
import com.lt.hm.wovideo.model.Category;
import com.lt.hm.wovideo.widget.CustomTopbar;
import com.lt.hm.wovideo.widget.DividerDecoration;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

public class PersonalitySet extends BaseActivity implements CustomTopbar.myTopbarClicklistenter, OnCateItemListener {

    private static final int TOTAL_LINE = 5;

    @BindView(R.id.person_topbar)
    CustomTopbar personTopbar;
    @BindView(R.id.recycler_personal_mine)
    RecyclerView mineCateRecycler;
    @BindView(R.id.recycler_personal_all)
    RecyclerView allCateRecycler;
    @BindView(R.id.text_change)
    TextView changeText;
    @BindView(R.id.text_unchange)
    TextView unchangeText;

    private CategoryAdapter adapter;
    private CategoryAdapter allAdapter;
    private List<Category> list = new ArrayList<>();
    private List<Category> allList = new ArrayList<>();

    @Override
    protected int getLayoutId() {
        return R.layout.activity_personality_set;
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        personTopbar.setLeftIsVisible(true);
        personTopbar.setRightIsVisible(true);
        personTopbar.setRightText("编辑");
        personTopbar.setOnTopbarClickListenter(this);
        changeText.setText("我的频道");
        unchangeText.setText("频道栏目");
    }

    @Override
    public void initViews() {
        adapter = new CategoryAdapter(this, list, this,Category.FIRST_TYPE);
        allAdapter = new CategoryAdapter(this, allList, this,Category.SECOND_TYPE);
        GridLayoutManager mLayoutManager = new GridLayoutManager(this, TOTAL_LINE);
        GridLayoutManager mLayoutManager2 = new GridLayoutManager(this, TOTAL_LINE);
        DividerDecoration decoration = new DividerDecoration(getApplicationContext());
        mineCateRecycler.addItemDecoration(decoration);
        allCateRecycler.addItemDecoration(decoration);
        mineCateRecycler.setLayoutManager(mLayoutManager);
        allCateRecycler.setLayoutManager(mLayoutManager2);
        mineCateRecycler.setAdapter(adapter);
        allCateRecycler.setAdapter(allAdapter);
    }

    @Override
    public void initDatas() {
        list.add(new Category("我的频道", 0));
        list.add(new Category("奶油", 1));
        list.add(new Category("威化", 1));
        list.add(new Category("凤梨", 1));
        list.add(new Category("烧饼", 1));
        list.add(new Category("烧饼", 1));
        list.add(new Category("烧饼", 1));
        list.add(new Category("烧饼", 1));

        allList.add(new Category("频道栏目", 0));
        allList.add(new Category("饼干", 1));
        allList.add(new Category("饼干", 1));
        allList.add(new Category("饼干", 1));
        allList.add(new Category("饼干", 1));
        allList.add(new Category("饼干", 1));
        allList.add(new Category("饼干", 1));
    }

    @Override
    public void leftClick() {
        this.finish();
    }

    @Override
    public void rightClick() {
      adapter.toggleCanDelete();
        setEditorText(adapter.isCanDel);
    }


    @Override
    public void OnItemClick(int type, int pos) {
        btnAddItem(type,pos);
        btnRemoveItem(type, pos);
        Toast.makeText(this, "---" + pos, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void OnItemLongClick() {
        if (adapter.isCanDel) return;//已处于删除状态,长按无效
        setEditorText(true);
        adapter.toggleCanDelete();
    }

    private void setEditorText(boolean editor){
        personTopbar.setRightText(editor?"完成":"编辑");
    }

    /**
     * 添加某项
     * @param type
     * @param pos
     */
    public void btnAddItem(int type,int pos) {
        if (type == Category.FIRST_TYPE) {
            allList.add(0, list.get(pos));
            allAdapter.notifyDataSetChanged();
            return;
        }
        list.add(allList.get(pos));
        adapter.notifyDataSetChanged();
    }

    /**
     * 删除某项
     * @param type
     * @param pos
     */
    public void btnRemoveItem(int type, int pos) {
        if (type == Category.FIRST_TYPE){
            if (!list.isEmpty()) {
                list.remove(pos);
            }
            adapter.notifyItemRemoved(pos);
            return;
        }
        if (!allList.isEmpty()) {
            allList.remove(pos);
        }
        allAdapter.notifyItemRemoved(pos);
    }




}
