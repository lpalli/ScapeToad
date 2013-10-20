/**
 * 
 */
package ch.epfl.scapetoad;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
    public void areaOfQuadrangleTest() {
        assertEquals(1, Geometry.areaOfQuadrangle(0, 0, 1, 0, 1, 1, 0, 1),
                0.001);
    }

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