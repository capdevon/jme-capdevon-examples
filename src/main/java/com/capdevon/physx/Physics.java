package com.capdevon.physx;

import java.util.ArrayList;
import java.util.List;

import com.jme3.bounding.BoundingBox;
import com.jme3.bounding.BoundingSphere;
import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.PhysicsTickListener;
import com.jme3.bullet.collision.PhysicsCollisionListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.PhysicsRayTestResult;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.SphereCollisionShape;
import com.jme3.bullet.control.BetterCharacterControl;
import com.jme3.bullet.control.PhysicsControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Spatial;
import com.jme3.util.TempVars;

public class Physics {
	
    /**
     * DefaultRaycastLayers ALL LAYERS
     */
    private static final int DefaultRaycastLayers = ~0;
    
    private Physics() {
    	// private constructor.
    }
    
    public static void addObject(Spatial sp) {
        PhysicsSpace.getPhysicsSpace().add(sp);
    }

    public static void addControl(PhysicsControl control) {
        PhysicsSpace.getPhysicsSpace().add(control);
    }

    public static void addCollisionListener(PhysicsCollisionListener listener) {
        PhysicsSpace.getPhysicsSpace().addCollisionListener(listener);
    }

    public static void addTickListener(PhysicsTickListener listener) {
        PhysicsSpace.getPhysicsSpace().addTickListener(listener);
    }
 
    /**
     * 
     * @param spatial
     * @param radius
     * @param height
     * @param mass 
     */
    public static void addCapsuleCollider(Spatial spatial, float radius, float height, float mass) {
        BetterCharacterControl bcc = new BetterCharacterControl(radius, height, mass);
        spatial.addControl(bcc);
        PhysicsSpace.getPhysicsSpace().add(bcc);
    }

    public static void addCapsuleCollider(Spatial spatial) {
        BoundingBox bb = (BoundingBox) spatial.getWorldBound();
        float radius = Math.min(bb.getXExtent(), bb.getZExtent());
        float height = Math.max(bb.getYExtent(), radius * 2.5f);
        float mass = 50f;
        addCapsuleCollider(spatial, radius, height, mass);
    }

    public static void addBoxCollider(Spatial sp, float mass, boolean isKinematic) {
        BoundingBox bb = (BoundingBox) sp.getWorldBound();
        BoxCollisionShape box = new BoxCollisionShape(bb.getExtent(null));
        addRigidBody(box, sp, mass, isKinematic);
    }

    public static void addSphereCollider(Spatial sp, float mass, boolean isKinematic) {
        BoundingSphere bs = (BoundingSphere) sp.getWorldBound();
        SphereCollisionShape sphere = new SphereCollisionShape(bs.getRadius());
        addRigidBody(sphere, sp, mass, isKinematic);
    }

    public static void addMeshCollider(Spatial sp, float mass, boolean isKinematic) {
        CollisionShape shape = CollisionShapeFactory.createMeshShape(sp);
        addRigidBody(shape, sp, mass, isKinematic);
    }

    public static void addDynamicMeshCollider(Spatial sp, float mass, boolean isKinematic) {
        CollisionShape shape = CollisionShapeFactory.createDynamicMeshShape(sp);
        addRigidBody(shape, sp, mass, isKinematic);
    }

    public static void addRigidBody(CollisionShape shape, Spatial sp, float mass, boolean isKinematic) {
        RigidBodyControl rgb = new RigidBodyControl(shape, mass);
        sp.addControl(rgb);
        rgb.setKinematic(isKinematic);
        PhysicsSpace.getPhysicsSpace().add(rgb);
    }
 
    
    /**
     * @param rb
     * @param explosionForce	- The force of the explosion (which may be modified by distance).
     * @param explosionPosition	- The centre of the sphere within which the explosion has its effect.
     * @param explosionRadius	- The radius of the sphere within which the explosion has its effect.
     */
    public static void addExplosionForce(PhysicsRigidBody rb, float explosionForce, Vector3f explosionPosition, float explosionRadius) {
        Vector3f expCenter2Body = rb.getPhysicsLocation().subtract(explosionPosition);
        float distance = expCenter2Body.length();
        if (distance < explosionRadius) {
            // apply proportional explosion force
            float strength = (1.f - FastMath.clamp(distance / explosionRadius, 0, 1)) * explosionForce;
            rb.setLinearVelocity(expCenter2Body.normalize().mult(strength));
        }
    }

    /**
     * Casts a ray through the scene and returns all hits.
     * 
     * @param origin
     * @param direction
     * @param maxDistance
     * @param layerMask
     * @return 
     */
    public static List<RaycastHit> raycastAll(Vector3f origin, Vector3f direction, float maxDistance, int layerMask) {

        List<RaycastHit> lstResults = new ArrayList<>();

        TempVars t = TempVars.get();
        Vector3f beginVec = t.vect1.set(origin);
        Vector3f finalVec = t.vect2.set(direction).multLocal(maxDistance).addLocal(origin);

        List<PhysicsRayTestResult> results = PhysicsSpace.getPhysicsSpace().rayTest(beginVec, finalVec);

        for (PhysicsRayTestResult ray : results) {
            PhysicsCollisionObject pco = ray.getCollisionObject();

            if (ray.getHitFraction() < maxDistance && applyMask(layerMask, pco.getCollisionGroup())) {

                RaycastHit hitInfo = new RaycastHit();
                hitInfo.rigidBody   = pco;
                hitInfo.collider    = pco.getCollisionShape();
                hitInfo.gameObject  = (Spatial) pco.getUserObject();
                hitInfo.normal      = ray.getHitNormalLocal();
                hitInfo.distance    = finalVec.subtract(beginVec).length() * ray.getHitFraction();
                hitInfo.point.interpolateLocal(beginVec, finalVec, ray.getHitFraction());

                lstResults.add(hitInfo);
            }
        }

        t.release();
        return lstResults;
    }
     
