package programminglife.model.drawing;

import org.junit.*;
import programminglife.model.*;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SubGraphTest {
    private static final String TEST_DB = "test.db";

    GenomeGraph graph;
    DrawableNode centerNode;

    private static String TEST_PATH, TEST_FAULTY_PATH;

    @BeforeClass
    public static void setUpClass() throws Exception {
        DataManager.initialize(TEST_DB);

        TEST_PATH = new File(GenomeGraphTest.class.getResource("/test.gfa").toURI()).getAbsolutePath();
        TEST_FAULTY_PATH = new File(
                GenomeGraphTest.class.getClass().getResource("/test-faulty.gfa").toURI()
        ).getAbsolutePath();
    }

    @Before
    public void setUp() throws Exception {
        graph = new GenomeGraph("test graph");
        centerNode = new DrawableNode(new Segment(4));
    }

    @After
    public void tearDown() throws Exception {
        DataManager.clearDB(TEST_DB);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        DataManager.removeDB(TEST_DB);
    }

    @Test
    public void topoSortTest() throws Exception {
        SubGraph sg = new SubGraph(centerNode, 5);
        List<DrawableNode> actual = sg.topoSort();

        assertEquals(8, actual.size());

        Set<DrawableNode> found = new HashSet<>();
        for (DrawableNode n : actual) {
            // assert that all parents were already found.
            assertTrue(found.containsAll(n.getParents())); // All parents of this node were already found
            assertTrue(Collections.disjoint(found, n.getChildren())); // none of the children of this node were found
            found.add(n);
        }
    }

    @Test
    public void atLocationTest() {
        SubGraph sg = new SubGraph(centerNode, 0); // only include centerNode
        sg.layout();
        double x = centerNode.getX();
        double y = centerNode.getY();
        double width = centerNode.getWidth();
        double height = centerNode.getHeight();
        // Note: use == to make sure it is the exact same object.
        // in order: assert that all corners, the center, and some other points inside the node can be found.
        assertTrue(centerNode == sg.atLocation(x, y));
        assertTrue(centerNode == sg.atLocation(x + width, y));
        assertTrue(centerNode == sg.atLocation(x, y + height));
        assertTrue(centerNode == sg.atLocation(x + width, y + height));
        assertTrue(centerNode == sg.atLocation(x + width / 2, y + height / 2));
        assertTrue(centerNode == sg.atLocation(x + width / 3, y + height / 6));
        assertTrue(centerNode == sg.atLocation(x + width / 4, y + height));

        // assert that places outside the node do not return the node (return null)
        assertNull(sg.atLocation(x - 1, y));
        assertNull(sg.atLocation(x, y - 1));
        assertNull(sg.atLocation(x - 1, y - 1));
        assertNull(sg.atLocation(x + width + 1, y));
        assertNull(sg.atLocation(x, y + height + 1));
        assertNull(sg.atLocation(x + width + 1, y + height + 1));
    }
}
