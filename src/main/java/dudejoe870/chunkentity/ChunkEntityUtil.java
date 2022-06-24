package dudejoe870.chunkentity;

import net.minecraft.entity.*;
import net.minecraft.util.collection.SortedArraySet;
import net.minecraft.util.math.*;

import java.util.ArrayList;

public class ChunkEntityUtil {
    public static Vec3d transformPointToEntity(Vec3d point, Entity entity) {
        return point
                .rotateY((float)Math.toRadians(180.0f - entity.getYaw()))
                .rotateX((float)Math.toRadians(entity.getPitch()))
                .add(entity.getPos());
    }

    public static class Ray {
        private Vec3d r1;
        private Vec3d dR;

        public Ray(Vec3d position, Vec3d direction) {
            r1 = position;
            Vec3d r2 = position.add(direction);
            dR = r1.subtract(r2);
        }
    }

    private static class ChunkEntityBoundingBox {
        private ChunkEntity entity;

        private Vec3d p1;
        private Vec3d p2;
        private Vec3d p3;
        private Vec3d p4;
        private Vec3d p5;
        private Vec3d p6;
        private Vec3d p7;
        private Vec3d p8;

        private ChunkEntityBoundingBox(ChunkEntity entity) {
            this.entity = entity;

            p1 = Vec3d.ZERO; // min
            p7 = new Vec3d(entity.getSizeX(), entity.getSizeY(), entity.getSizeZ()); // max

            p2 = new Vec3d(p1.x, p1.y, p7.z);
            p3 = new Vec3d(p7.x, p1.y, p7.z);
            p4 = new Vec3d(p7.x, p1.y, p1.z);
            p5 = new Vec3d(p1.x, p7.y, p1.z);
            p6 = new Vec3d(p1.x, p7.y, p7.z);
            p8 = new Vec3d(p7.x, p7.y, p1.z);

            p1 = transformPointToEntity(p1, entity);
            p2 = transformPointToEntity(p2, entity);
            p3 = transformPointToEntity(p3, entity);
            p4 = transformPointToEntity(p4, entity);
            p5 = transformPointToEntity(p5, entity);
            p6 = transformPointToEntity(p6, entity);
            p7 = transformPointToEntity(p7, entity);
            p8 = transformPointToEntity(p8, entity);
        }
    }

    public static boolean checkChunkEntityAtPosition(Vec3d position, ChunkEntity entity) {
        return checkChunkEntityAtPosition(position, new ChunkEntityBoundingBox(entity));
    }

    // Reference: https://math.stackexchange.com/questions/1472049/check-if-a-point-is-inside-a-rectangular-shaped-area-3d
    private static boolean checkChunkEntityAtPosition(Vec3d position, ChunkEntityBoundingBox boundingBox) {
        Vec3d i = boundingBox.p2.subtract(boundingBox.p1);
        Vec3d j = boundingBox.p4.subtract(boundingBox.p1);
        Vec3d k = boundingBox.p5.subtract(boundingBox.p1);

        Vec3d v = position.subtract(boundingBox.p1);
        double vDotI = v.dotProduct(i);
        double vDotJ = v.dotProduct(j);
        double vDotK = v.dotProduct(k);

        double iDotI = i.dotProduct(i);
        double jDotJ = j.dotProduct(j);
        double kDotK = k.dotProduct(k);

        return (0 < vDotI && vDotI < iDotI) &&
               (0 < vDotJ && vDotJ < jDotJ) &&
               (0 < vDotK && vDotK < kDotK);
    }

    public static ChunkEntity getChunkEntityAtPosition(Vec3d position) {
        for (ChunkEntity entity : ChunkEntity.getAllLoadedChunkEntities()) {
            if (checkChunkEntityAtPosition(position, entity))
                return entity;
        }

        return null;
    }

    public static class ChunkEntityIntersectionInfo {
        public ChunkEntity entity;
        public Vec3d intersectionPoint;
        public double distance;

        private ChunkEntityIntersectionInfo(ChunkEntity entity, Vec3d intersectionPoint, double distance) {
            this.entity = entity;
            this.intersectionPoint = intersectionPoint;
            this.distance = distance;
        }
    }

