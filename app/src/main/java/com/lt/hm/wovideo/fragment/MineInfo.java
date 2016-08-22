package com.lt.hm.wovideo.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.gson.Gson;
import com.lt.hm.wovideo.R;
import com.lt.hm.wovideo.acache.ACache;
import com.lt.hm.wovideo.base.BaseActivity;
import com.lt.hm.wovideo.base.BaseFragment;
import com.lt.hm.wovideo.db.NetUsageDatabase;
import com.lt.hm.wovideo.handler.UnLoginHandler;
import com.lt.hm.wovideo.handler.UserHandler;
import com.lt.hm.wovideo.http.HttpApis;
import com.lt.hm.wovideo.http.HttpUtils;
import com.lt.hm.wovideo.http.RespHeader;
import com.lt.hm.wovideo.http.ResponseCode;
import com.lt.hm.wovideo.http.ResponseObj;
import com.lt.hm.wovideo.http.parser.ResponseParser;
import com.lt.hm.wovideo.model.SearchResult;
import com.lt.hm.wovideo.model.UserModel;
import com.lt.hm.wovideo.utils.DialogHelp;
import com.lt.hm.wovideo.utils.FileUtil;
import com.lt.hm.wovideo.utils.ImageUtils;
import com.lt.hm.wovideo.utils.SharedPrefsUtils;
import com.lt.hm.wovideo.utils.StringUtils;
import com.lt.hm.wovideo.utils.TLog;
import com.lt.hm.wovideo.utils.UIHelper;
import com.lt.hm.wovideo.utils.UT;
import com.lt.hm.wovideo.widget.CircleImageView;
import com.lt.hm.wovideo.widget.SecondTopbar;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import okhttp3.Call;

import static com.lt.hm.wovideo.R.id.bref_expand;
import static com.lt.hm.wovideo.R.id.p_info_logo;
import static com.lt.hm.wovideo.R.id.pc_username;
import static com.lt.hm.wovideo.utils.FileUtil.*;


/**
 * Created by xuchunhui on 16/8/16.
 */
public class MineInfo extends BaseFragment implements View.OnClickListener {
    public static final int ACTION_TYPE_ALBUM = 0;
    public static final int ACTION_TYPE_PHOTO = 1;
    @BindView(R.id.head_icon)
    ImageView headIcon;
    @BindView(R.id.login_tag)
    TextView loginTag;
    @BindView(R.id.regist_tag)
    TextView registTag;
    @BindView(R.id.person_head_bg)
    LinearLayout personHeadBg;
    @BindView(R.id.order)
    RelativeLayout order;
    @BindView(R.id.integral)
    RelativeLayout integral;
    @BindView(R.id.history)
    RelativeLayout history;
    @BindView(R.id.collect)
    RelativeLayout collect;
    @BindView(R.id.mine_tag)
    RelativeLayout mineTag;
    @BindView(R.id.active)
    RelativeLayout active;
    @BindView(R.id.btn_set)
    ImageView btnSet;
    @BindView(R.id.btn_person_back)
    ImageView btnPersonBack;
    @BindView(R.id.unlogin_layout)
    LinearLayout unloginLayout;
    @BindView(R.id.login_layout)
    LinearLayout login_layout;
    @BindView(pc_username)
    TextView tv_username;
    @BindView(R.id.network_usage_text)
    TextView tv_network_usage;
    @BindView(R.id.person_etime)
    TextView person_etime;
    private Uri origUri;
    private Uri cropUri;
    private String theLarge;
    protected NetUsageDatabase netUsageDatabase;
    private final static String FILE_SAVEPATH = Environment
            .getExternalStorageDirectory().getAbsolutePath()
            + "/WoVideo/Portrait/";
    private File protraitFile;
    private Bitmap protraitBitmap;
    private String protraitPath;
    private final static int CROP = 500;
    private View view;
    private Unbinder unbinder;

    @Override
    protected int getLayoutId() {
        return R.layout.layout_person;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (view == null) {
            view = inflater.inflate(getLayoutId(), container, false);
            unbinder = ButterKnife.bind(this, view);
            initView(view);
            initData();
        }
        return view;
    }

    @Override
    public void initView(View view) {
        initPersonInfo();
    }

