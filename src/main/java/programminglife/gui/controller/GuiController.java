package programminglife.gui.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import jp.uphy.javafx.console.ConsoleView;
import programminglife.ProgrammingLife;
import programminglife.controller.MiniMapController;
import programminglife.controller.RecentFileController;
import programminglife.gui.ResizableCanvas;
import programminglife.model.Feature;
import programminglife.model.GenomeGraph;
import programminglife.model.drawing.*;
import programminglife.parser.AnnotationParser;
import programminglife.parser.GraphParser;
import programminglife.utility.Alerts;
import programminglife.utility.Console;
import programminglife.utility.NumbersOnlyListener;
import programminglife.utility.ProgressCounter;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;

/**
 * The controller for the GUI that is used in the application.
 * The @FXML tag is needed in initialize so that javaFX knows what to do.
 */
public class GuiController implements Observer {
    //static finals
    private static final String INITIAL_CENTER_NODE = "1";
    private static final String INITIAL_MAX_DRAW_DEPTH = "10";

    //FXML imports.
    @FXML private MenuItem btnOpenGFA;
    @FXML private MenuItem btnOpenGFF;
    @FXML private MenuItem btnQuit;
    @FXML private MenuItem btnBookmarks;
    @FXML private MenuItem btnAbout;
    @FXML private MenuItem btnInstructions;
    @FXML private Menu menuRecentGFA;
    @FXML private Menu menuRecentGFF;
    @FXML private RadioMenuItem btnToggle;
    @FXML private RadioMenuItem btnMiniMap;
    @FXML private Button btnZoomReset;
    @FXML private Button btnTranslateReset;
    @FXML private Button btnDraw;
    @FXML private Button btnDrawRandom;
    @FXML private Button btnBookmark;
    @FXML private Button btnClipboard;
    @FXML private Button btnClipboard2;
    @FXML private ProgressBar progressBar;
    @FXML private Tab searchTab;

    @FXML private TextField txtMaxDrawDepth;
    @FXML private TextField txtCenterNode;

    @FXML private ResizableCanvas canvas;
    @FXML private AnchorPane anchorLeftControlPanel;
    @FXML private AnchorPane anchorGraphPanel;
    @FXML private AnchorPane anchorGraphInfo;
    @FXML private javafx.scene.canvas.Canvas miniMap;

    private double orgSceneX, orgSceneY;

    private double scale;
    private GraphController graphController;
    private RecentFileController recentFileControllerGFA;
    private RecentFileController recentFileControllerGFF;
    private MiniMapController miniMapController;
    private File file;
    private File recentFileGFA = new File("RecentGFA.txt");
    private File recentFileGFF = new File("RecentGFF.txt");
    private Map<String, Feature> features;
    private Thread parseThread;

    private final ExtensionFilter extFilterGFF = new ExtensionFilter("GFF files (*.gff)", "*.GFF");
    private final ExtensionFilter extFilterGFA = new ExtensionFilter("GFA files (*.gfa)", "*.GFA");

    private static final double MAX_SCALE = 5.0d;
    private static final double MIN_SCALE = .02d;
    private static final double ZOOM_FACTOR = 1.05d;


    /**
     * The initialize will call the other methods that are run in the .
     */
    @FXML
    @SuppressWarnings("unused")
    private void initialize() {
        this.graphController = new GraphController(null, this.canvas, this.anchorGraphInfo);
        this.scale = 1;

        this.recentFileControllerGFA = new RecentFileController(this.recentFileGFA, this.menuRecentGFA);
        this.recentFileControllerGFA.setGuiController(this);
        this.recentFileControllerGFF = new RecentFileController(this.recentFileGFF, this.menuRecentGFF);
        this.recentFileControllerGFF.setGuiController(this);
        initMenuBar();
        initBookmarkMenu();
        initLeftControlpanelScreenModifiers();
        initLeftControlpanelDraw();
        initMouse();
        initShowInfoTab();
        initConsole();
        initRightSearchTab();
    }

