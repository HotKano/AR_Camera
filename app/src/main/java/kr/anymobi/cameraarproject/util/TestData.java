package kr.anymobi.cameraarproject.util;

public class TestData {
    double lat, lon;
    String pointName;

    // 지도 표시용 Dummy Data Class 지점명, 위도, 경도
    public TestData(String point, double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
        this.pointName = point;
    }

    public String getPointName() {
        return pointName;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }
}
