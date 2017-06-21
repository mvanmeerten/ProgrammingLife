package programminglife.model.drawing;

import javafx.scene.shape.Rectangle;
import programminglife.model.GenomeGraph;
import programminglife.model.XYCoordinate;

import java.util.Collection;
import java.util.LinkedHashSet;

/**
 * A segment that also implements {@link Drawable}.
 */
public abstract class DrawableNode extends Rectangle implements Drawable {
    static final int NODE_HEIGHT = 10;

    private final GenomeGraph graph;
    private final int id;
    private final Collection<Integer> genomes;

    private boolean drawDimensionsUpToDate = false;

    /**
     * Construct a {@link DrawableNode}.
     * @param graph the {@link GenomeGraph} to draw from
     * @param id the ID of the {@link DrawableNode}
     */
    DrawableNode(GenomeGraph graph, int id) {
        this.graph = graph;
        this.id = id;
        this.genomes = new LinkedHashSet<>();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!this.getClass().equals(o.getClass())) {
            return false;
        }
        return this.getIdentifier() == ((DrawableNode) o).getIdentifier();
    }

    @Override
    public final int hashCode() {
        int result = getGraph().hashCode();
        result = 31 * result + getIdentifier();
        return result;
    }

    /**
     * Get the ID.
     * @return the ID
     */
    public final int getIdentifier() {
        return this.id;
    }

    /**
     * Get the {@link GenomeGraph}.
     * @return the graph
     */
    public final GenomeGraph getGraph() {
        return graph;
    }

    /**
     * Set if the dimensions are up to date.
     * @param upToDate {@link boolean} true if up to date else false
     */
    final void setDrawDimensionsUpToDate(boolean upToDate) {
        this.drawDimensionsUpToDate = upToDate;
    }

    /**
     * Get if the dimensions are up to date.
     * @return boolean true if up to date else false
     */
    final boolean isDrawDimensionsUpToDate() {
        return drawDimensionsUpToDate;
    }

    /**
     * Get the IDs of children of this.
     * @return IDs of drawable children
     */
    public abstract Collection<Integer> getChildren();

    /**
     * Get the IDs of parents of this.
     * @return IDs of drawable parents.
     */
    public abstract Collection<Integer> getParents();

    /**
     * Replace a parent with another one.
     * @param oldParent the parent to replace
     * @param newParent the new parent
     */
    abstract void replaceParent(DrawableNode oldParent, DrawableNode newParent);

    /**
     * Replace child with another one.
     * @param oldChild the child to replace
     * @param newChild the new child
     */
    abstract void replaceChild(DrawableNode oldChild, DrawableNode newChild);

    /**
     * Information {@link String} about this.
     * @return info
     */
    public abstract String details();

    /**
     * Checks if the children of this {@link DrawableNode} can be merged as a SNP.
     * @param subGraph the {@link SubGraph} this {@link DrawableNode} is in
     * @return null if children cannot be SNP'ed, SNP with (parent, child and mutation) otherwise
     */
    public abstract DrawableSNP createSNPIfPossible(SubGraph subGraph);

    /**
     * Color this according to contents.
     * @param sg the {@link SubGraph} this {@link DrawableNode} is in
     */
    public abstract void colorize(SubGraph sg);

    /**
     * Set the location to draw this.
     * @param x the x location
     * @param y the y location
     */
    protected void setLocation(int x, int y) {
        this.setX(x);
        this.setY(y);
    }

    /**
     * Set the size of this drawing.
     */
    protected abstract void setDrawDimensions();

    /**
     * Get a {@link XYCoordinate} representing the size of the Segment.
     * @return The size of the Segment
     */
    public XYCoordinate getSize() {
        if (!drawDimensionsUpToDate) {
            setDrawDimensions();
        }
        return new XYCoordinate((int) this.getWidth(), (int) this.getHeight());
    }

    /**
     * getter for the center of the left border.
     * @return XYCoordinate.
     */
    public XYCoordinate getLeftBorderCenter() {
        if (!drawDimensionsUpToDate) {
            setDrawDimensions();
        }
        return this.getCenter().add(-(this.getSize().getX() / 2), 0);
    }

    /**
     * getter for the center.
     * @return XYCoordinate.
     */
    private XYCoordinate getCenter() {
        if (!drawDimensionsUpToDate) {
            setDrawDimensions();
        }
        return this.getLocation().add(this.getSize().multiply(0.5));
    }

    /**
     * getter for the center of the right border.
     * @return XYCoordinate.
     */
    public XYCoordinate getRightBorderCenter() {
        if (!drawDimensionsUpToDate) {
            setDrawDimensions();
        }
        return this.getCenter().add(this.getSize().getX() / 2, 0);
    }

    /**
     * Getter for top left corner of a Segment.
     * @return {@link XYCoordinate} with the values of the top left corner.
     */
    XYCoordinate getLocation() {
        return new XYCoordinate((int) this.getX(), (int) this.getY());
    }

    public Collection<Integer> getGenomes() {
        return this.genomes;
    }

    /**
     * Add genomes to the collection.
     * @param genomes a {@link Collection} of genome IDs
     */
    public final void addGenomes(Collection<Integer> genomes) {
        this.genomes.addAll(genomes);
    }

    /**
     * Return this {@link DrawableNode} if it is a {@link DrawableSegment}, else return its parent.
     * @return the 'closest' parent {@link DrawableSegment}
     */
    public abstract DrawableNode getParentSegment();

    /**
     * Return this {@link DrawableNode} if it is a {@link DrawableSegment}, else return its child.
     * @return the 'closest' child {@link DrawableSegment}
     */
    public abstract DrawableNode getChildSegment();
}
