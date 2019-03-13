package com.delaroystudios.imagepdf;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.theartofdev.edmodo.cropper.CropImage;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.engine.impl.PicassoEngine;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.util.ArrayList;
import java.util.HashMap;

import static com.delaroystudios.imagepdf.Constants.AUTHORITY_APP;
import static com.delaroystudios.imagepdf.Constants.DEFAULT_PAGE_SIZE;
import static com.delaroystudios.imagepdf.Constants.DEFAULT_PAGE_SIZE_TEXT;
import static com.delaroystudios.imagepdf.Constants.IMAGE_SCALE_TYPE_ASPECT_RATIO;
import static com.delaroystudios.imagepdf.Constants.RESULT;
import static com.delaroystudios.imagepdf.StringUtils.showSnackbar;


public class BlankFragment extends Fragment implements View.OnClickListener, OnPDFCreatedInterface {

    Activity mActivity;
    Button btn_select, btn_convert;
    private String mPath = "/storage/emulated/0/PDFfiles/";
    private String mFileName = "rezaul11 ";
    TextView mNoOfImages;
    public static ArrayList<String> mImagesUri = new ArrayList<>();
    ImageToPDFOptions imageToPDFOptions;
    public static String mImageScaleType = IMAGE_SCALE_TYPE_ASPECT_RATIO;
    Context context;
    public static final String MASTER_PWD_STRING = "master_password";
    public static final String appName = "PDF Converter";

    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT = 1;
    private static final int INTENT_REQUEST_APPLY_FILTER = 10;
    private static final int INTENT_REQUEST_PREVIEW_IMAGE = 11;
    private static final int INTENT_REQUEST_REARRANGE_IMAGE = 12;
    private static final int INTENT_REQUEST_GET_IMAGES = 13;


    @Override
    public void onAttach(Context contex) {
        super.onAttach(contex);
        mActivity = (Activity) contex;
        context = contex;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_blank, container, false);
        imageToPDFOptions = new ImageToPDFOptions();
        getRuntimePermissions(false);
        init(root);
        listener();
        return root;
    }

    private void init(View view) {
        mNoOfImages = view.findViewById(R.id.tv_NoOfImages);
        btn_select = view.findViewById(R.id.btn_select);
        btn_convert = view.findViewById(R.id.btn_convert);
        btn_convert.setEnabled(false);
    }

    private void listener() {
        btn_select.setOnClickListener(this);
        btn_convert.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_select:
                if (getRuntimePermissions(true)) {
                    selectImages();
                }
                break;
            case R.id.btn_convert:
                if (createPdf(false)) {
                    Intent intent = new Intent(getContext(), PDFViewActivity.class);
                    intent.putExtra("filePath", mPath);
                    intent.putExtra("fileName", mFileName);
                    startActivity(intent);
                } else {
                    createPdf(true);
                }
                break;
        }
    }

    private void selectImages() {
        Matisse.from(this)
                .choose(MimeType.ofImage(), false)
                .countable(true)
                .capture(true)
                .captureStrategy(new CaptureStrategy(true, AUTHORITY_APP))
                .maxSelectable(1000)
                .imageEngine(new PicassoEngine())
                .forResult(INTENT_REQUEST_GET_IMAGES);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null)
            return;
        switch (requestCode) {
            case INTENT_REQUEST_GET_IMAGES:
                mImagesUri.clear();
                mImagesUri.addAll(Matisse.obtainPathResult(data));
                if (mImagesUri.size() > 0) {
                    mNoOfImages.setText(String.format(mActivity.getResources()
                            .getString(R.string.images_selected), mImagesUri.size()));
                    btn_convert.setEnabled(true);
                    showSnackbar(mActivity, R.string.snackbar_images_added);
                    btn_convert.setEnabled(true);
                } else {
                    btn_convert.setEnabled(false);
                }
                break;
            case CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE:
                HashMap<Integer, Uri> croppedImageUris =
                        (HashMap) data.getSerializableExtra(CropImage.CROP_IMAGE_EXTRA_RESULT);
                for (int i = 0; i < mImagesUri.size(); i++) {
                    if (croppedImageUris.get(i) != null) {
                        mImagesUri.set(i, croppedImageUris.get(i).getPath());
                        showSnackbar(mActivity, R.string.snackbar_imagecropped);
                    }
                }
                break;
            case INTENT_REQUEST_APPLY_FILTER:
                mImagesUri.clear();
                ArrayList<String> mFilterUris = data.getStringArrayListExtra(RESULT);
                int size = mFilterUris.size() - 1;
                for (int k = 0; k <= size; k++)
                    mImagesUri.add(mFilterUris.get(k));
                break;
            case INTENT_REQUEST_PREVIEW_IMAGE:
                mImagesUri = data.getStringArrayListExtra(RESULT);
                if (mImagesUri.size() > 0) {
                    btn_convert.setVisibility(View.VISIBLE);
                } else {
                    btn_convert.setVisibility(View.GONE);
                }
                break;
            case INTENT_REQUEST_REARRANGE_IMAGE:
                mImagesUri = data.getStringArrayListExtra(RESULT);
                showSnackbar(mActivity, R.string.images_rearranged);
                break;
        }
    }

    @SuppressLint("SdCardPath")
    private boolean createPdf(boolean check) {
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String mHomePath = String.valueOf(mSharedPreferences.getString("result", ""));
        imageToPDFOptions.setImagesUri(mImagesUri);
        imageToPDFOptions.setPageSize(mSharedPreferences.getString(DEFAULT_PAGE_SIZE_TEXT, DEFAULT_PAGE_SIZE));
        imageToPDFOptions.setImageScaleType(mImageScaleType);
        imageToPDFOptions.setOutFileName(mFileName);
        imageToPDFOptions.setPageNumStyle(Constants.PG_NUM_STYLE_PAGE_X_OF_N);
        imageToPDFOptions.setMasterPwd(mSharedPreferences.getString(MASTER_PWD_STRING, appName));
        imageToPDFOptions.setQualityString("30");

        new CreatePdf(imageToPDFOptions, mPath, this).execute();
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length < 1)
            return;
        switch (requestCode) {
            case PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    boolean mOpenSelectImages = false;
                    if (mOpenSelectImages)
                        selectImages();
                    showSnackbar(mActivity, R.string.snackbar_permissions_given);
                } else
                    showSnackbar(mActivity, R.string.snackbar_insufficient_permissions);
            }
        }
    }

    @Override
    public void onPDFCreationStarted() {

    }

    @Override
    public void onPDFCreated(boolean success, String path) {

    }

    private boolean getRuntimePermissions(boolean openImagesActivity) {
        if ((ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED) ||
                (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE_RESULT);
            return false;
        }
        return true;
    }
}
