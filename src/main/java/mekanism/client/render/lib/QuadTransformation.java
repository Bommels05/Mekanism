package mekanism.client.render.lib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import mekanism.common.lib.Color;
import mekanism.common.lib.math.Quaternion;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;

public interface QuadTransformation {
    // down up north south west east
    static final Direction[][] ROTATION_MATRIX = new Direction[][] {{ Direction.SOUTH, Direction.NORTH, Direction.DOWN, Direction.DOWN, Direction.DOWN, Direction.DOWN },
                                                                    { Direction.NORTH, Direction.SOUTH, Direction.UP, Direction.UP, Direction.UP, Direction.UP },
                                                                    { Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST },
                                                                    { Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST },
                                                                    { Direction.WEST, Direction.WEST, Direction.WEST, Direction.EAST, Direction.SOUTH, Direction.NORTH },
                                                                    { Direction.EAST, Direction.EAST, Direction.EAST, Direction.WEST, Direction.NORTH, Direction.SOUTH }};

    static QuadTransformation identity = q -> {};

    static QuadTransformation fullbright = light(1);

    static QuadTransformation color(Color color) {
        return new ColorTransformation(color);
    }

    static QuadTransformation light(float light) {
        return new LightTransformation(light, light);
    }

    static QuadTransformation translate(Vec3d translation) {
        return new TranslationTransformation(translation);
    }

    static QuadTransformation rotate(Direction side) {
        if (side == null)
            return identity;
        switch (side) {
            case UP: return rotate(90, 0, 0);
            case DOWN: return rotate(-90, 0, 0);
            case WEST: return rotate(0, 90, 0);
            case EAST: return rotate(0, -90, 0);
            case SOUTH: return rotate(0, 180, 0);
            default:
                return identity;
        }
    }

    static QuadTransformation rotateY(double degrees) {
        return rotate(0, degrees, 0);
    }

    static QuadTransformation rotate(double rotationX, double rotationY, double rotationZ) {
        return rotate(new Quaternion(rotationX, rotationY, rotationZ, true));
    }

    static QuadTransformation rotate(Quaternion quat) {
        return new RotationTransformation(quat);
    }

    static QuadTransformation sideRotate(Direction side) {
        return new SideTransformation(side);
    }

    static QuadTransformation list(QuadTransformation... transforms) {
        return TransformationList.of(transforms);
    }

    void transform(Quad quad);

    default QuadTransformation and(QuadTransformation other) {
        return list(this, other);
    }

    public class SideTransformation implements QuadTransformation {

        private final Direction side;

        public SideTransformation(Direction side) {
            this.side = side;
        }

        @Override
        public void transform(Quad quad) {
            if (side == null)
                return;
            quad.setSide(ROTATION_MATRIX[quad.getSide().ordinal()][side.ordinal()]);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof SideTransformation && side == ((SideTransformation) other).side;
        }

        @Override
        public int hashCode() {
            return side != null ? side.hashCode() : -1;
        }
    }

    public class ColorTransformation implements QuadTransformation {

        private final Color color;

        public ColorTransformation(Color color) {
            this.color = color;
        }

        @Override
        public void transform(Quad quad) {
            quad.vertexTransform(v -> v.color(color));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ColorTransformation && color.equals(((ColorTransformation) other).color);
        }

        @Override
        public int hashCode() {
            return color.hashCode();
        }
    }

    public class LightTransformation implements QuadTransformation {

        private final float lightU;
        private final float lightV;

        public LightTransformation(float lightU, float lightV) {
            this.lightU = lightU;
            this.lightV = lightV;
        }

        @Override
        public void transform(Quad quad) {
            quad.vertexTransform(v -> v.light(lightU, lightV));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof LightTransformation && lightU == ((LightTransformation) other).lightU && lightV == ((LightTransformation) other).lightV;
        }

        @Override
        public int hashCode() {
            return Objects.hash(lightU, lightV);
        }
    }

    public class RotationTransformation implements QuadTransformation {

        // quaternion math isn't exact- we round to nearest ten-thousandth
        private static final double EPSILON = 10_000;

        private final Quaternion quaternion;

        public RotationTransformation(Quaternion quaternion) {
            this.quaternion = quaternion;
        }

        @Override
        public void transform(Quad quad) {
            quad.vertexTransform(v -> {
                v.pos(round(quaternion.rotate(v.getPos().subtract(0.5, 0.5, 0.5)).add(0.5, 0.5, 0.5)));
                v.normal(round(quaternion.rotate(v.getNormal()).normalize()));
            });
        }

        private static Vec3d round(Vec3d vec) {
            return new Vec3d(Math.round(vec.x * EPSILON) / EPSILON, Math.round(vec.y * EPSILON) / EPSILON, Math.round(vec.z * EPSILON) / EPSILON);
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof RotationTransformation && quaternion.equals(((RotationTransformation) other).quaternion);
        }

        @Override
        public int hashCode() {
            return quaternion.hashCode();
        }
    }

    public class TranslationTransformation implements QuadTransformation {

        private Vec3d translation;

        public TranslationTransformation(Vec3d translation) {
            this.translation = translation;
        }

        @Override
        public void transform(Quad quad) {
            quad.vertexTransform(v -> v.pos(v.getPos().add(translation)));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TranslationTransformation && translation.equals(((TranslationTransformation) other).translation);
        }

        @Override
        public int hashCode() {
            return translation.hashCode();
        }
    }

    public class TextureFilteredTransformation implements QuadTransformation {

        private QuadTransformation original;
        private Predicate<ResourceLocation> verifier;

        public TextureFilteredTransformation(QuadTransformation original, Predicate<ResourceLocation> verifier) {
            this.original = original;
            this.verifier = verifier;
        }

        public static TextureFilteredTransformation of(QuadTransformation original, Predicate<ResourceLocation> verifier) {
            return new TextureFilteredTransformation(original, verifier);
        }

        @Override
        public void transform(Quad quad) {
            if (verifier.test(quad.getTexture().getName())) {
                quad.transform(v -> original.transform(quad));
            }
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TextureFilteredTransformation && verifier.equals(((TextureFilteredTransformation) other).verifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(original, verifier);
        }
    }

    public static class TransformationList implements QuadTransformation {

        private List<QuadTransformation> list = new ArrayList<>();
        private int hashCode;

        public TransformationList(List<QuadTransformation> list) {
            this.list = list;
            hashCode = list.hashCode();
        }

        public static TransformationList of(QuadTransformation... trans) {
            return new TransformationList(Arrays.asList(trans));
        }

        @Override
        public void transform(Quad quad) {
            list.forEach(transformation -> transformation.transform(quad));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof TransformationList && list.equals(((TransformationList) other).list);
        }

        @Override
        public QuadTransformation and(QuadTransformation other) {
            List<QuadTransformation> newList = new ArrayList<>(list);
            newList.add(other);
            return new TransformationList(newList);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
