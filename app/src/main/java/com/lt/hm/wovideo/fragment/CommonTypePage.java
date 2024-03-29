package com.lt.hm.wovideo.fragment;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.daimajia.slider.library.Indicators.PagerIndicator;
import com.daimajia.slider.library.SliderTypes.BaseSliderView;
import com.lt.hm.wovideo.R;
import com.lt.hm.wovideo.adapter.home.FilmListAdapter;
import com.lt.hm.wovideo.adapter.home.LikeListAdapter;
import com.lt.hm.wovideo.adapter.home.SmallVideoAdapter;
import com.lt.hm.wovideo.adapter.recommend.GridAdapter;
import com.lt.hm.wovideo.adapter.recommend.LiveAdapter;
import com.lt.hm.wovideo.base.BaseLazyFragment;
import com.lt.hm.wovideo.http.HttpApis;
import com.lt.hm.wovideo.http.HttpCallback;
import com.lt.hm.wovideo.http.HttpUtils;
import com.lt.hm.wovideo.http.NetUtils;
import com.lt.hm.wovideo.interf.OnPlaceChangeListener;
import com.lt.hm.wovideo.interf.OnUpdateLocationListener;
import com.lt.hm.wovideo.interf.onChangeLister;
import com.lt.hm.wovideo.model.BannerList;
import com.lt.hm.wovideo.model.CateTagListModel;
import com.lt.hm.wovideo.model.CateTagModel;
import com.lt.hm.wovideo.model.ChannelModel;
import com.lt.hm.wovideo.model.FilmMode;
import com.lt.hm.wovideo.model.LikeModel;
import com.lt.hm.wovideo.model.LocalCityModel;
import com.lt.hm.wovideo.model.response.ResponseBanner;
import com.lt.hm.wovideo.model.response.ResponseCateTag;
import com.lt.hm.wovideo.model.response.ResponseFilms;
import com.lt.hm.wovideo.model.response.ResponseLikeList;
import com.lt.hm.wovideo.model.response.ResponseLocalCityModel;
import com.lt.hm.wovideo.ui.CityListPage;
import com.lt.hm.wovideo.ui.LivePage;
import com.lt.hm.wovideo.ui.NewClassDetailPage;
import com.lt.hm.wovideo.utils.StringUtils;
import com.lt.hm.wovideo.utils.TLog;
import com.lt.hm.wovideo.utils.UIHelper;
import com.lt.hm.wovideo.utils.UT;
import com.lt.hm.wovideo.utils.UpdateLocationMsg;
import com.lt.hm.wovideo.utils.ViewPageChangeMsg;
import com.lt.hm.wovideo.utils.imageloader.ImageLoaderUtil;
import com.lt.hm.wovideo.widget.AutoSliderView;
import com.lt.hm.wovideo.widget.CustomGridView;
import com.lt.hm.wovideo.widget.CustomListView;
import com.lt.hm.wovideo.widget.SliderLayout;
import com.lt.hm.wovideo.widget.SpacesItemDecoration;
import com.lt.hm.wovideo.widget.TopTileView;
import com.lt.hm.wovideo.widget.ptrpull.SamplePtrFrameLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import in.srain.cube.views.ptr.PtrDefaultHandler;
import in.srain.cube.views.ptr.PtrFrameLayout;
import in.srain.cube.views.ptr.PtrHandler;

import static android.support.v7.widget.RecyclerView.GONE;
import static android.support.v7.widget.RecyclerView.OnClickListener;
import static android.support.v7.widget.RecyclerView.VISIBLE;

/**
 * Created by xuchunhui on 16/8/8.
 */
