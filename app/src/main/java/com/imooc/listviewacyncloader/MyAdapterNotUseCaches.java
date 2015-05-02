package com.imooc.listviewacyncloader;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.List;

public class MyAdapterNotUseCaches extends BaseAdapter {

    private LayoutInflater mInflater;
    private List<String> mData;
    private ImageLoaderWithoutCaches mImageLoader;

    public MyAdapterNotUseCaches(Context context, List<String> data) {
        this.mData = data;
        mInflater = LayoutInflater.from(context);
        mImageLoader = new ImageLoaderWithoutCaches();
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String url = mData.get(position);
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            convertView = mInflater.inflate(R.layout.listview_item, null);
            viewHolder.imageView = (ImageView) convertView.findViewById(R.id.iv_lv_item);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.imageView.setTag(url);
        viewHolder.imageView.setImageResource(R.drawable.ic_launcher);
        mImageLoader.showImageByASync(viewHolder.imageView, url);
        return convertView;
    }

    public class ViewHolder {
        public ImageView imageView;
    }
}
