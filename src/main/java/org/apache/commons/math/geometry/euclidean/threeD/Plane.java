/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.math.geometry.euclidean.threeD;

import org.apache.commons.math.geometry.euclidean.oneD.Point1D;
import org.apache.commons.math.geometry.euclidean.twoD.Point2D;
import org.apache.commons.math.geometry.euclidean.twoD.PolygonsSet;
import org.apache.commons.math.geometry.partitioning.BSPTree;
import org.apache.commons.math.geometry.partitioning.Hyperplane;
import org.apache.commons.math.geometry.partitioning.Point;
import org.apache.commons.math.geometry.partitioning.Region;
import org.apache.commons.math.geometry.partitioning.SubHyperplane;
import org.apache.commons.math.geometry.partitioning.SubSpace;
import org.apache.commons.math.util.FastMath;

/** The class represent planes in a three dimensional space.
 * @version $Revision$ $Date$
 */
public class Plane implements Hyperplane {

    /** Offset of the origin with respect to the plane. */
    private double originOffset;

    /** Origin of the plane frame. */
    private Point3D origin;

    /** First vector of the plane frame (in plane). */
    private Vector3D u;

    /** Second vector of the plane frame (in plane). */
    private Vector3D v;

    /** Third vector of the plane frame (plane normal). */
    private Vector3D w;

    /** Build a plane normal to a given direction and containing the origin.
     * @param normal normal direction to the plane
     * @exception IllegalArgumentException if the normal norm is too small
     */
    public Plane(final Vector3D normal) {
        setNormal(normal);
        originOffset = 0;
        setFrame();
    }

    /** Build a plane from a point and a normal.
     * @param p point belonging to the plane
     * @param normal normal direction to the plane
     * @exception IllegalArgumentException if the normal norm is too small
     */
    public Plane(final Vector3D p, final Vector3D normal) {
        setNormal(normal);
        originOffset = -Vector3D.dotProduct(p, w);
        setFrame();
    }

    /** Build a plane from three points.
     * <p>The plane is oriented in the direction of
     * {@code (p2-p1) ^ (p3-p1)}</p>
     * @param p1 first point belonging to the plane
     * @param p2 second point belonging to the plane
     * @param p3 third point belonging to the plane
     * @exception IllegalArgumentException if the points do not constitute a plane
     */
    public Plane(final Vector3D p1, final Vector3D p2, final Vector3D p3) {
        this(p1, Vector3D.crossProduct(p2.subtract(p1), p3.subtract(p1)));
    }

    /** Copy constructor.
     * <p>The instance created is completely independant of the original
     * one. A deep copy is used, none of the underlying object are
     * shared.</p>
     * @param plane plane to copy
     */
    public Plane(final Plane plane) {
        originOffset = plane.originOffset;
        origin = plane.origin;
        u      = plane.u;
        v      = plane.v;
        w      = plane.w;
    }

    /** Copy the instance.
     * <p>The instance created is completely independant of the original
     * one. A deep copy is used, none of the underlying objects are
     * shared (except for immutable objects).</p>
     * @return a new hyperplane, copy of the instance
     */
    public Hyperplane copySelf() {
        return new Plane(this);
    }

    /** Reset the instance as if built from a point and a normal.
     * @param p point belonging to the plane
     * @param normal normal direction to the plane
     */
    public void reset(final Vector3D p, final Vector3D normal) {
        setNormal(normal);
        originOffset = -Vector3D.dotProduct(p, w);
        setFrame();
    }

    /** Reset the instance from another one.
     * <p>The updated instance is completely independant of the original
     * one. A deep reset is used none of the underlying object is
     * shared.</p>
     * @param original plane to reset from
     */
    public void reset(final Plane original) {
        originOffset = original.originOffset;
        origin       = original.origin;
        u            = original.u;
        v            = original.v;
        w            = original.w;
    }

    /** Set the normal vactor.
     * @param normal normal direction to the plane (will be copied)
     * @exception IllegalArgumentException if the normal norm is too small
     */
    private void setNormal(final Vector3D normal) {
        final double norm = normal.getNorm();
        if (norm < 1.0e-10) {
            throw new IllegalArgumentException("null norm");
        }
        w = new Vector3D(1.0 / norm, normal);
    }

    /** Reset the plane frame.
     */
    private void setFrame() {
        origin = new Point3D(-originOffset, w);
        u = w.orthogonal();
        v = Vector3D.crossProduct(w, u);
    }

    /** Get the origin point of the plane frame.
     * <p>The point returned is the orthogonal projection of the
     * 3D-space origin in the plane.</p>
     * @return the origin point of the plane frame (point closest to the
     * 3D-space origin)
     */
    public Point3D getOrigin() {
        return origin;
    }

