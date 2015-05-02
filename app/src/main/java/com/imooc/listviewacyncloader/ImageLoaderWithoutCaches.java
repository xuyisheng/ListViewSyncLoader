package com.imooc.listviewacyncloader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageLoaderWithoutCaches {

    private Handler mHandler;

    /**
     * Using ASyncTask
     *
     * @param imageView
     * @param url
     */
    public void showImageByASync(ImageView imageView, String url) {
        ASyncDownloadImage task = new ASyncDownloadImage(imageView, url);
        task.execute(url);
    }

    /**
     * Using Thread
     *
     * @param imageView
     * @param url
     */
    public void showImageByThread(final ImageView imageView, final String url) {

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                ImgHolder holder = (ImgHolder) msg.obj;
                if (holder.imageView.getTag().equals(holder.url)) {
                    holder.imageView.setImageBitmap(holder.bitmap);
                }
            }
        };
        new Thread() {
            @Override
            public void run() {
                Bitmap bitmap = getBitmapFromUrl(url);
                Message message = Message.obtain();
                message.obj = new ImgHolder(imageView, bitmap, url);
                mHandler.sendMessage(message);
            }
        }.start();
    }

    private static Bitmap getBitmapFromUrl(String urlString) {
        Bitmap bitmap;
        InputStream is = null;
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            is = new BufferedInputStream(conn.getInputStream());
            bitmap = BitmapFactory.decodeStream(is);
            conn.disconnect();
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null)
                    is.close();
            } catch (IOException e) {
            }
        }
        return null;
    }

    private class ImgHolder {
        public Bitmap bitmap;
        public ImageView imageView;
        public String url;

        public ImgHolder(ImageView iv, Bitmap bm, String url) {
            this.imageView = iv;
            this.bitmap = bm;
            this.url = url;
        }
    }

    private class ASyncDownloadImage extends AsyncTask<String, Void, Bitmap> {

        private ImageView imageView;
        private String url;

        public ASyncDownloadImage(ImageView imageView, String url) {
            this.imageView = imageView;
            this.url = url;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            return getBitmapFromUrl(params[0]);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (imageView.getTag().equals(url)) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
}
