package programminglife.model.drawing;

import javafx.scene.paint.Color;
import programminglife.model.GenomeGraph;
import programminglife.model.Link;
import programminglife.model.XYCoordinate;

import java.util.*;

/**
 * A Segment that also Implements {@link Drawable}.
 */
public class DrawableSegment implements DrawableNode {
    public static final double NODE_HEIGHT = 10;

    private final GenomeGraph graph;
    private final int id;

    private double strokeWidth;
    private Color fillColor;
    private Color strokeColor;
    private XYCoordinate location;
    private double width;
    private double height;
    private boolean drawDimensionsUpToDate = false;

    private Set<Integer> parents;
    private Set<Integer> children;

    /**
     * Create a DrawableSegment from a Segment.
     * @param graph the graph this Segment is in
     * @param nodeID The segment to create this DrawableSegment from.
     */
    public DrawableSegment(GenomeGraph graph, int nodeID) {
        this.graph = graph;
        this.id = nodeID;

        parents = new LinkedHashSet<>();
        children = new LinkedHashSet<>();

        Arrays.stream(graph.getParentIDs(nodeID)).forEach(id -> parents.add(id));
        Arrays.stream(graph.getChildIDs(nodeID)).forEach(id -> children.add(id));

        this.setDrawDimensions();
    }

    /**
     * Get all the children of the node {@link DrawableSegment}.
     * @return children {@link Collection} are the direct children of the node {@link DrawableSegment}.
     */
    @Override
    public Set<Integer> getChildren() {
        return children;
    }

    /**
     * Get all the parents of the node {@link DrawableSegment}.
     * @return parent {@link Collection} are the direct parents of the node {@link DrawableSegment}.
     **/
    public Set<Integer> getParents() {
        return parents;
    }

    /**
     * Replace a child node with a dummy node.
     * @param oldChild The {@link DrawableSegment} to replace.
     * @param newChild The {@link DrawableSegment} to replace with.
     */
    public void replaceChild(DrawableNode oldChild, DrawableNode newChild) {
        if (!this.children.remove(oldChild.getIdentifier())) {
            throw new NoSuchElementException("The node to be replaced is not a child of this node.");
        }
        this.children.add(newChild.getIdentifier());
    }

    @Override
    public String details() {
        return String.format("Sequence: %s%nGenomes: %s",
                this.getSequence(), this.getGraph().getGenomeNames(this.getGenomes()));
    }

    /**
     * Replace a parent node with a dummy node.
     * @param oldParent The {@link DrawableSegment} to replace.
     * @param newParent The {@link DrawableSegment} to replace with.
     */
    public void replaceParent(DrawableNode oldParent, DrawableNode newParent) {
        if (!this.parents.remove(oldParent.getIdentifier())) {
            throw new NoSuchElementException(
                    String.format("The node to be replaced (%d) is not a parent of this node (%d).",
                            oldParent.getIdentifier(), this.getIdentifier()));
        }
        this.parents.add(newParent.getIdentifier());
    }

    public XYCoordinate getLocation() {
        return this.location;
    }

    /**
     * Set the size {@link XYCoordinate} of the Segment.
     * @param width The double representing the width of the DrawableSegment
     * @param height The double representing the height of the DrawableSegment
     */
    void setSize(double width, double height) {
        this.setWidth(width);
        this.setHeight(height);
        this.setDrawDimensionsUpToDate(true);
    }

    /**
     * Set the location of the Segment.
     * @param x the x location
     * @param y the y location
     */
    @Override
    public void setLocation(double x, double y) {
        this.location = new XYCoordinate(x, y);
    }

    /**
     * Set the width.
     * @param width The width to set to
     */
    private void setWidth(double width) {
        this.width = width;
    }

    /**
     * Set the height.
     * @param height The height to set to
     */
    private void setHeight(double height) {
        this.height = height;
    }

    /**
     * getter for the width coordinate.
     * @return XYCoordinate.
     */
    public XYCoordinate getWidthCoordinate() {
        if (!this.isDrawDimensionsUpToDate()) {
            setDrawDimensions();
        }
        return new XYCoordinate((int) this.getWidth(), 0);
    }

    /**
     * getter for the height coordinate.
     * @return XYCoordinate.
     */
    public XYCoordinate getHeightCoordinate() {
        if (!this.isDrawDimensionsUpToDate()) {
            setDrawDimensions();
        }
        return new XYCoordinate(0, (int) this.getHeight());
    }

