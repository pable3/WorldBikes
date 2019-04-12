package es.upv.pabgalm2.worldbikes.services;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class QwantAPI {

    private OkHttpClient client;
    private OnGetImagesCompletedCallback callback;

    public QwantAPI() {
        this.client = new OkHttpClient();
    }

    public void setOnGetImagesCompletedCallback(OnGetImagesCompletedCallback callback){
        this.callback = callback;
    }

    public void getImagesURLAsync(String search){

        String url = "https://api.qwant.com/api/search/images?count=8&q=" + search + "&t=images&safesearch=1&uiv=4";

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                assert response.body() != null;
                final String json = response.body().string();

                try {
                    JSONObject jsonBody = new JSONObject(json);
                    JSONArray images = jsonBody.getJSONObject("data").getJSONObject("result").getJSONArray("items");
                    String url = images.getJSONObject(1).getString("media");

                    if(callback != null) {
                        callback.onGetImagesCompleted(url);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public interface OnGetImagesCompletedCallback {

        void onGetImagesCompleted(String url);

    }
}
