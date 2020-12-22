package kr.anymobi.cameraarproject.os;

final public class ArBoolean {

    public boolean x;
    public boolean y;
    public boolean z;

    public ArBoolean() {
        x = false;
        y = false;
        z = false;
    }

    public ArBoolean(ArBoolean value) {
        x = value.x;
        y = value.y;
        z = value.z;
    }

    public ArBoolean(boolean fx, boolean fy, boolean fz) {
        x = fx;
        y = fy;
        z = fz;
    }

    @Override
    public String toString() {
        return "x::" + x + "::y::" + y + "::z::" + z;
    }
}
