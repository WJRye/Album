package com.example.album;

import java.util.ArrayList;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Images;
import android.widget.GridView;

import com.example.album.adapter.MainActivityAdapter;

/**
 * @author Administrator
 * 
 */
public class MainActivity extends Activity {

	private GridView mGridView;
	private MainActivityAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mGridView = (GridView) findViewById(R.id.album_gv);
		mGridView.setSelector(new ColorDrawable());// 去掉gridview默认的选择颜色
		mAdapter = new MainActivityAdapter(this, mGridView, getUris());
		mGridView.setAdapter(mAdapter);
	}

	/**
	 * 获得手机里所有的图片的路径
	 */
	private ArrayList<String> getUris() {
		ArrayList<String> uris = new ArrayList<String>();
		Uri uri = Images.Media.EXTERNAL_CONTENT_URI;
		String data = Images.Media.DATA;
		String[] projection = new String[] { Images.Media._ID,
				Images.Media.DATA };
		Cursor imageCursor = getContentResolver().query(uri, projection, null,
				null, null);
		int count = imageCursor.getCount();
		if (count > 0) {
			// 从最近一张开始加载
			for (int i = count - 1; i >= 0; i--) {
				int index = imageCursor.getColumnIndex(data);
				imageCursor.moveToPosition(i);
				uris.add(imageCursor.getString(index));
			}
		}
		imageCursor.close();
		return uris;
	}

//	@Override
//	public void onWindowFocusChanged(boolean hasFocus) {
//		super.onWindowFocusChanged(hasFocus);
//		if (hasFocus) {
//			mAdapter.reloadFirstPhoto();
//		}
//	}

	@Override
	protected void onDestroy() {
		mAdapter.clear();
		super.onDestroy();
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

}
