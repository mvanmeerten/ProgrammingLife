package programminglife.gui.controller;

import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import programminglife.gui.ResizableCanvas;
import programminglife.model.GenomeGraph;
import programminglife.model.XYCoordinate;
import programminglife.model.drawing.*;
import programminglife.utility.Console;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Controller for drawing the graph.
 */
class GraphController {

    private GenomeGraph graph;
    private double locationCenterY;
    private double locationCenterX;
    private LinkedList<DrawableNode> oldMinMaxList = new LinkedList<>();
    private SubGraph subGraph;
    private LinkedList<DrawableNode> oldGenomeList = new LinkedList<>();
    private final ResizableCanvas canvas;


    private int centerNodeInt;
    private boolean drawSNP = false;
    private DrawableSegment highlightSegmentShift;
    private DrawableSegment highlightSegment;

    /**
     * Initialize controller object.
     *
     * @param canvas the {@link Canvas} to draw in
     */
    GraphController(ResizableCanvas canvas) {
        this.graph = null;
        this.canvas = canvas;
    }

    public int getCenterNodeInt() {
        return this.centerNodeInt;
    }

    /**
     * Utility function for benchmarking purposes.
     *
     * @param description the description to print
     * @param r           the {@link Runnable} to run/benchmark
     */
    private void time(String description, Runnable r) {
        long start = System.nanoTime();
        r.run();
        Console.println(String.format("%s: %d ms", description, (System.nanoTime() - start) / 1000000));
    }

    /**
     * Method to draw the subGraph decided by a center node and radius.
     *
     * @param center the node of which the radius starts.
     * @param radius the amount of layers to be drawn.
     */
    public void draw(int center, int radius) {
        time("Total drawing", () -> {
            DrawableSegment centerNode = new DrawableSegment(graph, center, 1);
            centerNodeInt = centerNode.getIdentifier();
            GraphicsContext gc = canvas.getGraphicsContext2D();

            time("Find subgraph", () -> subGraph = new SubGraph(centerNode, radius, drawSNP));

            time("Colorize", this::colorize);

            time("Drawing", () -> draw(gc));

            centerOnNodeId(center);
            highlightNode(center, Color.DARKORANGE);
        });
    }

    /**
     * Method to do the coloring of the to be drawn graph.
     */
    private void colorize() {
        subGraph.colorize();
    }

    /**
     * Fill the rectangles with the color.
     *
     * @param nodes the Collection of {@link Integer Integers} to highlight.
     * @param color the {@link Color} to highlight with.
     */
    private void highlightNodesByID(Collection<Integer> nodes, Color color) {
        for (int i : nodes) {
            highlightNode(i, color);
        }
    }

    /**
     * Method to highlight a collection of nodes.
     *
     * @param nodes The nodes to highlight.
     * @param color The color to highlight with.
     */
    private void highlightNodes(Collection<DrawableNode> nodes, Color color) {
        for (DrawableNode drawNode : nodes) {
            highlightNode(drawNode, color);
        }
    }

    /**
     * Fill the rectangle with the color.
     *
     * @param nodeID the nodeID of the node to highlight.
     * @param color  the {@link Color} to highlight with.
     */
    private void highlightNode(int nodeID, Color color) {
        DrawableNode node = subGraph.getNodes().get(nodeID);
        highlightNode(node, color);
    }

    /**
     * Highlights a single node.
     *
     * @param node  {@link DrawableNode} to highlight.
     * @param color {@link Color} to color with.
     */
    private void highlightNode(DrawableNode node, Color color) {
        node.setStrokeColor(color);
        node.setStrokeWidth(5.0 * subGraph.getZoomLevel());
        drawNode(canvas.getGraphicsContext2D(), node);
    }

    /**
     * Method to highlight a Link. Changes the stroke color of the Link.
     *
     * @param edge  {@link DrawableEdge} is the edge to highlight.
     * @param color {@link Color} is the color in which the Link node needs to highlight.
     */
    private void highlightEdge(DrawableEdge edge, Color color) {
        edge.setStrokeColor(color);
    }

    /**
     * Method to highlight a dummy node. Changes the stroke color of the node.
     *
     * @param node  {@link DrawableDummy} is the dummy node that needs highlighting.
     * @param color {@link Color} is the color in which the dummy node needs a highlight.
     */
    private void highlightDummyNode(DrawableDummy node, Color color) {
        node.setStrokeColor(color);
    }