    /**
     * Open and parse a GFA file.
     * @param file The {@link File} to open.
     * @throws IOException if the {@link File} is not found.
     * @return the parser to be notified when it is finished
     */
    public GraphParser openFile(File file) throws IOException {
        if (file != null) {
            if (this.graphController != null && this.graphController.getGraph() != null) {
                this.graphController.getGraph().close();
                this.canvas.getGraphicsContext2D().clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
            }

            disableGraphUIElements(true);

            GraphParser graphParser = new GraphParser(file);
            graphParser.addObserver(this);
            graphParser.getProgressCounter().addObserver(this);

            if (this.parseThread != null) {
                this.parseThread.interrupt();
            }
            this.parseThread = new Thread(graphParser);
            this.parseThread.start();
            this.setFile(file);

            return graphParser;
        }

        return null;
    }

    /**
     * Open and parse a GFF file.
     * @param file The {@link File} to open.
     * @throws IOException if the {@link File} is not found.
     * @return AnnotationParser to be notified when finished.
     */
    private AnnotationParser openAnnotationFile(File file) throws IOException {
        AnnotationParser annotationParser = null;
        if (file != null) {
            Console.println("Opening annotation " + file);
            annotationParser = new AnnotationParser(file);
            annotationParser.addObserver(this);
            annotationParser.getProgressCounter().addObserver(this);

            if (this.parseThread != null) {
                this.parseThread.interrupt();
            }
            this.parseThread = new Thread(annotationParser);
            this.parseThread.start();
        }
        return annotationParser;
    }

    @Override
    public void update(Observable o, Object arg) {
        if (o instanceof GraphParser) {
            if (arg instanceof GenomeGraph) {
                GenomeGraph graph = (GenomeGraph) arg;

                Console.println("[%s] File Parsed.", Thread.currentThread().getName());

                this.setGraph(graph);
            } else if (arg instanceof Exception) {
                Exception e = (Exception) arg;
                e.printStackTrace();
                Alerts.error(e.getMessage());
            } else if (arg instanceof String) {
                String msg = (String) arg;
                Platform.runLater(() -> ProgrammingLife.getStage().setTitle(msg));
            }
        } else if (o instanceof AnnotationParser) {
            if (arg instanceof Map) {
                Console.println("[%s] Annotations parsed.", Thread.currentThread().getName());
                this.setFeatures(((AnnotationParser) o).getFeatures());
            }
        } else if (o instanceof ProgressCounter) {
            progressBar.setVisible(true);
            ProgressCounter progress = (ProgressCounter) o;
            this.getProgressBar().setProgress(progress.percentage());
            if (progressBar.getProgress() == 1.0d) {
                progressBar.setVisible(false);
            }
        }
    }

    /**
     * Set the graph for this GUIController.
     * @param graph {@link GenomeGraph} to use.
     */
    public void setGraph(GenomeGraph graph) {
        this.graphController.setGraph(graph);
        disableGraphUIElements(graph == null);
        searchTab.setDisable(graph == null);
        Platform.runLater(() -> {
            assert graph != null;
            ProgrammingLife.getStage().setTitle(graph.getID());
        });

        if (graph != null) {
            this.miniMapController = new MiniMapController(this.miniMap, graph.size());
            miniMap.setWidth(anchorGraphPanel.getWidth());
            miniMap.setHeight(50.d);
            Console.println("[%s] Graph was set to %s.", Thread.currentThread().getName(), graph.getID());
            Console.println("[%s] The graph has %d nodes", Thread.currentThread().getName(), graph.size());
        }
    }

