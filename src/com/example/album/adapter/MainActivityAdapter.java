package com.example.album.adapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.R.integer;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.example.album.ISActivity;
import com.example.album.MainActivity;
import com.example.album.R;
import com.example.album.VPActivity;

public class MainActivityAdapter extends BaseAdapter {

	private int mFirstVisibleItem;
	private int mVisibleItemCount;
	private int mTotalItemCount;
	private boolean mIsScroll = false;
	private int[] mWH;
	private Context mContext;
	private ArrayList<String> mUris;
	private LayoutInflater mInflater;
	private GridView mGridView;
	private LruCache<String, Bitmap> mLruCache;
	private Set<BitmapAsyncTask> mBitmapAsyncTasks;

	@SuppressLint("NewApi")
	public MainActivityAdapter(Context context, GridView gridView,
			ArrayList<String> uris) {
		mUris = uris;
		mContext = context;
		mWH = getWidthAndHeight();
		mInflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mGridView = gridView;
		mGridView.setOnScrollListener(new ScrollListener());
		mLruCache = new LruCache<String, Bitmap>((int) Runtime.getRuntime()
				.maxMemory() / 8) {
			@Override
			protected int sizeOf(String key, Bitmap value) {
				return value.getByteCount();
			}
		};
		mBitmapAsyncTasks = new HashSet<BitmapAsyncTask>(uris.size());
	}

	@Override
	public int getCount() {
		return mUris.size();
	}

	@Override
	public Object getItem(int position) {
		return mUris.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@SuppressLint("NewApi")
	@SuppressWarnings("unchecked")
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final ViewHolder viewHolder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.gridview_item, null);
			viewHolder = new ViewHolder();
			viewHolder.picture = (ImageView) convertView
					.findViewById(R.id.gv_image);
			convertView.setTag(viewHolder);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		// 刚进入页面的时候不会调用接口OnScrollListener中的方法
		if (!mIsScroll) {
			BitmapAsyncTask bat = new BitmapAsyncTask(mContext,
					viewHolder.picture, mUris.get(position), mWH);
			mBitmapAsyncTasks.add(bat);
			bat.execute(mLruCache);
		} else {
			if (mLruCache.get(mUris.get(position)) == null) {
				viewHolder.picture
						.setImageResource(R.drawable.image_default_bg);
			} else {
				viewHolder.picture.setImageBitmap(mLruCache.get(mUris
						.get(position)));
			}
		}
		viewHolder.picture.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(mContext, VPActivity.class);
				intent.putExtra(VPActivity.POSITION, position);
				intent.putStringArrayListExtra(VPActivity.URIS, mUris);
				mContext.startActivity(intent);
				((MainActivity) mContext).overridePendingTransition(
						android.R.anim.fade_in, android.R.anim.fade_out);
			}
		});

		return convertView;
	}

	private static final class ViewHolder {
		private ImageView picture;
	}

	/**
	 * 根据屏幕的大小、gridview的horizontalSpacing和padding获取图片的宽高
	 * 
	 * @return 图片的宽高值
	 */
	private int[] getWidthAndHeight() {
		Resources resources = mContext.getResources();
		int hs = resources.getDimensionPixelSize(R.dimen.gv_horizontalSpacing);
		int padding = resources.getDimensionPixelSize(R.dimen.gv_padding);
		int screenWidth = resources.getDisplayMetrics().widthPixels;

		int wh = (screenWidth - 2 * hs - 2 * padding) / 3;

		return new int[] { wh, wh };
	}

	// // 重新加载第一张图片
	// public void reloadFirstPhoto() {
	// getView(0, null, null);
	// notifyDataSetChanged();
	// }

	/**
	 * 退出Activity时调用，清除引用，清除占用的内存
	 */
	public void clear() {
		if (mLruCache != null) {
			mLruCache.evictAll();
			mLruCache = null;
		}
		if (mBitmapAsyncTasks != null && !mBitmapAsyncTasks.isEmpty()) {
			mBitmapAsyncTasks.clear();
			mBitmapAsyncTasks = null;
		}
	}

	private class ScrollListener implements OnScrollListener {

		@SuppressWarnings("unchecked")
		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {
			switch (scrollState) {
			case OnScrollListener.SCROLL_STATE_IDLE:
				// 取消未加载完的任务
				for (BitmapAsyncTask bat : mBitmapAsyncTasks) {
					if (bat != null
							&& bat.getStatus() != AsyncTask.Status.FINISHED) {
						bat.cancel(true);
						bat = null;
					}
				}
				// 加载当前页面显示的图片
				for (int i = 0; i < mVisibleItemCount; i++) {
					String uri = mUris.get(mFirstVisibleItem + i);
					ViewHolder viewHolder = (ViewHolder) view.getChildAt(i)
							.getTag();
					if (mLruCache.get(uri) == null) {
						BitmapAsyncTask bitmapAsyncTask = new BitmapAsyncTask(
								mContext, viewHolder.picture, uri, mWH);
						mBitmapAsyncTasks.add(bitmapAsyncTask);
						bitmapAsyncTask.execute(mLruCache);
					} else {
						viewHolder.picture.setImageBitmap(mLruCache.get(uri));
					}
				}
				// // 清空除当前页面图片占用的内存
				// for (int j = 0; j < mFirstVisibleItem; j++) {
				// String uri = uris.get(j);
				// Bitmap bitmap = lruCache.get(uri);
				// if (bitmap != null && !bitmap.isRecycled()) {
				// bitmap.recycle();
				// bitmap = null;
				// lruCache.remove(uri);
				// }
				// }
				// int invisibleAfter = mFirstVisibleItem + mVisibleItemCount;
				// for (int k = invisibleAfter; k < mTotalItemCount; k++) {
				// String uri = uris.get(k);
				// Bitmap bitmap = lruCache.get(uri);
				// if (bitmap != null && !bitmap.isRecycled()) {
				// bitmap.recycle();
				// bitmap = null;
				// lruCache.remove(uri);
				// }
				// }
				break;
			case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				if (!mIsScroll) {
					mIsScroll = true;
				}
				break;
			}
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			mFirstVisibleItem = firstVisibleItem;
			mVisibleItemCount = visibleItemCount;
			mTotalItemCount = totalItemCount;
		}

	}

}
