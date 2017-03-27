package com.gif.gifmaker;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gif.gifmaker.gifmaker.AnimatedGifEncoder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.nereo.multi_image_selector.MultiImageSelector;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final String TAG = "MainActivity";
    protected static final int REQUEST_STORAGE_READ_ACCESS_PERMISSION = 101;

    private static final int REQUEST_IMAGE = 2;
    private int delayTime;//帧间隔
    private EditText file_text;
    private TextView delay_text;
    private GifImageView gif_image;
    private List<String> pics = new ArrayList<>();
    private PhotoAdapter adapter;
    private AlertDialog alertDialog;
    private ArrayList<String> mSelectPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        alertDialog = new AlertDialog.Builder(this).setView(new ProgressBar(this))
                .setMessage("正在生成gif图片").create();
    }

    private void initView() {
        GridView grid_view = (GridView) findViewById(R.id.grid_view);
        file_text = (EditText) findViewById(R.id.file_text);
        SeekBar delay_bar = (SeekBar) findViewById(R.id.delay_bar);
        gif_image = (GifImageView) findViewById(R.id.gif_image);
        delay_text = (TextView) findViewById(R.id.delay_text);
        findViewById(R.id.generate).setOnClickListener(this);
        findViewById(R.id.clear).setOnClickListener(this);
        findViewById(R.id.share).setOnClickListener(this);

        adapter = new PhotoAdapter(this, null);
        grid_view.setAdapter(adapter);

        file_text.setText("demo");
        delayTime = delay_bar.getProgress();
        delay_bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                delayTime = progress;
                delay_text.setText("帧间隔时长：" + progress + "ms");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.generate://生成gif图
                alertDialog.show();

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String fileName = file_text.getText().toString();
                        createGif(TextUtils.isEmpty(fileName) ? "demo" : fileName, delayTime);

                        alertDialog.dismiss();
                    }
                }).start();
                break;
            case R.id.clear:
                clearData();
                break;
            case R.id.share:
                String fileName = file_text.getText().toString();
                fileName = TextUtils.isEmpty(fileName) ? "Demo" : fileName;
                final String path = Environment.getExternalStorageDirectory().getPath() +
                        "/GIFMakerDemo/" + fileName + ".gif";
                share("分享", Uri.fromFile(new File(path)));
                break;
        }
    }

    private void share(String content, Uri uri) {

//        Uri localUri = Uri.fromFile(SaveGifActivity.this.createdGifFile);
//            paramAnonymousView.putExtra("android.intent.extra.STREAM", localUri);
//            SaveGifActivity.this.startActivity(Intent.createChooser(paramAnonymousView, "Share with"));

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        if (uri != null) {
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.setType("image/gif");
            // 当用户选择短信时使用sms_body取得文字
            shareIntent.putExtra("sms_body", content);
        } else {
            shareIntent.setType("text/plain");
        }
        // 自定义选择框的标题
        startActivity(Intent.createChooser(shareIntent, "邀请好友"));
        // 系统默认标题
    }

    /**
     * 清除当前的数据内容
     */
    private void clearData() {
        pics.clear();
        adapter.setList(null);
        gif_image.setImageDrawable(null);
    }

    /**
     * 生成gif图
     *
     * @param delay 图片之间间隔的时间
     */
    private void createGif(String fileName, int delay) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        AnimatedGifEncoder localAnimatedGifEncoder = new AnimatedGifEncoder();
        localAnimatedGifEncoder.start(baos);//start
        localAnimatedGifEncoder.setRepeat(0);//设置生成gif的开始播放时间。0为立即开始播放
        localAnimatedGifEncoder.setDelay(delay);

        //【注意1】开始生成gif的时候，是以第一张图片的尺寸生成gif图的大小，后面几张图片会基于第一张图片的尺寸进行裁切
        //所以要生成尺寸完全匹配的gif图的话，应先调整传入图片的尺寸，让其尺寸相同
        //【注意2】如果传入的单张图片太大的话会造成OOM，可在不损失图片清晰度先对图片进行质量压缩
        if (pics.isEmpty()) {
            localAnimatedGifEncoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.pic_1));
            localAnimatedGifEncoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.pic_2));
            localAnimatedGifEncoder.addFrame(BitmapFactory.decodeResource(getResources(), R.drawable.pic_3));
        } else {
            for (int i = 0; i < pics.size(); i++) {
                Log.d("CYL","addFrame start");
                localAnimatedGifEncoder.addFrame(BitmapUtil.scalBitmap(pics.get(i)));
                Log.d("CYL","addFrame end");
            }
        }
        localAnimatedGifEncoder.finish();//finish

        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/GIFMakerDemo");
        if (!file.exists()) file.mkdir();
        final String path = Environment.getExternalStorageDirectory().getPath() + "/GIFMakerDemo/" + fileName + ".gif";
        Log.d(TAG, "createGif: ---->" + path);

        try {
            FileOutputStream fos = new FileOutputStream(path);
            baos.writeTo(fos);
            baos.flush();
            fos.flush();
            baos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    GifDrawable gifDrawable = new GifDrawable(path);
                    gif_image.setImageDrawable(gifDrawable);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Toast.makeText(MainActivity.this, "Gif已生成。保存路径：\n" + path, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void photoPick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN // Permission was added in API Level 16
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                    getString(R.string.mis_permission_rationale),
                    REQUEST_STORAGE_READ_ACCESS_PERMISSION);
        } else {
            MultiImageSelector selector = MultiImageSelector.create(MainActivity.this);
//            selector.origin(mSelectPath);
            selector.start(MainActivity.this, REQUEST_IMAGE);
        }
    }

    private void requestPermission(final String permission, String rationale, final int requestCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.mis_permission_dialog_title)
                    .setMessage(rationale)
                    .setPositiveButton(R.string.mis_permission_dialog_ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, requestCode);
                        }
                    })
                    .setNegativeButton(R.string.mis_permission_dialog_cancel, null)
                    .create().show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) {
                mSelectPath = data.getStringArrayListExtra(MultiImageSelector.EXTRA_RESULT);
                for (String p : mSelectPath) {
                    pics.add(p);
                }
                adapter.setList(pics);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        alertDialog.dismiss();
    }

    private void shareWeChat(String path) {
        Uri uriToImage = Uri.fromFile(new File(path));
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uriToImage);
        shareIntent.setType("image/jpeg");
        startActivity(Intent.createChooser(shareIntent, "分享图片"));
    }
}