    /**
     * Handles the fileChooser when open a file.
     * @param filter ExtensionFilter of which file type to open.
     * @param isGFA boolean to check if it is a GFA file.
     */
    private void fileChooser(ExtensionFilter filter, boolean isGFA) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(filter);
        if (file != null) {
            File existDirectory = file.getParentFile();
            fileChooser.setInitialDirectory(existDirectory);
        }
        try {
            file = fileChooser.showOpenDialog(ProgrammingLife.getStage());
            if (file != null) {
                if (isGFA) {
                    this.openFile(file);
                    Platform.runLater(() -> recentFileControllerGFA.updateRecent(recentFileGFA, file));
                } else {
                    this.openAnnotationFile(file);
                    Platform.runLater(() -> recentFileControllerGFF.updateRecent(recentFileGFF, file));
                }
            }
        } catch (FileNotFoundException e) {
            Alerts.error("This GFA file can't be found");
        } catch (IOException e) {
            Alerts.error("This GFA file can't be opened");
        }
    }

    /**
     * Initializes the open button so that the user can decide which file to open.
     * Sets the action for the open MenuItem.
     * Sets the event for the quit MenuItem.
     */
    private void initMenuBar() {
        btnOpenGFA.setOnAction((ActionEvent event) -> fileChooser(extFilterGFA, true));
        btnOpenGFF.setOnAction((ActionEvent event) -> fileChooser(extFilterGFF, false));

        btnOpenGFA.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCodeCombination.CONTROL_DOWN));

        btnMiniMap.setOnAction(event -> miniMapController.toggleVisibility());
        btnMiniMap.setAccelerator(new KeyCodeCombination(KeyCode.M, KeyCodeCombination.CONTROL_DOWN));
        btnQuit.setOnAction(event -> Alerts.quitAlert());
        btnQuit.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCodeCombination.CONTROL_DOWN));
        btnAbout.setOnAction(event -> Alerts.infoAboutAlert());
        btnAbout.setAccelerator(new KeyCodeCombination(KeyCode.I, KeyCodeCombination.CONTROL_DOWN));
        btnInstructions.setOnAction(event -> Alerts.infoInstructionAlert());
        btnInstructions.setAccelerator(new KeyCodeCombination(KeyCode.H, KeyCodeCombination.CONTROL_DOWN));
    }


    /**
     * Initializes the bookmark buttons in the menu.
     */
    private void initBookmarkMenu() {

        btnBookmarks.setOnAction(event -> {
            try {
                FXMLLoader loader = new FXMLLoader(ProgrammingLife.class.getResource("/LoadBookmarkWindow.fxml"));
                AnchorPane page = loader.load();
                GuiLoadBookmarkController gc = loader.getController();
                gc.setGuiController(this);
                gc.initBookmarks();
                if (this.graphController.getGraph() != null) {
                    gc.setBtnCreateBookmarkActive(true);
                }
                Scene scene = new Scene(page);
                Stage bookmarkDialogStage = new Stage();
                bookmarkDialogStage.setResizable(false);
                bookmarkDialogStage.setScene(scene);
                bookmarkDialogStage.setTitle("Load Bookmark");
                bookmarkDialogStage.initOwner(ProgrammingLife.getStage());
                bookmarkDialogStage.showAndWait();
            } catch (IOException e) {
                Alerts.error("The bookmarks file can't be opened");
            }
        });
        btnBookmarks.setAccelerator(new KeyCodeCombination(KeyCode.B, KeyCodeCombination.CONTROL_DOWN));
    }

    /**
     * Method to disable the UI Elements on the left of the GUI.
     * @param isDisabled boolean, true disables the left anchor panel.
     */
    private void disableGraphUIElements(boolean isDisabled) {
        anchorLeftControlPanel.setDisable(isDisabled);
    }

    /**
     * Initializes the buttons on the panel on the left side which do translation/zoom.
     */
    private void initLeftControlpanelScreenModifiers() {
        disableGraphUIElements(true);

        btnTranslateReset.setOnAction(event -> this.draw());


        btnZoomReset.setOnAction(event -> this.draw());
    }

    /**
     * Method to reset the zoom levels.
     */
    private void resetZoom() {
            graphController.setZoomLevel(1);
            scale = 1;
            canvas.setScaleX(1);
            canvas.setScaleY(1);
    }

    /**
     * Initializes the button on the left side that are used to draw the graph.
     */
    private void initLeftControlpanelDraw() {
        disableGraphUIElements(true);

        btnDraw.setOnAction(e -> this.draw());

        btnDrawRandom.setOnAction(event -> {
            int randomNodeID = (new Random()).nextInt(this.graphController.getGraph().size() - 1) + 1;
            txtCenterNode.setText(Integer.toString(randomNodeID));
            this.draw();
        });

        btnBookmark.setOnAction(event -> buttonBookmark());

        txtMaxDrawDepth.textProperty().addListener(new NumbersOnlyListener(txtMaxDrawDepth));
        txtMaxDrawDepth.setText(INITIAL_MAX_DRAW_DEPTH);

        txtCenterNode.textProperty().addListener(new NumbersOnlyListener(txtCenterNode));
        txtCenterNode.setText(INITIAL_CENTER_NODE);
    }

    /**
     * Draw the current graph with current center node and depth settings.
     */
    void draw() {
        Console.println("[%s] Drawing graph...", Thread.currentThread().getName());
        int centerNode = 0;
        int maxDepth = 0;
        try {
            centerNode = Integer.parseInt(txtCenterNode.getText());
            try {
                maxDepth = Integer.parseInt(txtMaxDrawDepth.getText());
            } catch (NumberFormatException e) {
                Alerts.warning("Radius is not a number, try again with a number as input.");
            }
        } catch (NumberFormatException e) {
            Alerts.warning("Center node ID is not a number, try again with a number as input.");
        }

        resetZoom();

        if (graphController.getGraph().contains(centerNode)) {
            this.graphController.clear();
            this.graphController.draw(centerNode, maxDepth);
            this.miniMapController.showPosition(centerNode);
            Console.println("[%s] Graph drawn.", Thread.currentThread().getName());
        } else {
            Alerts.warning("The centernode is not a existing node, try again with a number that exists as a node.");
        }
    }

    /**
     * Handles the events of the bookmark button.
     */
    private void buttonBookmark() {
        try {
            FXMLLoader loader = new FXMLLoader(ProgrammingLife.class.getResource("/CreateBookmarkWindow.fxml"));
            AnchorPane page = loader.load();
            GuiCreateBookmarkController gc = loader.getController();
            gc.setGuiController(this);
            gc.setText(txtCenterNode.getText(), txtMaxDrawDepth.getText());
            Scene scene = new Scene(page);
            Stage bookmarkDialogStage = new Stage();
            bookmarkDialogStage.setResizable(false);
            bookmarkDialogStage.setScene(scene);
            bookmarkDialogStage.setTitle("Create Bookmark");
            bookmarkDialogStage.initOwner(ProgrammingLife.getStage());
            bookmarkDialogStage.showAndWait();
        } catch (IOException e) {
            Alerts.error("This bookmark cannot be created.");
        }
    }

    /**
     * Initialises the mouse events.
     */
    private void initMouse() {
        anchorGraphPanel.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            orgSceneX = event.getSceneX();
            orgSceneY = event.getSceneY();
        });
        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            if (event.isShiftDown()) {
                mouseClick(event.getX(), event.getY(), true);
            } else {
                mouseClick(event.getX(), event.getY(), false);

            }
        });
        anchorGraphPanel.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            double xDifference = event.getSceneX() - orgSceneX;
            double yDifference = event.getSceneY() - orgSceneY;
            orgSceneX += xDifference;
            orgSceneY += yDifference;
            graphController.translate(xDifference, yDifference);
            event.consume();
        });
        anchorGraphPanel.addEventHandler(ScrollEvent.SCROLL, event ->
                zoom(event.getDeltaX(), event.getDeltaY(), event.getSceneX(), event.getSceneY(), ZOOM_FACTOR));
    }

    /**
     * Mouse click method that does the show info handling.
     * @param x coordinate where is clicked.
     * @param y coordinate where is clicked.
     * @param shiftPressed boolean if shift is pressed it should be displayed in panel 2.
     */
    private void mouseClick(double x, double y, boolean shiftPressed) {
        DrawableNode clickedOn = graphController.onClick(x, y);
        if (clickedOn != null) {
            if (clickedOn instanceof DrawableSegment) {
                DrawableSegment segment = (DrawableSegment) clickedOn;
                if (shiftPressed) {
                    showInfoNode(segment, 240);
                } else {
                    showInfoNode(segment, 10);
                }
            }

        }
    }

    /**
     * Handles the zooming in and out of the group.
     * @param deltaX The scroll amount in the X direction. See {@link ScrollEvent#getDeltaX()}
     * @param deltaY The scroll amount in the Y direction. See {@link ScrollEvent#getDeltaY()}
     * @param sceneX double for the x location.
     * @param sceneY double for the y location.
     * @param delta double the factor by which is zoomed.
     */
    private void zoom(double deltaX, double deltaY, double sceneX, double sceneY, double delta) {
        double oldScale = scale;

        if (deltaX < 0 || deltaY < 0) {
            scale *= Math.pow(1.03, delta);
        } else {
            scale /= Math.pow(1.03, delta);
        }

        scale = clamp(scale, MIN_SCALE, MAX_SCALE);
        double factor = (scale / oldScale) - 1;

        graphController.zoom(factor + 1);
        //factor to determine the difference in the scales.

        Bounds bounds = canvas.localToScene(canvas.getBoundsInLocal());
        double dx = (sceneX - bounds.getMinX());
        double dy = (sceneY - bounds.getMinY());

        graphController.translate(factor * dx, factor * dy);
    }

    /**
     * Clamp function used for zooming in and out.
     * @param value double current scale.
     * @param min double min scale value.
     * @param max double max scale value.
     * @return double scale value.
     */
    private static double clamp(double value, double min, double max) {
        if (Double.compare(value, min) < 0) {
            return min;
        }
        if (Double.compare(value, max) > 0) {
            return max;
        }
        return value;
    }

    /**
     * Initialises the Console.
     */
    private void initConsole() {
        final ConsoleView console = new ConsoleView(Charset.forName("UTF-8"));
        AnchorPane root = new AnchorPane(console);
        Stage st = new Stage();
        st.setScene(new Scene(root, 500, 500, Color.GRAY));
        st.setMinWidth(500);
        st.setMinHeight(250);

        AnchorPane.setBottomAnchor(console, 0.d);
        AnchorPane.setTopAnchor(console, 0.d);
        AnchorPane.setRightAnchor(console, 0.d);
        AnchorPane.setLeftAnchor(console, 0.d);

        btnToggle.setOnAction(event -> {
            if (btnToggle.isSelected()) {
                st.show();
            } else {
                st.close();
            }
        });

        st.show();
        btnToggle.setSelected(true);
        root.visibleProperty().bind(btnToggle.selectedProperty());

        Console.setOut(console.getOut());
    }

    /**
     * Initializes the search tab in the left panel.
     * Button to be disabled without a graph loaded.
     */
    private void initRightSearchTab() {
        try {
            FXMLLoader loader = new FXMLLoader(ProgrammingLife.class.getResource("/HighlightWindow.fxml"));
            AnchorPane page = loader.load();
            final HighlightController highlightController = loader.getController();
            highlightController.setGraphController(this.getGraphController());
            highlightController.setGUIController(this);
            searchTab.setContent(page);
            searchTab.setDisable(true);
            searchTab.setOnSelectionChanged(event -> {
                highlightController.initGenome();
                highlightController.initAnnotations();
                highlightController.initMinMax();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private ProgressBar getProgressBar() {
        return this.progressBar;
    }

    /**
     * Initializes the info tab.
     */
    private void initShowInfoTab() {
        btnClipboard.setOnAction(event -> copyToClipboard(10));
        btnClipboard2.setOnAction(event -> copyToClipboard(250));
    }

    /**
     * Copies information to the clipboard.
     * @param x int used in the ID, to know which sequence to get.
     */
    private void copyToClipboard(int x) {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        String toClipboard = "";
        for (Node node : anchorGraphInfo.getChildren()) {
            if (node instanceof TextArea && node.getId().equals(x + " Sequence: ")) {
                toClipboard = toClipboard.concat(((TextArea) node).getText()) + System.getProperty("line.separator");
                toClipboard = toClipboard.replaceAll("(.{100})", "$1" + System.getProperty("line.separator"));
            }
        }
        StringSelection selection = new StringSelection(toClipboard);
        clipboard.setContents(selection, selection);
    }

    /**
     * Sets the text field for drawing the graph.
     * @param center The center node
     * @param radius The radius of the subGraph
     */
    void setText(int center, int radius) {
        txtCenterNode.setText(String.valueOf(center));
        txtMaxDrawDepth.setText(String.valueOf(radius));
    }

     /**
     * Method to show the information of an edge.
     * @param edge DrawableEdge the edge which has been clicked on.
     * @param x int the x location of the TextField.
     */
    private void showInfoEdge(DrawableEdge edge, int x) {
        anchorGraphInfo.getChildren().removeIf(node1 -> node1.getLayoutX() == x);

        Text idText = new Text("Genomes: "); idText.setLayoutX(x); idText.setLayoutY(65);
        Text parentsText = new Text("Parent: "); parentsText.setLayoutX(x); parentsText.setLayoutY(115);
        Text childrenText = new Text("Child: "); childrenText.setLayoutX(x); childrenText.setLayoutY(165);

        TextField id = getTextField("Genomes: ", x, 70,
                graphController.getGraph().getGenomeNames(edge.getGenomes()).toString());
        TextField parent = getTextField("Parent Node: ", x, 120, Integer.toString(edge.getStart().getIdentifier()));
        TextField child = getTextField("Child Node: ", x, 170, Integer.toString(edge.getEnd().getIdentifier()));

        anchorGraphInfo.getChildren().addAll(idText, parentsText, childrenText, id, parent, child);
    }


    /**
     * Method to show the information of a node.
     * @param node DrawableSegment the node which has been clicked on.
     * @param x int the x location of the TextField.
     */
    private void showInfoNode(DrawableSegment node, int x) {
        Text idText = new Text("ID: "); idText.setLayoutX(x); idText.setLayoutY(65);
        Text parentText = new Text("Parents: "); parentText.setLayoutX(x); parentText.setLayoutY(105);
        Text childText = new Text("Children: "); childText.setLayoutX(x); childText.setLayoutY(145);
        Text inEdgeText = new Text("Incoming Edges: "); inEdgeText.setLayoutX(x); inEdgeText.setLayoutY(185);
        Text outEdgeText = new Text("Outgoing Edges: "); outEdgeText.setLayoutX(x); outEdgeText.setLayoutY(225);
        Text seqLengthText = new Text("Sequence Length: "); seqLengthText.setLayoutX(x); seqLengthText.setLayoutY(265);
        Text genomeText = new Text("Genomes: "); genomeText.setLayoutX(x); genomeText.setLayoutY(305);
        Text seqText = new Text("Sequence: "); seqText.setLayoutX(x); seqText.setLayoutY(370);

        anchorGraphInfo.getChildren().removeIf(node1 -> node1.getLayoutX() == x);

        TextField idTextField = getTextField("ID: ", x, 70, Integer.toString(node.getIdentifier()));

        StringBuilder parentSB = new StringBuilder();
        node.getParents().forEach(id -> parentSB.append(id).append(", "));
        TextField parents;
        if (parentSB.length() > 2) {
            parentSB.setLength(parentSB.length() - 2);
            parents = getTextField("Parents: ", x, 110, parentSB.toString());
        } else {
            parentSB.replace(0, parentSB.length(), "This node has no parent(s)");
            parents = getTextField("Parents: ", x, 110, parentSB.toString());
        }

        StringBuilder childSB = new StringBuilder();
        node.getChildren().forEach(id -> childSB.append(id).append(", "));
        TextField children;
        if (childSB.length() > 2) {
            childSB.setLength(childSB.length() - 2);
            children = getTextField("Children: ", x, 150, childSB.toString());
        } else {
            childSB.replace(0, childSB.length(), "This node has no child(ren)");
            children = getTextField("Children: ", x, 150, childSB.toString());
        }

        String genomesString = graphController.getGraph().getGenomeNames(node.getGenomes()).toString();
        String sequenceString = node.getSequence().replaceAll("(.{24})", "$1" + System.getProperty("line.separator"));
        TextField inEdges = getTextField("Incoming Edges: ", x, 190, Integer.toString(node.getParents().size()));
        TextField outEdges = getTextField("Outgoing Edges: ", x, 230, Integer.toString(node.getChildren().size()));
        TextField seqLength = getTextField("Sequence Length: ", x, 270, Integer.toString(node.getSequence().length()));
        TextArea genome = getTextArea("Genome: ", x, 310, genomesString.substring(1, genomesString.length() - 1), 40);
        genome.setWrapText(true);
        TextArea seq = getTextArea("Sequence: ", x, 375, sequenceString, 250);
        anchorGraphInfo.getChildren().addAll(idText, parentText, childText, inEdgeText,
                outEdgeText, genomeText, seqLengthText, seqText);
        anchorGraphInfo.getChildren().addAll(idTextField, parents, children, inEdges, outEdges, genome, seqLength, seq);
    }


    /**
     * Returns a textField to be used by the edge and node information show panel.
     * @param id String the id of the textField.
     * @param x int the x coordinate of the textField inside the anchorPane.
     * @param y int the y coordinate of the textField inside the anchorPane.
     * @param text String the text to be shown by the textField.
     * @return TextField the created textField.
     */
    private TextField getTextField(String id, int x, int y, String text) {
        TextField textField = new TextField();
        textField.setId(id);
        textField.setText(text);
        textField.setLayoutX(x);
        textField.setLayoutY(y);
        textField.setEditable(false);
        textField.setStyle("-fx-text-box-border: transparent;-fx-background-color: none; -fx-background-insets: 0;"
                + " -fx-padding: 1 3 1 3; -fx-focus-color: transparent; "
                + "-fx-faint-focus-color: transparent; -fx-font-family: monospace;");
        textField.setPrefSize(220, 20);

        return textField;
    }

    /**
     * Returns a textField to be used by the edge and node information show panel.
     * @param id String the id of the textField.
     * @param x int the x coordinate of the textField inside the anchorPane.
     * @param y int the y coordinate of the textField inside the anchorPane.
     * @param text String the text to be shown by the textField.
     * @param height int of the height of the area.
     * @return TextField the created textField.
     */
    private TextArea getTextArea(String id, int x, int y, String text, int height) {
        TextArea textArea = new TextArea();
        textArea.setId(id);
        textArea.setText(text);
        textArea.setLayoutX(x);
        textArea.setLayoutY(y);
        textArea.setEditable(false);
        textArea.setStyle("-fx-text-box-border: transparent;-fx-background-color: none; -fx-background-insets: 0;"
                + " -fx-padding: 1 3 1 3; -fx-focus-color: transparent; "
                + "-fx-faint-focus-color: transparent; -fx-font-family: monospace;");
        textArea.setPrefSize(225, height);

        return textArea;
    }

    public File getFile() {
        return this.file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    private void setFeatures(Map<String, Feature> features) {
        this.features = features;
    }

    Map<String, Feature> getFeatures() {
        return this.features;
    }

    GraphController getGraphController() {
        return this.graphController;
    }
}
