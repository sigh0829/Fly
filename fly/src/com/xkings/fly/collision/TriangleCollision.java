package com.xkings.fly.collision;

import java.nio.FloatBuffer;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Plane;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.Ray;

/**
 * This class contains method copied and modified from
 * {@link Intersector} class. Standard methods from there
 * are using public static {@link Vector3#tmp} properties
 * that are accessed from all over the API. When run in
 * parallel thread, synchronization issues arise.
 * 
 * @author xkings
 * 
 */
public class TriangleCollision {

    private static Vector3 tmp = new Vector3();
    private static Vector3 tmp1 = new Vector3();
    private static Vector3 tmp2 = new Vector3();
    private static Vector3 tmp3 = new Vector3();

    private static Vector3 min = new Vector3(-Float.MAX_VALUE, -Float.MAX_VALUE,
            -Float.MAX_VALUE);
    private static Vector3 max = new Vector3(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);

    /**
     * {@inheritDoc}
     * 
     * <pre>
     * This method is optimized.
     * </pre>
     */
    public static boolean intersectTriangles(Ray ray, Mesh m) {
        return intersectTriangles(ray, m, min, max);
    }

    /**
     * {@inheritDoc}
     * 
     * <pre>
     * This method is optimized.
     * </pre>
     */
    public static boolean intersectTriangles(Ray ray, Mesh m, Vector3 a, Vector3 b) {
        VertexAttribute positionAttribute = m
                .getVertexAttribute(VertexAttributes.Usage.Position);

        if (positionAttribute.numComponents != 3) {
            throw new RuntimeException(
                    "Position attributes must have 3 components, insted of "
                            + positionAttribute.numComponents + " components");
        }

        int componentSize = 4;

        final FloatBuffer verts = m.getVerticesBuffer();
        final int offset = positionAttribute.offset / componentSize;
        final int vertexSize = m.getVertexAttributes().vertexSize / componentSize;

        for (int i = 0; i < m.getNumVertices(); i += 3) {

            int p0 = offset + ((i + 0) * vertexSize);
            int p1 = offset + ((i + 1) * vertexSize);
            int p3 = offset + ((i + 2) * vertexSize);

            boolean result = intersectRayTriangle(ray,
                    tmp1.set(verts.get(p0 + 0), verts.get(p0 + 1), verts.get(p0 + 2)),
                    tmp2.set(verts.get(p1 + 0), verts.get(p1 + 1), verts.get(p1 + 2)),
                    tmp3.set(verts.get(p3 + 0), verts.get(p3 + 1), verts.get(p3 + 2)), tmp);

            if (result) {
                if (Collision.inBetween(a, b, tmp)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static Vector3 tmp4 = new Vector3();

    private static Vector3 tmp(Vector3 v) {
        return tmp4.set(v);
    }

    /**
     * <pre>
     * This method is modified so it does not use public static
     * {@link Vector3#tmp} property. No synchronization issues.
     * </pre>
     * 
     * @param ray
     *            The ray
     * @param plane
     *            The plane
     * @param intersection
     *            The vector the intersection point is
     *            written to (optional)
     * @return Whether an intersection is present.
     * @see Intersector#intersectRayPlane(Ray ray, Plane
     *      plane, Vector3 intersection)
     */
    public static boolean intersectRayPlane(Ray ray, Plane plane, Vector3 intersection) {
        float denom = ray.direction.dot(plane.getNormal());
        if (denom != 0) {
            float t = -(ray.origin.dot(plane.getNormal()) + plane.getD()) / denom;
            if (t < 0) {
                return false;
            }

            if (intersection != null) {
                intersection.set(ray.origin).add(tmp(ray.direction).mul(t));
            }
            return true;
        } else if (plane.testPoint(ray.origin) == Plane.PlaneSide.OnPlane) {
            if (intersection != null) {
                intersection.set(ray.origin);
            }
            return true;
        } else {
            return false;
        }
    }

    private static final MyPlane p = new MyPlane(new Vector3(), 0);
    private static final Vector3 i = new Vector3();

    private final static Vector3 v0 = new Vector3();
    private final static Vector3 v1 = new Vector3();
    private final static Vector3 v2 = new Vector3();

    /**
     * <pre>
     * This method is modified so it does not use public static
     * {@link Vector3#tmp} property. No synchronization issues.
     * </pre>
     * 
     * @param ray
     *            The ray
     * @param t1
     *            The first vertex of the triangle
     * @param t2
     *            The second vertex of the triangle
     * @param t3
     *            The third vertex of the triangle
     * @param intersection
     *            The intersection point (optional)
     * 
     * @see Intersector#intersectRayTriangle(Ray, Vector3,
     *      Vector3, Vector3, Vector3)
     */
    public static boolean intersectRayTriangle(Ray ray, Vector3 t1, Vector3 t2, Vector3 t3,
            Vector3 intersection) {
        p.set(t1, t2, t3);
        if (!intersectRayPlane(ray, p, i)) {
            return false;
        }

        v0.set(t3).sub(t1);
        v1.set(t2).sub(t1);
        v2.set(i).sub(t1);

        float dot00 = v0.dot(v0);
        float dot01 = v0.dot(v1);
        float dot02 = v0.dot(v2);
        float dot11 = v1.dot(v1);
        float dot12 = v1.dot(v2);

        float denom = dot00 * dot11 - dot01 * dot01;
        if (denom == 0) {
            return false;
        }

        float u = (dot11 * dot02 - dot01 * dot12) / denom;
        float v = (dot00 * dot12 - dot01 * dot02) / denom;

        if (u >= 0 && v >= 0 && u + v <= 1) {
            if (intersection != null) {
                intersection.set(i);
            }
            return true;
        } else {
            return false;
        }

    }

    private static class MyPlane extends Plane {
        private static final long serialVersionUID = -1240652082930747866L;

        public MyPlane(Vector3 normal, float d) {
            super(normal, d);
        }

        private final static Vector3 tmp = new Vector3();
        private final static Vector3 tmp2 = new Vector3();

        private Vector3 tmp(Vector3 v) {
            return tmp.set(v);
        }

        private Vector3 tmp2(Vector3 v) {
            return tmp2.set(v);
        }

        /**
         * {@inheritDoc}
         * 
         * <pre>
         * This method is modified so it does not use public static
         * {@link Vector3#tmp} property. No synchronization issues.
         * </pre>
         */
        @Override
        public void set(Vector3 point1, Vector3 point2, Vector3 point3) {
            Vector3 l = tmp(point1).sub(point2);
            Vector3 r = tmp2(point2).sub(point3);
            Vector3 nor = l.crs(r).nor();
            normal.set(nor);
            d = -point1.dot(nor);
        }

    }

    /**
     * Returns <tt>xy</tt> distance between two points.
     * 
     * @param a
     *            point A
     * @param b
     *            point B
     * @return distance between points
     */
    public static float distance(Vector3 a, Vector3 b) {
        float tx = a.x - b.x;
        float ty = a.y - b.y;
        return (float) Math.sqrt(tx * tx + ty * ty);
    }

}