    /** Get the normalized normal vector.
     * <p>The frame defined by ({@link #getU getU}, {@link #getV getV},
     * {@link #getNormal getNormal}) is a rigth-handed orthonormalized
     * frame).</p>
     * @return normalized normal vector
     * @see #getU
     * @see #getV
     */
    public Vector3D getNormal() {
        return w;
    }

    /** Get the plane first canonical vector.
     * <p>The frame defined by ({@link #getU getU}, {@link #getV getV},
     * {@link #getNormal getNormal}) is a rigth-handed orthonormalized
     * frame).</p>
     * @return normalized first canonical vector
     * @see #getV
     * @see #getNormal
     */
    public Vector3D getU() {
        return u;
    }

    /** Get the plane second canonical vector.
     * <p>The frame defined by ({@link #getU getU}, {@link #getV getV},
     * {@link #getNormal getNormal}) is a rigth-handed orthonormalized
     * frame).</p>
     * @return normalized second canonical vector
     * @see #getU
     * @see #getNormal
     */
    public Vector3D getV() {
        return v;
    }

    /** Revert the plane.
     * <p>Replace the instance by a similar plane with opposite orientation.</p>
     * <p>The new plane frame is chosen in such a way that a 3D point that had
     * {@code (x, y)} in-plane coordinates and {@code z} offset with
     * respect to the plane and is unaffected by the change will have
     * {@code (y, x)} in-plane coordinates and {@code -z} offset with
     * respect to the new plane. This means that the {@code u} and {@code v}
     * vectors returned by the {@link #getU} and {@link #getV} methods are exchanged,
     * and the {@code w} vector returned by the {@link #getNormal} method is
     * reversed.</p>
     */
    public void revertSelf() {
        final Vector3D tmp = u;
        u = v;
        v = tmp;
        w = w.negate();
        originOffset = -originOffset;
    }

    /** Transform a 3D space point into an in-plane point.
     * @param point point of the space (must be a {@link Vector3D
     * Vector3D} instance)
     * @return in-plane point (really a {@link
     * org.apache.commons.math.geometry.euclidean.twoD.Point2D Point2D} instance)
     * @see #toSpace
     */
    public Point toSubSpace(final Point point) {
        final Vector3D p3D = (Vector3D) point;
        return new Point2D(Vector3D.dotProduct(p3D, u),
                           Vector3D.dotProduct(p3D, v));
    }

    /** Transform an in-plane point into a 3D space point.
     * @param point in-plane point (must be a {@link
     * org.apache.commons.math.geometry.euclidean.twoD.Point2D Point2D} instance)
     * @return 3D space point (really a {@link Vector3D Vector3D} instance)
     * @see #toSubSpace
     */
    public Point toSpace(final Point point) {
        final Point2D p2D = (Point2D) point;
        return new Point3D(p2D.x, u, p2D.y, v, -originOffset, w);
    }

    /** Get one point from the 3D-space.
     * @param inPlane desired in-plane coordinates for the point in the
     * plane
     * @param offset desired offset for the point
     * @return one point in the 3D-space, with given coordinates and offset
     * relative to the plane
     */
    public Vector3D getPointAt(final Point2D inPlane, final double offset) {
        return new Vector3D(inPlane.x, u, inPlane.y, v, offset - originOffset, w);
    }

    /** Check if the instance is similar to another plane.
     * <p>Planes are considered similar if they contain the same
     * points. This does not mean they are equal since they can have
     * opposite normals.</p>
     * @param plane plane to which the instance is compared
     * @return true if the planes are similar
     */
    public boolean isSimilarTo(final Plane plane) {
        final double angle = Vector3D.angle(w, plane.w);
        return ((angle < 1.0e-10) && (FastMath.abs(originOffset - plane.originOffset) < 1.0e-10)) ||
               ((angle > (FastMath.PI - 1.0e-10)) && (FastMath.abs(originOffset + plane.originOffset) < 1.0e-10));
    }

    /** Rotate the plane around the specified point.
     * <p>The instance is not modified, a new instance is created.</p>
     * @param center rotation center
     * @param rotation vectorial rotation operator
     * @return a new plane
     */
    public Plane rotate(final Vector3D center, final Rotation rotation) {

        final Vector3D delta = origin.subtract(center);
        final Plane plane = new Plane(center.add(rotation.applyTo(delta)),
                                rotation.applyTo(w));

        // make sure the frame is transformed as desired
        plane.u = rotation.applyTo(u);
        plane.v = rotation.applyTo(v);

        return plane;

    }

    /** Translate the plane by the specified amount.
     * <p>The instance is not modified, a new instance is created.</p>
     * @param translation translation to apply
     * @return a new plane
     */
    public Plane translate(final Vector3D translation) {

        final Plane plane = new Plane(origin.add(translation), w);

        // make sure the frame is transformed as desired
        plane.u = u;
        plane.v = v;

        return plane;

    }