    private void initPersonInfo() {
        if (!TextUtils.isEmpty(ACache.get(getApplicationContext()).getAsString("img_back_url"))) {
            personHeadBg.setBackground(FileUtil.getImageDrawable(ACache.get(getApplicationContext()).getAsString("img_back_url")));
        }
        UserModel model = UserHandler.getUserInfo(getApplicationContext());
        netUsageDatabase = new NetUsageDatabase(getApplicationContext());
        if (FILE_SAVEPATH != null) {
            Glide.with(this).load(ACache.get(getApplicationContext()).getAsString("img_url")).asBitmap().centerCrop().error(R.drawable.icon_head).into(headIcon);
        }
        if (model != null) {
            if (!StringUtils.isNullOrEmpty(model.getHeadImg())) {
                TLog.log(HttpUtils.appendUrl(model.getHeadImg().toString()));
                Glide.with(this).load(HttpUtils.appendUrl(model.getHeadImg())).asBitmap().centerCrop().into(headIcon);
            } else {
                headIcon.setImageDrawable(getResources().getDrawable(R.drawable.icon_head));
            }
            if (model.getNickName() != null) {
                tv_username.setText(model.getNickName());
            } else {
                String phoneNum = model.getPhoneNo();
                tv_username.setText(phoneNum.substring(0, phoneNum.length() - (phoneNum.substring(3)).length()) + "****" + phoneNum.substring(7));
            }
            if (model.getEtime() != null) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                Long time = new Long(model.getEtime());
                String day = format.format(time);
                person_etime.setText("有效期:" + day);
            }
            unloginLayout.setVisibility(View.GONE);
            login_layout.setVisibility(View.VISIBLE);
        } else {
            unloginLayout.setVisibility(View.VISIBLE);
            login_layout.setVisibility(View.GONE);
        }

        btnSet.setOnClickListener(this);
        headIcon.setOnClickListener(this);
        loginTag.setOnClickListener(this);
        registTag.setOnClickListener(this);
        order.setOnClickListener(this);
        integral.setVisibility(View.GONE);
        integral.setOnClickListener(this);
        history.setOnClickListener(this);
        collect.setOnClickListener(this);

        mineTag.setVisibility(UserHandler.isLogin(getApplicationContext()) ? View.VISIBLE : View.GONE);

        mineTag.setOnClickListener(this);
        active.setOnClickListener(this);
        btnPersonBack.setVisibility(View.GONE);
        tv_username.setOnClickListener(this);
        String text = String.format(getResources().getString(R.string.network), netUsageDatabase.querySum("") / 1024 / 1024);
        tv_network_usage.setText(text);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_set:
                UIHelper.ToSetPage(getActivity());
                break;
            case R.id.head_icon:
                // TODO: 16/6/6  变更头像 上传头像
//                String user = ACache.get(getApplicationContext()).getAsString("userinfo");
                if (UserHandler.isLogin(getApplicationContext())) {
                    handleSelectPicture();
                } else {
                    UnLoginHandler.unLogin(getActivity());
                }

                break;
            case R.id.login_tag:
                UIHelper.ToLogin(getActivity());
                break;
            case R.id.regist_tag:
                UIHelper.ToRegister(getActivity());
                break;
            case R.id.order:
                UIHelper.ToBillsPage(getActivity());
                break;
            case R.id.integral:
                UIHelper.ToMineIntegral(getActivity());
                break;
            case R.id.history:
                // TODO: 16/6/6 观看历史
                UIHelper.ToHistoryPage(getActivity());
                break;
            case R.id.collect:
                // TODO: 16/6/6 我的收藏
                if (UserHandler.isLogin(getApplicationContext())) {
                    UIHelper.ToCollectPage(getActivity());
                }

