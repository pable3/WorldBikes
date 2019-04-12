package es.upv.pabgalm2.worldbikes.pojo;

import android.graphics.Bitmap;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

import java.util.ArrayList;
import java.util.Collection;

public class Network {

    private String id;
    private String name;
    private String company;
    private String href;
    private String city;
    private String country;
    private LatLng location;
    private ArrayList<Marker> stations;
    private Bitmap networkImage;
    private Bitmap dotImage;

    public Network(String id, String name, String company, String href, String city, String country, LatLng location) {
        this.id = id;
        this.name = name;
        this.company = company;
        this.href = href;
        this.city = city;
        this.country = country;
        this.location = location;
        this.stations = new ArrayList<>();
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

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }

    public ArrayList<Marker> getStations() {
        return stations;
    }

    public void addStation(Marker station) {
        stations.add(station);
    }

    public void addAllStations(Collection<Marker> stations) {
        stations.addAll(stations);
    }

    public void clearStations() {
        stations = new ArrayList<>();
    }

    public Bitmap getNetworkImage() {
        return networkImage;
    }

    public void setNetworkImage(Bitmap networkImage) {
        this.networkImage = networkImage;
    }

    public Bitmap getDotImage() {
        return dotImage;
    }

    public void setDotImage(Bitmap dotImage) {
        this.dotImage = dotImage;
    }
}
