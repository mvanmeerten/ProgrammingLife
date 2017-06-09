package programminglife.model;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import programminglife.utility.InitFXThread;

import static org.junit.Assert.assertEquals;

/**
 * Tests for the {@link XYCoordinate} model.
 */
public class XYCoordinateTest {
    private XYCoordinate coordinate1;
    private XYCoordinate coordinate2;
    private static double DELTA = 0.000001;

    @BeforeClass
    public static void setUpClass() throws Exception {
        InitFXThread.setupClass();
    }

    @Before
    public void setUp() {
        this.coordinate1 = new XYCoordinate(8, 54);
        this.coordinate2 = new XYCoordinate(1, -1);
    }

    @Test
    public void getterTest() {
        assertEquals(8, coordinate1.getX(), DELTA);
        assertEquals(54, coordinate1.getY(), DELTA);
    }

    @Test
    public void setterTest() {
        coordinate1.setX(2);
        coordinate1.setY(-4);

        assertEquals(2, coordinate1.getX(), DELTA);
        assertEquals(-4, coordinate1.getY(), DELTA);
    }

    @Test
    public void addCoordinateTest() {
        XYCoordinate sum = coordinate1.add(coordinate2);

        assertEquals(9, sum.getX(), DELTA);
        assertEquals(53, sum.getY(), DELTA);
    }

    @Test
    public void addIntsTest() {
        XYCoordinate sum = coordinate1.add(1, -1);

        assertEquals(9, sum.getX(), DELTA);
        assertEquals(53, sum.getY(), DELTA);
    }

    @Test
    public void multiplyTest() {
        XYCoordinate multiplied = coordinate1.multiply(0.5);

        assertEquals(4, multiplied.getX(), DELTA);
        assertEquals(27, multiplied.getY(), DELTA);
    }

    @Test
    public void multiplyRoundedTest() {
        XYCoordinate multipliedRounded = coordinate1.multiply(0.1);

        assertEquals(0, multipliedRounded.getX(), DELTA);
        assertEquals(5, multipliedRounded.getY(), DELTA);
    }

    @Test
    public void toStringTest() {
        assertEquals("(8.0, 54.0)", coordinate1.toString());
    }
}