    /**
     * Draws a edge on the location it has.
     *
     * @param gc     {@link GraphicsContext} is the GraphicsContext required to draw.
     * @param parent {@link DrawableNode} is the node to be draw from.
     * @param child  {@link DrawableNode} is the node to draw to.
     */
    private void drawEdge(GraphicsContext gc, DrawableNode parent, DrawableNode child) {
        DrawableEdge edge = new DrawableEdge(parent, child);

        edge.colorize(subGraph);

        gc.setLineWidth(edge.getStrokeWidth());
        gc.setStroke(edge.getStrokeColor());

        XYCoordinate startLocation = edge.getStartLocation();
        XYCoordinate endLayerLocation = new XYCoordinate(
                parent.getLeftBorderCenter().getX() + parent.getLayer().getWidth(),
                parent.getLeftBorderCenter().getY());
        XYCoordinate endLocation = edge.getEndLocation();

        gc.strokeLine(startLocation.getX(), startLocation.getY(), endLayerLocation.getX(), endLayerLocation.getY());
        gc.strokeLine(endLayerLocation.getX(), endLayerLocation.getY(), endLocation.getX(), endLocation.getY());
    }

    /**
     * Draws a node on the location it has.
     *
     * @param gc           {@link GraphicsContext} is the GraphicsContext required to draw.
     * @param drawableNode {@link DrawableNode} is the node to be drawn.
     */
    private void drawNode(GraphicsContext gc, DrawableNode drawableNode) {
        gc.setStroke(drawableNode.getStrokeColor());
        gc.setFill(drawableNode.getFillColor());
        gc.setLineWidth(drawableNode.getStrokeWidth());

        double width = drawableNode.getWidth();
        double height = drawableNode.getHeight();
        double locX = drawableNode.getLocation().getX();
        double locY = drawableNode.getLocation().getY();

        if (drawableNode instanceof DrawableSNP) {
            int size = ((DrawableSNP) drawableNode).getMutations().size();
            drawSNP(gc, (DrawableSNP) drawableNode, size);

        } else if (drawableNode instanceof DrawableDummy) {
            gc.strokeRect(drawableNode.getLeftBorderCenter().getX(), locY, width, height);
            gc.fillRect(drawableNode.getLeftBorderCenter().getX(), locY, width, height);

        } else {
            gc.strokeRoundRect(drawableNode.getLeftBorderCenter().getX(), locY, width, height, 5, 5);
            gc.fillRoundRect(drawableNode.getLeftBorderCenter().getX(), locY, width, height, 5, 5);
        }

    }

    private void drawSNP(GraphicsContext gc, DrawableSNP drawableNode, int size) {
        double width = drawableNode.getWidth();
        double height = drawableNode.getHeight();
        double locX = drawableNode.getLocation().getX();
        double locY = drawableNode.getLocation().getY();

        gc.save();

        gc.setStroke(Color.BLACK);
        gc.translate(drawableNode.getCenter().getX(), drawableNode.getCenter().getY());
        gc.rotate(45);
        gc.translate(-drawableNode.getCenter().getX(), -drawableNode.getCenter().getY());

        int seqNumber = 0;
        gc.strokeRoundRect(locX, locY, width, height, 5, 5);

        for (DrawableSegment drawableSegment : drawableNode.getMutations()) {
            String seqChar = drawableSegment.getSequence();
            switch (seqChar) {
                case "A":
                    gc.setFill(Color.GREEN);
                    break;
                case "C":
                    gc.setFill(Color.BLUE);
                    break;
                case "G":
                    gc.setFill(Color.ORANGE);
                    break;
                case "T":
                    gc.setFill(Color.RED);
                    break;
            }
            if (seqNumber < size) {
                seqNumber++;
                gc.fillRect(locX - width / size + (width / size) * seqNumber, locY, width / size, height);
            }
        }
        gc.restore();
    }

    /**
     * Getter for the graph.
     *
     * @return - The graph
     */
    public GenomeGraph getGraph() {
        return graph;
    }

    /**
     * Setter for the graph.
     *
     * @param graph The graph
     */
    void setGraph(GenomeGraph graph) {
        this.graph = graph;
    }