    /**
     * Casts a ray through the scene and returns all hits.
     * 
     * @param origin
     * @param direction
     * @param maxDistance
     * @return
     */
    public static List<RaycastHit> raycastAll(Vector3f origin, Vector3f direction, float maxDistance) {
        return raycastAll(origin, direction, maxDistance, DefaultRaycastLayers);
    }
    
    /**
     * Casts a ray, from point origin, in direction direction, of length
     * maxDistance, against all colliders in the scene. You may optionally
     * provide a LayerMask, to filter out any Colliders you aren't interested in
     * generating collisions with.
     *
     * @param origin        - The starting point of the ray in world coordinates. (not null, unaffected)
     * @param direction     - The direction of the ray. (not null, unaffected)
     * @param hitInfo       - If true is returned, hitInfo will contain more information about where the closest collider was hit. (See Also: RaycastHit).
     * @param maxDistance   - The max distance the ray should check for collisions.
     * @param layerMask     - A Layer mask that is used to selectively ignore Colliders when casting a ray.
     * @return Returns true if the ray intersects with a Collider, otherwise
     * false.
     */
    public static boolean Raycast(Vector3f origin, Vector3f direction, RaycastHit hitInfo, float maxDistance, int layerMask) {
        
        boolean collision = false;
        float hf = maxDistance;

        TempVars t = TempVars.get();
        Vector3f beginVec = t.vect1.set(origin);
        Vector3f finalVec = t.vect2.set(direction).multLocal(maxDistance).addLocal(origin);

        List<PhysicsRayTestResult> results = PhysicsSpace.getPhysicsSpace().rayTest(beginVec, finalVec);

        for (PhysicsRayTestResult ray : results) {
            PhysicsCollisionObject pco = ray.getCollisionObject();

            if (ray.getHitFraction() < hf && applyMask(layerMask, pco.getCollisionGroup())) {
                collision = true;
                hf = ray.getHitFraction();

                hitInfo.rigidBody   = pco;
                hitInfo.collider    = pco.getCollisionShape();
                hitInfo.gameObject  = (Spatial) pco.getUserObject();
                hitInfo.normal      = ray.getHitNormalLocal();
                hitInfo.distance    = finalVec.subtract(beginVec).length() * hf;
                hitInfo.point.interpolateLocal(beginVec, finalVec, hf);
            }
        }

        if (!collision) {
            hitInfo.clear();
        }

        t.release();
        return collision;
    }
    
    public static boolean Raycast(Ray ray, RaycastHit hitInfo, float maxDistance) {
        return Raycast(ray.origin, ray.direction, hitInfo, maxDistance, DefaultRaycastLayers);
    }

    public static boolean Raycast(Ray ray, RaycastHit hitInfo, float maxDistance, int layerMask) {
        return Raycast(ray.origin, ray.direction, hitInfo, maxDistance, layerMask);
    }

    public static boolean Raycast(Vector3f origin, Vector3f direction, RaycastHit hitInfo, float maxDistance) {
        return Raycast(origin, direction, hitInfo, maxDistance, DefaultRaycastLayers);
    }
    
    /**
     * @param beginVec  - (not null, unaffected)
     * @param finalVec  - (not null, unaffected)
     * @param hitInfo   - If true is returned, hitInfo will contain more information about where the closest collider was hit. (See Also: RaycastHit).
     * @param layerMask - A Layer mask that is used to selectively ignore Colliders when casting a ray.
     * @return Returns true if the ray intersects with a Collider, otherwise
     * false.
     */
    public static boolean Linecast(Vector3f beginVec, Vector3f finalVec, RaycastHit hitInfo, int layerMask) {

        boolean collision = false;
        float hf = finalVec.length();

        List<PhysicsRayTestResult> results = PhysicsSpace.getPhysicsSpace().rayTest(beginVec, finalVec);
        for (PhysicsRayTestResult ray : results) {

            PhysicsCollisionObject pco = ray.getCollisionObject();

            if (ray.getHitFraction() < hf && applyMask(layerMask, pco.getCollisionGroup())) {
                collision = true;
                hf = ray.getHitFraction();

                hitInfo.rigidBody   = pco;
                hitInfo.collider    = pco.getCollisionShape();
                hitInfo.gameObject  = (Spatial) pco.getUserObject();
                hitInfo.normal      = ray.getHitNormalLocal();
                hitInfo.point       = FastMath.interpolateLinear(hf, beginVec, finalVec);
                hitInfo.distance    = beginVec.distance(hitInfo.point);
            }
        }

        if (!collision) {
            hitInfo.clear();
        }

        return collision;
    }
        
    /**
     * 
     * @param beginVec
     * @param finalVec
     * @param hitInfo
     * @return 
     */
    public static boolean Linecast(Vector3f beginVec, Vector3f finalVec, RaycastHit hitInfo) {
        return Linecast(beginVec, finalVec, hitInfo, DefaultRaycastLayers);
    }

    /**
     * Check if a collisionGroup is in a layerMask
     *
     * @param layerMask
     * @param collisionGroup
     * @return
     */
    private static boolean applyMask(int layerMask, int collisionGroup) {
        return layerMask == (layerMask | collisionGroup);
    }
}