public class CommonTypePage extends BaseLazyFragment implements SwipeRefreshLayout.OnRefreshListener, OnUpdateLocationListener, onChangeLister
        , BaseSliderView.OnSliderClickListener, SliderLayout.disScroll {

    public static final String CHANNEL = "channel";
    private View view;
    private String channelCode;
    private ChannelModel channel;
    private String cityCode;
    private List<CateTagModel> cateTags = new ArrayList<>();//标签
    private List<LocalCityModel> localCites = new ArrayList<>();
    private List<BannerList.Banner> banner_list = new ArrayList<>();//bar
    private List<LikeModel> grid_list = new ArrayList<LikeModel>();//猜你喜欢
    private List<FilmMode> films = new ArrayList<>();//电影电视剧列表
    private BaseQuickAdapter listAdapter;

    @BindView(R.id.ptr_refresh)
    SamplePtrFrameLayout ptrFrameLayout;
//    ImageIndicatorView imageIndicatorView;

    @BindView(R.id.recycler_recommend)
    RecyclerView mRecyclerView;
    private CustomGridView cateGv;
    private TextView changeCityBtn;
    private View recommendImg;
    private View topPageFl;
    private View autoloopView;
    SliderLayout sliderLayout;
    PagerIndicator pagerIndicator;
    ImageView imgTopPage;
    TextView tvTitle;
    TextView tvLine;
    TextView tvType;
    TextView tvDesc;

    View titleRl;
    CustomListView liveLv;
    TextView tv_right;
    private View mHeadView;

    private boolean isOnline = true;
    private LiveAdapter liveAdapter;
    private Context context;
    private boolean isHasView = false;//防止重复加载view
    Unbinder unbinder;

    private LinearLayoutManager layoutManager;

    private boolean isLoading = false;//防止scrollview滚动多次请求数据
    private boolean isNoData = false;//数据加载完了
    private int pageNum = 1;//页码
    private int numPerPage = 10;//每页条数
    private String seed;//翻页查询种子，这个参数如果不传会随机查询，然后会返回这个值，在翻页的时候要将这个值传入，否则会出现重复推荐，可选
    private String tag;//兴趣标签，用户的user信息,可选

    public static CommonTypePage getInstance(ChannelModel channel) {
        CommonTypePage common = new CommonTypePage();
        Bundle bundle = new Bundle();
        bundle.putSerializable(CHANNEL, channel);
        common.setArguments(bundle);
        return common;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.frag_common_type_page;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (!isHasView) {
            view = inflater.inflate(getLayoutId(), container, false);
            unbinder = ButterKnife.bind(this, view);
            UpdateLocationMsg.getInstance().addRegisterSucListeners(this);
            ViewPageChangeMsg.getInstance().addRegisterSucListeners(this);
        }
        //缓存的rootView需要判断是否已经被加过parent， 如果有parent需要从parent删除，要不然会发生这个rootview已经有parent的错误。
        ViewGroup parent = (ViewGroup) view.getParent();
        if (parent != null) {
            parent.removeView(view);
        }
        return view;
    }

    @Override
    public void onFirstUserVisible() {
        if (isHasView) return;
        TLog.error("OnFirst---");
        isHasView = true;
        initBundleData();
        initHeadView();
        initView(null);
        autoRefresh();
    }

    /**
     * 获取bundle值
     */
    private void initBundleData() {
        context = getApplicationContext();
        Bundle bundle = getArguments();
        if (bundle != null) {
            channel = (ChannelModel) bundle.getSerializable(CHANNEL);
            assert channel != null;
            channelCode = channel.getFunCode();
        }
        TLog.error(channelCode + "");
    }

    private void initHeadView() {
        mHeadView = View.inflate(context, R.layout.include_header_recycler, null);
        autoloopView = mHeadView.findViewById(R.id.layout_ent_gallery);
        sliderLayout = (SliderLayout) mHeadView.findViewById(R.id.slider);
        pagerIndicator = (PagerIndicator) mHeadView.findViewById(R.id.custom_indicator);
        recommendImg = mHeadView.findViewById(R.id.frame_recommend);
        changeCityBtn = (TextView) mHeadView.findViewById(R.id.text_change_city);
        cateGv = (CustomGridView) mHeadView.findViewById(R.id.recycle_cate);
        liveLv = (CustomListView) mHeadView.findViewById(R.id.lv_live);
        topPageFl = mHeadView.findViewById(R.id.fl_page);
        tv_right = (TextView) mHeadView.findViewById(R.id.tv_right);
        titleRl = mHeadView.findViewById(R.id.rl_title);
        imgTopPage = (ImageView) mHeadView.findViewById(R.id.item_img_bg);
        tvTitle = (TextView) mHeadView.findViewById(R.id.item_title);
        tvLine = (TextView) mHeadView.findViewById(R.id.line_tv);
        tvType = (TextView) mHeadView.findViewById(R.id.item_type);
        tvDesc = (TextView) mHeadView.findViewById(R.id.item_desc);

        initSlider();
    }

    /*初始化导航图*/
    private void initSlider() {
        if (sliderLayout == null || pagerIndicator == null) return;
        sliderLayout.setPresetTransformer(SliderLayout.Transformer.Default);
        sliderLayout.setPresetIndicator(SliderLayout.PresetIndicators.Center_Bottom);
        sliderLayout.setDuration(4000);
        sliderLayout.setCustomIndicator(pagerIndicator);
        sliderLayout.setDisScroll(this);
    }

    /**
     * 初始化轮播图
     *
     * @param mList
     */
    private void initSlider(List<BannerList.Banner> mList) {
        autoloopView.setVisibility(VISIBLE);
        for (BannerList.Banner pageIconBean : mList) {
            AutoSliderView textSliderView = new AutoSliderView(this.getActivity(), pageIconBean);
            // initialize a SliderLayout
            textSliderView
                    .description(pageIconBean.getTypeName())
                    .image(HttpUtils.appendUrl(pageIconBean.getImg()))
                    .setScaleType(BaseSliderView.ScaleType.Fit)
                    .setOnSliderClickListener(this);

            //add your extra information
            Bundle bundle = new Bundle();
            bundle.putString("typeId", pageIconBean.getVfType());
            bundle.putString("id", pageIconBean.getOutid());
            textSliderView.bundle(bundle);
            sliderLayout.addSlider(textSliderView);
        }
    }

    @Override
    public void initView(View view) {
        addLikeListView();
        initLiveTopView();

        ptrFrameLayout.disableWhenHorizontalMove(true);
        ptrFrameLayout.setPtrHandler(new PtrHandler() {
            @Override
            public boolean checkCanDoRefresh(PtrFrameLayout frame, View content, View header) {
                return PtrDefaultHandler.checkContentCanBePulledDown(ptrFrameLayout, mRecyclerView, header);
            }

            @Override
            public void onRefreshBegin(PtrFrameLayout frame) {
                initData();
            }
        });

        changeCityBtn.setOnClickListener((View v) -> {
            startActivityForResult(new Intent(getContext(), CityListPage.class), 99);
        });
        topPageFl.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (channelCode) {
                    case ChannelModel.LOCAL_ID://地方
                        if (localCityModel != null)
                            ToLivePage(localCityModel.getUrl(), localCityModel.getTvName(), localCityModel.getProperty());
                        break;
                    default:
                        // 跳转视频详情页面
                        if (filmMode != null)
                            changePage(filmMode.getTypeId(), filmMode.getVfinfo_id());
                        break;
                }
            }
        });
    }

    private void autoRefresh() {
        ptrFrameLayout.postDelayed(() -> ptrFrameLayout.autoRefresh(), 500);
    }

    /**
     * 初始化数据
     */
    @Override
    public void initData() {
        pageNum = 1;
        isNoData = false;
        seed = "";
        switch (channelCode) {
            case ChannelModel.RECOMMEND_ID://推荐
                grid_list.clear();
                if (banner_list.size() == 0) {
                    getBarData();
                }
                getYouLikeData();
                break;
            case ChannelModel.LOCAL_ID://地方
                localCites.clear();
                grid_list.clear();
                getTvsByCityCode();
                getYouLikeData();
                break;
            case ChannelModel.FILM_ID://电影
                films.clear();
                if (cateTags.size() == 0) {
                    getCateTag("1");
                }
                getListByType();
                break;
            case ChannelModel.TELEPLAY_ID://电视剧
                if (cateTags.size() == 0) {
                    getCateTag("2");
                }
                getListByType();
                break;
            case ChannelModel.SPORTS_ID://体育
                if (cateTags.size() == 0) {
                    getCateTag("4");
                }
                getListByType();
                break;
            case ChannelModel.VARIATY_ID://综艺
                if (cateTags.size() == 0) {
                    getCateTag("3");
                }
                getListByType();
                break;
            default://其他
                getListByType();
        }
    }

    /*
     地方直播列表
     */
    private LocalCityModel localCityModel;

    private void addLocalListView() {
        liveLv.setVisibility(VISIBLE);
        topPageFl.setVisibility(View.VISIBLE);
        changeCityBtn.setVisibility(View.VISIBLE);
        localCityModel = localCites.get(0);
        setDataToTopView(localCityModel.getTvName(), "", localCityModel.getNowPro(), localCityModel.getImg());
        localCites.remove(0);
        if (localCites.size() == 0) return;
        liveAdapter.notifyView(localCites);
        liveLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ToLivePage(localCites.get(position - 1).getUrl(), localCites.get(position - 1).getTvName(), localCites.get(position - 1).getProperty());
            }
        });
    }

    private void ToLivePage(String url, String name, String property) {
        Bundle bundle = new Bundle();
        bundle.putString(LivePage.PLAY_URL, url);
        bundle.putString(LivePage.PLAY_NAME, name);
        bundle.putString(LivePage.PLAY_PROPERTY, property);
        UIHelper.ToLivePage(getActivity(), bundle);
    }

    /**
     * 第一个布局图显示字体
     */
    private void setDataToTopView(String name, String typeStr, String descStr, String imgUrl) {

        tvTitle.setText(name);
        ImageLoaderUtil.getInstance().loadImage(imgTopPage, imgUrl, true);
        if (TextUtils.isEmpty(typeStr)) {
            tvDesc.setVisibility(GONE);
            tvLine.setVisibility(GONE);
            tvType.setCompoundDrawables(null, null, null, null);
            tvType.setText("正在播放:" + descStr);
        } else {
            tvType.setText(typeStr);
        }
        tvDesc.setText(descStr);
    }

    /**
     * 添加bar滚动
     *
     * @param mList
     */
    private void addBarView(List<BannerList.Banner> mList) {
        initSlider(mList);
    }


    /*
     *地方直播列表
     *先加载这个方法,以免recycleview加不了header
     */
    private boolean isNotFilm = true;
    private FilmMode filmmode;
    private LikeModel likemodel;
    private SpacesItemDecoration decoration;


    /**
     * 初始化recyclerview
     */
    private void addLikeListView() {
        layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        if (channelCode.equals(ChannelModel.RECOMMEND_ID) || channelCode.equals(ChannelModel.LOCAL_ID)) {
            listAdapter = new LikeListAdapter(R.layout.layout_new_home_item, grid_list);
            listAdapter.setOnRecyclerViewItemClickListener((view1, i) -> {
                likemodel = (LikeModel) listAdapter.getData().get(i);
                // 跳转视频详情页面
                changePage(likemodel.getTypeId(), likemodel.getId());
            });
        } else {
            if (channelCode.equals(ChannelModel.FILM_ID)) {
                isNotFilm = false;
                layoutManager = new GridLayoutManager(getApplicationContext(), 3);
                layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
                if (decoration == null)
                    decoration = new SpacesItemDecoration(10);
                mRecyclerView.addItemDecoration(decoration);
                listAdapter = new FilmListAdapter(R.layout.layout_new_home_movie_item, films, isNotFilm);
            } else if (channelCode.equals(ChannelModel.TELEPLAY_ID) || channelCode.equals(ChannelModel.VARIATY_ID) || channelCode.equals(ChannelModel.SPORTS_ID)) {
                listAdapter = new FilmListAdapter(R.layout.layout_new_home_item, films, isNotFilm);
            } else {
                listAdapter = new SmallVideoAdapter(films);
            }
            listAdapter.setOnRecyclerViewItemClickListener((view1, i) -> {
                filmmode = (FilmMode) listAdapter.getData().get(i);
                // 跳转视频详情页面
                changePage(filmmode.getTypeId(), filmmode.getVfinfo_id());
            });
        }

        mRecyclerView.setLayoutManager(layoutManager);
        if (judgeIsSmallVideo() || channelCode.equals(ChannelModel.RECOMMEND_ID) || channelCode.equals(ChannelModel.LOCAL_ID)) {
            listAdapter.addHeaderView(mHeadView);
        }
        listAdapter.openLoadMore(numPerPage, true);
        listAdapter.setOnLoadMoreListener(new BaseQuickAdapter.RequestLoadMoreListener() {
            @Override
            public void onLoadMoreRequested() {
                if (isLoading || isNoData) return;
                isLoading = true;
                TLog.error("上拉加载----");

                switch (channelCode) {
                    case ChannelModel.RECOMMEND_ID://推荐
                    case ChannelModel.LOCAL_ID://地方
                        getYouLikeData();
                        break;
                    default://其他
                        getListByType();
                }
            }
        });
        mRecyclerView.setAdapter(listAdapter);


    }

    /**
     * 初始化直播列表
     */
    private void initLiveTopView() {
        TopTileView headView1 = new TopTileView(context);
        headView1.setTitleTv("北京直播");
        headView1.setImageVisiable(true);
        headView1.setImage(R.drawable.icon_more);
        headView1.setImageText("");
        headView1.setOnClickListener(v -> {
            TLog.error("直播--->");
            UIHelper.ToLivePage(getActivity());
        });
        liveLv.addHeaderView(headView1);

        liveAdapter = new LiveAdapter(context, localCites, R.layout.item_live_cate);
        liveLv.setAdapter(liveAdapter);
    }

    /**
     * 获取bar滚动条
     */
    private void getBarData() {
        NetUtils.getBarData(0, this);
    }

    /**
     * 获取兴趣列表
     */
    private void getYouLikeData() {
        NetUtils.getYouLikeData(pageNum, numPerPage, seed, tag, "", this);
    }

    /*
     *获取标签
     */
    private void getCateTag(String type) {
        NetUtils.getCateTag(type, this);
    }

    /**
     * 获取电台列表
     */
    private void getTvsByCityCode() {
        NetUtils.getTvsByCityCode(cityCode, this);
    }

    /**
     * 获取电影,电视剧列表
     */
    private void getListByType() {
        NetUtils.getListByType(channelCode, pageNum, numPerPage, this);
    }

    /**
     * 电影,电视剧分类gridview
     */
    private void addCateView() {
        if (cateTags.size() > 9) {//最多显示9个标签
            cateTags = cateTags.subList(0, 9);
            cateTags.add(new CateTagModel("更多", -1));
        }
        cateGv.setVisibility(View.VISIBLE);
        GridAdapter gridAdapter = new GridAdapter(getApplicationContext(), cateTags, R.layout.item_first_cate);
        cateGv.setAdapter(gridAdapter);
        cateGv.setNumColumns(5);
        cateGv.setPadding(20, 15, 20, 15);
        cateGv.setGravity(Gravity.CENTER);
        cateGv.setSelector(new ColorDrawable(Color.TRANSPARENT));
        cateGv.setOnItemClickListener((parent, view1, position, id) -> NewClassDetailPage.getInstance(getActivity(), cateTags.get(position), channelCode, cateTagListModel));
    }

    private CateTagListModel cateTagListModel;
    private FilmMode filmMode;

    @Override
    public <T> void onSuccess(T value, int flag) {
        super.onSuccess(value, flag);
        switch (flag) {//电影,电视剧标签
            case HttpApis.http_cate_tag:
                ResponseCateTag responseCateTag = (ResponseCateTag) value;
                cateTags = responseCateTag.getBody().getLx();
                cateTagListModel = responseCateTag.getBody();
                if (cateTags == null || cateTags.size() == 0) return;
                addCateView();
                break;
            case HttpApis.http_bar://获取bar
                ResponseBanner responseBar = (ResponseBanner) value;
                banner_list = responseBar.getBody().getBannerList();
                if (banner_list == null || banner_list.size() == 0) return;
                addBarView(banner_list);
                break;
            case HttpApis.http_you_like://获取like列表
                ResponseLikeList re = (ResponseLikeList) value;
                seed = re.getBody().getSeed();
                grid_list = re.getBody().getLikeList();
                if (grid_list == null || grid_list.size() == 0) {
                    isNoData = true;
                    UT.showNormal("暂无数据");
                    return;
                }

                if (channelCode.equals(ChannelModel.RECOMMEND_ID)) {
                    recommendImg.setVisibility(View.VISIBLE);
                }
                if (channelCode.equals(ChannelModel.LOCAL_ID)) {
                    titleRl.setVisibility(View.VISIBLE);
                }
                if (pageNum == 1) {
                    listAdapter.setNewData(grid_list);
                } else {
                    listAdapter.notifyDataChangedAfterLoadMore(grid_list, true);
                }
                pageNum++;
                break;
            case HttpApis.http_city_tv://获取城市电台
                ResponseLocalCityModel cityRe = (ResponseLocalCityModel) value;
                localCites = cityRe.getBody().getCitys();
                if (localCites == null || localCites.size() == 0) return;
                addLocalListView();
                break;
            case HttpApis.http_video_list:
                ResponseFilms filmRe = (ResponseFilms) value;
                films = filmRe.getBody().getTypeList();
                if (films == null || films.size() == 0) return;

                if (pageNum == 1 && judgeIsSmallVideo()) {
                    topPageFl.setVisibility(View.VISIBLE);
                    filmMode = films.get(0);
                    setDataToTopView(filmMode.getName(), filmMode.getTypeName(), filmMode.getHit(), filmMode.gethImg());
                    films.remove(0);
                }

                if (!judgeIsSmallVideo()) {
                    setSmallViewItemType(films);
                }

                if (films.size() == 0) {
                    isNoData = true;
                    UT.showNormal("暂无数据");
                    return;
                }

                if (pageNum == 1) {
                    listAdapter.setNewData(films);
                } else {
                    listAdapter.notifyDataChangedAfterLoadMore(films, true);
                }
                pageNum++;
                break;
        }
    }

    /**
     * 小视屏列表需要插入type值
     *
     * @param films
     */
    private void setSmallViewItemType(List<FilmMode> films) {
        for (int i = 0; i < films.size(); i++) {
            films.get(i).setItemType(i % 4 == 0 ? 0 : 1);
        }
    }

    /**
     * 判断是不是小视屏列表
     *
     * @return
     */
    private boolean judgeIsSmallVideo() {
        return channelCode.equals(ChannelModel.TELEPLAY_ID) ||
                channelCode.equals(ChannelModel.FILM_ID) ||
                channelCode.equals(ChannelModel.VARIATY_ID) ||
                channelCode.equals(ChannelModel.SPORTS_ID);
    }

    @Override
    public void onAfter(int flag) {
        super.onAfter(flag);
        switch (flag) {
            case HttpApis.http_you_like:
            case HttpApis.http_video_list:
                isLoading = false;
                if (ptrFrameLayout != null && ptrFrameLayout.isRefreshing()) {
                    ptrFrameLayout.refreshComplete();
                }
                break;
        }
    }

    //下拉刷新
    @Override
    public void onRefresh() {
        initData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (unbinder != null) {
            unbinder.unbind();
        }
    }

    private OnPlaceChangeListener onPlaceChangeListener;

    public void setOnPlaceChangeListener(OnPlaceChangeListener listener) {
        this.onPlaceChangeListener = listener;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (resultCode) {
            case CityListPage.CITY_RESULT:
                cityCode = data.getStringExtra("city");
                if (onPlaceChangeListener != null) {
                    onPlaceChangeListener.onChangePlaceListener(cityCode.replaceAll("[^\u4E00-\u9FA5]", ""));
                }
                cityCode = StringUtils.submitNum(cityCode);
                initData();
                break;
        }
    }

    private void changePage(int typeId, String vfId) {
        UIHelper.ToAllCateVideo(getActivity(), typeId, vfId);
    }

    @Override
    public void onUpdateLocListener(String name, String code) {
        cityCode = code;
    }

    @Override
    public void onDestroyView() {
        UpdateLocationMsg.getInstance().removeRegisterSucListeners(this);
        ViewPageChangeMsg.getInstance().removeRegisterSucListeners(this);
        super.onDestroyView();
    }

    @Override
    public void onChangeLister(boolean isEnable) {
    }

    @Override
    public void onSliderClick(BaseSliderView slider) {
        Bundle bundle = slider.getBundle();
        if (bundle == null) {
            UT.showNormal("播放地址无效");
            return;
        }
        String typeId = bundle.getString("typeId");
        String id = bundle.getString("id");
        changePage(Integer.valueOf(typeId), id);
    }

    @Override
    public void disScroll(boolean dis) {
        TLog.error("slider---ACTION_DOWN" + dis);
        ptrFrameLayout.setDisScroll(dis);
    }
}