    /**
     * Setter for the dimension of the node.
     */
    public void setDrawDimensions() {
        int segmentLength = this.getSequenceLength();
        double width, height;

        width = 10 + Math.pow(segmentLength, 1.0 / 2);
        height = NODE_HEIGHT;

        this.setSize(width, height);
        this.setDrawDimensionsUpToDate(true);
    }

    public double getGenomeFraction() {
        return this.getGraph().getGenomeFraction(this.getIdentifier());
    }

    @Override
    public int[] getGenomes() {
        return this.getGraph().getGenomes(this.getIdentifier());
    }

    /**
     * get the length of the sequence of this segment.
     * @return the length of the sequence of this segment
     */
    private int getSequenceLength() {
        return this.getGraph().getSequenceLength(this.getIdentifier());
    }

    /**
     * Returns the sequence of all the segments that are part of the DrawableNode.
     * @return A string containing all the sequences appended.
     */
    public String getSequence() {
        return this.getGraph().getSequence(getIdentifier());
    }

    /**
     * Method to return a string with information about the {@link DrawableSegment}.
     * @return a {@link String} representation of a {@link DrawableSegment}.
     */
    @Override
    public String toString() {
        return "Segment: "
                + getIdentifier()
                + " "
                + "Location: "
                + this.getLocation().getX()
                + ","
                + this.getLocation().getY();
    }


    @Override
    public boolean equals(Object other) {
        if (other instanceof DrawableSegment) {
            DrawableSegment that = (DrawableSegment) other;
            if (that.getIdentifier() == this.getIdentifier()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getIdentifier();
    }

    /**
     * Get the {@link Link} between this node and the child drawable node.
     * @param child The {@link DrawableSegment} that the link goes to.
     * @return {@link Link} between the two nodes.
     */
    @Override
    public Link getLink(DrawableNode child) {
        if (child instanceof DrawableDummy) {
            return child.getLink(null);
        } else {
            return getGraph().getLink(this.getIdentifier(), child.getIdentifier());
        }
    }

    /**
     * Color a {@link DrawableSegment} depending on its properties.
     */
    @Override
    public void colorize() {
        double genomeFraction = this.getGenomeFraction();
        double maxSaturation = 0.8, minSaturation = 0.05;
        double saturation = minSaturation + genomeFraction * (maxSaturation - minSaturation);

        Color fillColor = Color.hsb(227, saturation, 1.d);
        Color strokeColor = Color.hsb(227, maxSaturation, 1.d);

        this.setColors(fillColor, strokeColor);
    }

    public void setStrokeColor(Color strokeColor) {
        this.strokeColor = strokeColor;
    }

    public void setStrokeWidth(double strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public void setColors(Color fillColor, Color strokeColor) {
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
    }

    @Override
    public GenomeGraph getGraph() {
        return this.graph;
    }

    @Override
    public void setDrawDimensionsUpToDate(boolean upToDate) {
        this.drawDimensionsUpToDate = upToDate;
    }

    @Override
    public boolean isDrawDimensionsUpToDate() {
        return this.drawDimensionsUpToDate;
    }

    @Override
    public double getWidth() {
        return this.width;
    }

    public double getHeight() {
        return this.height;
    }

    /**
     * getter for the center of the left border.
     * @return XYCoordinate.
     */
    public XYCoordinate getLeftBorderCenter() {
        if (!drawDimensionsUpToDate) {
            setDrawDimensions();
        }
        return this.getCenter().add(-(this.getWidth() / 2), 0);
    }

    /**
     * getter for the center.
     * @return XYCoordinate.
     */
    public XYCoordinate getCenter() {
        if (!drawDimensionsUpToDate) {
            setDrawDimensions();
        }
        return this.getLocation().add(this.getWidth() * 0.5, this.getHeight() * 0.5);
    }

    /**
     * getter for the center of the right border.
     * @return XYCoordinate.
     */
    public XYCoordinate getRightBorderCenter() {
        if (!drawDimensionsUpToDate) {
            setDrawDimensions();
        }
        return this.getCenter().add(this.getWidth() / 2, 0);
    }

    @Override
    public Color getStrokeColor() {
        return null;
    }

    @Override
    public int getIdentifier() {
        return this.id;
    }
}