    /** Get the intersection of a line with the instance.
     * @param line line intersecting the instance
     * @return intersection point between between the line and the
     * instance (null if the line is parallel to the instance)
     */
    public Point3D intersection(final Line line) {
        final Vector3D direction = line.getDirection();
        final double   dot       = Vector3D.dotProduct(w, direction);
        if (FastMath.abs(dot) < 1.0e-10) {
            return null;
        }
        final Vector3D point = (Vector3D) line.toSpace(Point1D.ZERO);
        final double   k     = -(originOffset + Vector3D.dotProduct(w, point)) / dot;
        return new Point3D(1.0, point, k, direction);
    }

    /** Build the line shared by the instance and another plane.
     * @param other other plane
     * @return line at the intersection of the instance and the
     * other plane (really a {@link Line Line} instance)
     */
    public SubSpace intersection(final Hyperplane other) {
        final Plane otherP = (Plane) other;
        final Vector3D direction = Vector3D.crossProduct(w, otherP.w);
        if (direction.getNorm() < 1.0e-10) {
            return null;
        }
        return new Line(intersection(this, otherP, new Plane(direction)),
                        direction);
    }

    /** Get the intersection point of three planes.
     * @param plane1 first plane1
     * @param plane2 second plane2
     * @param plane3 third plane2
     * @return intersection point of three planes, null if some planes are parallel
     */
    public static Vector3D intersection(final Plane plane1, final Plane plane2, final Plane plane3) {

        // coefficients of the three planes linear equations
        final double a1 = plane1.w.getX();
        final double b1 = plane1.w.getY();
        final double c1 = plane1.w.getZ();
        final double d1 = plane1.originOffset;

        final double a2 = plane2.w.getX();
        final double b2 = plane2.w.getY();
        final double c2 = plane2.w.getZ();
        final double d2 = plane2.originOffset;

        final double a3 = plane3.w.getX();
        final double b3 = plane3.w.getY();
        final double c3 = plane3.w.getZ();
        final double d3 = plane3.originOffset;

        // direct Cramer resolution of the linear system
        // (this is still feasible for a 3x3 system)
        final double a23         = b2 * c3 - b3 * c2;
        final double b23         = c2 * a3 - c3 * a2;
        final double c23         = a2 * b3 - a3 * b2;
        final double determinant = a1 * a23 + b1 * b23 + c1 * c23;
        if (FastMath.abs(determinant) < 1.0e-10) {
            return null;
        }

        final double r = 1.0 / determinant;
        return new Vector3D(
                            (-a23 * d1 - (c1 * b3 - c3 * b1) * d2 - (c2 * b1 - c1 * b2) * d3) * r,
                            (-b23 * d1 - (c3 * a1 - c1 * a3) * d2 - (c1 * a2 - c2 * a1) * d3) * r,
                            (-c23 * d1 - (b1 * a3 - b3 * a1) * d2 - (b2 * a1 - b1 * a2) * d3) * r);

    }

    /** Build a region covering the whole hyperplane.
     * @return a region covering the whole hyperplane
     */
    public Region wholeHyperplane() {
        return new PolygonsSet();
    }

    /** Build a region covering the whole space.
     * @return a region containing the instance (really a {@link
     * PolyhedronsSet PolyhedronsSet} instance)
     */
    public Region wholeSpace() {
        return new PolyhedronsSet();
    }

    /** Check if the instance contains a point.
     * @param p point to check
     * @return true if p belongs to the plane
     */
    public boolean contains(final Point3D p) {
        return FastMath.abs(getOffset(p)) < 1.0e-10;
    }

    /** Get the offset (oriented distance) of a parallel plane.
     * <p>This method should be called only for parallel planes otherwise
     * the result is not meaningful.</p>
     * <p>The offset is 0 if both planes are the same, it is
     * positive if the plane is on the plus side of the instance and
     * negative if it is on the minus side, according to its natural
     * orientation.</p>
     * @param plane plane to check
     * @return offset of the plane
     */
    public double getOffset(final Plane plane) {
        return originOffset + (sameOrientationAs(plane) ? -plane.originOffset : plane.originOffset);
    }

    /** Get the offset (oriented distance) of a point.
     * <p>The offset is 0 if the point is on the underlying hyperplane,
     * it is positive if the point is on one particular side of the
     * hyperplane, and it is negative if the point is on the other side,
     * according to the hyperplane natural orientation.</p>
     * @param point point to check
     * @return offset of the point
     */
    public double getOffset(final Point point) {
        return Vector3D.dotProduct((Vector3D) point, w) + originOffset;
    }