    /**
     * Clear the draw area.
     */
    public void clear() {
        this.canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    public double getLocationCenterX() {
        return locationCenterX;
    }


    public double getLocationCenterY() {
        return locationCenterY;
    }

    /**
     * Centers on the given node.
     *
     * @param nodeId is the node to center on.
     */
    private void centerOnNodeId(int nodeId) {
        DrawableNode drawableCenterNode = subGraph.getNodes().get(nodeId);
        double xCoordinate = drawableCenterNode.getCenter().getX();

        Bounds bounds = canvas.getParent().getLayoutBounds();
        double boundsHeight = bounds.getHeight();
        double boundsWidth = bounds.getWidth();

        locationCenterY = boundsHeight / 4;
        locationCenterX = boundsWidth / 2 - xCoordinate;

        translate(locationCenterX, locationCenterY);
    }

    /**
     * Translate function for the nodes. Used to change the location of nodes instead of mocing the canvas.
     *
     * @param xDifference double with the value of the change in the X (horizontal) direction.
     * @param yDifference double with the value of the change in the Y (vertical) direction.
     */
    public void translate(double xDifference, double yDifference) {
        subGraph.translate(xDifference, yDifference);
        draw(canvas.getGraphicsContext2D());
    }

    /**
     * Zoom function for the nodes. Used to increase the size of the nodes instead of zooming in on the canvas itself.
     *
     * @param scale double with the value of the increase of the nodes. Value higher than 1 means that the node size
     *              decreases, value below 1 means that the node size increases.
     */
    public void zoom(double scale) {
        subGraph.zoom(scale);
        draw(canvas.getGraphicsContext2D());
    }

    /**
     * Draw method for the whole subgraph. Calls draw node and draw Edge for every node and edge.
     *
     * @param gc is the {@link GraphicsContext} required to draw.
     */
    private void draw(GraphicsContext gc) {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        subGraph.checkDynamicLoad(0, canvas.getWidth());

        for (DrawableNode drawableNode : subGraph.getNodes().values()) {
            for (DrawableNode child : subGraph.getChildren(drawableNode)) {
                drawEdge(gc, drawableNode, child);
            }
        }

        for (DrawableNode drawableNode : subGraph.getNodes().values()) {
            drawNode(gc, drawableNode);
        }
    }

    /**
     * Method to do highlighting based on a min and max amount of genomes in a node.
     *
     * @param min   The minimal amount of genomes
     * @param max   The maximal amount of genomes
     * @param color the {@link Color} in which the highlight has to be done.
     */
    public void highlightMinMax(int min, int max, Color color) {
        LinkedList<DrawableNode> drawNodeList = new LinkedList<>();

        removeHighlight(oldMinMaxList);
        removeHighlight(oldGenomeList);
        for (DrawableNode drawableNode : subGraph.getNodes().values()) {
            if (drawableNode != null && !(drawableNode instanceof DrawableDummy)) {
                int genomeCount = drawableNode.getGenomes().size();
                if (genomeCount >= min && genomeCount <= max) {
                    drawNodeList.add(drawableNode);
                }
            }
        }
        oldMinMaxList = drawNodeList;
        highlightNodes(drawNodeList, color);
    }

    /**
     * Resets the node highlighting to remove highlights.
     *
     * @param nodes are the nodes to remove the highlight from.
     */
    private void removeHighlight(Collection<DrawableNode> nodes) {
        for (DrawableNode node: nodes) {
            node.colorize(subGraph);
        }
        this.draw(canvas.getGraphicsContext2D());
    }


    /**
     * Highlights based on genomeID.
     *
     * @param genomeID the GenomeID to highlight on.
     */
    public void highlightByGenome(int genomeID) {
        LinkedList<DrawableNode> drawNodeList = new LinkedList<>();
        removeHighlight(oldGenomeList);
        removeHighlight(oldMinMaxList);
        for (DrawableNode drawableNode : subGraph.getNodes().values()) {
            Collection<Integer> genomes = drawableNode.getGenomes();
            for (int genome : genomes) {
                if (genome == genomeID && !(drawableNode instanceof DrawableDummy)) {
                    drawNodeList.add(drawableNode);
                }
            }
        }
        oldGenomeList = drawNodeList;
        highlightNodes(drawNodeList, Color.YELLOW);
    }

    /**
     * Sets if the glyph  snippets will be drawn or not.
     */
    void setSNP() {
        drawSNP = !drawSNP;
    }

    /**
     * Returns the node clicked on else returns null.
     *
     * @param x position horizontally where clicked
     * @param y position vertically where clicked
     * @return nodeClicked {@link DrawableNode} returns null if no node is clicked.
     */
    public DrawableNode onClick(double x, double y) {
        DrawableNode nodeClicked = null;
        if (subGraph == null) {
            return null;
        }

        for (DrawableNode drawableNode : subGraph.getNodes().values()) {
            if (x >= drawableNode.getLocation().getX() && y >= drawableNode.getLocation().getY()
                    && x <= drawableNode.getLocation().getX() + drawableNode.getWidth()
                    && y <= drawableNode.getLocation().getY() + drawableNode.getHeight()) {
                nodeClicked = drawableNode;
                break;
            }
        }
        return nodeClicked;
    }

    /**
     * Method to hightlight the node clicked on.
     *
     * @param segment      is the {@link DrawableSegment} clicked on.
     * @param shiftPressed boolean true if shift was pressed during the click.
     */
    public void highlightClicked(DrawableSegment segment, boolean shiftPressed) {
        if (shiftPressed) {
            if (highlightSegmentShift != null) {
                this.highlightSegmentShift.colorize(subGraph);
            }
            this.highlightSegmentShift = segment;
            highlightNode(segment, Color.DARKTURQUOISE);
            segment.setStrokeWidth(5.0 * subGraph.getZoomLevel()); //Correct thickness when zoomed
        } else {
            if (highlightSegment != null) {
                this.highlightSegment.colorize(subGraph);
            }
            this.highlightSegment = segment;
            highlightNode(segment, Color.PURPLE);
            segment.setStrokeWidth(5.0 * subGraph.getZoomLevel()); //Correct thickness when zoomed
        }
        draw(canvas.getGraphicsContext2D());
    }

    /**
     * Method to reset the zoomLevel.
     */
    public void resetZoom() {
        this.subGraph.setZoomLevel(1);
    }
}
