package com.imooc.listviewacyncloader;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

import java.util.Arrays;
import java.util.List;


public class MainActivity extends Activity {

    private ListView mListView;
    private List<String> mData;
    private MyAdapterUseDoubleCaches mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mListView = (ListView) findViewById(R.id.lv);
        mData = Arrays.asList(Images.IMAGE_URLS);
        mAdapter = new MyAdapterUseDoubleCaches(this, mData, mListView);
        mListView.setAdapter(mAdapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mAdapter.flushCache();
    }
}
