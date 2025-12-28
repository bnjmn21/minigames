package bnjmn21.minigames.data.data_types;

public class PosRot {
    public Vec3 pos;
    public float pitch;
    public float yaw;

    public PosRot(Vec3 pos, float pitch, float yaw) {
        this.pos = pos;
        this.pitch = pitch;
        this.yaw = yaw;
    }
}