    public static ChunkEntityIntersectionInfo getChunkEntityAlongRay(Ray ray) {
        for (ChunkEntity entity : ChunkEntity.getAllLoadedChunkEntities()) {
            ChunkEntityBoundingBox boundingBox = new ChunkEntityBoundingBox(entity);

            if (checkChunkEntityAtPosition(ray.r1, boundingBox))
                return new ChunkEntityIntersectionInfo(entity, ray.r1, 0.0);

            QuadIntersectionInfo frontFace = quadRayIntersection(ray, boundingBox.p5, boundingBox.p8, boundingBox.p1);
            QuadIntersectionInfo backFace = quadRayIntersection(ray, boundingBox.p6, boundingBox.p7, boundingBox.p2);

            QuadIntersectionInfo leftFace = quadRayIntersection(ray, boundingBox.p6, boundingBox.p5, boundingBox.p2);
            QuadIntersectionInfo rightFace = quadRayIntersection(ray, boundingBox.p8, boundingBox.p7, boundingBox.p4);

            QuadIntersectionInfo topFace = quadRayIntersection(ray, boundingBox.p6, boundingBox.p7, boundingBox.p5);
            QuadIntersectionInfo bottomFace = quadRayIntersection(ray, boundingBox.p2, boundingBox.p3, boundingBox.p1);

            ArrayList<Vec3d> intersections = new ArrayList<>(6);

            if (frontFace.intersects) intersections.add(frontFace.intersectionPoint);
            if (backFace.intersects) intersections.add(backFace.intersectionPoint);

            if (leftFace.intersects) intersections.add(leftFace.intersectionPoint);
            if (rightFace.intersects) intersections.add(rightFace.intersectionPoint);

            if (topFace.intersects) intersections.add(topFace.intersectionPoint);
            if (bottomFace.intersects) intersections.add(bottomFace.intersectionPoint);

            double currentDistance = Double.MAX_VALUE;
            Vec3d currentIntersection = Vec3d.ZERO;
            for (Vec3d p : intersections) {
                double d = ray.r1.distanceTo(p);
                if (d < currentDistance) {
                    currentDistance = d;
                    currentIntersection = p;
                }
            }

            if (!intersections.isEmpty())
                return new ChunkEntityIntersectionInfo(entity, currentIntersection, currentDistance);
        }

        return new ChunkEntityIntersectionInfo(null, Vec3d.ZERO, Double.POSITIVE_INFINITY);
    }

    public static ChunkEntityIntersectionInfo getChunkEntityAlongRay(Vec3d position, Vec3d direction) {
        return getChunkEntityAlongRay(new Ray(position, direction));
    }

    public static class QuadIntersectionInfo {
        public boolean intersects;
        public Vec3d intersectionPoint;

        private QuadIntersectionInfo(boolean intersects, Vec3d intersectionPoint) {
            this.intersects = intersects;
            this.intersectionPoint = intersectionPoint;
        }
    }

    public static QuadIntersectionInfo quadRayIntersection(
            Ray ray,
            Vec3d p1, Vec3d p2, Vec3d p3) {
        Vec3d dP21 = p2.subtract(p1);
        Vec3d dP31 = p3.subtract(p1);
        Vec3d n = dP21.crossProduct(dP31);

        double ndotdR = n.dotProduct(ray.dR);

        if (Math.abs(ndotdR) < 1e-6f)
            return new QuadIntersectionInfo(false, Vec3d.ZERO);

        double t = -n.dotProduct(ray.r1.subtract(p1)) / ndotdR;
        Vec3d M = ray.r1.add(ray.dR.multiply(t));

        Vec3d dMP1 = M.subtract(p1);
        double u = dMP1.dotProduct(dP21);
        double v = dMP1.dotProduct(dP31);

        return new QuadIntersectionInfo(
                u >= 0.0f && u <= dP21.dotProduct(dP21)
                && v >= 0.0f && v <= dP31.dotProduct(dP31), M);
    }

    public static QuadIntersectionInfo quadRayIntersection(
            Vec3d position, Vec3d direction,
            Vec3d p1, Vec3d p2, Vec3d p3) {
        return quadRayIntersection(new Ray(position, direction), p1, p2, p3);
    }
}