                break;
            case R.id.mine_tag:
                UIHelper.ToTagPage(getActivity(), true);
                break;
            case R.id.active:
                UIHelper.ToEventPage(getActivity());
                break;
            case R.id.btn_person_back:
                getActivity().finish();
                break;
            case pc_username:
                UIHelper.ToPersonInfoPage(getActivity());
                break;
        }
    }


    private void handleSelectPicture() {
        DialogHelp.getSelectDialog(getContext(), "选择图片", getActivity().getResources().getStringArray(R.array.choose_picture), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                goToSelectPicture(i);
            }
        }).show();
    }

    private void goToSelectPicture(int position) {
        switch (position) {
            case ACTION_TYPE_ALBUM:
                startImagePick();
                break;
            case ACTION_TYPE_PHOTO:
                startTakePhoto();
                break;
            default:
                break;
        }
    }

    /**
     * 选择图片裁剪
     */
    private void startImagePick() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "选择图片"),
                    ImageUtils.REQUEST_CODE_GETIMAGE_BYCROP);
        } else {
            intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(Intent.createChooser(intent, "选择图片"),
                    ImageUtils.REQUEST_CODE_GETIMAGE_BYCROP);
        }
    }

    private void startTakePhoto() {
        Intent intent;
        // 判断是否挂载了SD卡
        String savePath = "";
        String storageState = Environment.getExternalStorageState();
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            savePath = Environment.getExternalStorageDirectory()
                    .getAbsolutePath() + "/WoVideo/Camera/";
            TLog.log(savePath);
            File savedir = new File(savePath);
            if (!savedir.exists()) {
                boolean flagg = savedir.mkdirs();
                if (!flagg) {
                    TLog.log("图片保存失败，请稍后重试");
                    savedir.mkdir();
                }
            }
        }

        // 没有挂载SD卡，无法保存文件
        if (StringUtils.isEmpty(savePath)) {
//            ToastUtils.showToastShort("无法保存照片，请检查SD卡是否挂载");
            TLog.log("无法保存照片，请检查SD卡是否挂载");
            return;
        }

        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(new Date());
        String fileName = "osc_" + timeStamp + ".jpg";// 照片命名
        File out = new File(savePath, fileName);
        Uri uri = Uri.fromFile(out);
        origUri = uri;

        theLarge = savePath + fileName;// 该照片的绝对路径

        intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent,
                ImageUtils.REQUEST_CODE_GETIMAGE_BYCAMERA);
    }

    // 裁剪头像的绝对路径
    private Uri getUploadTempFile(Uri uri) {
        String storageState = Environment.getExternalStorageState();
        if (storageState.equals(Environment.MEDIA_MOUNTED)) {
            File savedir = new File(FILE_SAVEPATH);
            if (!savedir.exists()) {
                savedir.mkdirs();
            }
        } else {
//            AppContext.showToast("无法保存上传的头像，请检查SD卡是否挂载");
            return null;
        }
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(new Date());
        String thePath = ImageUtils.getAbsolutePathFromNoStandardUri(uri);

        // 如果是标准Uri
        if (StringUtils.isEmpty(thePath)) {
            thePath = ImageUtils.getAbsoluteImagePath(getActivity(), uri);
        }
        String ext = getFileFormat(thePath);
        ext = StringUtils.isEmpty(ext) ? "jpg" : ext;
        // 照片命名
        String cropFileName = "wovideo_crop_" + timeStamp + "." + ext;
        // 裁剪头像的绝对路径
        protraitPath = FILE_SAVEPATH + cropFileName;
        protraitFile = new File(protraitPath);

        cropUri = Uri.fromFile(protraitFile);
        return this.cropUri;
    }

    /**
     * 上传新照片
     */
    private void uploadNewPhoto() {
//        showWaitDialog("正在上传头像...");

        // 获取头像缩略图
        if (!StringUtils.isEmpty(protraitPath) && protraitFile.exists()) {
            protraitBitmap = ImageUtils
                    .loadImgThumbnail(protraitPath, 200, 200);
        } else {
//            AppContext.showToast("图像不存在，上传失败");
            TLog.log("图像不存在，上传失败");
        }
        ACache.get(getApplicationContext()).put("img_url", protraitFile.getAbsolutePath());

        if (protraitBitmap != null) {
            String img64 = ImageUtils.imgToBase64(protraitFile.getAbsolutePath(), protraitBitmap, "JPG");
            HashMap<String, Object> map = new HashMap<>();
            String string = ACache.get(getApplicationContext()).getAsString("userinfo");
            map.put("phone", UserHandler.getUserInfo(getApplicationContext()).getPhoneNo());
            map.put("base", "image/jpg;base64," + img64);
            TLog.log(map.toString());
            HttpApis.uploadHeadImg(map, new StringCallback() {
                @Override
                public void onError(Call call, Exception e, int id) {
                    TLog.log(e.getMessage());
                }

                @Override
                public void onResponse(String response, int id) {
                    TLog.log(response);
                    Glide.with(getApplicationContext()).load(ACache.get(getApplicationContext()).getAsString("img_url")).asBitmap().centerCrop().error(R.drawable.icon_head).into(headIcon);
                    // TODO: 16/7/6 刷新个人中心头像图片
                }
            });

        }
    }

    /**
     * 拍照后裁剪
     *
     * @param data 原始图片
     */
    private void startActionCrop(Uri data) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(data, "image/*");
        intent.putExtra("output", this.getUploadTempFile(data));
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", 1);// 裁剪框比例
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", CROP);// 输出图片大小
        intent.putExtra("outputY", CROP);
        intent.putExtra("scale", true);// 去黑边
        startActivityForResult(intent,
                ImageUtils.REQUEST_CODE_GETIMAGE_BYSDCARD);
    }


    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
                                 final Intent imageReturnIntent) {
        if (resultCode != Activity.RESULT_OK)
            return;
        switch (requestCode) {
            case ImageUtils.REQUEST_CODE_GETIMAGE_BYCAMERA:
                startActionCrop(origUri);// 拍照后裁剪
                break;
            case ImageUtils.REQUEST_CODE_GETIMAGE_BYCROP:
                startActionCrop(imageReturnIntent.getData());// 选图后裁剪
                break;
            case ImageUtils.REQUEST_CODE_GETIMAGE_BYSDCARD:
                uploadNewPhoto();
                break;
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        initPersonInfo();
    }


}


