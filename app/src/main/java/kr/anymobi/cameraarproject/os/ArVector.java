package kr.anymobi.cameraarproject.os;

final public class ArVector {

    public float x;
    public float y;
    public float z;

    public ArVector() {
        x = 0.0f;
        y = 0.0f;
        z = 0.0f;
    }

    public ArVector(ArVector value) {
        x = value.x;
        y = value.y;
        z = value.z;
    }

    public ArVector(float fx, float fy, float fz) {
        x = fx;
        y = fy;
        z = fz;
    }

    public ArVector add(ArVector v) {
        return new ArVector(x + v.x, y + v.y, z + v.z);
    }

    public ArVector sub(ArVector v) {
        return new ArVector(x - v.x, y - v.y, z - v.z);
    }

    public ArVector mul(ArVector v) {
        return new ArVector(x * v.x, y * v.y, z * v.z);
    }

    public ArVector div(ArVector v) {
        return new ArVector(x / v.x, y / v.y, z / v.z);
    }

    public ArVector add(float v) {
        return new ArVector(x + v, y + v, z + v);
    }

    public ArVector sub(float v) {
        return new ArVector(x - v, y - v, z - v);
    }

    public ArVector mul(float v) {
        return new ArVector(x * v, y * v, z * v);
    }

    public ArVector div(float v) {
        return new ArVector(x / v, y / v, z / v);
    }

    @Override
    public String toString() {
        return "x::" + x + "::y::" + y + "::z::" + z;
    }
}
