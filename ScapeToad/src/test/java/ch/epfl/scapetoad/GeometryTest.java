/**
 * 
 */
package ch.epfl.scapetoad;

import static org.junit.Assert.assertArrayEquals;

import org.junit.Test;

import ch.epfl.scapetoad.compute.Geometry;

/**
 * @author luca
 * 
 */
@SuppressWarnings("static-method")
public class GeometryTest {

    /**
     * 
     */
    @Test
    public void intersectionOfSegmentsTest() {
        double[] result = { 0.5, 0.5 };
        assertArrayEquals(result,
                Geometry.intersectionOfSegments(0, 0, 1, 1, 0, 1, 1, 0), 0.001);
    }
}