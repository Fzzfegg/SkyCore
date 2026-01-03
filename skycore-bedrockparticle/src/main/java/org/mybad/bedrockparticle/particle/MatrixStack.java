/*
 * Original work Copyright (C) 2023 Ocelot
 * Original code licensed under MIT License
 *
 * Modified by 17Artist on 2025-3-29
 * Modifications and redistribution licensed under GNU Lesser General Public License v3.0
 *
 * Changes:
 * - Renamed package from 'org.mybad.bedrockparticle.particle.*' to 'priv.seventeen.artist.*' (all subpackages)
 * - Changed license from MIT to LGPL v3.0
 * - Merged interface and implementation into a single entity class
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package org.mybad.bedrockparticle.particle;

import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionfc;

/**
 * @author Ocelot
 */
@ApiStatus.Internal
public class MatrixStack {

    private final Matrix4fStack positionStack;
    private final Matrix3f normal;
    private boolean dirtyNormal;

    public MatrixStack() {
        this.positionStack = new Matrix4fStack(64);
        this.normal = new Matrix3f();
    }

    public  void translate(double x, double y, double z) {
        this.translate((float) x, (float) y, (float) z);
    }

    public  void translate(float x, float y, float z) {
        this.position().translate(x, y, z);
    }

    public  void scale(double xyz) {
        this.scale((float) xyz, (float) xyz, (float) xyz);
    }

    public  void scale(float xyz) {
        this.scale(xyz, xyz, xyz);
    }

    public  void scale(double x, double y, double z) {
        this.scale((float) x, (float) y, (float) z);
    }


    public void reset() {
        this.positionStack.clear();
        this.normal.identity();
        this.dirtyNormal = false;
    }

    public void rotate(Quaternionfc rotation) {
        this.position().rotate(rotation);
        this.dirtyNormal = true;
    }

    public void rotate(float amount, float x, float y, float z) {
        this.position().rotate(amount, x, y, z);
        this.dirtyNormal = true;
    }

    public void rotateXYZ(float x, float y, float z) {
        this.position().rotateXYZ(x, y, z);
        this.dirtyNormal = true;
    }

    public void rotateZYX(float z, float y, float x) {
        this.position().rotateZYX(z, y, x);
        this.dirtyNormal = true;
    }


    public void scale(float x, float y, float z) {
        this.position().scale(x, y, z);
        this.dirtyNormal = true;
    }


    public void copy(MatrixStack stack) {
        this.position().set(stack.position());
        this.normal.set(stack.normal());
        this.dirtyNormal = false;
    }


    public void pushMatrix() {
        this.positionStack.pushMatrix();
        this.dirtyNormal = true;
    }


    public void popMatrix() {
        this.positionStack.popMatrix();
        this.dirtyNormal = true;
    }


    public Matrix4f position() {
        return this.positionStack;
    }

    public Matrix3f normal() {
        if (this.dirtyNormal) {
            this.dirtyNormal = false;
            return this.positionStack.normal(this.normal);
        }
        return this.normal;
    }
}
