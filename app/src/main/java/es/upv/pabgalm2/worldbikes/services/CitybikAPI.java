package es.upv.pabgalm2.worldbikes.services;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RotateDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.graphics.Palette;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

import es.upv.pabgalm2.worldbikes.R;
import es.upv.pabgalm2.worldbikes.pojo.Network;
import es.upv.pabgalm2.worldbikes.pojo.Station;
import es.upv.pabgalm2.worldbikes.pojo.StationBitmapHelper;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class CitybikAPI {

    private static String BASE_URL = "https://api.citybik.es";

    private GoogleMap mMap;
    private ArrayList<Marker> networkMarkers;
    private Context context;
    private OkHttpClient client;
    private OnGetBikesCompletedCallback callback;

    public CitybikAPI(Context context, GoogleMap mMap, ArrayList<Marker> networkMarkers) {
        this.context = context;
        this.mMap = mMap;
        this.networkMarkers = networkMarkers;
        this.client = new OkHttpClient();
    }

    public void getNetworksFromFile(){

        try {
            JSONObject jsonBody = createJSONFromFile(R.raw.networks);
            JSONArray networks = jsonBody.getJSONArray("networks");

            for(int i = 0; i < networks.length(); i++) {

                JSONObject jsonObject = networks.getJSONObject(i);

                if(jsonObject.getString("name").equals("Onroll")) { continue; }

                LatLng location = new LatLng(jsonObject.getJSONObject("location").getDouble("latitude"), jsonObject.getJSONObject("location").getDouble("longitude"));

                Object company = jsonObject.get("company");

                if (!jsonObject.isNull("company")) {
                    if (company instanceof String) {
                        company = jsonObject.getString("company");
                    } else {
                        company = jsonObject.getJSONArray("company").getString(0);
                    }
                }  else {
                    company = null;
                }

                Network network = new Network(
                        jsonObject.getString("id"),
                        jsonObject.getString("name"),
                        (String) company,
                        jsonObject.getString("href"),
                        jsonObject.getJSONObject("location").getString("city"),
                        jsonObject.getJSONObject("location").getString("country"),
                        location
                );

                Bitmap networkImage = BitmapFactory.decodeResource(context.getResources(), getResId(network.getId().replace("-", "_"), R.drawable.class));
                networkImage = scaleDown(networkImage, 165, false);

                network.setNetworkImage(networkImage);

                Bitmap mDotMarkerBitmap = getDotBitmap(networkImage);

                network.setDotImage(mDotMarkerBitmap);

                com.google.android.gms.maps.model.Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(network.getLocation())
                        .icon(BitmapDescriptorFactory.fromBitmap(mDotMarkerBitmap)));

                marker.setTag(network);
                networkMarkers.add(marker);

            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public void getNetworksAsync(){

        String networksURL = BASE_URL + "/v2/networks";

        Request request = new Request.Builder()
                .url(networksURL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(final Call call, Response response) throws IOException {

                Handler mainHandler = new Handler(context.getMainLooper());
                assert response.body() != null;
                final String json = response.body().string();

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            JSONObject jsonBody = new JSONObject(json);
                            JSONArray networks = jsonBody.getJSONArray("networks");

                            for(int i = 0; i < networks.length(); i++) {

                                JSONObject jsonObject = networks.getJSONObject(i);

                                LatLng location = new LatLng(jsonObject.getJSONObject("location").getDouble("latitude"), jsonObject.getJSONObject("location").getDouble("longitude"));

                                Object company = jsonObject.get("company");
                                if (!jsonObject.isNull("company")) {
                                    if (company instanceof String) {
                                        company = jsonObject.getString("company");
                                    } else {
                                        company = jsonObject.getJSONArray("company").getString(0);
                                    }
                                }  else {
                                    company = null;
                                }

                                Network network = new Network(
                                        jsonObject.getString("id"),
                                        jsonObject.getString("name"),
                                        (String) company,
                                        jsonObject.getString("href"),
                                        jsonObject.getJSONObject("location").getString("city"),
                                        jsonObject.getJSONObject("location").getString("country"),
                                        location
                                );

                                //getNetworkImage(network);

                                Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), getResId(network.getId().replace("-", "_"), R.drawable.class));
                                com.google.android.gms.maps.model.Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(network.getLocation())
                                        .icon(BitmapDescriptorFactory.fromBitmap(bitmap)));

                                marker.setTag(network);
                                networkMarkers.add(marker);

                                if(callback != null) {
                                    callback.onGetBikesCompleted();
                                }
                            }

                        } catch (JSONException e) {
                            System.out.println(e.getMessage());
                        }
                    }

                });
            }
        });

    }

    public void getStationsAsync(final Network network) {

        String stationsURL = BASE_URL + network.getHref();

        Request request = new Request.Builder()
                .url(stationsURL)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                Handler mainHandler = new Handler(context.getMainLooper());
                assert response.body() != null;
                final String json = response.body().string();

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            JSONObject jsonBody = new JSONObject(json);
                            JSONArray stations = jsonBody.getJSONObject("network").getJSONArray("stations");

                            for(int i = 0; i < stations.length(); i++) {

                                JSONObject stationJSON = stations.getJSONObject(i);

                                LatLng location = new LatLng(stationJSON.getDouble("latitude"), stationJSON.getDouble("longitude"));

                                Station station = new Station(
                                        stationJSON.getString("id"),
                                        stationJSON.getString("name"),
                                        stationJSON.getString("free_bikes"),
                                        stationJSON.getString("empty_slots"),
                                        location,
                                        stationJSON.getJSONObject("extra")
                                );

                                StationBitmapHelper stationBitmap = getStationBitmap(network.getNetworkImage());

                                station.setBitmap(stationBitmap);

                                Marker marker = mMap.addMarker(new MarkerOptions()
                                        .position(station.getLocation())
                                        .icon(BitmapDescriptorFactory.fromBitmap(stationBitmap.getBitmap())));

                                marker.setTag(station);
                                network.addStation(marker);
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                });
            }
        });

    }

    public void getNetworkImage(final Network network){

        Request request = new Request.Builder()
                .url("https://www.google.es/search?tbm=isch&q=" + network.getName() + "+logo&tbs=ic:trans")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                Handler mainHandler = new Handler(context.getMainLooper());

                final String string = response.body().string();

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        HtmlCleaner cleaner = new HtmlCleaner();
                        TagNode root = cleaner.clean(string);
                        TagNode firstImage = root.findElementByName("img", true);
                        String url = firstImage.getAttributeByName("src");

                        Glide.with(context)
                                .asBitmap()
                                .load(url)
                                .into(new SimpleTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {

                                        resource = createTransparentBitmapFromBitmap(resource, Color.WHITE);

                                        saveImage(resource, network);
                                    }
                                });
                    }
                });
            }
        });

    }

    private JSONObject createJSONFromFile(int fileID) {

        JSONObject result = null;

        try {
            // Read file into string builder
            InputStream inputStream = context.getResources().openRawResource(fileID);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder builder = new StringBuilder();

            for (String line = null; (line = reader.readLine()) != null ; ) {
                builder.append(line).append("\n");
            }

            // Parse into JSONObject
            String resultStr = builder.toString();
            JSONTokener tokener = new JSONTokener(resultStr);
            result = new JSONObject(tokener);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }

    private static Bitmap createTransparentBitmapFromBitmap(Bitmap bitmap, int replaceThisColor) {
        if (bitmap != null) {
            int picw = bitmap.getWidth();
            int pich = bitmap.getHeight();
            int[] pix = new int[picw * pich];
            int diff = 0x10101;
            bitmap.getPixels(pix, 0, picw, 0, 0, picw, pich);

            for (int y = 0; y < pich; y++) {
                // from left to right
                for (int x = 0; x < picw; x++) {
                    int index = y * picw + x;
                    int r = (pix[index] >> 16) & 0xff;
                    int g = (pix[index] >> 8) & 0xff;
                    int b = pix[index] & 0xff;

                    if (pix[index] == replaceThisColor || pix[index] == replaceThisColor - diff || pix[index] == replaceThisColor - (diff*2) || pix[index] == replaceThisColor - (diff*3) || pix[index] == replaceThisColor - (diff*4)) {
                        pix[index] = Color.TRANSPARENT;
                    } else {
                        break;
                    }
                }

                // from right to left
                for (int x = picw - 1; x >= 0; x--) {
                    int index = y * picw + x;
                    int r = (pix[index] >> 16) & 0xff;
                    int g = (pix[index] >> 8) & 0xff;
                    int b = pix[index] & 0xff;

                    if (pix[index] == replaceThisColor || pix[index] == replaceThisColor - diff || pix[index] == replaceThisColor - (diff*2) || pix[index] == replaceThisColor - (diff*3) || pix[index] == replaceThisColor - (diff*4)) {
                        pix[index] = Color.TRANSPARENT;
                    } else {
                        break;
                    }
                }
            }

            Bitmap bm = Bitmap.createBitmap(pix, picw, pich,
                    Bitmap.Config.ARGB_4444);

            return bm;
        }
        return null;
    }

    private void saveImage(Bitmap image, Network network) {
        String savedImagePath = null;

        String nameId = network.getId();

        nameId = nameId.replace("-", "_");

        String imageFileName = nameId + ".png";
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/worldbikes");
        boolean success = true;
        if (!storageDir.exists()) {
            success = storageDir.mkdirs();
        }
        if (success) {
            File imageFile = new File(storageDir, imageFileName);
            savedImagePath = imageFile.getAbsolutePath();
            try {
                OutputStream fOut = new FileOutputStream(imageFile);
                image.compress(Bitmap.CompressFormat.PNG, 100, fOut);
                fOut.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Add the image to the system gallery
            galleryAddPic(savedImagePath);
            Toast.makeText(context, "IMAGE SAVED", Toast.LENGTH_LONG).show();
        }
    }

    private void galleryAddPic(String imagePath) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(imagePath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    private static int getResId(String resName, Class<?> c) {

        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static Bitmap scaleDown(Bitmap realImage, float maxImageSize,
                                    boolean filter) {
        float ratio = Math.min(
                (float) maxImageSize / realImage.getWidth(),
                (float) maxImageSize / realImage.getHeight());
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        return newBitmap;
    }

    private Bitmap getDotBitmap(Bitmap networkImage){

        int px = 20;
        Bitmap mDotMarkerBitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mDotMarkerBitmap);
        Drawable shape = context.getResources().getDrawable(R.drawable.dot_marker);
        Palette palette = Palette.from(networkImage).generate();
        GradientDrawable gradientDrawable = (GradientDrawable) shape;
        gradientDrawable.setColor(palette.getVibrantColor(Color.BLACK));
        shape.setBounds(0, 0, mDotMarkerBitmap.getWidth(), mDotMarkerBitmap.getHeight());
        shape.draw(canvas);

        return mDotMarkerBitmap;

    }

    private StationBitmapHelper getStationBitmap(Bitmap networkImage){

        int px = context.getResources().getDimensionPixelSize(R.dimen.map_dot_marker_size);
        Bitmap mDotMarkerBitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mDotMarkerBitmap);
        Drawable shape = context.getResources().getDrawable(R.drawable.station_marker);
        Drawable logo = new BitmapDrawable(networkImage);
        ((LayerDrawable) shape).setDrawableByLayerId(R.id.logo, logo);
        Palette palette = Palette.from(networkImage).generate();
        int color = palette.getVibrantColor(Color.BLACK);
        GradientDrawable gradientDrawable = (GradientDrawable) ((RotateDrawable) ((LayerDrawable) shape).findDrawableByLayerId(R.id.gradientDrawble)).getDrawable();
        gradientDrawable.setColor(color);
        shape.setBounds(0, 0, mDotMarkerBitmap.getWidth(), mDotMarkerBitmap.getHeight());
        shape.draw(canvas);

        return new StationBitmapHelper(mDotMarkerBitmap, gradientDrawable, color);

    }

    public interface OnGetBikesCompletedCallback {

        void onGetBikesCompleted();

    }
}
