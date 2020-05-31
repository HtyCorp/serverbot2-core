import io.mamish.serverbot2.testutils.ReflectiveEquals;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Objects;

public class ReflectiveEqualsTest {

    @Test
    void testNullBehaviour() {
        Object onull1 = null;
        Object onull2 = null;
        Object a1 = new ClassA(1, "v1");

        Assertions.assertTrue(ReflectiveEquals.areEqual(onull1, onull2));
        assertMatch(onull1, onull2, true);
        assertMatch(onull1, a1, false);
        assertMatch(a1, onull2, false);
    }

    @Test
    void testSameObject() {
        Object a1 = new ClassA(1, "v1");
        assertMatch(a1, a1, true);
    }

    @Test
    void testDifferentClasses() {
        Object nonNested = new ClassA(1, "v1");
        Object nested = new NestedA(new ClassA(1, "v1"));
        assertMatch(nonNested, nested, false);
    }

    @Test
    void testPrimitiveCompare() {
        Object sameA = new PrimitivesOnly(5, 3.2d, 'x', false);
        Object sameB = new PrimitivesOnly(5, 3.2d, 'x', false);
        Object diffInt = new PrimitivesOnly(4, 3.2d, 'x', false);
        Object diffDouble = new PrimitivesOnly(5, 2.2d, 'x', false);
        Object diffChar = new PrimitivesOnly(5, 3.2d, 'y', false);
        Object diffBool = new PrimitivesOnly(5, 3.2d, 'x', true);

        assertBoth(sameA, sameB, sameA, true);
        assertBoth(sameA, sameB, diffInt, false);
        assertBoth(sameA, sameB, diffDouble, false);
        assertBoth(sameA, sameB, diffChar, false);
        assertBoth(sameA, sameB, diffBool, false);
    }

    @Test
    void testHasValidEquals() {
        HasEqualsValid sameA = new HasEqualsValid(1, "blah");
        HasEqualsValid sameB = new HasEqualsValid(1, "blah");
        HasEqualsValid diff = new HasEqualsValid(2, "BLAH");

        assertMatch(sameA, sameB, true);
        assertMatch(sameB, diff, false);
    }

    @Test
    // Test that equals() is being invoked by using an equals() implementation with a known, predictable bug
    void testWithBrokenEquals() {
        HasEqualsButBugged sameA = new HasEqualsButBugged(1, "blah");
        HasEqualsButBugged sameB = new HasEqualsButBugged(1, "blah");
        HasEqualsButBugged diff = new HasEqualsButBugged(2, "BLAH");

        // Object are the same but bugged equals() skews the compare values to be different
        assertMatch(sameA, sameB, false);
        // Objects are different but bugged equals() skews the compare values to be the same
        assertMatch(diff, sameA, true);
    }

    @Test
    void testExtendedObject() {
        ExtendedA sameA = new ExtendedA(1, "blah", 'x');
        ExtendedA sameB = new ExtendedA(1, "blah", 'x');
        ExtendedA diff = new ExtendedA(2, "BLAH", 'y');

        assertBoth(sameA, sameB, sameA, true);
        assertBoth(sameA, sameB, diff, false);
    }

    private void assertMatch(Object object1, Object object2, boolean expectEqual) {
        Assertions.assertEquals(expectEqual, ReflectiveEquals.areEqual(object1, object2));
    }

    private void assertBoth(Object expected1, Object expected2, Object actual, boolean expectEqual) {
        assertMatch(expected1, actual, expectEqual);
        assertMatch(expected2, actual, expectEqual);
    }

    static class ClassA {
        private final int i;
        private final String s;

        public ClassA(int i, String s) {
            this.i = i;
            this.s = s;
        }
    }

    static class ExtendedA extends ClassA {
        private final char c;

        public ExtendedA(int i, String s, char c) {
            super(i, s);
            this.c = c;
        }
    }

    static class NestedA {
        private final ClassA a;

        public NestedA(ClassA a) {
            this.a = a;
        }
    }

    static class PrimitivesOnly {
        private final int i;
        private final double d;
        private final char c;
        private final boolean b;

        public PrimitivesOnly(int i, double d, char c, boolean b) {
            this.i = i;
            this.d = d;
            this.c = c;
            this.b = b;
        }
    }

    static class HasEqualsValid {
        private final int i;
        private final String s;

        public HasEqualsValid(int i, String s) {
            this.i = i;
            this.s = s;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof HasEqualsValid)) {
                return false;
            }
            HasEqualsValid other = (HasEqualsValid) obj;
            return i == other.i && Objects.equals(s, other.s);
        }

        @Override
        public int hashCode() {
            return i * s.hashCode();
        }
    }

    static class HasEqualsButBugged {
        private final int i;
        private final String s;

        public HasEqualsButBugged(int i, String s) {
            this.i = i;
            this.s = s;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof HasEqualsButBugged)) {
                return false;
            }
            HasEqualsButBugged other = (HasEqualsButBugged) obj;
            return i == (other.i + 1) && s.equals(s.toUpperCase());
        }

        @Override
        public int hashCode() {
            return i * s.hashCode();
        }
    }

}
