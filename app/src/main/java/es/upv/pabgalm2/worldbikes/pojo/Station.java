package es.upv.pabgalm2.worldbikes.pojo;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONObject;

public class Station {

    private String id;
    private String name;
    private String bikes;
    private String slots;
    private LatLng location;
    private JSONObject extra;
    private StationBitmapHelper bitmap;

    public Station(String id, String name, String bikes, String slots, LatLng location, JSONObject extra) {
        this.id = id;
        this.name = name;
        this.bikes = bikes;
        this.slots = slots;
        this.location = location;
        this.extra = extra;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBikes() {
        return bikes;
    }

    public void setBikes(String bikes) {
        this.bikes = bikes;
    }

    public String getSlots() {
        return slots;
    }

    public void setSlots(String slots) {
        this.slots = slots;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public JSONObject getExtra() {
        return extra;
    }

    public void setExtra(JSONObject extra) {
        this.extra = extra;
    }

    public StationBitmapHelper getBitmap() {
        return bitmap;
    }

    public void setBitmap(StationBitmapHelper bitmap) {
        this.bitmap = bitmap;
    }
}
