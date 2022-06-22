package dudejoe870.chunkentity;

import net.minecraft.entity.*;
import net.minecraft.util.math.*;

public class ChunkEntityUtil {
    public static Vec3d transformPointToEntity(Vec3d point, Entity entity) {
        return point
                .rotateY((float)Math.toRadians(180.0f - entity.getYaw()))
                .rotateX((float)Math.toRadians(entity.getPitch()))
                .add(entity.getPos());
    }

    // Reference: https://math.stackexchange.com/questions/1472049/check-if-a-point-is-inside-a-rectangular-shaped-area-3d
    public static ChunkEntity getChunkEntityAtPosition(Vec3d position) {
        for (ChunkEntity entity : ChunkEntity.getAllLoadedChunkEntities()) {
            Vec3d p1 = Vec3d.ZERO; // min
            Vec3d p7 = new Vec3d(entity.getSizeX(), entity.getSizeY(), entity.getSizeZ()); // max

            Vec3d p2 = new Vec3d(p1.x, p1.y, p7.z);
            Vec3d p4 = new Vec3d(p7.x, p1.y, p1.z);
            Vec3d p5 = new Vec3d(p1.x, p7.y, p1.z);

            p1 = transformPointToEntity(p1, entity);
            p2 = transformPointToEntity(p2, entity);
            p4 = transformPointToEntity(p4, entity);
            p5 = transformPointToEntity(p5, entity);

            Vec3d i = p2.subtract(p1);
            Vec3d j = p4.subtract(p1);
            Vec3d k = p5.subtract(p1);

            Vec3d v = position.subtract(p1);
            double vDotI = v.dotProduct(i);
            double vDotJ = v.dotProduct(j);
            double vDotK = v.dotProduct(k);

            double iDotI = i.dotProduct(i);
            double jDotJ = j.dotProduct(j);
            double kDotK = k.dotProduct(k);

            if ((0 < vDotI && vDotI < iDotI) && (0 < vDotJ && vDotJ < jDotJ) && (0 < vDotK && vDotK < kDotK))
                return entity;
        }

        return null;
    }
}