    /** Check if the instance has the same orientation as another hyperplane.
     * @param other other hyperplane to check against the instance
     * @return true if the instance and the other hyperplane have
     * the same orientation
     */
    public boolean sameOrientationAs(final Hyperplane other) {
        return Vector3D.dotProduct(((Plane) other).w, w) > 0.0;
    }

    /** Compute the relative position of a sub-hyperplane with respect
     * to the instance.
     * @param sub sub-hyperplane to check
     * @return one of {@link org.apache.commons.math.geometry.partitioning.Hyperplane.Side#PLUS PLUS},
     * {@link org.apache.commons.math.geometry.partitioning.Hyperplane.Side#MINUS MINUS},
     * {@link org.apache.commons.math.geometry.partitioning.Hyperplane.Side#BOTH BOTH},
     * {@link org.apache.commons.math.geometry.partitioning.Hyperplane.Side#HYPER HYPER}
     */
    public Side side(final SubHyperplane sub) {

        final Plane otherPlane = (Plane) sub.getHyperplane();
        final Line  inter      = (Line) intersection(otherPlane);

        if (inter == null) {
            // the hyperplanes are parallel,
            // any point can be used to check their relative position
            final double global = getOffset(otherPlane);
            return (global < -1.0e-10) ? Side.MINUS : ((global > 1.0e-10) ? Side.PLUS : Side.HYPER);
        }

        // create a 2D line in the otherPlane canonical 2D frame such that:
        //   - the line is the crossing line of the two planes in 3D
        //   - the line splits the otherPlane in two half planes with an
        //     orientation consistent with the orientation of the instance
        //     (i.e. the 3D half space on the plus side (resp. minus side)
        //      of the instance contains the 2D half plane on the plus side
        //      (resp. minus side) of the 2D line
        Point2D p = (Point2D) otherPlane.toSubSpace(inter.toSpace(Point1D.ZERO));
        Point2D q = (Point2D) otherPlane.toSubSpace(inter.toSpace(Point1D.ONE));
        if (Vector3D.dotProduct(Vector3D.crossProduct(inter.getDirection(),
                                                      otherPlane.getNormal()),
                                                      w) < 0) {
            final Point2D tmp = p;
            p           = q;
            q           = tmp;
        }
        final Hyperplane line2D = new org.apache.commons.math.geometry.euclidean.twoD.Line(p, q);

        // check the side on the 2D plane
        return sub.getRemainingRegion().side(line2D);

    }

    /** Split a sub-hyperplane in two parts by the instance.
     * @param sub sub-hyperplane to split
     * @return an object containing both the part of the sub-hyperplane
     * on the plus side of the instance and the part of the
     * sub-hyperplane on the minus side of the instance
     */
    public SplitSubHyperplane split(final SubHyperplane sub) {

        final Plane otherPlane = (Plane) sub.getHyperplane();
        final Line  inter      = (Line) intersection(otherPlane);

        if (inter == null) {
            // the hyperplanes are parallel
            final double global = getOffset(otherPlane);
            return (global < -1.0e-10) ? new SplitSubHyperplane(null, sub) : new SplitSubHyperplane(sub, null);
        }

        // the hyperplanes do intersect
        Point2D p = (Point2D) otherPlane.toSubSpace(inter.toSpace(Point1D.ZERO));
        Point2D q = (Point2D) otherPlane.toSubSpace(inter.toSpace(Point1D.ONE));
        if (Vector3D.dotProduct(Vector3D.crossProduct(inter.getDirection(),
                                                      otherPlane.getNormal()),
                                                      w) < 0) {
            final Point2D tmp = p;
            p           = q;
            q           = tmp;
        }
        final SubHyperplane l2DMinus =
            new SubHyperplane(new org.apache.commons.math.geometry.euclidean.twoD.Line(p, q));
        final SubHyperplane l2DPlus =
            new SubHyperplane(new org.apache.commons.math.geometry.euclidean.twoD.Line(q, p));

        final BSPTree splitTree =
            sub.getRemainingRegion().getTree(false).split(l2DMinus);
        final BSPTree plusTree  = Region.isEmpty(splitTree.getPlus()) ?
                                  new BSPTree(Boolean.FALSE) :
                                  new BSPTree(l2DPlus, new BSPTree(Boolean.FALSE),
                                              splitTree.getPlus(), null);

        final BSPTree minusTree = Region.isEmpty(splitTree.getMinus()) ?
                                  new BSPTree(Boolean.FALSE) :
                                  new BSPTree(l2DMinus, new BSPTree(Boolean.FALSE),
                                              splitTree.getMinus(), null);

        return new SplitSubHyperplane(new SubHyperplane(otherPlane.copySelf(),
                                                        new PolygonsSet(plusTree)),
                                                        new SubHyperplane(otherPlane.copySelf(),
                                                                          new PolygonsSet(minusTree)));

    }

}
